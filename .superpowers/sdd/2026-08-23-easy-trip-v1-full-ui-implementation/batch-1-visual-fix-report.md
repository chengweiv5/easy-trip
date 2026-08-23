# Batch 1 visual fix report

## Frame 差异与修复

- `zIbEu`：补齐 69dp 页头、副标题、48dp 个人入口、56dp 行李语义插画；空态下移并将 CTA 固定为居中 220×48dp。
- `K9h3r`：主卡改为 20dp 圆角，元信息横排，设置/删除操作并排；其他旅行保持 12dp 卡片，创建入口收窄至 138dp 并靠右。
- `dzhkC`：保留独立创建页及安全区/IME 滚动；空天数与占位方式保持自然输入，CTA 调整为 52dp。
- `yIGiQ`：保留真实 ViewModel 字段错误、错误语义及底部修正提示，禁用/提交状态不改变。
- `oW9mK`：替换默认 AlertDialog 为 334dp、18dp 圆角的定制确认层，加入 52dp 警示区、影响摘要区、并排 44dp 操作和危险色；删除/保留/不可撤销及失败重试语义均保留。
- `d1sTtb`：列表仍由 Room Flow 驱动；删除后无旅行时回到与 `zIbEu` 相同空态，有剩余旅行时展示内容列表。

## RED / GREEN

- RED：`./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripListContentTest,com.yangchengwei.easytrip.trip.ui.CreateTripContentTest`
  - 预期失败：缺页头副标题、个人入口、插画；创建按钮非 220dp。
- GREEN：同一命令，两个 AVD 共 24 个测试通过。
- JVM/lint/build：`./gradlew testDebugUnitTest lintDebug assembleDebug` 通过。
- Flow/Room：`./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1PencilFlowTest,com.yangchengwei.easytrip.trip.ui.TripFlowTest,com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProviderTest,com.yangchengwei.easytrip.CascadeDeleteTest`，两个 AVD 共 16 个测试通过。
- 全量 instrumentation：一个 AVD 全部通过；另一 AVD 仅 `TripFlowTest.createAndSettingsChoicesUseExclusiveSelectablePills` 在全量并发运行时找不到列表设置节点，单独纳入上述 Flow/Room 命令时通过，判断为设备侧时序波动。

## 修改文件

- `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListContent.kt`
- `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripContent.kt`
- `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/ConfirmationDialog.kt`
- `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/EasyTripButton.kt`
- `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripListContentTest.kt`
- `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/CreateTripContentTest.kt`
- `app/src/androidTest/java/com/yangchengwei/easytrip/V1PencilFlowTest.kt`

## Commit

首轮 agent commit：`e5b46cd846bcb69a3119c8784adff38012433117`。

## Fix round 1

- TripList RED：新增 Pencil eyebrow、Profile 点击、72dp 其他旅行行及 280dp/2× 字体元信息边界断言；测试分别因缺少 `onProfile` 接口、旧卡片结构及缺少 metadata 节点失败。GREEN 后页头改用“周末，去远一点”，Profile 经 Route/Content 显式回调，空态使用纯 Compose Canvas/Path 行李插画，其他旅行采用 72dp 紧凑行，主卡元信息改为纵向响应式布局。
- CreateTrip RED：新增 58dp 页头、22dp 步骤、52dp 名称字段、66dp 日期区、82dp 出行方式、40dp 提示和 52dp CTA 结构断言，首先因结构标签缺失失败；GREEN 后按 frame 尺寸实现，并把字段错误文字移出固定高度输入框，保留滚动、IME 与原状态机。
- ConfirmationDialog RED：新增 280dp、2× 字体及完整影响列表测试，按钮因固定 334dp/整层滚动不可见而失败；GREEN 后改为安全水平边距、334dp 最大宽度、640dp 最大高度、独立滚动内容和固定底部操作。
- 天数测试 ruling：`CreateTripViewModel` 原实现已经满足初始空值、输入 `3` 及 SavedState 缺少 days 时恢复空值，因此新增真实 ViewModel regression coverage 后直接通过；不计作生产 bug 修复。
- 数据模型 ruling：`TripCardUiModel` 只有名称、天数、日期和出行方式，当前 domain `TripSummary` 不提供地点数、行程数、进度或倒计时；未伪造 Pencil 示例数据，仅用真实字段复现主次层级。

## Fix round 1 最终验证

- JVM、lint、构建：`./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`，BUILD SUCCESSFUL。
- Compose、Flow、Room：目标 7 个测试类共 22 个测试，在 `easy_trip_p60pro` 与 `trail_map_api36` 分别全部通过，共 44 次通过。
- `trail_map_api36` 首次发现校验错误文字存在但位于滚动视口外；测试改为对真实错误节点执行 `performScrollTo()` 后断言可见，随后单测及完整目标组在两个 AVD 均通过。生产滚动布局未因测试时序调整。
- `git diff --check` 通过。
- 已运行 `graphify update .`；Graphify 生成物保持在工作区，不纳入提交。
- Android UI 已由 instrumentation 执行创建、校验、Profile、删除确认与 Room/Flow 交互；未在浏览器验证（本批为原生 Android Compose UI）。

## 未解决关注点

- Profile 在本批没有目标页面，Route/Content 已显式暴露回调，导航层当前采用无副作用默认实现。
- 主卡未展示当前模型不提供的地点数、行程数、准备度或倒计时，避免伪造数据。
- 空态插画使用无外部资源的语义化 Compose Canvas/Path，尺寸、层级和可访问性与 frame 对齐，但并非设计稿中的逐路径矢量。

## Fix round 1 commit

本轮提交范围：`e5b46cd..HEAD`；最终 SHA 由提交完成后的 Git 输出确认。
