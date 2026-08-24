# Task 7 第一阶段报告：加入行程状态

## 范围

- 仅实现状态模型、独立 `AddToItineraryViewModel`、`SavedStateHandle` 草稿恢复和纯 JVM 测试。
- 未修改 `WorkspaceOverlay`、`TripWorkspaceContent`、Route/Screen，未连接 UI，未运行设备，未更新 graphify。

## 实现

- 全链路使用 `String` ID。
- 以 `List<String>` 作为选择顺序的唯一真相，派生 `selectedPlaceIdSet`；不把 `SavedPlaceRowUi.scheduled` 当作选择状态。
- 持久化选择顺序、`targetDayId`、当前编辑目标；不恢复 `isSubmitting`、`result`、undo token。
- 日期和地点快照 reconcile 时移除失效地点；目标日失效只清目标并进入重选日，保留仍有效选择。
- 提交期间使用同步 guard 防止重复提交和改选；请求使用进入提交时的有序快照。
- partial success 仅保留失败地点，并将准确 `createdItemIds` 作为一次性 undo token。
- `TargetDayMissing` 恢复领域返回的选择并清目标；按 Task 6 契约不暴露已由级联删除失效的 created IDs 为 undo。
- undo 仅传递 `createdItemIds` 给 `UndoAddedItemsUseCase`，不按 place ID 删除历史 occurrence；双击只执行一次。

## TDD 与验证

- RED：先创建 `AddToItineraryStateTest`；因状态模型、编辑目标、步骤和 ViewModel 均不存在而编译失败。
- GREEN：`./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStateTest"` 通过，共 7 个测试。
- `./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStateTest" compileDebugKotlin` 通过。

## Fix 1

- 提交期间冻结 start/toggle/select/reconcile 等草稿动作；`cancel` 明确取消 Job，并以 generation + request snapshot 阻断旧 completion 回写新流程。
- ViewModel 内维护有效 day/place ID 快照，入口事件直接拒绝无效 ID，不依赖第二阶段 UI 时序。
- submit/undo 互斥；新提交启动即消费旧 undo token，成功或 partial 以本次精确 created IDs 替换 token。
- `UndoAddedItemsUseCase` 现在返回 `UndoAddedItemsOutcome(deletedItemIds, remainingItemIds, failure)`。逐项删除遇到未知失败立即停止；已删除 ID 不再进入 token，只保留失败项及未尝试项供重试。该契约要求 repository 对仍在 token 中的 item ID 可重试；已成功删除 ID 不会重放。
- 新增回归覆盖：提交时 start/reconcile/toggle/select 冻结、cancel 后旧 completion 隔离、undo 中途失败精确保留与重试、submit/undo 互斥、无效 IDs、全失败无 token、新成功替换旧 token。
- 聚焦状态与 Task 6 undo JVM 测试、`compileDebugKotlin` 均通过。

## Fix 2

- 新增不持久化的 `validityInitialized`；从 `SavedStateHandle` 恢复的草稿在首次 `reconcile` 前不可提交。
- `canSubmit` 和 `submit` 同时要求 validity 已初始化、目标日仍有效、所有有序选择仍属于当前地点快照。
- 提交或撤销期间 `reconcile` 继续接收并保存最新 day/place 快照，但冻结草稿；异步 completion 后再按最新快照清理 selection/target，防止请求期间删除的实体被无效重试。
- 新增回归覆盖：恢复草稿首次 validity 快照前拒绝提交；请求期间 day/place 失效后，completion 应用最新 validity 并阻止无效重试。
- 最终验证：`AddToItineraryStateTest`、`UndoAddedItemsUseCaseTest` 与 `compileDebugKotlin` 同次执行通过。

## 后续阶段

第二阶段再把该 ViewModel 接到 Workspace owner、Overlay、Content 和 Compose 流程测试；本阶段没有触碰这些共享 UI 文件。
