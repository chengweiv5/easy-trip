# Batch 2 地点池视觉修复报告

## 状态

**DONE_WITH_CONCERNS**

本轮仅修复地点池 sheet 的高度/位置、列表滚动与底部避让、顶部安全区，以及普通地点池与搜索返回态的视觉区别。未修改触控策略、LayerMenu 遮罩/重叠或详情语义。

## Diff 审计

接管时 HEAD 为 `6b91132`，有 10 个源码/测试文件、约 `143 insertions / 14 deletions` 的未提交修改。

审计结论：

- `PlacePoolSheet` 的 `LazyColumn.weight(1f)` 与 24dp bottom content padding 是必要且最小的滚动/safe-drawing 修复。
- `TripWorkspaceContent` 的 396dp（普通）与 412dp（搜索返回）固定高度直接对应唯一基线 `design/easy-trip-v1.0.pen`：A9EKX sheet 为 y=448、高 396；I62qd5 sheet 为 y=432、高 412。删除了按设备高度向上缩放的实现，因为它会偏离设计基线。
- `WindowInsets.safeDrawing` 应用于 scaffold，生产 XML 的顶部控件 bounds 从 y=115 开始，底部内容未越过导航栏。
- 搜索返回态必须跨 Navigation 返回边界传递；`SavedStateHandle` 是现有导航结构下的最小可靠通道。但不应把短生命周期返回事件塞进持久 `TripWorkspaceViewModel` 状态。本轮简化为单个 `WorkspaceSearchReturn(recentlyCollectedPoiIds)`，只沿 `AppNavigation → TripWorkspaceRoute → TripWorkspaceScreen → TripWorkspaceContent` 传递。
- `PlaceSearchViewModel` 原先用“当前集合减初始快照”推导本次收藏，存在首个 Room emission 尚未到达时把历史收藏误判为新收藏的竞态。本轮改为仅在本搜索会话成功 Save/Remove 时维护集合。
- `SavedPlaceRowUi.recentlyCollected` 保留为默认 false 的纯渲染字段，正常地点池显示“仅收藏”，搜索返回态显示“刚刚收藏 · 待安排行程”。

## RED / GREEN

接管前新增测试存在，但未找到先前 Agent 保存的真实 RED 命令输出或报告，因此不伪造；该事实明确记为：**先前 Agent 未记录视觉修复 RED 证据**。

接管后聚焦测试首次运行 7 项全通过。简化实现后复测暴露测试自身两个错误假设：

- 将 396dp 设计高度误写成随测试设备 952dp root 保持比例；
- 用固定两次 swipe 假设第 5 卡必然进入语义树。

这次失败不是原视觉实现的 RED。测试改为直接断言 396dp，并用 `performScrollToNode` 验证真实 LazyColumn 可滚动。最终 `TripWorkspaceContentTest` 7/7 通过。

## 设计与设备证据

Pencil v1.0 节点：

- `A9EKX`：390×844；sheet y=448，高 396。
- `I62qd5`：390×844；sheet y=432，高 412。
- `lsr1I`：390×844；空状态 sheet y=398，高 384。当前实现保留统一普通池 396dp，接近该基线且不引入额外空态分支。

设备：`trail_map_api36`，API 36，Emulator 进程参数包含 `-gpu swiftshader_indirect`。APK 通过 `adb install -r` 安装成功。

证据目录：`/tmp/easy-trip-batch2-visual-fix/`

- `A9EKX-place-pool.xml/.png/.jpg`：重新进入工作台后的普通地点池；两项均显示“仅收藏”。
- `I62qd5-search-return.xml/.png/.jpg`：同一搜索会话连续收藏两项并主动返回；两项均显示“刚刚收藏 · 待安排行程”。
- `lsr1I-empty-place-pool.xml/.png/.jpg`：新建旅行的空地点池，含“还没有收藏地点”和“搜索地点”。

三张 JPEG 均为 573×1280。按限制只读取了 A9EKX 与 I62qd5 两张。

XML 断言：

- 顶部栏控件 bounds 均从 y=115 起，避开状态栏。
- 普通态首卡 y=1897，搜索返回态首卡 y=1849；后者上移约 48px，与 412/396dp 相对差异一致。
- 普通态状态文案为“仅收藏”，返回态为“刚刚收藏 · 待安排行程”。
- 第二卡普通态 bottom=2435、返回态 bottom=2387，均位于 2700px 根底部和系统导航区之上。
- UIAutomator 未把 Compose `LazyColumn` 导出为 `scrollable=true`；可滚动性由生产列表语义的 `performScrollToNode` instrumentation 用例验证，不能伪称 XML 自身提供该标志。

## 验证

- `TripWorkspaceContentTest`：7/7 PASS。
- Batch 2 目标 suite：22/22 PASS（`PlaceSearchContentTest`、`PlacePoolFlowTest`、`MapLayerFlowTest`、`RoomSavedPlaceRepositoryTest`）。
- `./gradlew testDebugUnitTest lintDebug assembleDebug`：BUILD SUCCESSFUL。

## Concerns

- Gate 仍为 **in progress**：真实网络失败和加载中状态尚未稳定采集，未伪造生产状态。
- 物理设备验收仍待用户授权；本轮证据来自 API 36 AVD。
- XML 对 Compose LazyColumn 没有输出 `scrollable=true`；滚动能力由 instrumentation 直接滚动到离屏节点证明。

---

## Fix round 1（2026-08-24）

### Findings 修复

1. `searchReturnPoiIds` 改为 exactly-once 事件：搜索页通过统一回调发布；workspace 在 `LaunchedEffect` 中从当前 back stack entry 的 `SavedStateHandle` 读取并立即写 `null` ack，之后只保留本次组合生命周期内的瞬时 UI 状态。空收藏返回写 `null`，不会进入 412dp 状态。进入下一次搜索、进入设置或切换 workspace tab 时清除瞬时状态，恢复普通 396dp；进程恢复不会重放已 ack 事件。
2. sheet 改为约束感知：v1.0 基线高度仍为普通 396dp、搜索返回 412dp；实际高度为 `minOf(desired, maxHeight)`，不提前实现 Task 11 三档 sheet。280dp 高、2×字体下列表仍能滚到最后一项。
3. 新增最小 Navigation 协调器测试，直接使用与 `AppNavigation` 相同的 `publishWorkspaceSearchReturn` / `consumeWorkspaceSearchReturn`，覆盖旧事件不重复、空返回普通态、再次搜索只返回新 payload。`PlaceSearchRoute` 的页面返回和系统 Back 均走同一个 `onBack` 回调，并各有 instrumentation 覆盖。
4. 测试增强：普通/返回高度分别约 396/412dp；safe inset 改为相对 root 检查并限制不出现双 padding；滚动到最后一项验证底部 24dp；覆盖 280dp 高与 2×字体。

### RED 记录

- `WorkspaceSearchReturnNavigationTest` 首次编译失败：`publishWorkspaceSearchReturn`、`consumeWorkspaceSearchReturn`、`WORKSPACE_SEARCH_RETURN_KEY` 均不存在。
- 280dp/2×字体用例在旧固定 396dp 实现下失败；修复前首次运行同时被生产 helper 尚未实现的编译 RED 阻断，随后单独执行确认旧固定高度链路不满足新约束。
- 系统 Back 测试在修复前失败并抛出 `NoActivityResumedException: Pressed back and killed the app`，证明 `PlaceSearchRoute` 未消费系统返回。

### GREEN

- `WorkspaceSearchReturnNavigationTest`：3/3 PASS。
- `TripWorkspaceContentTest` + `PlaceSearchContentTest`：16/16 PASS，包含页面返回、系统返回、小窗大字体及最后项滚动。
- Batch 2 目标 suite 因新增 2 个 `PlaceSearchContentTest` 用例现为 24/24 PASS。首次合并运行出现 `PlacePoolFlowTest.searchSaveEditAndFilterThroughPlacePool` 单次 5 秒超时；该用例隔离重跑 PASS，完整 24 项随即重跑 PASS，未发现生产回归，未为偶发环境时序改代码。
- `testDebugUnitTest lintDebug assembleDebug`：BUILD SUCCESSFUL。
- Fix round 1 设备证据：`/tmp/easy-trip-batch2-visual-fix-round1/01-production.xml/.jpg` 与 `02-small-window.xml/.jpg`；后者使用临时 `wm size 900x600` 后已 reset。

---

## Fix round 2（2026-08-24）

- 新增 NavGraph 生命周期内的 `WorkspaceSearchReturnTransientState`：由 `AppNavigation` 的 `remember` 持有，配置变化重组保留；新建 host/进程得到空实例，不重放已 ack 的 `SavedStateHandle` payload。
- payload 仍先由 previous back stack entry 发布，workspace collect 后立即 ack；随后展示来自非持久 transient state。
- 只有实际从当前 section 切到不同 section 才清除；重复点击当前地点池不清。进入新搜索或设置仍清除。
- workspace 根布局显式叠加 `imePadding()`，`BoxWithConstraints.maxHeight` 因 IME 可用高度收缩；safe drawing 仍只在同一根容器应用一次。
- RED：`WorkspaceSearchReturnTransientState` 与 `shouldConsumeSearchReturn` 缺失导致 `WorkspaceSearchReturnNavigationTest` 编译失败。
- GREEN：`WorkspaceSearchReturnNavigationTest` 与 `compileDebugAndroidTestKotlin` 均成功。按本轮限制未操作共享模拟器，仅执行聚焦 JVM/Compose 编译检查。
