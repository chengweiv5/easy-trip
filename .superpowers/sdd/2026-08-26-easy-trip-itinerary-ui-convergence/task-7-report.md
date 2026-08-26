# Task 7 报告

## 状态

最终候选自动化门禁通过；真机验收 DEFERRED。

## 验收记录

- 创建 `docs/testing/itinerary-ui-acceptance.md`，记录自动化结果、统一延期的真机项目和 carried review items。
- 创建 `docs/testing/itinerary-ui-conflicts.md`，记录 PENDING/CALCULATING 合并、双新增旅行日入口、Ready-only RouteLeg 编辑和 120dp 拖动步长裁决。
- 更新 SDD ledger `progress.md`。

## Selector 迁移

Catalog 首次运行 47 tests 中 1 failed：场景 14 仍断言旧 UI 文案“等待计算”和“计算中”。生产已按批准设计将 PENDING 与 CALCULATING 映射为统一的“正在计算路线”。

仅修改 `V1ScenarioExecutable.kt`：保留两个领域状态 fixture，以 `assertCountEquals(2)` 精确断言统一文案出现两次；没有删除状态、action 或错误断言，也没有弱化为模糊匹配。首次编辑误用了当前 Compose Test 版本不存在的 `assertExists`，编译失败后改用仓库已有的多节点数量断言；随后 Catalog 47/47 通过。

## 验证

- `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`：PASS；JVM 338 tests，0 failures，0 skipped；lint、app APK、test APK 成功。
- focused 行程套件：87 tests，0 failed，0 skipped，设备 `easy_trip_p60pro(AVD) - 12`。
- `V1ScenarioCatalogTest`：47/47，0 failed，0 skipped。
- `V1FullUiAcceptanceTest`：47/47，0 failed，0 skipped。
- 每次 connected 门禁前均确认无残留 connected/UTP/instrumentation 任务；设备串行使用。
- `graphify update .`：完成；`NetworkMonitor.kt` 与 `RoutePlanner.kt` 报告既有 AST 部分提取 warning。
- `git diff --check`：通过。

## 真机状态

DEFERRED。按用户决定统一延期，未记为 PASS。待验收：半屏信息密度、展开长列表、长地点名与长错误、handle/menu 误触、RouteLeg 四态、全程只读层级及 120dp 拖动手感。

## Whole-branch review 关注点

- zero elevation 测试未直接观测运行时 elevation。
- editable/read-only geometry 测试比较共享 primitive，未分别渲染完整行组件。
- `more` 仍是文本 glyph，缺少显式 20–22dp 图标尺寸。
- RouteLeg 长错误测试未直接证明没有 fixed height。
- RouteLeg 四态文本测试未全部限定 selector scope 到各自 leg tag。

## 范围

- 未触碰 `diagrams/`。
- 未 push。
