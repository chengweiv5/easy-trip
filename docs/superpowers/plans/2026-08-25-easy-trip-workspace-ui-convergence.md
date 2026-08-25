# Easy Trip 工作台 UI 收敛实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变业务状态机、导航、数据和权限语义的前提下，将工作台地图、顶部控件、搜索入口、地图控件、普通 Tab、三档 BottomSheet、地点池和行程容器收敛到已确认的方案 B。

**Architecture:** 新增纯布局指标与 `WorkspaceScaffold`，由它统一计算 Sheet 锚点和地图浮层避让边界；TopBar、SearchBar、MapControls、MapLegend 和 Tabs 只消费该边界与主题 token。地点池和行程继续拥有各自业务组件，只移除重复外边距并补齐宽度约束，不改 ViewModel、Repository、导航 reducer 或异步 generation 逻辑。

**Tech Stack:** Kotlin、Jetpack Compose、Material 3、Navigation Compose、JUnit 4、Compose UI Test、高德 Android SDK。

**Spec:** `docs/superpowers/specs/2026-08-25-easy-trip-workspace-ui-convergence-design.md`

## Global Constraints

- 视觉基线采用 Easy Trip Forest Sage：背景 `#F5F3EE`、Surface `#FFFFFF`、Soft Surface `#E7EFE2`、Primary `#2D5E3A`、Primary Dark `#1B3A28`、Secondary `#4A6B52`、Muted `#6E7F72`、Border `#D6DDD0`、Accent `#D96F3B`、Danger `#BA1A1A`。
- 标题使用 Funnel Sans，正文和控件使用 Inter；页面不得复制品牌色形成第二套色板。
- 保留 `COLLAPSED / HALF / EXPANDED` 三档、默认 `HALF`、一步一档转换、SavedState 恢复及现有返回优先级。
- 工作台搜索仍是只读入口；搜索收藏后不自动返回、不自动加入行程，返回工作台后保持最近收藏高亮。
- 地点池是加入行程的唯一入口；全程行程继续只读。
- 不修改 Room schema、Repository、地图生命周期、权限协调和异步路线 generation 逻辑。
- 图标视觉尺寸 20–22dp；密集地图控件触控区 40dp；搜索、返回、更多等主要操作触控区 44dp；独立主按钮和危险操作 48dp；相邻触控区间距至少 4dp。
- Mate 60 Pro `1260×2720` 仅为正常竖屏比例参考；生产代码使用 dp、当前窗口约束与 `WindowInsets`，不得写死设备像素。
- 本阶段验证正常竖屏、默认字体、长旅行名和关键工作台状态；280dp 小窗、横屏、分屏和 2×字体不作为阻断门禁，也不新增专门布局分支。
- 保留现有 test tag、`contentDescription`、selected、disabled 和 `Role.Tab` 语义。
- 业务或可测试布局变化遵循 RED → GREEN；每项生产修改前必须先看到对应测试因预期原因失败。
- 每个任务结束运行 `graphify update .`；不主动提交，计划中的 commit 仅作为用户授权后的建议边界。
- ADB、模拟器、真机安装和 connected tests 全局互斥；执行前确认没有其他任务占用设备。
- 批次只因功能/状态错误、数据不一致、崩溃、流程不可达、严重裁切、遮挡、关键交互损坏或基础无障碍失败而阻断；细小视觉差异进入冲突清单。

---

## File Structure

### 新建

- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceScaffold.kt`：计算安全可用高度、Sheet 锚点、实时 Sheet 顶边和地图浮层避让边界，并承载工作台三层结构。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceTopBar.kt`：旅行标题、日期、返回和更多操作的唯一工作台顶部栏。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceIcons.kt`：工作台返回、更多、搜索、定位、图层、关闭和选中图标。
- `app/src/test/java/com/yangchengwei/easytrip/workspace/WorkspaceLayoutMetricsTest.kt`：锚点、拖动阈值、位移限制和浮层边界纯函数测试。
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceChromeTest.kt`：TopBar、搜索、地图控件、图例和 Tab 的尺寸与语义测试。
- `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/WorkspacePlacePoolLayoutTest.kt`：地点池单层 padding、长文本和操作区测试。
- `docs/testing/workspace-ui-conflicts.md`：仅记录无法按规格优先级直接消解的设计冲突与最终裁决。

### 修改

- `app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/EasyTripTokens.kt`：增加工作台专用尺寸、圆角、间距和 elevation token。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceBottomSheet.kt`：改用锚点对象、dp 手势阈值和受限拖动位移。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`：迁移到 `WorkspaceScaffold`，移除私有 TopBar、硬编码浮层位置和旧 Material Sheet 遗留。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/SearchSurface.kt`：成为唯一 `WorkspaceSearchBar` 绘制实现并消费主题。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/MapControls.kt`：图标化、40dp 密集触控区、selected 语义和菜单最大宽度。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/MapLegend.kt`：使用统一边界与内容型宽度、补足内部 padding。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceTabs.kt`：44dp 触控高度、普通文字 Tab 和 3dp 指示线。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceUiModels.kt`：在展示状态中加入由现有旅行日期计算的 `dateLabel`。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt`：从现有 `TripWithDays.startDate` 与旅行日生成工作台日期副标题，不改变领域模型或 Repository。
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt`：显式接受宿主 `PaddingValues`，避免工作台重复水平 padding。
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/SavedPlaceRow.kt`：长标题和地址省略，固定操作列不被正文挤出。
- `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WorkspaceItineraryContent.kt`：显式接受宿主 padding，保留 rail 与正文之间的结构间距。
- `app/src/test/java/com/yangchengwei/easytrip/workspace/WorkspaceSheetSyncTest.kt`：迁移到锚点和拖动纯函数。
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentTest.kt`：工作台边界、三档 Sheet、地图降级和内容可达性。
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceSearchTabsTest.kt`：唯一搜索入口与 Tab 语义。
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt`：真实 NavHost 代表旅程。
- `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryScopeRailTest.kt`：行程 rail 在新壳层中的边界。
- `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContentTest.kt`：全程内容宽度与只读语义。

### 原则上不修改，仅回归

- `app/src/main/java/com/yangchengwei/easytrip/workspace/AmapComposeMap.kt`
- `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioExecutable.kt`
- `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioCatalogTest.kt`
- `app/src/androidTest/java/com/yangchengwei/easytrip/V1FullUiAcceptanceTest.kt`

---

### Task 1: 建立工作台视觉 token 与图标基础

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/EasyTripTokens.kt:10-46`
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceIcons.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/SearchSurface.kt:23-38`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/MapControls.kt:22-78`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceChromeTest.kt`

**Interfaces:**
- Consumes: `EasyTripTheme.sizes`、`EasyTripTheme.spacing`、Material 3 语义颜色。
- Produces: `workspaceTopBarHeight`、`workspaceSearchHeight`、`workspacePrimaryTouchTarget`、`workspaceDenseTouchTarget`、`workspaceIconSize`、`workspaceOverlayGap`，以及工作台图标 Composable。

- [ ] **Step 1: 新增失败的工作台图标语义测试**

创建 `WorkspaceChromeTest.kt`，先覆盖地图控件中占位文字和字符：

```kotlin
class WorkspaceChromeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun mapControlsUseIconsWithoutPlaceholderActionText() {
        compose.setContent {
            EasyTripTheme {
                MapControls(
                    layer = MapLayer.STANDARD,
                    overlay = WorkspaceOverlay.LayerMenu,
                    onOpenLayerMenu = {},
                    onCloseOverlay = {},
                    onSelectLayer = {},
                    onLocate = {},
                )
            }
        }

        compose.onNodeWithContentDescription("定位").assertHasClickAction()
        compose.onNodeWithContentDescription("关闭图层菜单").assertHasClickAction()
        compose.onNodeWithText("定位").assertDoesNotExist()
        compose.onNodeWithText("关闭").assertDoesNotExist()
        compose.onNodeWithText("✓ 标准").assertDoesNotExist()
        compose.onNodeWithTag("layer-STANDARD").assertIsSelected()
    }
}
```

- [ ] **Step 2: 运行测试并确认 RED**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceChromeTest
```

预期：失败于“定位”“关闭”仍是可见文字，或图层节点缺少 selected 语义。

- [ ] **Step 3: 增加工作台专用 token**

在 `EasyTripSizes` 中增加，不修改现有全局 `topBarHeight`：

```kotlin
val workspaceTopBarHeight: Dp = 52.dp,
val workspaceSearchHeight: Dp = 46.dp,
val workspacePrimaryTouchTarget: Dp = 44.dp,
val workspaceDenseTouchTarget: Dp = 40.dp,
val workspaceIconSize: Dp = 22.dp,
```

在 `EasyTripSpacing` 中增加：

```kotlin
val workspaceOverlayGap: Dp = 20.dp,
val workspaceControlGap: Dp = 4.dp,
```

- [ ] **Step 4: 创建无业务逻辑的图标 Composable**

`WorkspaceIcons.kt` 提供：

```kotlin
@Composable internal fun WorkspaceBackIcon(modifier: Modifier = Modifier)
@Composable internal fun WorkspaceMoreIcon(modifier: Modifier = Modifier)
@Composable internal fun WorkspaceSearchIcon(modifier: Modifier = Modifier)
@Composable internal fun WorkspaceLocateIcon(modifier: Modifier = Modifier)
@Composable internal fun WorkspaceLayerIcon(modifier: Modifier = Modifier)
@Composable internal fun WorkspaceCloseIcon(modifier: Modifier = Modifier)
@Composable internal fun WorkspaceCheckIcon(modifier: Modifier = Modifier)
```

优先使用项目已有 Compose/Material 图标依赖；若依赖中没有对应图标，沿用现有 `Canvas` 绘制方式，但颜色从 `MaterialTheme.colorScheme` 获取，不新增硬编码品牌色。

- [ ] **Step 5: 迁移搜索和地图控件**

`SearchSurface.kt`：

- `Color.White` 改为 `MaterialTheme.colorScheme.surface`。
- 高度改用 `EasyTripTheme.sizes.workspaceSearchHeight`。
- 加入 `WorkspaceSearchIcon`。
- 保留 `workspace-search-surface`、`workspace-search-launcher` 和“搜索地点”描述。

`MapControls.kt`：

- 控件采用 40dp 触控区、22dp 图标、4dp 间距。
- `layer-menu-panel` 使用 `widthIn(max = 280.dp)`。
- 关闭按钮使用图标且描述为“关闭图层菜单”。
- 图层选项使用 `selectable(selected = ..., role = Role.RadioButton)`，文字只显示“标准”或“卫星”。

- [ ] **Step 6: 运行聚焦测试并确认 GREEN**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceChromeTest
```

预期：全部通过。

- [ ] **Step 7: 回归编译和 Graphify**

```bash
./gradlew testDebugUnitTest compileDebugAndroidTestKotlin

graphify update .
```

- [ ] **Step 8: 建议提交边界（仅在用户授权 commit 后执行）**

```bash
git add app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/EasyTripTokens.kt \
  app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceIcons.kt \
  app/src/main/java/com/yangchengwei/easytrip/workspace/SearchSurface.kt \
  app/src/main/java/com/yangchengwei/easytrip/workspace/MapControls.kt \
  app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceChromeTest.kt \
  graphify-out
git commit -m "Refine workspace visual primitives"
```

---

### Task 2: 修正 BottomSheet 锚点与手势几何

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceBottomSheet.kt:28-123`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/workspace/WorkspaceSheetSyncTest.kt:9-54`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentTest.kt:115-168`

**Interfaces:**
- Consumes: `WorkspaceSheetLevel`、窗口可用 `Dp`、`LocalDensity`。
- Produces: `WorkspaceSheetAnchors`、`workspaceSheetAnchors(...)`、`resolveWorkspaceSheetDrag(...)`、`clampWorkspaceSheetDragOffsetPx(...)`。

- [ ] **Step 1: 为纯几何函数写失败测试**

将现有高度断言迁移为锚点断言，并新增：

```kotlin
@Test fun `regular window produces ordered anchors and search return lift`() {
    assertEquals(
        WorkspaceSheetAnchors(34.dp, 396.dp, 712.8.dp),
        workspaceSheetAnchors(792.dp, searchReturn = false),
    )
    assertEquals(412.dp, workspaceSheetAnchors(792.dp, searchReturn = true).half)
}

@Test fun `drag threshold is interpreted in pixels supplied by density`() {
    assertEquals(
        WorkspaceSheetLevel.HALF,
        resolveWorkspaceSheetDrag(WorkspaceSheetLevel.COLLAPSED, -49f, 48f),
    )
    assertEquals(
        WorkspaceSheetLevel.COLLAPSED,
        resolveWorkspaceSheetDrag(WorkspaceSheetLevel.COLLAPSED, -47f, 48f),
    )
}

@Test fun `drag moves only one adjacent level`() {
    assertEquals(
        WorkspaceSheetLevel.HALF,
        resolveWorkspaceSheetDrag(WorkspaceSheetLevel.COLLAPSED, -500f, 24f),
    )
    assertEquals(
        WorkspaceSheetLevel.HALF,
        resolveWorkspaceSheetDrag(WorkspaceSheetLevel.EXPANDED, 500f, 24f),
    )
}

@Test fun `drag offset is clamped to legal anchor range`() {
    assertEquals(-300f, clampWorkspaceSheetDragOffsetPx(400f, 40f, 700f, -999f))
    assertEquals(360f, clampWorkspaceSheetDragOffsetPx(400f, 40f, 700f, 999f))
}
```

- [ ] **Step 2: 运行 JVM 测试并确认 RED**

```bash
./gradlew testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.workspace.WorkspaceSheetSyncTest'
```

预期：新类型和函数未定义。

- [ ] **Step 3: 实现最小锚点和手势纯函数**

```kotlin
internal data class WorkspaceSheetAnchors(
    val collapsed: Dp,
    val half: Dp,
    val expanded: Dp,
) {
    operator fun get(level: WorkspaceSheetLevel): Dp = when (level) {
        WorkspaceSheetLevel.COLLAPSED -> collapsed
        WorkspaceSheetLevel.HALF -> half
        WorkspaceSheetLevel.EXPANDED -> expanded
    }
}

internal fun workspaceSheetAnchors(
    availableHeight: Dp,
    searchReturn: Boolean,
): WorkspaceSheetAnchors

internal fun resolveWorkspaceSheetDrag(
    current: WorkspaceSheetLevel,
    dragDeltaPx: Float,
    thresholdPx: Float,
): WorkspaceSheetLevel

internal fun clampWorkspaceSheetDragOffsetPx(
    currentHeightPx: Float,
    collapsedHeightPx: Float,
    expandedHeightPx: Float,
    requestedOffsetPx: Float,
): Float
```

沿用当前正常窗口锚点结果；极端窗口只要求锚点有序且不超过可用高度，不新增专门 UI 分支。

- [ ] **Step 4: 在 Compose 层使用 density 和受限 offset**

```kotlin
val density = LocalDensity.current
val dragThresholdPx = with(density) { 24.dp.toPx() }
val currentHeightPx = with(density) { anchors[value].toPx() }
val collapsedHeightPx = with(density) { anchors.collapsed.toPx() }
val expandedHeightPx = with(density) { anchors.expanded.toPx() }
```

`onVerticalDrag` 只更新 clamp 后的 offset；`onDragEnd` 调用 `resolveWorkspaceSheetDrag`，仍然一步一档。

- [ ] **Step 5: 更新 Compose 边界测试**

保留三档高度有序断言；将现有 `constrainedHeightKeepsPlacePoolReachableAtSmallWindowAndLargeFont` 降为非阻断回归观察，不允许其反向决定正常窗口布局。新增：

```kotlin
@Test fun collapsedSheetKeepsHandleAndHidesBusinessContent()
@Test fun sheetAlwaysStaysInsideWorkspaceRoot()
```

分别断言 handle 存在、地点池内容在折叠态不可见，以及三档 Sheet bounds 不越过 root。

- [ ] **Step 6: 运行 JVM 与 Compose 测试**

```bash
./gradlew testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.workspace.WorkspaceSheetSyncTest'

./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.TripWorkspaceContentTest
```

预期：全部通过，三档和搜索返回高度行为不变。

- [ ] **Step 7: 更新 Graphify 并记录建议提交边界**

```bash
graphify update .
```

建议提交：`Fix workspace sheet drag geometry`。

---

### Task 3: 建立统一 WorkspaceScaffold 边界

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceScaffold.kt`
- Create: `app/src/test/java/com/yangchengwei/easytrip/workspace/WorkspaceLayoutMetricsTest.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt:107-203`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/MapLegend.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceMapFallback.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentTest.kt`

**Interfaces:**
- Consumes: Task 2 的 `WorkspaceSheetAnchors`。
- Produces: `WorkspaceLayoutMetrics` 和 `WorkspaceScaffold(...)`，后续 chrome、地点池和行程任务均通过 slot 接入。

- [ ] **Step 1: 写布局指标失败测试**

```kotlin
class WorkspaceLayoutMetricsTest {
    @Test fun `sheet top and overlay inset share the selected anchor`() {
        val anchors = WorkspaceSheetAnchors(34.dp, 396.dp, 712.8.dp)
        val metrics = workspaceLayoutMetrics(
            availableHeight = 792.dp,
            anchors = anchors,
            level = WorkspaceSheetLevel.HALF,
            overlayGap = 20.dp,
        )

        assertEquals(396.dp, metrics.sheetHeight)
        assertEquals(396.dp, metrics.sheetTop)
        assertEquals(416.dp, metrics.overlayBottomInset)
    }

    @Test fun `layout metrics never emit negative viewport`() {
        val metrics = workspaceLayoutMetrics(
            availableHeight = 320.dp,
            anchors = WorkspaceSheetAnchors(34.dp, 240.dp, 288.dp),
            level = WorkspaceSheetLevel.EXPANDED,
            overlayGap = 20.dp,
        )
        assertTrue(metrics.sheetTop >= 0.dp)
    }
}
```

- [ ] **Step 2: 运行测试并确认 RED**

```bash
./gradlew testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.workspace.WorkspaceLayoutMetricsTest'
```

预期：`WorkspaceLayoutMetrics` 和 `workspaceLayoutMetrics` 未定义。

- [ ] **Step 3: 实现纯布局指标**

```kotlin
@Immutable
internal data class WorkspaceLayoutMetrics(
    val availableHeight: Dp,
    val sheetHeight: Dp,
    val sheetTop: Dp,
    val overlayBottomInset: Dp,
)

internal fun workspaceLayoutMetrics(
    availableHeight: Dp,
    anchors: WorkspaceSheetAnchors,
    level: WorkspaceSheetLevel,
    overlayGap: Dp = 20.dp,
): WorkspaceLayoutMetrics {
    val sheetHeight = anchors[level].coerceAtMost(availableHeight)
    val sheetTop = (availableHeight - sheetHeight).coerceAtLeast(0.dp)
    return WorkspaceLayoutMetrics(
        availableHeight = availableHeight,
        sheetHeight = sheetHeight,
        sheetTop = sheetTop,
        overlayBottomInset = sheetHeight + overlayGap,
    )
}
```

- [ ] **Step 4: 实现 Scaffold slot API**

```kotlin
@Composable
internal fun WorkspaceScaffold(
    sheetLevel: WorkspaceSheetLevel,
    searchReturn: Boolean,
    onSheetLevelChange: (WorkspaceSheetLevel) -> Unit,
    modifier: Modifier = Modifier,
    map: @Composable BoxScope.(WorkspaceLayoutMetrics) -> Unit,
    topOverlay: @Composable BoxScope.(WorkspaceLayoutMetrics) -> Unit,
    sheetHeader: @Composable () -> Unit,
    sheetContent: @Composable () -> Unit,
)
```

Scaffold 是唯一应用 `WindowInsets.safeDrawing` 和 `imePadding()` 的工作台根容器；内部以 `BoxWithConstraints` 计算 anchors/metrics，然后渲染 map、topOverlay 和 `WorkspaceBottomSheet`。

- [ ] **Step 5: 迁移工作台背景和浮层**

在 `TripWorkspaceContent.kt`：

- 将地图、fallback、顶部栏、地图控件、图例和搜索放入 Scaffold slots。
- `SearchSurface` 使用 `metrics.overlayBottomInset`。
- `MapLegend` 不再使用固定 `bottom = 82.dp`。
- `WorkspaceMapFallback` 继续位于 Sheet 顶边以上。
- 删除未使用的 `BottomSheetScaffold`、`SheetValue`、`rememberBottomSheetScaffoldState`、`rememberStandardBottomSheetState` 和 `toSheetValue()`。

- [ ] **Step 6: 写并运行 Compose 空间关系测试**

新增：

```kotlin
@Test fun searchLegendAndMapControlsStayAboveCurrentSheet()
@Test fun overlayPositionsMoveBetweenHalfAndExpandedLevels()
@Test fun mapFailureRetryRemainsAboveSheet()
```

每项通过 `getUnclippedBoundsInRoot()` 比较目标节点 bottom 与 `workspace-sheet` top，不做像素截图比较。

```bash
./gradlew testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.workspace.WorkspaceLayoutMetricsTest' \
  --tests 'com.yangchengwei.easytrip.workspace.WorkspaceSheetSyncTest'

./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.TripWorkspaceContentTest
```

- [ ] **Step 7: 更新 Graphify 并记录建议提交边界**

```bash
graphify update .
```

建议提交：`Coordinate workspace overlays with sheet`。

---

### Task 4: 收敛 TopBar、搜索、地图控件、图例和普通 Tab

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceTopBar.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt:184-230`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/SearchSurface.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/MapControls.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/MapLegend.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceTabs.kt:25-63`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceUiModels.kt:3-33`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt:164-215`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentStateTest.kt:72-78`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceChromeTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceSearchTabsTest.kt:42-72`

**Interfaces:**
- Consumes: Task 1 token/图标和 Task 3 `WorkspaceLayoutMetrics`。
- Produces: 唯一 `WorkspaceTopBar(...)`、唯一 `WorkspaceSearchBar(...)` 和稳定 chrome tags。

- [ ] **Step 1: 写 TopBar 和 Tabs 的失败测试**

```kotlin
@Test fun topBarKeepsActionsVisibleWithLongTripName() {
    setWorkspace(title = "这是一个非常非常长但仍然不能挤掉返回和更多按钮的旅行名称")
    compose.onNodeWithTag("workspace-back").assertIsDisplayed().assertHasClickAction()
    compose.onNodeWithTag("workspace-more").assertIsDisplayed().assertHasClickAction()
    compose.onNodeWithTag("workspace-trip-title").assertIsDisplayed()
}

@Test fun workspaceTabsUse44DpTouchHeightAndThreeDpIndicator() {
    setWorkspace()
    assertEquals(44.dp, compose.onNodeWithTag("section-PLACE_POOL").getUnclippedBoundsInRoot().height)
    assertEquals(3.dp, compose.onNodeWithTag("workspace-tab-indicator-PLACE_POOL").getUnclippedBoundsInRoot().height)
}
```

另在 `WorkspaceSearchTabsTest` 增加一次点击只产生一个 `OpenSearch` 的断言，并继续验证入口不是可编辑文本字段。

- [ ] **Step 2: 运行测试并确认 RED**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceChromeTest,com.yangchengwei.easytrip.workspace.WorkspaceSearchTabsTest
```

预期：当前 TopBar 仍显示文字按钮，缺少稳定 action tag；Tab 高度没有固定为 44dp。

- [ ] **Step 3: 抽出唯一 WorkspaceTopBar**

```kotlin
@Composable
internal fun WorkspaceTopBar(
    title: String,
    onBack: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
)
```

- 使用 52dp 视觉高度。
- 返回/更多使用 44dp 触控区和 22dp 图标。
- 标题使用 `Modifier.weight(1f)`、`maxLines = 1`、`overflow = TextOverflow.Ellipsis`。
- 设置 tags：`workspace-back`、`workspace-trip-title`、`workspace-more`。
- “更多”继续派发当前 `OpenSettings`；本阶段不更改其产品目的。

同时先在 `TripWorkspaceContentStateTest.kt` 添加日期展示测试：

```kotlin
@Test fun readyTripExposesWorkspaceDateLabel() = runTest(dispatcher) {
    val trips = Trips()
    val model = model(trips)
    trips.value.value = TripWithDays(
        "trip",
        "北京",
        LocalDate.of(2026, 8, 23),
        TravelMode.FLEXIBLE,
        listOf(TripDay("day-1", 0), TripDay("day-2", 1), TripDay("day-3", 2)),
    )
    advanceUntilIdle()
    assertEquals("8月23日 — 8月25日", (model.pageState.value as TripWorkspacePageState.Ready).content.dateLabel)
}
```

运行并确认 RED：

```bash
./gradlew testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.workspace.TripWorkspaceContentStateTest.readyTripExposesWorkspaceDateLabel'
```

然后在 `TripWorkspaceUiState` 与 `TripWorkspaceReadyState` 中增加 `dateLabel: String?`，并由 `TripWorkspaceViewModel.mapWorkspaceState()` 使用现有 `TripWithDays.startDate` 和有效旅行日数量计算：无开始日期时为 `null`，一天显示单日，多天显示起止日期。不修改领域模型或 Repository。`WorkspaceTopBar` 接收并显示该副标题。

- [ ] **Step 4: 保留唯一搜索入口实现**

将实际绘制函数命名为：

```kotlin
@Composable
fun WorkspaceSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

如现有调用广泛使用 `SearchSurface`，可在同一任务内一次性改调用并删除旧名称；最终不能同时保留两个绘制实现。删除 `TripWorkspaceScreen.kt` 中未使用的 `WorkspaceSearchLauncher`，保留现有两个 search tag。

- [ ] **Step 5: 完成 chrome 视觉约束**

- `WorkspaceTabs`：44dp 高、普通文字、3dp 指示线、`Role.Tab` 和 selected。
- `MapLegend`：`wrapContentWidth()`，明确水平/垂直 padding，不使用固定设备宽度。
- `MapControls`：消费 Task 1 token；菜单不进入 Sheet 区域。
- TopBar、搜索和地图控件统一使用 Forest Sage 语义色与 elevation token。

- [ ] **Step 6: 运行聚焦和静态验证**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceChromeTest,com.yangchengwei.easytrip.workspace.WorkspaceSearchTabsTest,com.yangchengwei.easytrip.workspace.TripWorkspaceContentTest

./gradlew testDebugUnitTest lintDebug
```

- [ ] **Step 7: 更新冲突清单与 Graphify**

创建 `docs/testing/workspace-ui-conflicts.md`，写入字段和当前状态：

```markdown
| ID | Frame/页面 | 冲突双方 | 临时实现 | 用户影响 | 风险 | 阻断 | 状态 |
|---|---|---|---|---|---|---|---|
| UI-01 | 工作台更多入口 | 设计稿图标 / Profile 或设置目标未统一 | 保留现有 OpenSettings | 无行为变化 | product-semantics | 否 | OPEN |
```

```bash
graphify update .
```

建议提交：`Align workspace chrome with design`。

---

### Task 5: 将地点池接入共享 Sheet

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt:62-187`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/SavedPlaceRow.kt:18-55`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt:140-154`
- Create: `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/WorkspacePlacePoolLayoutTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentTest.kt:170-213`

**Interfaces:**
- Consumes: Scaffold 提供的单层 Sheet padding。
- Produces: `PlacePoolContent(..., contentPadding: PaddingValues)`；现有 `PlacePoolAction` 完全不变。

- [ ] **Step 1: 写地点池布局失败测试**

```kotlin
@Test fun longPlaceNameKeepsActionsInsideRow() {
    setPlacePool(
        name = "西湖风景名胜区附近一个非常长的收藏地点名称",
        address = "这是一条足以触发省略但不能挤出编辑和删除操作的地址",
    )

    val row = compose.onNodeWithTag("saved-place-place").getUnclippedBoundsInRoot()
    val edit = compose.onNodeWithTag("edit-place-place").getUnclippedBoundsInRoot()
    val delete = compose.onNodeWithTag("delete-place-place").getUnclippedBoundsInRoot()
    assertTrue(edit.right <= row.right)
    assertTrue(delete.right <= row.right)
}

@Test fun workspacePlacePoolUsesOnlySheetHorizontalInset() {
    setWorkspacePlacePool()
    val tabs = compose.onNodeWithTag("workspace-tabs").getUnclippedBoundsInRoot()
    val list = compose.onNodeWithTag("workspace-place-list").getUnclippedBoundsInRoot()
    assertEquals(tabs.left, list.left)
    assertEquals(tabs.right, list.right)
}
```

- [ ] **Step 2: 运行测试并确认 RED**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.WorkspacePlacePoolLayoutTest
```

预期：当前地点池额外 `padding(16.dp)` 导致左右边界不一致，编辑按钮也缺少 tag 或被长文本挤压。

- [ ] **Step 3: 增加显式宿主 padding 参数**

两个 `PlacePoolContent` overload 都增加：

```kotlin
contentPadding: PaddingValues = PaddingValues(16.dp)
```

根 Column 改为：

```kotlin
Column(modifier.padding(contentPadding))
```

工作台调用传入：

```kotlin
contentPadding = PaddingValues(bottom = 20.dp)
```

Sheet 外层继续统一提供左右 20dp；独立 PlacePool 调用保持默认 16dp。

- [ ] **Step 4: 约束 SavedPlaceRow 文本和操作区**

- 文本 Column 保持 `weight(1f)` 并增加 `widthIn(min = 0.dp)`。
- 名称 `maxLines = 1`、`overflow = TextOverflow.Ellipsis`。
- 地址 `maxLines = 2`、`overflow = TextOverflow.Ellipsis`。
- 编辑按钮增加 `edit-place-${place.id}` tag。
- 操作列不改变按钮数量和事件。
- 不引入新的菜单或单按钮方案。

- [ ] **Step 5: 运行地点池测试**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.WorkspacePlacePoolLayoutTest,com.yangchengwei.easytrip.place.ui.PlacePoolFlowTest,com.yangchengwei.easytrip.workspace.TripWorkspaceContentTest
```

预期：长文案不越界，地点池列表可滚动，添加到行程入口和已有状态文案保持不变。

- [ ] **Step 6: 更新 Graphify 并记录建议提交边界**

```bash
graphify update .
```

建议提交：`Align place pool with workspace sheet`。

---

### Task 6: 将单日与全程行程接入共享 Sheet

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WorkspaceItineraryContent.kt:14-40`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt:155-169`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryScopeRailTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContentTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentTest.kt`

**Interfaces:**
- Consumes: Scaffold 单层 Sheet padding 和现有 `ItineraryScopeRail`。
- Produces: `WorkspaceItineraryContent(..., contentPadding: PaddingValues)`；所有 itinerary actions 与 ViewModel 保持不变。

- [ ] **Step 1: 写行程容器失败测试**

在既有测试中增加：

```kotlin
@Test fun scopeRailAndDayContentStayInsideSheetBounds()
@Test fun wholeTripFitsWorkspaceWidthAndRemainsReadOnly()
@Test fun halfSheetKeepsScopeRailAndSelectedDayReachable()
@Test fun expandedSheetKeepsWholeTripContentReachable()
```

断言：

- `itinerary-scope-rail` 和右侧内容 bounds 均位于 `workspace-sheet` 内。
- 全程内容不存在 timing、move、delete、mode、retry 等编辑 tag。
- 切换单日/全程后 map scope 与当前既有测试一致。

- [ ] **Step 2: 运行测试并确认 RED**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryScopeRailTest,com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryContentTest,com.yangchengwei.easytrip.workspace.TripWorkspaceContentTest
```

预期：当前工作台外层 20dp 与行程内部固定间距叠加，新增边界断言失败。

- [ ] **Step 3: 增加行程宿主 padding 参数**

```kotlin
@Composable
fun WorkspaceItineraryContent(
    days: List<TripDay>,
    selected: ItineraryScope,
    wholeTripDays: List<WholeTripDayUi>,
    onSelect: (ItineraryScope) -> Unit,
    dayContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onAddDay: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues.Zero,
)
```

根 Row 使用 `modifier.padding(contentPadding)`；内部只保留 rail 与正文之间的 12dp 结构间距。工作台不得再给同一内容叠加额外水平 padding。

- [ ] **Step 4: 保持领域组件不变**

确认 diff 不包含：

- `DayItineraryViewModel`
- Repository 接口或实现
- Route generation/cancellation
- 编辑、移动、删除和 retry action 类型

若壳层变化暴露行程卡片操作密度问题，记录到 `docs/testing/workspace-ui-conflicts.md`，不在本任务扩大范围。

- [ ] **Step 5: 运行聚焦回归**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryScopeRailTest,com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryContentTest,com.yangchengwei.easytrip.workspace.TripWorkspaceContentTest
```

随后运行现有行程编辑回归：

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryEditingTest
```

该测试覆盖编辑、移动、删除和 RouteLeg 相关交互；不得用不存在的测试类名替代。

- [ ] **Step 6: 更新 Graphify 并记录建议提交边界**

```bash
graphify update .
```

建议提交：`Fit itinerary content into workspace shell`。

---

### Task 7: 真实导航、全量回归与 Mate 60 Pro 验收

**Files:**
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceSearchTabsTest.kt`
- Modify only if a genuine catalog binding regression is found: `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioExecutable.kt`
- Modify: `docs/testing/workspace-ui-conflicts.md`
- Modify: `.superpowers/sdd/2026-08-23-easy-trip-v1-full-ui-implementation/progress.md`

**Interfaces:**
- Consumes: Tasks 1–6 的最终工作台 UI。
- Produces: 自动化门禁结果、代表性生产旅程结果、Mate 60 Pro 真机验收记录和剩余冲突清单。

- [ ] **Step 1: 写真实 NavHost 回归测试**

在 `WorkspaceFlowTest.kt` 增加一条完整旅程：

```kotlin
@Test fun realNavigationEntersWorkspaceSearchesReturnsAndSwitchesSections() {
    // 使用现有真实 NavHost fixture 创建并进入旅行
    // 点击 workspace-search-launcher
    // 从搜索页返回工作台
    // 断言最近收藏高亮仍存在
    // 点击 section-ITINERARY 后断言已选中
    // 点击 section-PLACE_POOL 后断言已选中
}
```

该测试必须复用现有 Room/NavHost fixture，不得以局部 Boolean 状态模拟导航。

另增加：

```kotlin
@Test fun longTripNameKeepsBackMoreAndSearchPhysicallyClickable()
@Test fun mapFailureKeepsLocalTabsAndActionsReachable()
```

- [ ] **Step 2: 运行测试并确认 RED 或护栏有效**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceFlowTest,com.yangchengwei.easytrip.workspace.WorkspaceSearchTabsTest
```

若新测试立即通过，明确说明它证明的是已有行为护栏；检查断言是否真的覆盖新壳层的物理点击和 bounds，而不是仅检查节点存在。

- [ ] **Step 3: 只修复接线型回归**

允许修复：

- Scaffold slot 接线错误。
- test tag 或语义丢失。
- 点击目标被覆盖。
- Insets 重复应用。
- 搜索返回后 Sheet 边界未刷新。

禁止修改：导航 reducer、SavedState key、地点池/行程业务规则、地图生命周期和权限流程。

- [ ] **Step 4: 运行 JVM、lint 和 APK 门禁**

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

预期：全部成功，无新增 lint error。

- [ ] **Step 5: 运行工作台聚焦设备测试**

在确认没有其他任务占用模拟器或真机后：

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.TripWorkspaceContentTest,com.yangchengwei.easytrip.workspace.WorkspaceChromeTest,com.yangchengwei.easytrip.workspace.WorkspaceSearchTabsTest,com.yangchengwei.easytrip.workspace.WorkspaceFlowTest,com.yangchengwei.easytrip.place.ui.WorkspacePlacePoolLayoutTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryScopeRailTest,com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryContentTest
```

- [ ] **Step 6: 运行 47 场景目录与完整 acceptance**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1ScenarioCatalogTest

./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1FullUiAcceptanceTest
```

预期：目录 47/47，完整 acceptance 无 skipped、无 failed。若 instrumentation class 过滤得到 0 tests，视为门禁未执行，必须修正入口后重跑。

- [ ] **Step 7: Mate 60 Pro 正常竖屏抽查**

安装当前 debug APK 后，仅在系统默认字体和正常竖屏检查：

1. 半屏地点池。
2. 半屏单日行程。
3. 展开全程行程。
4. 搜索后返回工作台。
5. 长旅行名。
6. 地图失败或隐私未同意。

每项记录：Git SHA/source state、设备型号、SDK、窗口像素、density、font scale、进入步骤、是否遮挡/裁切、关键操作是否可点击。无需逐屏截图或像素对比。

- [ ] **Step 8: 更新冲突清单和进度记录**

对每个剩余差异写入：frame/页面、冲突双方、临时实现、用户影响、风险类别、是否阻断、证据路径和状态。更新进度文件时明确：

```text
自动化门禁：PASS/FAIL
Mate 60 Pro 正常竖屏：PASS/PENDING/BLOCKED
极端小窗、横屏、分屏、2×字体：DEFERRED，不属于本阶段阻断条件
```

- [ ] **Step 9: 最终 Graphify 与工作区检查**

```bash
graphify update .
git diff --check
git status --short
```

确认临时 `.superpowers/brainstorm/` 内容不进入提交范围。

- [ ] **Step 10: 建议最终提交边界（仅在用户授权 commit 后执行）**

```bash
git add app/src/main app/src/test app/src/androidTest \
  docs/testing/workspace-ui-conflicts.md \
  docs/superpowers/specs/2026-08-25-easy-trip-workspace-ui-convergence-design.md \
  docs/superpowers/plans/2026-08-25-easy-trip-workspace-ui-convergence.md \
  .superpowers/sdd/2026-08-23-easy-trip-v1-full-ui-implementation/progress.md \
  graphify-out
git commit -m "Converge workspace UI with design"
```

提交前再次用 `git status --short` 确认没有加入 `.superpowers/brainstorm/` 或其他临时产物。

---

## Execution Notes

- Tasks 1–4 修改共享工作台文件，必须顺序执行。
- Task 5 和 Task 6 在 Task 4 完成后可使用独立 worktree 并行，但不能同时修改 `TripWorkspaceContent.kt`；建议先让各自分支只改领域内容文件与测试，最后由主工作树串行完成接线。
- Task 7 独占模拟器、真机和 ADB。
- 每个任务完成后先做独立代码审查，再进入下一任务。
- 若实施中发现设计稿与现有产品语义冲突，不暂停所有无争议工作；记录到 `docs/testing/workspace-ui-conflicts.md`，仅暂停依赖该决策的最小任务。
