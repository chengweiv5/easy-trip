# Task 1 报告：建立工作台视觉 token 与图标基础

## 修改文件

- `app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/EasyTripTokens.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceIcons.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/SearchSurface.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/MapControls.kt`
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceChromeTest.kt`
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/MapLayerFlowTest.kt`
- `graphify-out/.graphify_labels.json`
- `graphify-out/GRAPH_REPORT.md`
- `graphify-out/graph.html`
- `graphify-out/graph.json`
- `graphify-out/manifest.json`

## RED 证据

命令：

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceChromeTest
```

结果：测试成功安装并执行，失败于找不到 `contentDescription = "关闭图层菜单"` 的可点击节点。该失败符合预期，证明旧实现仍使用“关闭”占位文字且缺少目标图标语义。

## GREEN / 回归

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceChromeTest,com.yangchengwei.easytrip.workspace.MapLayerFlowTest
```

结果：`BUILD SUCCESSFUL`，共 5 个测试，0 failed。

```bash
./gradlew testDebugUnitTest compileDebugAndroidTestKotlin
```

结果：`BUILD SUCCESSFUL`。

```bash
graphify update .
```

结果：成功重建图谱，3882 nodes、8475 edges、195 communities。工具继续报告两个既有 Kotlin 文件存在部分 AST 解析警告：`NetworkMonitor.kt` 和 `RoutePlanner.kt`。

## 自审

- 精确加入 5 个工作台尺寸 token 和 2 个间距 token，未修改全局 `topBarHeight`。
- 提供简报要求的 7 个无业务逻辑图标 Composable，颜色全部来自 Material 3 语义颜色。
- 搜索 Surface 使用主题 surface、46dp 高度和搜索图标，保留原 test tag 与描述。
- 地图控件使用 40dp 触控区、22dp 图标、4dp 间距；菜单最大宽度 280dp；关闭图标具有“关闭图层菜单”描述；图层项使用 RadioButton selectable 与 selected 语义。
- 更新既有 `MapLayerFlowTest` 中已被新规范取代的 48dp 和 `✓` 文本断言，保留并加强 selected 语义覆盖。
- 未修改业务状态、导航、Repository、权限或地图生命周期；未触碰 `.superpowers/brainstorm/`。

## Commit SHA

提交后补充。

## 遗留关注点

- 未进行完整应用人工浏览器/真机视觉走查；本任务已在 `easy_trip_p60pro` 模拟器执行聚焦 Compose 测试。
- Graphify 对两个既有 Kotlin 文件仍有 AST 解析警告，与本任务修改无关。
