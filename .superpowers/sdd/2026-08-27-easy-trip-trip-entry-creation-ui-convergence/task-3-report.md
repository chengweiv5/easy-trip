# Task 3 Report

## 状态

完成旅行删除确认的 busy 锁定、失败恢复与真实菜单入口接线，复用 Task 1 的 `TripDeletionUiState`，未新增平行删除状态。

## 实现

- `ConfirmationDialog` 新增默认值为 `false` 的 `busy` 参数，保持既有非 busy 调用语义。
- busy 时禁用确认/取消按钮，禁止 Back 和点击外部关闭，并显示“处理中…”与进度指示。
- 删除失败时保留完整影响摘要、显示错误并允许再次确认。
- `TripListScreen` 按 `LoadingImpact`、`ImpactFailure`、`Ready` 渲染删除流程：查询中保留旅行名且不伪造数字；查询失败显示“未删除旅行”、错误、取消与重试；Ready/Deleting/DeleteFailure 共用同一精确确认模型。
- 真实删除流程测试改从旅行卡片 `···` 菜单进入，并验证精确影响与只删除一次。
- 检查全部 `ConfirmationDialog` 调用点，仅迁移本次参数更名所需的 workspace 调用。

## TDD

RED 已确认：

- `ConfirmationDialogTest` 因缺少 `busy` 参数编译失败。
- impact failure 新断言因缺少“未删除旅行”提示而失败。

GREEN 已确认：

- `ConfirmationDialogTest`：3 tests passed。
- `TripFlowTest`：5 tests passed。
- `TripListViewModelTest`：passed。
- `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`：passed。
- `graphify update .`：完成。
- `git diff --check`：通过。

## Fix round 1

- 删除 service 成功后继续保留 `Ready(isDeleting = true)`；仅当列表实际消费到不含对应 `tripId` 的 Flow emission 后关闭确认框。
- 等待标记绑定删除 generation 与目标 `tripId`，旧 emission、仍含目标的 emission 或其他 target 不会误关闭。
- 增加可控 Flow 单测，覆盖 service 已成功但目标仍在列表、随后目标消失、以及旧/无关 emission。
- outside-dismiss 测试改为对独立 Dialog root 遮罩坐标注入真实触摸，不再调用 `rootView.performClick()`。
- busy 进度指示增加稳定测试标签，并显式断言 `ProgressBarRangeInfo.Indeterminate` 语义。

验证：`ConfirmationDialogTest` 3/3、`TripFlowTest` 5/5、`TripListViewModelTest` 9/9 通过；主代码与 AndroidTest 编译通过。

## Fix round 2

- 删除完成判定不再读取会被 Loading/Error 人为清空的 UI `trips` 快照；仅在 `observeTrips()` 成功 collect 分支记录 emission 版本与真实 trip IDs。
- service 成功后绑定 generation、目标 `tripId` 与当时成功 emission 版本，必须等待后续真实 emission 且其中不含目标才进入 `Idle`。
- 增加竞态测试，覆盖重新订阅或 Flow Error 清空 UI 后 service 才成功，以及确认前最后一次成功 emission 已不含目标的边界；两项旧实现均按预期 RED。

验证：`TripListViewModelTest` 11/11 通过。

## Fix round 3

- 将重新订阅竞态测试细分为三个可观察阶段：新 Flow 首次 emission 前保持 `Loading`，推进后进入 `Error`，最后成功发出不含目标的列表。
- 在 Loading 与 Error 阶段分别确认删除状态仍为绑定目标的 `Ready(isDeleting = true)`；仅最后的成功 emission 关闭确认框。
- 生产实现无需修改。

验证：`TripListViewModelTest` 11/11 通过；主代码与单元测试代码编译通过。

## 关注点

- `graphify update .` 报告两个既有文件存在语法提取警告：`NetworkMonitor.kt`、`RoutePlanner.kt`；不影响 Kotlin 编译与本任务测试。
- 仪器测试运行于 `easy_trip_p60pro(AVD) - 12`，覆盖了本任务真实 UI 交互。
