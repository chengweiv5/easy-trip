# Task 4 报告

## 完成内容

- `RouteLegUiState` 收敛为 `Ready / Calculating / WaitingForNetwork / Failed` 四态。
- domain `PENDING` 与 `CALCULATING` 统一映射为 UI `Calculating`。
- `Ready` 直接持有距离米数和时长秒数；交通方式继续由 `RouteLegUi.mode` 单一持有。
- 失败映射保持 typed `RouteErrorKind` 中文摘要优先、legacy `errorCode` 回退，最终默认文案为“路线计算失败”。
- 共享 `RouteLegContent` 统一单日可编辑和全程只读展示：
  - 仅 Ready 暴露交通方式编辑入口。
  - 仅 Failed 在提供回调时暴露当前 leg 重试入口。
  - Calculating 展示进度和“正在计算路线”。
  - WaitingForNetwork 展示网络图形和“等待联网”。
  - 长错误最多两行，无固定行高。
  - 字符 `↓` 替换为清除装饰语义的 Canvas 连接图形。
- 未修改 domain、Repository 或 generation。

## TDD 证据

- JVM RED：新增测试后编译失败，缺失 `RouteLegUiState.Calculating`，且旧 `Ready` 参数不符合新模型。
- JVM GREEN：focused mapper tests 通过。
- Compose RED：新增交互 API 测试后编译失败，缺少 nullable `onMode/onRetry`；等待联网图形语义测试随后按预期失败。
- Compose GREEN：focused 设备测试 27/27 通过。

## 验证

- `./gradlew testDebugUnitTest --tests 'com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModelTest' --tests 'com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryMapperTest'`：通过。
- `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryTimelineContentTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryEditingTest`：27/27 通过，设备 `easy_trip_p60pro(AVD) - 12`。
- `./gradlew lintDebug assembleDebug assembleDebugAndroidTest`：通过。
- `git diff --check`：通过。
- `graphify update .`：完成；工具报告两个既有 Kotlin 文件部分 AST 解析警告，并提示社区标签可另行刷新。

## 关注点

- 本批按项目门禁使用 Compose 设备测试实际交互验证，未进行物理真机视觉验收。
- graphify 更新改动 `graphify-out/` 五个跟踪文件；未触碰 `diagrams/`。
