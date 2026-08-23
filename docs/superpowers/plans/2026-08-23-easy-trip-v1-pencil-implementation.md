# Easy Trip v1.0 Pencil Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `design/easy-trip-v1.0.pen` 的默认主流程高保真落地为可运行的 Jetpack Compose UI，并使用现有 Room、ViewModel、导航和高德地图完成“我的旅行 → 创建旅行 → 旅行工作台 → 返回 → 再次打开”的真实数据闭环。

**Architecture:** 保留现有 feature-first、ViewModel、Repository、Room 和手写 `AppContainer` 架构。每个主页面拆为负责状态收集与导航的 Route，以及只接收不可变状态和事件回调的 Content；Pencil 参数先收敛到 Compose 主题 token 和共享组件，再用于具体页面。

**Tech Stack:** Kotlin、Jetpack Compose、Material 3、Navigation Compose、ViewModel、StateFlow、SavedStateHandle、Room、高德 Android SDK、JUnit、kotlinx-coroutines-test、Compose UI Test。

**Spec:** `docs/superpowers/specs/2026-08-23-easy-trip-v1-pencil-implementation-design.md`

## Global Constraints

- 设计稿与现有 UI 或交互冲突时，以 `design/easy-trip-v1.0.pen` 为准。
- 设计稿未展示但已实现的功能必须保留，并适配 v1.0 信息架构和视觉语言。
- 第一批使用真实 Room、现有 ViewModel、Navigation Compose 和真实高德地图能力。
- 不引入 Hilt、Dagger、Koin 或其他新 DI 框架。
- 不修改 Room schema，不重写 Repository、Service 或高德 SDK 适配层。
- v1.0 没有明确深色稿时，只以浅色主题作为第一批视觉验收目标。
- 不提交 Pencil 导出的 HTML/CSS，不把固定画布坐标机械翻译成 Compose 绝对布局。
- 业务代码采用 TDD；先写失败测试，再写最小实现。
- 不主动执行 `git commit`；每个任务结束仅记录建议提交边界，由用户另行决定是否提交。
- UI 完成必须在 `easy_trip_p60pro` 模拟器实际操作；模拟器不满足条件时明确记录未完成设备验证。
- 修改代码后运行 `graphify update .`。

---

## File Structure

### 新建文件

- `docs/design/easy-trip-v1-pencil-reference.md`：记录第一批 Pencil frame、组件 ID、精确 token 和截图基线。
- `app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/Color.kt`：v1.0 浅色语义颜色。
- `app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/Type.kt`：v1.0 字体层级。
- `app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/Shape.kt`：v1.0 共享形状。
- `app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/EasyTripTokens.kt`：spacing、sizes、elevation token 和 CompositionLocal。
- `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/EasyTripButton.kt`：主、次、危险按钮唯一视觉实现。
- `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/EasyTripIconButton.kt`：设计稿图标按钮。
- `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/FeedbackState.kt`：加载、空态和持久错误容器。
- `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListUiModels.kt`：列表页面状态、卡片模型和动作。
- `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListContent.kt`：纯旅行列表 UI。
- `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripUiState.kt`：创建表单状态、动作和校验结果。
- `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripValidator.kt`：纯表单校验与日期/天数转换。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceUiModels.kt`：工作台页面状态、地图状态和动作。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`：纯工作台外壳。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceMapFallback.kt`：地图加载、授权和失败降级 UI。
- `app/src/test/java/com/yangchengwei/easytrip/trip/ui/CreateTripValidatorTest.kt`：创建表单纯逻辑测试。
- `app/src/test/java/com/yangchengwei/easytrip/trip/ui/TripListViewModelTest.kt`：列表、创建、错误和一次性导航测试。
- `app/src/test/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentStateTest.kt`：工作台 loading/not-found/error 状态测试。
- `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripListContentTest.kt`：列表纯 UI 测试。
- `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/CreateTripDialogTest.kt`：创建表单 Compose 测试。
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentTest.kt`：工作台与地图降级测试。
- `app/src/androidTest/java/com/yangchengwei/easytrip/V1PencilFlowTest.kt`：真实 Room 与导航闭环测试。

### 修改文件

- `app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/Theme.kt:6-9`：安装 v1.0 主题与扩展 token。
- `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/CompactActionButton.kt:20-82`：保留兼容 wrapper，委托统一按钮。
- `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/SelectablePill.kt:21-65`：保留选择语义，改用 v1.0 token。
- `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListScreen.kt:30-83`：收敛为 Route，并连接纯 Content。
- `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripDialog.kt:30-91`：改成 state/action API。
- `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListViewModel.kt:19-74`：显式页面状态、SavedStateHandle、校验、防重、失败恢复和成功导航。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt:69-231`：显式 loading/not-found/error 状态，移除 `filterNotNull()` 导致的永久空白。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt:44-332`：收敛为 Route，委托纯 Content。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/AmapComposeMap.kt:262-315`：将 host 创建或 render 异常上报给 UI 降级状态。
- `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt:71-182`：连接新 Route、真实创建后导航和工作台降级状态。
- `app/src/androidTest/java/com/yangchengwei/easytrip/core/ui/component/SelectablePillTest.kt`：回归 v1.0 尺寸和选择语义。
- `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripFlowTest.kt:38-115`：更新创建与导航断言。
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt:48-200`：回归工作台 section、sheet 和业务入口。
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/AmapComposeMapTest.kt:50-173`：增加 host/render 错误回调测试。
- `app/src/androidTest/java/com/yangchengwei/easytrip/V1AcceptanceTest.kt:33-101`：保留现有真实数据回归。

---

### Task 1: 冻结 Pencil 主流程基线

**Files:**
- Read: `design/easy-trip-v1.0.pen`
- Create: `docs/design/easy-trip-v1-pencil-reference.md`
- Output: `build/pencil-reference/` 下的临时 PNG，仅用于本地对照，不加入版本控制

**Interfaces:**
- Consumes: Pencil components `Qt2FO`, `PlOrR`, `E6x4E3`, `N7LYWb`。
- Produces: 第一批 frame ID、页面状态矩阵、语义 token 表和截图路径，供 Tasks 2–7 使用。

- [ ] **Step 1: 确认 Pencil 当前文件和组件**

使用 Pencil `get_app_state`，断言活动文件是：

```text
/Users/bytedance/Code/easy-trip/design/easy-trip-v1.0.pen
```

记录四个公共组件：

```text
Qt2FO  Primary Button
PlOrR  Secondary Button
E6x4E3 Filter Pill
N7LYWb Icon Button
```

- [ ] **Step 2: 读取第一批 frame 清单**

使用 Pencil `execute` 的只读 visitor，只打印顶层 frame 的 `id | name | width | height`，筛出：

```text
我的旅行：默认、空、加载、错误
创建旅行：默认、校验错误、提交中、提交失败
旅行工作台：默认、加载、旅行不存在、地图未授权、地图失败
```

若某个错误或加载 frame 不存在，在参考文档中明确写“由现有 v1.0 视觉语言组合”，不得伪造一个设计稿 frame ID。

- [ ] **Step 3: 读取精确视觉属性**

对目标 frame 和四个组件使用 `Get(id, {depth: 6, resolveVariables: true, resolveInstances: true})`，记录：

```text
颜色：background / surface / primary / secondary / error / text primary / text secondary / divider
字体：family / size / weight / lineHeight
形状：button / card / field / dialog / sheet / icon button cornerRadius
间距：页面边距 / section gap / card padding / control gap
尺寸：按钮高度 / icon size / top bar / card minimum height
阴影：卡片 / 浮层 / dialog elevation
```

- [ ] **Step 4: 输出视觉截图基线**

对每个目标 frame 调用 `TakeScreenshot` 检查视觉，并用 `Export(..., "png", "./build/pencil-reference", {scale: 2})` 输出本地基线。

- [ ] **Step 5: 写参考文档**

`docs/design/easy-trip-v1-pencil-reference.md` 必须包含：

```markdown
# Easy Trip v1.0 Pencil Reference

## Source
- File: design/easy-trip-v1.0.pen
- Primary Button: Qt2FO
- Secondary Button: PlOrR
- Filter Pill: E6x4E3
- Icon Button: N7LYWb

## Main Flow Frames
| State | Frame ID | Frame name | Size |
|---|---|---|---|

## Semantic Tokens
| Compose semantic name | Pencil source node | Resolved value |
|---|---|---|

## Responsive Notes
| Fixed-canvas behavior | Compose behavior |
|---|---|
```

表格必须填入 MCP 实际读取值，不写猜测值。

- [ ] **Step 6: 验证参考完整性**

确认每个第一批页面至少有一个主 frame，每个公共组件都有尺寸、颜色、字体、圆角和状态差异记录。若缺专属图片、插画、Logo 或字体文件，暂停并向用户列出；普通图标继续映射 Android Material Symbols。

**Checkpoint:** 只产生设计参考和本地截图，不修改业务代码，不提交。

---

### Task 2: 建立 v1.0 Theme 与共享组件

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/Color.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/Type.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/Shape.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/EasyTripTokens.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/core/ui/theme/Theme.kt:6-9`
- Create: `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/EasyTripButton.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/EasyTripIconButton.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/FeedbackState.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/CompactActionButton.kt:20-82`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/SelectablePill.kt:21-65`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/core/ui/component/SelectablePillTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/core/ui/component/EasyTripButtonTest.kt`

**Interfaces:**
- Consumes: Task 1 的语义 token 表。
- Produces: `EasyTripTheme.spacing`, `EasyTripTheme.sizes`, `EasyTripTheme.elevation`；`EasyTripPrimaryButton`、`EasyTripSecondaryButton`、`EasyTripDangerButton`、`EasyTripIconButton`、`FeedbackState`；保留现有 `Compact*` 与 `SelectablePill` 调用兼容。

- [ ] **Step 1: 写共享组件失败测试**

新增 `EasyTripButtonTest`，测试：

```kotlin
@get:Rule val compose = createComposeRule()

@Test
fun primaryButton_exposesButtonRole_andDisabledState() {
    compose.setContent {
        EasyTripTheme {
            EasyTripPrimaryButton(onClick = {}, enabled = false) { Text("创建") }
        }
    }
    compose.onNodeWithText("创建")
        .assertHasClickAction()
        .assertIsNotEnabled()
}
```

扩展 `SelectablePillTest`，断言 `Role.RadioButton`、selected/unselected 和 disabled 语义保持。

- [ ] **Step 2: 运行组件测试并确认失败**

Run:

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.core.ui.component.EasyTripButtonTest,com.yangchengwei.easytrip.core.ui.component.SelectablePillTest
```

Expected: `EasyTripPrimaryButton` 尚不存在或新视觉断言失败。

- [ ] **Step 3: 实现 token 类型**

在 `EasyTripTokens.kt` 定义稳定接口：

```kotlin
@Immutable
data class EasyTripSpacing(
    val xSmall: Dp,
    val small: Dp,
    val medium: Dp,
    val large: Dp,
    val xLarge: Dp,
)

@Immutable
data class EasyTripSizes(
    val buttonHeight: Dp,
    val iconButtonSize: Dp,
    val iconSize: Dp,
    val topBarHeight: Dp,
)

@Immutable
data class EasyTripElevation(
    val card: Dp,
    val floating: Dp,
    val dialog: Dp,
)

object EasyTripTheme {
    val spacing: EasyTripSpacing
        @Composable @ReadOnlyComposable get() = LocalEasyTripSpacing.current
    val sizes: EasyTripSizes
        @Composable @ReadOnlyComposable get() = LocalEasyTripSizes.current
    val elevation: EasyTripElevation
        @Composable @ReadOnlyComposable get() = LocalEasyTripElevation.current
}
```

所有实际 `Dp` 值使用 Task 1 参考表，不引入未在主流程重复出现的 token。

- [ ] **Step 4: 安装浅色 Material 主题**

在 `Color.kt`、`Type.kt`、`Shape.kt` 中将 Task 1 的语义值映射为 `lightColorScheme`、`Typography`、`Shapes`。修改 `Theme.kt`：

```kotlin
@Composable
fun EasyTripTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalEasyTripSpacing provides easyTripSpacing,
        LocalEasyTripSizes provides easyTripSizes,
        LocalEasyTripElevation provides easyTripElevation,
    ) {
        MaterialTheme(
            colorScheme = easyTripLightColorScheme,
            typography = easyTripTypography,
            shapes = easyTripShapes,
            content = content,
        )
    }
}
```

- [ ] **Step 5: 实现统一按钮与图标按钮**

`EasyTripButton.kt` 对外提供：

```kotlin
@Composable fun EasyTripPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
)

@Composable fun EasyTripSecondaryButton(...)
@Composable fun EasyTripDangerButton(...)
```

`CompactPrimaryButton`、`CompactSecondaryButton`、`CompactDangerButton` 保持现有签名，内部委托对应 `EasyTrip*Button`。`SelectablePill` 保留现有 API 和 `selectable` 语义，只替换尺寸、颜色和形状来源。

- [ ] **Step 6: 实现反馈状态容器**

```kotlin
enum class FeedbackKind { LOADING, EMPTY, ERROR }

@Composable
fun FeedbackState(
    kind: FeedbackKind,
    title: String,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

错误态有持久重试按钮；loading 使用进度语义；empty 不伪装成错误。

- [ ] **Step 7: 运行组件与现有调用回归**

Run:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Expected: PASS，现有 `TripSettingsScreen`、地点和行程调用无需批量改名即可编译。

**Checkpoint:** Theme 和公共组件可独立合入；不提交。

---

### Task 3: 拆分并实现“我的旅行”页面

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListUiModels.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListScreen.kt:30-83`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListViewModel.kt:20-43`
- Create: `app/src/test/java/com/yangchengwei/easytrip/trip/ui/TripListViewModelTest.kt`
- Create: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripListContentTest.kt`

**Interfaces:**
- Consumes: Task 2 主题、按钮和反馈组件；现有 `TripSummary`。
- Produces: `TripListPageState`、`TripCardUiModel`、`TripListAction`、`TripListContent`；`TripListScreen` 作为 Route 保持 `AppNavigation` 可调用。

- [ ] **Step 1: 写页面状态失败测试**

在 `TripListViewModelTest` 使用 fake repository 的可控 Flow，验证：

```kotlin
@Test
fun startsLoading_thenShowsEmpty() = runTest {
    val repository = FakeTripRepository(MutableSharedFlow())
    val viewModel = TripListViewModel(service, repository, impacts, SavedStateHandle())
    assertEquals(TripListPageState.Loading, viewModel.state.value.page)

    repository.emitTrips(emptyList())
    assertEquals(TripListPageState.Empty, viewModel.state.value.page)
}

@Test
fun repositoryFailure_showsRetryableError() = runTest {
    repository.fail(RuntimeException("db unavailable"))
    assertEquals("无法加载旅行", (viewModel.state.value.page as TripListPageState.Error).message)
}
```

- [ ] **Step 2: 运行 ViewModel 测试并确认失败**

Run:

```bash
./gradlew testDebugUnitTest --tests '*TripListViewModelTest'
```

Expected: FAIL，因为 `TripListPageState` 和 `SavedStateHandle` 构造参数尚不存在。

- [ ] **Step 3: 定义列表 UI 模型**

```kotlin
sealed interface TripListPageState {
    data object Loading : TripListPageState
    data object Empty : TripListPageState
    data class Content(val trips: List<TripCardUiModel>) : TripListPageState
    data class Error(val message: String) : TripListPageState
}

data class TripCardUiModel(
    val id: String,
    val name: String,
    val dayCountLabel: String,
    val dateLabel: String?,
    val travelModeLabel: String,
)

sealed interface TripListAction {
    data object CreateTrip : TripListAction
    data object Retry : TripListAction
    data class OpenTrip(val tripId: String) : TripListAction
    data class OpenSettings(val tripId: String) : TripListAction
    data class RequestDelete(val tripId: String) : TripListAction
}
```

提供 `TripSummary.toTripCardUiModel()`，格式化只依赖领域字段，不访问 Android Context。

- [ ] **Step 4: 实现显式加载与错误状态**

将 `TripListUiState.trips` 替换或包裹为 `page: TripListPageState = Loading`。`observeTrips()` 使用 `catch` 转为 `Error("无法加载旅行")`，`retry()` 重新启动一次观察 job，且始终只有一个活动 collector。

- [ ] **Step 5: 写 Content 失败测试**

覆盖四态和动作：

```kotlin
@Test fun emptyState_opensCreate() { /* 点击“创建旅行”，断言 TripListAction.CreateTrip */ }
@Test fun content_clicksTripAndSettingsWithoutDuplicateParentClick() { /* 各动作恰好一次 */ }
@Test fun errorState_retries() { /* 点击“重试”，断言 Retry */ }
@Test fun tripCard_hasReadableAccessibilityLabels() { /* 卡片、设置、删除语义 */ }
```

- [ ] **Step 6: 实现 `TripListContent`**

```kotlin
@Composable
fun TripListContent(
    state: TripListUiState,
    onAction: (TripListAction) -> Unit,
    modifier: Modifier = Modifier,
)
```

使用 `Scaffold`、v1.0 顶部区域、`LazyColumn` 和 Task 2 组件。旅行卡片只有一个主点击节点；设置与删除是独立按钮，不在嵌套 clickable 中重复触发。

- [ ] **Step 7: 将 `TripListScreen` 收敛为 Route**

保留生命周期感知的 navigation collector，将 state/action 连接到 ViewModel。创建和删除弹窗仍由 Route 组合，但只把纯状态和回调传入 UI。

- [ ] **Step 8: 运行定向测试**

Run:

```bash
./gradlew testDebugUnitTest --tests '*TripListViewModelTest'
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripListContentTest
```

Expected: PASS。

**Checkpoint:** 旅行列表四态和动作可独立测试；不提交。

---

### Task 4: 实现创建旅行状态机与真实提交

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripUiState.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripValidator.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripDialog.kt:30-91`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListViewModel.kt:19-74`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt:71-79`
- Test: `app/src/test/java/com/yangchengwei/easytrip/trip/ui/CreateTripValidatorTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/trip/ui/TripListViewModelTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/CreateTripDialogTest.kt`
- Modify Test: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripFlowTest.kt:65-115`

**Interfaces:**
- Consumes: `TripService.createTrip(CreateTrip): String`、`TripService.setStartDate(String, LocalDate?)`、Task 3 Route/Content。
- Produces: `CreateTripUiState`、`CreateTripAction`、`validateCreateTrip()`；成功发送一次 `TripListNavigation.OpenWorkspace(id)`。

- [ ] **Step 1: 写校验失败测试**

```kotlin
@Test fun blankName_returnsNameError()
@Test fun dayCountBelowOne_returnsDayCountError()
@Test fun datedTripWithoutStartDate_returnsDateError()
@Test fun validDraft_returnsCommandWithoutStartDate()
@Test fun validDatedTrip_returnsCommandAndStartDate()
```

核心断言：

```kotlin
val result = validateCreateTrip(
    CreateTripUiState(name = "东京", dayCount = "3", timeMode = CreateTimeMode.DATED, startDate = LocalDate.of(2026, 10, 1))
)
assertEquals(CreateTrip("东京", 3, TravelMode.FLEXIBLE), result.command)
assertEquals(LocalDate.of(2026, 10, 1), result.startDate)
```

- [ ] **Step 2: 运行校验测试并确认失败**

Run:

```bash
./gradlew testDebugUnitTest --tests '*CreateTripValidatorTest'
```

Expected: FAIL，因为状态和 validator 尚不存在。

- [ ] **Step 3: 定义创建状态和动作**

```kotlin
data class CreateTripUiState(
    val visible: Boolean = false,
    val name: String = "",
    val dayCount: String = "",
    val timeMode: CreateTimeMode = CreateTimeMode.DRAFT,
    val startDate: LocalDate? = null,
    val travelMode: TravelMode = TravelMode.FLEXIBLE,
    val nameError: String? = null,
    val dayCountError: String? = null,
    val dateError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
)

sealed interface CreateTripAction {
    data object Dismiss : CreateTripAction
    data class NameChanged(val value: String) : CreateTripAction
    data class DayCountChanged(val value: String) : CreateTripAction
    data class TimeModeChanged(val value: CreateTimeMode) : CreateTripAction
    data class StartDateChanged(val value: LocalDate) : CreateTripAction
    data class TravelModeChanged(val value: TravelMode) : CreateTripAction
    data object Submit : CreateTripAction
}
```

- [ ] **Step 4: 实现纯 validator**

`validateCreateTrip(state)` 返回字段错误或：

```kotlin
data class ValidCreateTrip(
    val command: CreateTrip,
    val startDate: LocalDate?,
)
```

现有领域模型使用“开始日期 + 天数”，因此 UI 不新增结束日期数据库字段；若 v1.0 展示结束日期，只在 UI 中以 `startDate.plusDays(dayCount - 1)` 派生。

- [ ] **Step 5: 写 ViewModel 提交失败测试**

覆盖：

```kotlin
@Test fun duplicateSubmit_callsServiceOnce()
@Test fun createFailure_preservesInputAndShowsError()
@Test fun createSuccess_setsStartDateAndEmitsWorkspaceOnce()
@Test fun savedStateHandle_restoresDraftFieldsButNotSubmitting()
```

成功测试应收集一个事件后确认没有第二个：

```kotlin
assertEquals(TripListNavigation.OpenWorkspace("trip-1"), navigation.receive())
assertNull(navigation.tryReceive().getOrNull())
```

- [ ] **Step 6: 实现 SavedStateHandle 与提交状态机**

修改 ViewModel 构造器：

```kotlin
class TripListViewModel(
    private val service: TripService,
    private val repository: TripRepository,
    private val impacts: DeleteImpactProvider,
    private val savedState: SavedStateHandle,
) : ViewModel()
```

Factory 使用 `CreationExtras.createSavedStateHandle()`。持久化 name、dayCount、timeMode、startDate、travelMode；`isSubmitting` 始终以 `false` 初始化。提交先校验，再原子设置 `isSubmitting = true`，成功后清理表单并发送 `OpenWorkspace(id)`，失败后保留字段、设置 `submitError` 并恢复按钮。

- [ ] **Step 7: 将 CreateTripDialog 改为纯 UI API**

```kotlin
@Composable
fun CreateTripDialog(
    state: CreateTripUiState,
    onAction: (CreateTripAction) -> Unit,
    initialDateMillis: Long? = null,
)
```

字段展示 `isError` 与 supporting text；提交中按钮禁用并显示明确进度；日期 picker 关闭时不意外清空已选日期。

- [ ] **Step 8: 更新 Route 与导航连接**

`TripListScreen` 把 `CreateTripAction` 转给 ViewModel。`AppNavigation` 继续使用现有 navigation collector；成功事件直接导航到 `trips/$id`，不得由 UI state 布尔值触发。

- [ ] **Step 9: 运行创建流程测试**

Run:

```bash
./gradlew testDebugUnitTest --tests '*CreateTripValidatorTest' --tests '*TripListViewModelTest'
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.CreateTripDialogTest,com.yangchengwei.easytrip.trip.ui.TripFlowTest
```

Expected: PASS。

**Checkpoint:** 创建表单可恢复、可失败重试、不可重复提交，成功进入真实 tripId；不提交。

---

### Task 5: 拆分工作台并实现地图降级

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceUiModels.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceMapFallback.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt:69-231`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt:44-332`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/AmapComposeMap.kt:262-315`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt:80-182`
- Test: `app/src/test/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentStateTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentTest.kt`
- Modify Test: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/AmapComposeMapTest.kt:50-173`
- Modify Test: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt:48-200`

**Interfaces:**
- Consumes: `TripWorkspaceUiState` 中现有 section、scope、map、sheet、marker 数据；现有 `AmapComposeMap` 和 slot 内容。
- Produces: `TripWorkspacePageState`、`WorkspaceMapState`、`TripWorkspaceAction`、`TripWorkspaceContent`；地图失败不会阻断本地工作台。

- [ ] **Step 1: 写工作台页面状态失败测试**

```kotlin
@Test fun initialState_isLoading()
@Test fun nullTrip_becomesNotFound()
@Test fun repositoryFailure_becomesPageError()
@Test fun mapFailure_doesNotReplaceReadyWorkspaceContent()
```

要求 `observeTrip(tripId)` 发出 `null` 后得到 `NotFound`，不再因 `filterNotNull()` 永久停留默认空白。

- [ ] **Step 2: 运行状态测试并确认失败**

Run:

```bash
./gradlew testDebugUnitTest --tests '*TripWorkspaceContentStateTest'
```

Expected: FAIL，因为显式页面状态尚不存在。

- [ ] **Step 3: 定义页面、地图与动作模型**

```kotlin
sealed interface TripWorkspacePageState {
    data object Loading : TripWorkspacePageState
    data object NotFound : TripWorkspacePageState
    data class Error(val message: String) : TripWorkspacePageState
    data class Ready(val content: TripWorkspaceReadyState) : TripWorkspacePageState
}

sealed interface WorkspaceMapState {
    data object Loading : WorkspaceMapState
    data object Ready : WorkspaceMapState
    data object ConsentRequired : WorkspaceMapState
    data class Failed(val message: String) : WorkspaceMapState
}

sealed interface TripWorkspaceAction {
    data object Back : TripWorkspaceAction
    data object OpenSettings : TripWorkspaceAction
    data object OpenPrivacySettings : TripWorkspaceAction
    data object OpenSearch : TripWorkspaceAction
    data object Retry : TripWorkspaceAction
    data class SelectSection(val section: WorkspaceSection) : TripWorkspaceAction
    data class SelectItineraryScope(val scope: ItineraryScope) : TripWorkspaceAction
    data class SelectMapLayer(val layer: MapLayer) : TripWorkspaceAction
    data class SetSheetLevel(val level: WorkspaceSheetLevel) : TripWorkspaceAction
}
```

`TripWorkspaceReadyState` 承载当前 `TripWorkspaceUiState` 已有可渲染字段，避免页面同时检查空字符串和空列表推断加载状态。

- [ ] **Step 4: 修正 ViewModel 的 trip 观察**

移除：

```kotlin
trips.observeTrip(tripId).filterNotNull()
```

改为显式处理：

```text
订阅开始 → Loading
发出 null → NotFound
发出 TripWithDays → 观察 snapshots 并产生 Ready
上游异常 → Error("无法加载旅行")
```

不得把地图授权或地图渲染错误转成页面级 `Error`。

- [ ] **Step 5: 写工作台纯 UI 失败测试**

覆盖：

```kotlin
@Test fun notFound_showsBackToTripsAction()
@Test fun consentRequired_keepsSectionControlsAndLocalContent()
@Test fun mapFailure_showsPersistentFallbackAndRetry()
@Test fun ready_keepsSearchSettingsBackPlacePoolAndItineraryActions()
@Test fun sectionTabs_keepExclusiveSelectionSemantics()
```

- [ ] **Step 6: 实现地图降级容器**

```kotlin
@Composable
fun WorkspaceMapFallback(
    state: WorkspaceMapState,
    onPrivacySettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
)
```

授权缺失显示隐私设置入口；初始化/render 失败显示持久错误和重试；loading 使用进度语义。该容器只替换地图画布，不替换顶部栏、section、sheet 或本地内容。

- [ ] **Step 7: 为 AmapComposeMap 增加通用错误回调**

保持现有可注入 `hostFactory`，新增：

```kotlin
onMapError: (Throwable) -> Unit = {}
```

在 host 创建及 render 边界捕获异常并调用一次回调；不得吞掉图层错误的 retained layer 逻辑。扩展 `AmapComposeMapTest`，用 throwing fake host 分别验证创建失败和 render 失败。

- [ ] **Step 8: 实现 TripWorkspaceContent**

```kotlin
@Composable
fun TripWorkspaceContent(
    pageState: TripWorkspacePageState,
    mapState: WorkspaceMapState,
    onAction: (TripWorkspaceAction) -> Unit,
    placeContent: @Composable () -> Unit,
    dayItineraryContent: @Composable () -> Unit,
    mapContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
)
```

把现有顶部栏、地图 overlay、图层菜单、搜索入口、section controls、sheet handle 和 slots 移入 Content，并用 Task 2 token 替换散落的重复尺寸。保留 sheet 三态和 `WorkspaceItineraryContent` 行为。

- [ ] **Step 9: 将 TripWorkspaceScreen 收敛为 Route**

Route 负责：

- `collectAsStateWithLifecycle()`；
- 管理 map runtime 状态；
- 将 Content 动作连接到 ViewModel 与导航回调；
- 构建 `AmapComposeMap` slot；
- 保留 marker/POI dialog 的业务连接。

- [ ] **Step 10: 运行工作台测试**

Run:

```bash
./gradlew testDebugUnitTest --tests '*TripWorkspaceContentStateTest'
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.TripWorkspaceContentTest,com.yangchengwei.easytrip.workspace.AmapComposeMapTest,com.yangchengwei.easytrip.workspace.WorkspaceFlowTest
```

Expected: PASS；未授权和 fake map host 失败时，地点池、行程、设置和返回仍可访问。

**Checkpoint:** 工作台具有明确页面状态和独立地图降级；不提交。

---

### Task 6: 完成真实 Room 与导航闭环

**Files:**
- Create: `app/src/androidTest/java/com/yangchengwei/easytrip/V1PencilFlowTest.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt:61-182`
- Modify Test: `app/src/androidTest/java/com/yangchengwei/easytrip/V1AcceptanceTest.kt:33-101`
- Reference Test: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/data/RoomTripRepositoryTest.kt`

**Interfaces:**
- Consumes: Tasks 3–5 的 Route/Content、`TripListNavigation.OpenWorkspace(id)`、现有 `AppContainer` 与 Room repository。
- Produces: 使用真实或内存 Room 的端到端主流程测试，以及可重复进入同一 tripId 的导航行为。

- [ ] **Step 1: 写端到端失败测试**

`V1PencilFlowTest` 使用测试 Room 和可注入的 navigation composition root，验证：

```text
初始空列表
→ 点击创建旅行
→ 输入名称、天数并选择日期
→ 点击提交
→ Room 中出现旅行及旅行日
→ 当前 route 为 trips/{新 id}
→ 返回列表
→ 列表展示新旅行
→ 点击同一旅行
→ 再次进入 trips/{同一 id}
```

同时记录导航次数：创建事件只产生一次 `navigate("trips/$id")`。

- [ ] **Step 2: 运行闭环测试并确认失败**

Run:

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1PencilFlowTest
```

Expected: FAIL，直到依赖可注入入口和新 UI test tags 连接完成。

- [ ] **Step 3: 提取最小可测试导航入口**

保持生产 `AppNavigation()` 不变；为已有重载补一个只包含现有依赖的参数对象或明确参数，使测试可以传入内存 Room repository、service 和工作台依赖。不得引入 DI 框架，也不得为测试复制另一套路由表。

- [ ] **Step 4: 连接生产依赖**

生产入口仍从 `EasyTripApplication` 和 `AppContainer` 获取：

```text
TripService
TripRepository
DeleteImpactProvider
SavedPlaceRepository
ItineraryRepository
RouteLegRepository
MapPreferences
AmapConsentToken
```

所有 route 继续共享现有 Repository 实例，创建后列表更新来自 Room Flow，而不是手动把新旅行追加到 UI 列表。

- [ ] **Step 5: 运行闭环和既有 acceptance**

Run:

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1PencilFlowTest,com.yangchengwei.easytrip.V1AcceptanceTest
```

Expected: PASS。

**Checkpoint:** 默认主流程使用真实 Room 和正式导航闭环；不提交。

---

### Task 7: 全量回归、模拟器操作与视觉验收

**Files:**
- Modify if needed: 第一批 UI 和测试文件
- Update: `docs/design/easy-trip-v1-pencil-reference.md`，添加模拟器对照结果
- Update generated: `graphify-out/`

**Interfaces:**
- Consumes: Tasks 1–6 全部交付。
- Produces: 全量验证记录、视觉差异修复和最新知识图谱。

- [ ] **Step 1: 运行 JVM、lint 和构建**

Run:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleDebugAndroidTest
```

Expected: 全部 PASS。任何失败先按 `superpowers:systematic-debugging` 定位，不跳过测试、不使用 `--no-verify`。

- [ ] **Step 2: 严格确认目标模拟器**

Run:

```bash
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
AVD_NAME="$($ADB -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')"
printf '%s\n' "$AVD_NAME"
test "$AVD_NAME" = "easy_trip_p60pro"
```

Expected: 输出且严格等于 `easy_trip_p60pro`。否则停止 instrumentation，并记录“未完成设备验证”。

- [ ] **Step 3: 运行全量 instrumentation**

Run:

```bash
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest
```

Expected: PASS。

- [ ] **Step 4: 安装并启动正式 debug APK**

Run:

```bash
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
$ADB -s "$ANDROID_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
$ADB -s "$ANDROID_SERIAL" shell am start -n com.yangchengwei.easytrip/.MainActivity
```

- [ ] **Step 5: 实际操作默认主流程**

依次验证并截图：

1. 我的旅行空态和创建入口；
2. 创建表单、软键盘滚动、名称/天数/日期错误；
3. 提交中不可重复点击；
4. 创建成功直接进入正确工作台；
5. 工作台返回后新旅行仍在列表；
6. 再次点击进入同一旅行；
7. 拒绝地图授权后地点池、行程、设置和返回仍可用；
8. fake host 测试已覆盖地图失败的持久降级；
9. 字体缩放和 TalkBack 下关键动作可理解。

- [ ] **Step 6: 逐页视觉对照**

将模拟器截图与 Task 1 Pencil PNG 并排检查：

```text
信息层级
信息顺序
背景与语义颜色
字体 family/size/weight/lineHeight
页面边距与 section gap
按钮、Pill、卡片、字段、弹窗和浮层尺寸
圆角和阴影
状态栏、导航栏和软键盘安全区
```

仅接受由响应式、安全区、字体缩放或系统组件约束导致的差异。发现遗漏时直接修正现有 Compose 节点，不修改 v1.0 设计稿。

- [ ] **Step 7: 运行最终验证**

Run:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
git diff --check
graphify update .
```

若目标模拟器可用，再运行：

```bash
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest
```

Expected: 所有已执行命令 PASS，`git diff --check` 无输出，Graphify 成功更新。

- [ ] **Step 8: 输出交付报告**

报告必须列出：

```text
已实现页面与状态
保留的既有能力
执行过的测试及结果
未执行的测试及原因
Pencil 与模拟器的已知响应式差异
是否在 easy_trip_p60pro 完成人工操作
是否发现需要用户提供的专属素材
```

**Checkpoint:** 第一批具备可运行、可测试、可视觉验收的完整闭环；不提交。
