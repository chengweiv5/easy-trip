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

提交后回填：见本次提交 SHA。

## 未解决关注点

- 全量 instrumentation 在 `trail_map_api36` 有一次已有流程测试时序失败；目标 Flow/Room 组与另一 AVD 全量均通过。
- 空态插画使用无外部资源的语义化 Compose 图形占位，尺寸、层级和可访问性与 frame 对齐，但并非设计稿中的逐路径矢量。
