# 旅行入口与创建 UI 候选验收

日期：2026-08-27

## 环境

- 自动化设备：`easy_trip_p60pro(AVD) - 12`，ADB 型号 `Android_SDK_built_for_arm64`。
- 门禁执行方式：确认无其他 connected instrumentation 任务后严格串行执行。
- 物理设备验收：DEFERRED（Mate 60 Pro 未执行）。

## TDD 证据

| 阶段 | 命令 | 结果 |
|---|---|---|
| RED | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1ScenarioCatalogTest` | 47 tests，2 failures；场景 01 仍点击旅行名、场景 36 仍断言旧空态文案 |
| GREEN | 同上 | 47 tests，0 failures |

首次 RED 编译尝试因测试代码导入/匹配器错误失败，不计为有效 RED；修正测试自身后，以上行为失败证明 catalog 能捕获旧 selector 与旧文案。

## 最终门禁

| 命令 | 测试数 | 失败数 | 结果 |
|---|---:|---:|---|
| `./gradlew :app:testDebugUnitTest` | 419 | 0 | PASS |
| `./gradlew :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` | N/A（构建门禁） | 0 | PASS |
| `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripListContentTest` | 7 | 0 | PASS |
| `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.CreateTripContentTest` | 12 | 0 | PASS |
| `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1PencilFlowTest` | 2 | 0 | PASS |
| `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1ScenarioCatalogTest` | 47 | 0 | PASS |
| `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1FullUiAcceptanceTest` | 47 | 0 | PASS |

没有出现 0 tests、超时、signal 9 或 instrumentation runner crash；未重启 AVD。

## 候选结论

场景 `01/01v/07/09/13/36/47` 已绑定当前生产 selector 与文案。自动化门禁覆盖唯一继续入口、稳定 ID 菜单删除及删除后列表状态、创建字段与提交、由 production state 渲染的开始/结束日期摘要、精确删除影响、新空态文案、字段容器内的对应错误，以及头像纯装饰约束。

## Fix round 1

- 场景 09 通过可控 production `CreateTripUiState` 回灌日期交互结果，直接断言 UI 显示 `2026-08-31` 开始日期与 inclusive `2026-09-02` 结束日期。
- 场景 47 复用 focused Content 测试中的字段容器祖先匹配，错误若移出对应 `*-container` 即失败。
- 场景 13 在确认删除后更新可控列表 state，覆盖 01v：目标卡消失且“东京秋日”保留旅行仍在。
- RED：Catalog 47 tests，1 failure（目标卡删除后仍存在）；GREEN：Catalog 47 tests，0 failures。

## Task 3 final fix

- 删除确认现覆盖 service/Flow 非协作时序：service 挂起期间成功 emission 已移除目标时仍保持 busy，service 返回后立即关闭；确认前的旧 emission 不生效。
- 等待删除确认期间列表 collector Error 自动重订阅一次，恢复后由目标缺失的成功 emission 关闭，删除 service 不会重复执行。
- 删除影响查询中显示不确定进度，并锁定取消、Back 与外部点击；查询失败后仍可取消或重试。
- 验证：`TripListViewModelTest` 13/13、`ConfirmationDialogTest` 3/3、`TripFlowTest` 5/5、`V1PencilFlowTest` 2/2 PASS。

## 新授权聚焦修复：删除确认同步恢复

- 自动 retry(1) 连续两次失败后，确认弹窗退出 busy，保留原 trip ID 与删除影响，显示“删除成功，但同步确认失败，请重新同步”。
- “重新同步”只重订阅 `observeTrips()`；删除 service 调用次数保持 1。同步中重复点击、取消、Back 与外部点击均被锁定；再次失败可继续重试，成功 emission 确认目标消失后回到 Idle。
- RED：`TripListViewModelTest` 14 tests，1 failure，失败点为连续两次 collector failure 后 `isDeleting` 仍为 true。
- UI RED：`compileDebugAndroidTestKotlin` 因 `ConfirmationDialog` 不支持 `confirmLabel` 覆盖而失败，证明缺少可达“重新同步”入口。
- GREEN：`TripListViewModelTest` 17/17、`ConfirmationDialogTest` 4/4、`TripFlowTest` 6/6 PASS。
- 顺序竞态 RED：confirm → Flow 连续两次失败耗尽 → service 成功后仍 `isDeleting=true`；修复后立即显示 SyncFailure，manual resync 的目标缺失 emission 回到 Idle，delete service 调用始终为 1。
- 滞后动作 RED：SyncFailure 下直接派发 `ConfirmDelete` 会重复删除；增加状态 guard 后该动作保持状态不变、deleteCalls 为 1，`RetryDeletionSync` 仍可完成恢复。GREEN：`TripListViewModelTest` 18/18。
