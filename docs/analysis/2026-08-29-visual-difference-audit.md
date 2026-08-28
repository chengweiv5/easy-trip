# 2026-08-29 Visual Difference Audit — Batch 0

## 结论

状态：`DONE_WITH_CONCERNS`。

已在 `emulator-5554`（`easy_trip_p60pro`）上以 390dp 等价宽度、默认 `fontScale=1.0` 串行运行 7 个受控 Compose 截图测试并实际读取 PNG。截图是 fixture 级组件/内容证据，不是含真实地图 SDK 的端到端工作台截图；因此地图、浮层和底部 sheet 宿主差异只能确认“缺失于本次 fixture”，不能据此断言生产路由必然缺失。

## 执行环境与证据

- 工作树：`/Users/bytedance/Code/easy-trip/.claude/worktrees/search-place-ui-convergence`
- Git SHA：`7cb1e24ec89c4a713f8a5c8cc758bacbc6e71711`
- 构建 source state：`DIRTY:430e78644bbfe9fce54ca1cb8b02d726cc475c1eab9cd03f1538984679e7addc`（本批新增 androidTest helper 与报告，故证据不能标为 clean-SHA 基线）。
- 设备：`emulator-5554`；API 31；440dpi；`Android SDK built for arm64`。
- 显示：1073 × 2321 px；390 × 772 dp；`fontScale=1.0`。
- 设计基线：`/tmp/easytrip-visual-baseline/pencil/`，Pencil canvas 390 × 844。
- Compose 证据：`/tmp/easytrip-visual-baseline/compose/`。该目录不纳入 Git；应用 external-files 中间副本同样不纳入 Git。

## 截图清单

| Pencil frame | Compose screenshot | Test / fixture | PNG SHA-256 |
|---|---|---|---|
| `U06l7P` 设置 | `/tmp/easytrip-visual-baseline/compose/settings-U06l7P.png` | `VisualBatch0EvidenceTest.settings_U06l7P` / `TripSettingsContent` | `06d9c5a29bac66522d65b9f8d37d91c674ac47c40efed6178fea8b2fcdfca320` |
| `IKTv5` 日期确认 | `/tmp/easytrip-visual-baseline/compose/date-confirmation-IKTv5.png` | `dateConfirmation_IKTv5` / `TripSettingsContent(AwaitingConfirmation)` | 见同名 manifest |
| `ofdn5` 搜索 | `/tmp/easytrip-visual-baseline/compose/search-ofdn5.png` | `search_ofdn5` / `PlaceSearchContent(Results)` | 见同名 manifest |
| `p4G1tS` 地点详情 | `/tmp/easytrip-visual-baseline/compose/place-detail-p4G1tS.png` | `placeDetail_p4G1tS` / `PlaceDetailPanel(Search)` | 见同名 manifest |
| `A9EKX` 地点池 | `/tmp/easytrip-visual-baseline/compose/place-pool-A9EKX.png` | `placePool_A9EKX` / `PlacePoolContent` | 见同名 manifest |
| `eHTX3` 单日行程 | `/tmp/easytrip-visual-baseline/compose/day-itinerary-eHTX3.png` | `dayItinerary_eHTX3` / `DayItineraryContent` | 见同名 manifest |
| `dzhkC` 创建 | `/tmp/easytrip-visual-baseline/compose/create-dzhkC.png` | `create_dzhkC` / `CreateTripContent` | 见同名 manifest |

每张 PNG 都有同目录同名 `.manifest.json`，记录 frame、test class/method、Git SHA/source state、设备 fingerprint/model/API、px/dp、density、font scale 与 SHA-256。

## 实测差异台账

| 区域 | 实际截图事实 | 设计对照与差异 | 维度 | 严重度 | 推荐批次 |
|---|---|---|---|---|---|
| 设置 `U06l7P` | 当前截图为标准 `CenterAlignedTopAppBar`，正文是“修改旅行名称”、ISO 日期、outlined 输入框、pill 和行尾“删除”；没有设计中的分组白卡、保存动作或危险操作卡。 | 与 Pencil 的“旅行设置”页面信息架构和承载结构显著不同。 | 结构、字段/按钮、间距、圆角/阴影 | 高 | Batch 1：设置页 chrome 与卡片 |
| 日期确认 `IKTv5` | 当前确认层具有遮罩、影响计数和取消/确认，但截图中 dialog 与背景同时可见且文本/控件密集重叠，确认区域可读性受损。 | Pencil 使用更紧凑的居中白色圆角卡、红色 warning panel 和清晰的底部双按钮；当前为功能/可读性问题，不是微小视觉差异。 | 结构、动态层级、按钮、间距 | 阻断级 | Batch 1：确认对话框 |
| 搜索 `ofdn5` | 当前结果页已是独立全页：圆角搜索框、结果数 pill、白色结果 surface 和行内书签按钮。 | 结构接近设计；但实际仅 4 项、统一 pin 图标、行高/字体显著更大，且缺少设计中的距离和不同 POI 图标。 | 字段、色彩/字体、间距 | 中 | Batch 2：搜索结果细节 |
| 地点详情 `p4G1tS` | fixture 截图仅为白底详情内容，标题/地址/备注/标签/两按钮后剩余大量空白。 | Pencil 有地图、地图控制和带圆角/阴影的下半屏详情 sheet；本 fixture 没有宿主层。组件内容本身也缺少图标块与卡片分组。 | 结构、圆角/阴影、动态层级 | 高 | Batch 2：详情 overlay + workspace host |
| 地点池 `A9EKX` | 当前 fixture 显示“添加到行程”、tag 文本和地点行，但没有地图、tab、sheet 或 marker；地点行信息密度高且重复备注。 | 设计强调地图+底部 sheet、双 tab、只收藏/已排入图例及快速加号；本次证据确认内容层与设计完整构图差异大。 | 结构、字段/按钮、间距 | 高 | Batch 2：地点池工作台合成 |
| 单日行程 `eHTX3` | 3 个行程项和 2 条路线均渲染，顺序和路线文本可见；截图整体颜色极浅、对比度偏低。 | 设计有 map/tab/日期 rail、彩色路线 chip 和更强层级；当前 fixture 仅 timeline，且可读性/对比度需复核。 | 结构、色彩/字体、动态层级 | 高 | Batch 3：行程工作台 |
| 创建 `dzhkC` | 标题、步骤条、名称、日期范围、天数、出行方式、提示和主按钮都渲染；页面结构可达。 | 与设计相比，日期呈“日期待定 + pill”而非 66dp 日期卡，出行方式为普通文本/pill 而不是两张卡，主按钮文案为“继续”而不是“创建旅行”。这是实质性字段/层级差异。 | 结构、字段/按钮、间距 | 高 | Batch 4：创建页收敛 |

## 共享视觉观察

- 设计 token 为背景 `#F5F3EE`、surface `#FFFFFF`、primary `#2D5E3A`、文字主色 `#1B3A28`、边框 `#D6DDD0`，见 `docs/design/easy-trip-v1-pencil-reference.md:29-43`。
- 实际截图总体使用同一绿/米白色系，这是事实；但设置、单日行程和地点池中浅色文案的对比度明显不足，是应优先复核的共享 token/disabled-state 风险。
- 设计关键尺寸为 20dp 页面边距、48dp 按钮、24dp 圆角、52dp 名称字段、66dp 日期行，见同文档 `78-97`。创建页已有高度 instrumentation 约束，但实际视觉分组仍未收敛。

## 证据实现

- 新增受控截图 suite：`app/src/androidTest/java/com/yangchengwei/easytrip/VisualBatch0EvidenceTest.kt`。
- 每个测试在 `EasyTripTheme` 下直接渲染固定 fixture，以 `UiAutomation.takeScreenshot()` 保存 PNG，并写入 manifest。它断言窗口宽为 390dp，防止错误设备尺寸被作为基线。
- 现有 helper `PlaceSearchEvidenceTest` 仍保留其 clean-source 约束；本新 suite 将 dirty source state 显式写入 manifest，不伪造 clean 状态。

## 验证

- `./gradlew :app:compileDebugAndroidTestKotlin`：通过。
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.VisualBatch0EvidenceTest`：通过，7/7。
- 保留应用安装后的串行 `am instrument`：通过，7/7；用于使 external-files 证据可复制至 `/tmp`。
- `git diff --check`：待本次报告更新及 graphify 更新后执行。

## 地图失败路径复核

- `AmapComposeMap` 的 creation、lifecycle（`onResume`）和 render 失败现各自有 exactly-once 计数断言；旧实现中 lifecycle/render 会直接调用 `onMapError`，再由 `mapFailureState` 的 `LaunchedEffect` 再调一次，实测计数为 3。
- 修复后 creation、lifecycle 与 render 都只写 attempt 级 failure state；单一 `LaunchedEffect` 通过 guard 消费并发布一次 `onMapError`，状态同时驱动 `SearchMapDetail` 恢复 UI。`retryKey` 会创建新 guard，因此下一 attempt 可独立上报一次新错误。已复跑 `AmapComposeMapTest` 14/14。
- disposal error 保持既有独立契约：仅在尚未有 terminal failure 时报告，避免把清理失败与 creation/lifecycle/render 的同一次失败合并为第二次通知。

## Concerns

1. 当前证据是在 dirty source state 生成，适合差异审计，不可冒充某个 clean commit 的长期黄金基线。
2. 日期确认截图出现严重层叠/低可读性，建议作为 Batch 1 的功能性视觉阻断项。
3. 工作台地图/真实 bottom sheet 宿主未在此 fixture suite 中运行；下一批应使用同一测试数据的工作台整屏状态补证。
4. 地图 failure 测试为 instrumentation fake host；真实 AMap SDK 的异步 creation/lifecycle 并发回调仍需最终真机验收。
