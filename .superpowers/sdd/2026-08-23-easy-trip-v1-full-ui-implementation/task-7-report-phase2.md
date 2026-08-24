# Task 7 Phase 2 Report

## 状态

完成可编译 checkpoint：新增地点多选与目标日选择 Content，并接入地点池入口、Workspace overlay 和 `AddToItineraryViewModel`。

## 实现

- 新增 `SelectPlacesContent`：地点池多选、scheduled 文案与 selected 状态分离、有序选择计数、空选择禁用继续、提交期间锁定操作。
- 新增 `SelectTargetDayContent`：独立滚动日期列表、固定底部 CTA、空日期状态、提交/撤销锁定、目标日缺失和部分成功反馈。
- Fix 1：partial success 进入结果 overlay，同时呈现成功/失败、精确 Undo 与失败地点重试；重试成功会累加此前创建项的 undo token。
- Fix 1：系统 Back、顶部 Back 和关闭按钮统一走 add-flow close；提交/撤销中禁止关闭，其余关闭清理 draft/result/token。
- Fix 1：日行程区域增加“从地点池添加”入口，使用 `startForDay` 预设目标日。
- 地点池新增“添加到行程”入口 action。
- 将加入流程 overlay 与跨天移动 overlay 拆分，加入流程不再使用 Long/hash payload。
- Navigation 创建 entry-scoped `AddToItineraryViewModel`，Route 负责 reconcile、step/overlay 映射和 action 转发。
- close/back 会取消进行中的加入草稿，并保留原有 PlacePool/Itinerary dialog 清理。

## TDD

先扩展 `PlacePoolFlowTest`，首次运行 `compileDebugAndroidTestKotlin` 因两个 Content 尚不存在而失败，确认 RED。随后完成最小生产实现并重新编译通过。

覆盖新增断言：

- scheduled 地点仍可选择。
- scheduled 与 selected 可同时成立。
- 空选择 Continue disabled。
- 有选择 Continue enabled。
- submitting 时 Submit disabled。
- `TargetDayMissing` 保留选择并要求重选。
- Content 测试仅验证点击 callback；真实 ViewModel JVM 测试验证按序更新 selected、Continue enable 与步骤推进。
- 280dp / 2x 字体下断言长日期末项初始未组合、滚动后可见且 CTA 始终固定可达（仅编译，未运行设备测试）。
- JVM 覆盖真实 ViewModel 的有序选择与 Continue 状态推进；Success/Partial 通过统一 helper 按 item ID 保序去重合并 undo token。
- Undo token 按目标日建模为批次；Success/Partial 将 created IDs 保序去重合并到对应日期。TargetDayMissing 的 created IDs 仅作审计，并只移除缺失日期对应批次，其他仍存在日期的 Undo token 保留。单个 Undo 会处理所有有效批次，失败后仅保留尚未删除的 IDs，重试不会重复删除已成功项。
- TargetDayMissing 保留完整地点选择并清空目标日；结果 UI 会区分“无可撤销项”和“其他日期仍有可撤销项”。
- Route 使用统一可单测 dismiss 策略：仅 add overlay 在提交/撤销期间锁定；异步替换出的 Feedback/Confirmation 等无关 overlay 可关闭且不会清 add draft/result/token。
- Undo 成功保留 COMPLETED 结果反馈和 overlay；用户明确关闭后由 `cancel()` 清空 draft/result/token，不自动关闭 overlay。

有序提交、partial success、精确 undo 继续由 phase 1 JVM 状态测试覆盖。

## 验证

- `./gradlew compileDebugKotlin compileDebugAndroidTestKotlin`：通过。
- `./gradlew testDebugUnitTest --tests 'com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStateTest' --tests 'com.yangchengwei.easytrip.itinerary.domain.UndoAddedItemsUseCaseTest'`：通过。
- 未运行 connected/device tests。
- 未做设备或浏览器交互验证。

## 未覆盖风险

- Compose 测试仅完成编译，未在设备运行，因此长列表实际滚动和 overlay 交互尚未做运行时验证。
- 成功/部分成功反馈已提供精确 undo 入口；未补充该 overlay 的独立 Compose 运行时测试，精确 undo 领域与状态契约由 JVM 测试覆盖。
