# Easy Trip 行程内容族 UI 收敛实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将单日行程、全程行程、行程项和 RouteLeg 收敛为紧凑时间线，使用独立拖动手柄和 `···` 菜单，并保持现有业务状态机、数据层和错误恢复语义。

**Architecture:** 提取共享只读地点 primitive 和 RouteLeg primitive；单日包装器在 primitive 上添加拖动手柄与行程项菜单，全程视图只消费只读版本。菜单只持有 Popup 展开 Boolean，业务目标仍由现有 `DayItineraryUiState` draft/confirmation/editor 保存；`DayItineraryContent` 继续把 UI 事件映射到既有 `DayItineraryAction`。

**Tech Stack:** Kotlin、Jetpack Compose、Material 3、ViewModel、StateFlow、Room、高德路线状态、JUnit 4、Compose UI Test。

**Spec:** `docs/superpowers/specs/2026-08-26-easy-trip-itinerary-ui-convergence-design.md`

## Global Constraints

- 单日采用紧凑时间线；全程保持只读。
- 地点名称、到达时间和停留时长为主信息；地址和辅助信息为次信息。
- 行程项仅常驻拖动手柄和 `···`；编辑时间、跨日移动、删除位于菜单中。
- 菜单只持有是否展开的纯 UI Boolean；不得在 ViewModel 新增 `selectedMenuItemId`，不得复制 edit/move/delete 业务状态。
- 菜单 action 必须一一映射现有 `RequestTiming(itemId)`、`RequestCrossDay(itemId)`、`RequestDelete(itemId)`。
- 删除仍经过 `ItineraryDeleteConfirmation`，不得从菜单直接调用 Repository。
- 拖动只能从 handle 发起；handle 与菜单触控区均为 40dp，图标 20–22dp，间距至少 4dp。
- 拖动步长必须由 dp 转为 px，禁止继续使用裸 `120f` pointer px。
- `PENDING` 与 `CALCULATING` 在 UI 层合并为明确 `Calculating`；domain `RouteStatus` 不变。
- 只有 Ready RouteLeg 可进入交通方式编辑；Failed 只提供当前 leg 的重试；WaitingForNetwork/Calculating 不提供 mode 或 retry。
- 全程 RouteLeg 显示状态和错误原因，但不提供 mode 或 retry。
- 统一简洁空态插画使用 Compose/矢量和 Forest Sage 语义色，不逐路径复刻 Pencil。
- 不修改 Room schema、Repository、路线 generation/cancellation、权限流程或工作台壳层。
- 不处理 395/396dp 临界跳变、280dp 小窗、横屏、分屏或 2× 字体。
- 常规竖屏长地点名最多两行、地址最多两行，操作区不得被挤出。
- 业务和布局变化遵循 RED → GREEN；先看到预期失败，再写最小实现。
- connected tests、ADB、模拟器和真机全局互斥；设备失败最多基础设施重试一次。
- 修改后运行 `graphify update .`。
- 不主动 push；每个任务可按用户后续授权小步 commit。

---

## File Structure

### 新建

- `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryItemMenu.kt`：行程项 `···` 菜单、菜单 action 和纯 UI 展开状态。
- `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEmptyIllustration.kt`：单日/无旅行日共用的简洁矢量插画。
- `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryTimelineContentTest.kt`：紧凑地点行、菜单、拖动 handle、RouteLeg 四态和空态 focused tests。
- `docs/testing/itinerary-ui-conflicts.md`：仅记录实现中无法按已确认规格消解的行程视觉冲突。

### 修改

- `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryItemRow.kt`：共享紧凑地点 primitive、单日可编辑包装器、独立 handle 和菜单入口。
- `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryPlaceRow.kt`：成为全程使用的只读地点行包装器。
- `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItinerarySheet.kt`：组合紧凑时间线、菜单 action、RouteLeg 和单日空态。
- `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryUiModels.kt`：RouteLeg UI 状态补齐为 Ready/Calculating/WaitingForNetwork/Failed。
- `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/RouteLegRow.kt`：轻量连接段、四态展示和按状态启用操作。
- `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContent.kt`：复用只读 primitive、空日与无旅行日空态。
- `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WorkspaceItineraryContent.kt`：将现有 `onAddDay` 转发到全程无旅行日空态。
- `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/EmptyState.kt`：增加可选 illustration 与 action slot。
- `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt`：菜单、拖动 handle、RouteLeg 四态与业务接线回归。
- `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContentTest.kt`：只读 primitive、RouteLeg 与空态动作。
- `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryScopeRailTest.kt`：无旅行日两个“新增”入口调用同一 action。
- `app/src/test/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModelTest.kt`：RouteLeg 四态映射与 legacy error fallback。
- `app/src/test/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryMapperTest.kt`：全程四态只读映射。
- `app/src/test/java/com/yangchengwei/easytrip/workspace/TripWorkspaceNavigationStateTest.kt`：菜单 action 只建立现有业务状态。
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt`：真实工作台 overlay 接线。
- `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioExecutable.kt`：仅在现有场景 selector 因新 UI 结构失效时迁移到稳定 tag，不改变场景断言。

### 原则上不修改

- `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModel.kt`
- `app/src/main/java/com/yangchengwei/easytrip/itinerary/domain/ItineraryRepository.kt`
- `app/src/main/java/com/yangchengwei/easytrip/route/domain/RouteLegRepository.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceScaffold.kt`
- Room entity、DAO 和 schema。

---

### Task 1: 建立共享紧凑地点 primitive

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryItemRow.kt:33-145`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryPlaceRow.kt`
- Create: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryTimelineContentTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContentTest.kt`

**Interfaces:**
- Consumes: `ItineraryItemUi` 的现有 `id/name/address/arrivalTime/stayMinutes`。
- Produces: `ItineraryPlaceContent(...)` 共享 primitive、只读 `ItineraryPlaceRow(...)`、可由 Task 2/3 注入 leading/trailing action 的地点结构。

- [ ] **Step 1: 写紧凑地点行失败测试**

创建 `ItineraryTimelineContentTest.kt`：

```kotlin
class ItineraryTimelineContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun compactPlaceRowShowsArrivalNameStayAndAddress() {
        compose.setContent {
            EasyTripTheme {
                ItineraryPlaceRow(
                    item = itineraryItem(
                        id = "i1",
                        name = "灵隐寺",
                        address = "浙江省杭州市西湖区法云弄1号",
                        arrivalTime = "09:30",
                        stayMinutes = 120,
                    ),
                    displayOrder = 1,
                )
            }
        }

        compose.onNodeWithText("09:30").assertIsDisplayed()
        compose.onNodeWithText("灵隐寺").assertIsDisplayed()
        compose.onNodeWithText("停留 120 分钟").assertIsDisplayed()
        compose.onNodeWithText("浙江省杭州市西湖区法云弄1号").assertIsDisplayed()
    }
}
```

再增加：

```kotlin
@Test fun longTextDoesNotPushTrailingActionOutsideRow()
@Test fun editableAndReadOnlyRowsSharePlaceContentGeometry()
@Test fun normalPlaceRowHasZeroElevationAndNoPermanentDeleteAction()
```

长文本测试用 `getUnclippedBoundsInRoot()` 断言 trailing action 的 right 不超过 `item-{id}` 的 right。

- [ ] **Step 2: 运行测试确认 RED**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryTimelineContentTest
```

预期：新接口不存在；当前卡片仍有 elevation 和常驻危险操作。

- [ ] **Step 3: 实现共享 primitive**

在 `ItineraryItemRow.kt` 提供：

```kotlin
@Composable
internal fun ItineraryPlaceContent(
    item: ItineraryItemUi,
    displayOrder: Int,
    modifier: Modifier = Modifier,
    leadingAction: (@Composable () -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 0.dp,
)
```

布局要求：

- 外层为轻量 Surface，默认 elevation 0dp。
- leading/trailing slot 各由调用方提供固定 40dp 容器。
- 正文 `Modifier.weight(1f)`。
- 名称 `maxLines = 2`、`TextOverflow.Ellipsis`。
- 地址 `maxLines = 2`、`TextOverflow.Ellipsis`。
- 时间使用 Primary 色；无时间时不伪造占位值。
- 保留 `item-{id}` tag。

在 `ItineraryPlaceRow.kt` 提供只读包装：

```kotlin
@Composable
fun ItineraryPlaceRow(
    item: ItineraryItemUi,
    displayOrder: Int,
    modifier: Modifier = Modifier,
) = ItineraryPlaceContent(item, displayOrder, modifier)
```

- [ ] **Step 4: 迁移全程地点渲染到只读包装器**

将 `WholeTripItineraryContent` 的 `ItineraryItemCard` 调用改为：

```kotlin
ItineraryPlaceRow(
    item = itineraryItem,
    displayOrder = index + 1,
    modifier = Modifier.fillMaxWidth(),
)
```

不得传空编辑回调伪造只读。

- [ ] **Step 5: 运行 GREEN 和全程回归**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryTimelineContentTest,com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryContentTest
```

预期：全部通过；全程仍不出现编辑 action tag。

- [ ] **Step 6: 静态验证与 Graphify**

```bash
./gradlew testDebugUnitTest compileDebugAndroidTestKotlin
graphify update .
```

- [ ] **Step 7: 建议提交边界**

```bash
git add app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryItemRow.kt \
  app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryPlaceRow.kt \
  app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContent.kt \
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryTimelineContentTest.kt \
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContentTest.kt \
  graphify-out
git commit -m "Build compact itinerary place rows" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: 实现行程项 `···` 菜单

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryItemMenu.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryItemRow.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItinerarySheet.kt:81-97`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryTimelineContentTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/workspace/TripWorkspaceNavigationStateTest.kt`

**Interfaces:**
- Consumes: Task 1 `ItineraryPlaceContent` 和现有 `DayItineraryAction`。
- Produces: `ItineraryItemMenuAction`、行内纯 UI Popup、`ItineraryItemRow(..., onMenuAction)`。

- [ ] **Step 1: 写菜单失败测试**

在 `ItineraryTimelineContentTest` 增加：

```kotlin
@Test fun itemShowsOnlyHandleAndMoreAsPermanentActions()
@Test fun itemMenuDispatchesTimingMoveAndDeleteForCurrentItem()
@Test fun dismissingMenuDoesNotDispatchBusinessAction()
@Test fun moreMenuDescriptionContainsPlaceName()
```

测试 tags：

```text
drag-handle-i1
more-i1
menu-timing-i1
menu-move-i1
menu-delete-i1
```

在 `ItineraryEditingTest` 将常驻按钮断言改为：先点击 `more-{id}`，再点击对应菜单项，并断言原 draft/confirmation/overlay 行为。

- [ ] **Step 2: 运行测试确认 RED**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryTimelineContentTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryEditingTest
```

预期：菜单 tags 不存在，旧“时间/移动到…/删除”按钮常驻。

- [ ] **Step 3: 定义菜单 action 与 Popup**

`ItineraryItemMenu.kt`：

```kotlin
internal enum class ItineraryItemMenuAction {
    EditTiming,
    MoveToOtherDay,
    Delete,
}

@Composable
internal fun ItineraryItemMenu(
    itemId: String,
    itemName: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAction: (ItineraryItemMenuAction) -> Unit,
    modifier: Modifier = Modifier,
)
```

实现约束：

- 使用 `DropdownMenu` / `DropdownMenuItem`。
- 展开 Boolean 由 `ItineraryItemRow` 的 `remember(item.id)` 保存；Activity 重建后无需自动重开。
- 菜单不保存 item 副本或业务目标 id。
- 点击菜单项先关闭，再调用当前行闭包。
- 删除项使用 error/danger 语义色。

- [ ] **Step 4: 改造 ItineraryItemRow 接口**

```kotlin
@Composable
fun ItineraryItemRow(
    item: ItineraryItemUi,
    index: Int,
    count: Int,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
    onMenuAction: (ItineraryItemMenuAction) -> Unit,
    modifier: Modifier = Modifier,
)
```

行内只保存：

```kotlin
var menuExpanded by remember(item.id) { mutableStateOf(false) }
```

- [ ] **Step 5: 集中映射现有 DayItineraryAction**

`DayItineraryContent` 中：

```kotlin
onMenuAction = { action ->
    onAction(
        when (action) {
            ItineraryItemMenuAction.EditTiming -> DayItineraryAction.RequestTiming(id)
            ItineraryItemMenuAction.MoveToOtherDay -> DayItineraryAction.RequestCrossDay(id)
            ItineraryItemMenuAction.Delete -> DayItineraryAction.RequestDelete(id)
        },
    )
}
```

不得新增 ViewModel menu state。

- [ ] **Step 6: 补业务状态单一来源测试**

在 `TripWorkspaceNavigationStateTest` 增加或强化：

```kotlin
@Test fun timingMenuActionCreatesOnlyEditDraft()
@Test fun moveMenuActionCreatesOnlyCrossDayDraft()
@Test fun deleteMenuActionCreatesOnlyDeleteConfirmation()
```

断言每次只有对应业务状态存在，其他 draft/confirmation 为 null。

- [ ] **Step 7: 运行 GREEN**

```bash
./gradlew testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.workspace.TripWorkspaceNavigationStateTest'

./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryTimelineContentTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryEditingTest
```

- [ ] **Step 8: Graphify 与建议提交**

```bash
graphify update .
```

建议提交：`Add itinerary item action menu`。

---

### Task 3: 将排序手势限制到拖动手柄

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryItemRow.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryTimelineContentTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt`
- Modify only if selectors need stable migration: `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioExecutable.kt`

**Interfaces:**
- Consumes: Task 2 `ItineraryItemRow` 与菜单 tags。
- Produces: `ItineraryDragHandle(...)`、density-aware reorder step、仅 handle 可排序的行为契约。

- [ ] **Step 1: 写手势范围和密度失败测试**

```kotlin
@Test fun longPressOnPlaceBodyDoesNotReorder()
@Test fun longPressOnMoreDoesNotReorder()
@Test fun longPressAndDragOnHandleCommitsReorder()
@Test fun reorderStepUsesDpAtTwoXDensity()
@Test fun middleItemKeepsMoveUpAndMoveDownActions()
```

2× density 测试：

```kotlin
CompositionLocalProvider(LocalDensity provides Density(2f, 1f)) {
    // render row
}
```

在 handle 上拖动少于 `120.dp.toPx()` 时不跨项，超过时跨一项；同样手势施加到正文时不触发 preview/commit。

- [ ] **Step 2: 运行测试确认 RED**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryTimelineContentTest
```

预期：当前整行可拖，且使用裸 `120f` px。

- [ ] **Step 3: 提取拖动手柄**

```kotlin
private val ReorderStep = 120.dp

@Composable
private fun ItineraryDragHandle(
    itemId: String,
    itemName: String,
    index: Int,
    count: Int,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
)
```

实现：

- 40dp 触控区，22dp drag handle 图标。
- `contentDescription = "拖动调整 $itemName 的顺序"`。
- `val reorderStepPx = with(LocalDensity.current) { ReorderStep.toPx() }`。
- `pointerInput(itemId, count, reorderStepPx)` 只挂在 handle。
- target 继续 `coerceIn(0, count - 1)`。
- cancel 清空 preview，不 commit。

- [ ] **Step 4: 保留整行无障碍排序 action**

整行继续暴露：

```text
$itemName，第 N 项，共 M 项
上移
下移
```

这组 custom action 直接调用 `onCommit(index ± 1)`，不依赖拖动手势。

- [ ] **Step 5: 迁移现有拖动测试 selector**

将 `ItineraryEditingTest` 和场景 fixture 中从整行坐标拖动改为：

```kotlin
compose.onNodeWithTag("drag-handle-i2").performTouchInput {
    longClick()
    swipeUp(startY = bottom - 2f, endY = top - 242f, durationMillis = 500)
}
```

精确手势值以测试 density 下超过 `120.dp.toPx()` 为准，不保留裸 15%/85% 魔法比例。

- [ ] **Step 6: 运行 GREEN**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryTimelineContentTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryEditingTest
```

- [ ] **Step 7: 静态验证、Graphify 和建议提交**

```bash
./gradlew testDebugUnitTest lintDebug
graphify update .
```

建议提交：`Limit itinerary reorder to drag handles`。

---

### Task 4: 将 RouteLeg 补齐为明确四态

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryUiModels.kt:12-47`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/RouteLegRow.kt:28-97`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModel.kt` only where existing mapper constructs `RouteLegUiState`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModelTest.kt`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryMapperTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryTimelineContentTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt`

**Interfaces:**
- Consumes: domain `RouteStatus` 和现有 typed error summary / legacy errorCode fallback。
- Produces: 明确 `Ready / Calculating / WaitingForNetwork / Failed` UI 状态与共享 `RouteLegContent`。

- [ ] **Step 1: 写 JVM 四态映射失败测试**

```kotlin
@Test fun `pending and calculating route statuses map to calculating ui state`()
@Test fun `success maps to ready with distance and duration`()
@Test fun `waiting network maps to waiting state`()
@Test fun `failed prefers typed summary and falls back to legacy code`()
```

明确模型：

```kotlin
sealed interface RouteLegUiState {
    data class Ready(
        val distanceMeters: Int?,
        val durationSeconds: Int?,
    ) : RouteLegUiState
    data object Calculating : RouteLegUiState
    data object WaitingForNetwork : RouteLegUiState
    data class Failed(val message: String) : RouteLegUiState
}
```

mode 继续由 `RouteLegUi.mode` 持有，避免重复事实来源。

- [ ] **Step 2: 运行 JVM 测试确认 RED**

```bash
./gradlew testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModelTest' \
  --tests 'com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryMapperTest'
```

预期：`Calculating` 尚不存在，PENDING/CALCULATING 仍映射到 Ready。

- [ ] **Step 3: 实现四态 mapper**

映射：

```kotlin
RouteStatus.SUCCESS -> Ready(distanceMeters, durationSeconds)
RouteStatus.PENDING, RouteStatus.CALCULATING -> Calculating
RouteStatus.WAITING_NETWORK -> WaitingForNetwork
RouteStatus.FAILED -> Failed(errorSummary ?: errorCode ?: "路线计算失败")
```

保留 typed summary 优先规则。

- [ ] **Step 4: 写 RouteLeg Compose 失败测试**

```kotlin
@Test fun routeLegShowsReadyCalculatingWaitingAndFailedText()
@Test fun onlyReadyLegOpensModeEditor()
@Test fun failedRetryDispatchesOnlyCurrentLeg()
@Test fun calculatingAndWaitingExposeNeitherModeNorRetry()
@Test fun longFailureMessageIsAtMostTwoLinesWithoutFixedHeight()
```

- [ ] **Step 5: 实现共享轻量 RouteLegContent**

```kotlin
@Composable
internal fun RouteLegContent(
    leg: RouteLegUi,
    modifier: Modifier = Modifier,
    onMode: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
)
```

行为：

- Ready：整段或明确交通方式区域可点击 `onMode`；显示方式、时间、距离。
- Calculating：显示 progress 与“正在计算路线”；不显示 mode/retry。
- WaitingForNetwork：显示图标与“等待联网”；不显示 mode/retry。
- Failed：错误最多两行；仅当 `onRetry != null` 显示重试。
- 不写固定高度。
- 替换字符 `↓` 为语义清晰、清除装饰语义的连接图形或 Material icon。

- [ ] **Step 6: 运行 GREEN**

```bash
./gradlew testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModelTest' \
  --tests 'com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryMapperTest'

./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryTimelineContentTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryEditingTest
```

- [ ] **Step 7: Graphify 与建议提交**

```bash
graphify update .
```

建议提交：`Clarify itinerary route leg states`。

---

### Task 5: 建立单日时间线与统一空态

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEmptyIllustration.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/EmptyState.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItinerarySheet.kt:48-99`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryTimelineContentTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt`

**Interfaces:**
- Consumes: Tasks 1–4 的地点行、菜单、handle 和 RouteLeg。
- Produces: 单日时间线、共享 EmptyState slots、`ItineraryEmptyIllustration`。

- [ ] **Step 1: 写时间线和空态失败测试**

```kotlin
@Test fun dayTimelineInterleavesPreviewItemsAndAdjacentLegs()
@Test fun emptyDayShowsIllustrationTitleMessageAndAddAction()
@Test fun emptyDayAddDispatchesAddPlacesOnly()
@Test fun noSelectedDayDoesNotOfferImplicitDayCreation()
@Test fun longTimelineScrollsLastItemAboveBottomPadding()
```

空态测试断言：

```text
itinerary-empty-illustration
第1天 · 暂无行程
从地点池添加地点，开始安排这一天
add-places-to-selected-day
```

- [ ] **Step 2: 运行测试确认 RED**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryTimelineContentTest
```

预期：插画 tag 不存在，空态仍为普通文字块。

- [ ] **Step 3: 扩展共享 EmptyState**

```kotlin
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    illustration: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
)
```

保持旧调用兼容：新参数均有默认值。

- [ ] **Step 4: 实现简洁插画**

```kotlin
@Composable
internal fun ItineraryEmptyIllustration(
    contentDescription: String,
    modifier: Modifier = Modifier,
)
```

- 采用 72–96dp Canvas/矢量容器。
- 使用 `MaterialTheme.colorScheme.primary`、`secondary`、`surfaceVariant`。
- 所有 stroke 通过 `dp.toPx()`。
- tag：`itinerary-empty-illustration`。
- 提供 content description，不把装饰路径分别暴露到语义树。

- [ ] **Step 5: 收敛 DayItineraryContent**

- 保持 `state.previewOrder` 为唯一顺序。
- 只在相邻 item 匹配时插入 leg。
- `LazyColumn` 使用 `PaddingValues(bottom = 24.dp)`。
- 空日调用共享 `EmptyState`。
- 有 selected day 时 action 只 dispatch `AddPlaces`。
- 无 selected day 时不显示隐式创建日按钮；新增旅行日仍由外部 scope rail 负责。
- `showDialogs` 行为不变。

- [ ] **Step 6: 运行 GREEN 与编辑回归**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryTimelineContentTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryEditingTest
```

- [ ] **Step 7: 静态验证与建议提交**

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
graphify update .
```

建议提交：`Build compact day itinerary timeline`。

---

### Task 6: 收敛全程只读视图与工作台接线

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContent.kt:22-108`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WorkspaceItineraryContent.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContentTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryScopeRailTest.kt`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/workspace/TripWorkspaceNavigationStateTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt`

**Interfaces:**
- Consumes: Tasks 1、4、5 的只读地点/RouteLeg primitive 与插画。
- Produces: `WholeTripItineraryContent(days, onAddDay, modifier)` 和真实菜单→业务 overlay 回归。

- [ ] **Step 1: 写全程只读与空态失败测试**

```kotlin
@Test fun wholeTripUsesReadOnlyPlaceAndRoutePrimitives()
@Test fun wholeTripExposesNoDragMenuTimingMoveDeleteModeOrRetry()
@Test fun noTripDaysShowsIllustrationAndAddDayAction()
@Test fun emptyDayKeepsHeadingAndShowsCompactEmptyMessage()
@Test fun longWholeTripScrollsLastDayAboveBottomPadding()
```

- [ ] **Step 2: 运行测试确认 RED**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryContentTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryScopeRailTest
```

预期：无旅行日插画/action 尚不存在；全程仍未完全复用新 primitive。

- [ ] **Step 3: 扩展全程接口并复用只读 primitive**

```kotlin
@Composable
fun WholeTripItineraryContent(
    days: List<WholeTripDayUi>,
    onAddDay: () -> Unit,
    modifier: Modifier = Modifier,
)
```

- `days.isEmpty()` 使用 `EmptyState` + `ItineraryEmptyIllustration` + “新增旅行日”。
- 每天按现有 mapper 顺序渲染。
- 地点使用 `ItineraryPlaceRow`。
- 路段使用 `RouteLegContent(leg)`，不传 mode/retry。
- 空日保留 heading 和“暂无行程”。
- `LazyColumn` 底部 padding 24dp。

`WorkspaceItineraryContent` 显式传入现有 `onAddDay`。左 rail 的 add 保留，两者调用同一事件。

- [ ] **Step 4: 验证两个新增日入口映射同一 action**

在 `ItineraryScopeRailTest` 分别点击：

```text
itinerary-add-day
whole-trip-add-day
```

每次单独渲染并断言 `onAddDay` 调用一次；不要求页面只能存在一个入口。

- [ ] **Step 5: 补真实菜单与 overlay 接线测试**

在 `TripWorkspaceNavigationStateTest` / `WorkspaceFlowTest` 增加：

```kotlin
@Test fun itemMenuTimingOpensEditOverlayForSameItem()
@Test fun itemMenuMoveOpensCrossDayOverlayForSameItem()
@Test fun itemMenuDeleteOpensConfirmationBeforeMutation()
@Test fun readyLegOpensModeOverlayForSameLeg()
@Test fun failedLegRetryDoesNotOpenModeOverlay()
```

断言业务目标只来自已有 draft/editor/confirmation；关闭 Popup 本身不调用 `DismissDialogs`。

- [ ] **Step 6: 运行 GREEN 与工作台回归**

```bash
./gradlew testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.workspace.TripWorkspaceNavigationStateTest'

./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryContentTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryScopeRailTest,com.yangchengwei.easytrip.workspace.WorkspaceFlowTest
```

- [ ] **Step 7: Graphify 与建议提交**

```bash
graphify update .
```

建议提交：`Complete read-only whole-trip timeline`。

---

### Task 7: 行程内容族回归与候选验收

**Files:**
- Modify only for stable selector migration: `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioExecutable.kt`
- Create: `docs/testing/itinerary-ui-acceptance.md`
- Modify: `docs/testing/itinerary-ui-conflicts.md`
- Modify: `.superpowers/sdd/<plan-workspace>/progress.md`

**Interfaces:**
- Consumes: Tasks 1–6 最终 UI。
- Produces: focused 门禁、47 场景回归、候选验收记录和统一真机清单。

- [ ] **Step 1: 运行完整 JVM 与静态门禁**

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

记录精确 tests、failures 和 skipped；任何失败必须先处理，不能只记录。

- [ ] **Step 2: 运行行程内容族 focused 门禁**

确认设备无其他 connected/ADB 任务后串行运行：

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryEditingTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryTimelineContentTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryScopeRailTest,com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryContentTest,com.yangchengwei.easytrip.workspace.TripWorkspaceContentTest,com.yangchengwei.easytrip.workspace.WorkspaceFlowTest
```

0 tests 不算通过。基础设施失败最多重试一次并记录证据。

- [ ] **Step 3: 运行 Catalog 和 Full UI Acceptance**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1ScenarioCatalogTest

./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1FullUiAcceptanceTest
```

两者均要求 47/47、0 failed；Full UI 不允许 skipped。

- [ ] **Step 4: 仅迁移失效 selector**

若 V1 场景因新结构失败：

- 常驻 `timing/move/delete` selector 改为先点击 `more-{id}`，再点击 `menu-*`。
- 拖动 selector 改为 `drag-handle-{id}`。
- RouteLeg 状态 selector 改为稳定 tag/语义。
- 不删除原业务 action、目标 id、错误文案或只读断言。

每项迁移先确认生产行为正确，再修改测试。

- [ ] **Step 5: 写候选验收记录**

`docs/testing/itinerary-ui-acceptance.md` 包含：

```markdown
# 行程内容族 UI 候选验收

## Automated
- JVM:
- Focused Compose:
- Catalog:
- Full UI:
- lint / APK:

## Deferred physical device
- 半屏单日信息密度
- 展开长行程滚动
- 长地点名与长错误
- handle / menu 误触
- RouteLeg 四态
- 全程只读层级

## Known differences
- ...
```

真机未执行时写 `DEFERRED`，不得写 PASS。

- [ ] **Step 6: 更新冲突清单**

`docs/testing/itinerary-ui-conflicts.md` 至少记录：

- PENDING 与 CALCULATING 合并为 UI Calculating 的裁决。
- 两个新增旅行日入口调用同一 action。
- 120dp 拖动步长的真机手感待统一验收。
- 任何设计稿与生产语义冲突。

- [ ] **Step 7: 最终 Graphify 与 diff 检查**

```bash
graphify update .
git diff --check
git status --short
```

- [ ] **Step 8: 建议候选提交**

```bash
git add app/src/main app/src/test app/src/androidTest \
  docs/testing/itinerary-ui-acceptance.md \
  docs/testing/itinerary-ui-conflicts.md \
  graphify-out
git commit -m "Converge itinerary timeline UI" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

不加入 `diagrams/` 或其他用户未跟踪文件。

---

## Execution Notes

- Tasks 1–6 修改同一行程组件链，必须顺序执行。
- Task 1 建立 primitive；Task 2 菜单依赖它；Task 3 handle 依赖菜单后的最终行结构；Task 4 RouteLeg 可在 Task 3 完成后独立实现，但主执行仍保持串行以避免测试和设备竞争。
- 每个 Task 使用新的实现 Agent，并进行规格与代码质量审查。
- connected tests、模拟器和真机始终串行；先 focused，后族套件，最后 Catalog/Full UI。
- 若实现发现产品语义冲突，登记到 `docs/testing/itinerary-ui-conflicts.md`；无争议任务继续，不因一个视觉决策暂停全阶段。
