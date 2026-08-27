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

场景 `01/01v/07/09/13/36/47` 已绑定当前生产 selector 与文案。自动化门禁覆盖唯一继续入口、稳定 ID 菜单删除、创建字段与提交、开始/结束日期语义、精确删除影响、新空态文案、字段附近错误，以及头像纯装饰约束。
