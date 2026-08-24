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
