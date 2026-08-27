# Easy Trip 旅行入口与创建 UI 收敛实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将旅行列表、创建旅行、进入工作台和删除旅行收敛到 `design/easy-trip-v1.0.pen` 的 Forest Sage 视觉语言，并形成 Room 支撑的完整入口闭环。

**Architecture:** `TripListViewModel` 与 `CreateTripViewModel` 继续作为业务状态唯一来源，Composable 只渲染状态并派发 action。列表由 Room Flow 刷新，删除与创建异步结果绑定实体或请求 identity；视觉组件按页头、卡片、空态和表单字段拆分，不引入新领域模型或 Profile 路由。

**Tech Stack:** Kotlin、Jetpack Compose Material 3、Navigation Compose、Lifecycle、Coroutines/Flow、SavedStateHandle、Room、JUnit、Compose UI Test。

**Spec:** `docs/superpowers/specs/2026-08-27-easy-trip-trip-entry-creation-ui-convergence-design.md`

## Global Constraints

- 目标视觉源为 `design/easy-trip-v1.0.pen`，参考画布为 390 × 844 dp。
- 页面使用 Forest Sage 语义色；状态不能只靠颜色表达。
- 页面水平边距 20dp；主要按钮 48dp；页面级返回和菜单 44dp；卡片内辅助操作 40dp；图标视觉尺寸 20–22dp。
- 头像只作装饰，不可点击，不声明 Button 角色，不新增 Profile 路由。
- 保持“旅行名称 + 开始日期 + 天数 + 出行方式”领域模型，不改为日期范围模型。
- 不修改 Room schema，不手动在 UI state 中增删旅行卡；列表只由 Room Flow 刷新。
- 不修改工作台、搜索/地点、行程内容、权限或旅行设置日期管理。
- 所有异步状态绑定请求及实体 identity；旧 completion 不得覆盖新状态；`CancellationException` 必须重抛。
- UI 修改必须执行 Compose/设备验证；细小视觉差异不阻断批次，功能、状态、数据、崩溃、不可达、严重裁切和关键交互错误会阻断。
- 真机未执行时必须记录 `DEFERRED`，不得用模拟器替代真机结论。
- 每个任务结束执行 `graphify update .`；不得修改或提交 `diagrams/`、`.pen` 或 `.kotlin/`。

---

## 文件结构

```text
app/src/main/java/com/yangchengwei/easytrip/trip/ui/
├── TripListUiModels.kt             # 列表与删除状态
├── TripListViewModel.kt            # Room 列表流与删除状态机
├── TripListScreen.kt               # route/effect/dialog 接线
├── TripListContent.kt              # 页面骨架与状态分派
├── TripListCards.kt                # 新建：主旅行卡、其他旅行行、菜单
├── TripListStates.kt               # 新建：加载、空态、错误
├── CreateTripUiState.kt            # 创建草稿、校验和派生日期
├── CreateTripViewModel.kt          # SavedState、提交 identity、导航 effect
├── CreateTripRoute.kt              # 返回锁和 effect 收集
├── CreateTripContent.kt            # 创建页骨架、滚动与 IME
└── CreateTripFormFields.kt         # 新建：字段、日期入口、方式、提示

app/src/main/java/com/yangchengwei/easytrip/core/ui/component/
└── ConfirmationDialog.kt           # busy/dismiss/back 锁定

docs/testing/
├── trip-entry-creation-ui-acceptance.md
├── trip-entry-creation-ui-conflicts.md
└── v1-full-ui-scenario-matrix.md
```

---

### Task 1: 建立列表与删除的确定性状态机

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListUiModels.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListViewModel.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/trip/ui/TripListViewModelTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/trip/ui/TripListUiModelsTest.kt`

**Interfaces:**
- Consumes: `TripRepository.observeTrips()`、`TripService.deleteTrip(tripId)`、`DeleteImpactProvider.trip(tripId)`、`ConfirmationUiModel`。
- Produces: `TripDeletionUiState` 与 identity-safe 的删除 action/state transition，供 Task 2/3 使用。

- [ ] **Step 1: 写列表状态和异步删除 RED 测试**

新增以下测试：

```kotlin
@Test fun loadingEmptyContentAndErrorMapWithoutStaleTrips()
@Test fun newDeleteTargetIgnoresOldImpactCompletion()
@Test fun impactFailureKeepsTargetAndCanRetry()
@Test fun cancelWhileImpactLoadingInvalidatesCompletion()
@Test fun repeatedConfirmDeletesExactlyOnce()
@Test fun deleteFailureKeepsExactImpactAndRetries()
@Test fun cancellationIsRethrownForImpactAndDelete()
```

A/B 竞态测试使用两个独立 `CompletableDeferred`，并让旧请求忽略取消后迟到，确保测试验证 identity 防线而非协程取消行为。

- [ ] **Step 2: 运行测试并确认 RED**

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.trip.ui.TripListViewModelTest' \
  --tests 'com.yangchengwei.easytrip.trip.ui.TripListUiModelsTest'
```

预期：旧实现至少在 impact failure、A/B completion 竞态、取消传播或统一状态类型上失败。

- [ ] **Step 3: 定义统一删除状态**

在 `TripListUiModels.kt` 增加：

```kotlin
sealed interface TripDeletionUiState {
    data object Idle : TripDeletionUiState
    data class LoadingImpact(val tripId: String, val tripName: String) : TripDeletionUiState
    data class ImpactFailure(
        val tripId: String,
        val tripName: String,
        val message: String,
    ) : TripDeletionUiState
    data class Ready(
        val tripId: String,
        val tripName: String,
        val confirmation: ConfirmationUiModel,
        val isDeleting: Boolean = false,
        val errorMessage: String? = null,
    ) : TripDeletionUiState
}
```

统一 action：

```kotlin
data class RequestDelete(val tripId: String) : TripListAction
data object RetryDeleteImpact : TripListAction
data object ConfirmDelete : TripListAction
data object CancelDelete : TripListAction
```

用单一 `deletion: TripDeletionUiState` 替换互相可能不一致的删除字段。

- [ ] **Step 4: 实现 identity-safe 删除状态机**

- 每次请求、取消和重试递增 `deleteGeneration`。
- impact/delete completion 写状态前同时比较 generation 与 tripId。
- 用显式 `try/catch`，遇到 `CancellationException` 立即重抛。
- 删除成功只清空删除状态，等待 Room Flow 更新列表。
- `observeTrips()` 重试前取消旧 collector，保持单 collector。

- [ ] **Step 5: 运行 GREEN 和回归**

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.trip.ui.TripListViewModelTest' \
  --tests 'com.yangchengwei.easytrip.trip.ui.TripListUiModelsTest'

graphify update .
git diff --check
```

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListUiModels.kt \
  app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListViewModel.kt \
  app/src/test/java/com/yangchengwei/easytrip/trip/ui/TripListViewModelTest.kt \
  app/src/test/java/com/yangchengwei/easytrip/trip/ui/TripListUiModelsTest.kt \
  graphify-out
git commit -m "Make trip deletion state identity-safe" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: 收敛旅行列表、卡片菜单和空态

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListContent.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListCards.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListStates.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripListContentTest.kt`

**Interfaces:**
- Consumes: `TripListUiState.page`、`TripCardUiModel`、Task 1 的删除 actions。
- Produces: 装饰性页头头像、主旅行卡、其他旅行行、按 tripId 绑定的菜单、约束驱动空态。

- [ ] **Step 1: 写视觉和交互 RED 测试**

新增或替换为：

```kotlin
@Test fun profileAvatarIsDecorativeAndHasNoClickOrButtonSemantics()
@Test fun primaryTripUsesSingleEntryActionAndFortyDpMenu()
@Test fun primaryMenuContainsOnlySettingsAndDeleteForBoundTrip()
@Test fun otherTripMenuIsBoundToStableTripId()
@Test fun emptyStateUsesRemainingSpaceWithoutFixedHeight()
@Test fun longNamesAtNarrowWidthAndLargeFontKeepActionsReachable()
@Test fun errorStateKeepsCreateAndRetryActions()
```

测试要求：

- 头像可见但无 click action、无 Button role、无“个人中心”描述。
- “继续规划”高 48dp，菜单 40×40dp。
- 不再存在独立设置/删除按钮。
- 320×480 与 280dp/2× 字体下内容可滚动到达，无水平溢出。
- 错误页同时保留创建和重试。

- [ ] **Step 2: 运行测试并确认 RED**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripListContentTest
```

- [ ] **Step 3: 实现页面骨架与卡片**

- 四态共用页头、系统 inset 与 20dp 内容边界。
- 移除 `onProfile` 点击参数；头像仅绘制装饰。
- 主卡使用白色、20dp 圆角、20dp 内边距、内容驱动高度。
- 旅行名最多两行；只保留一个明确“继续规划”入口。
- 主卡和其他卡都提供 40dp `···`；菜单只有设置和危险色删除。
- 菜单状态保存 tripId，不保存列表 index/model 引用。
- 其他旅行整行进入工作台，菜单点击不得冒泡。

- [ ] **Step 4: 实现约束驱动状态组件**

- 删除空态固定 `647.dp` 和固定文案宽度。
- 使用剩余空间居中，并提供高度不足时的滚动 fallback。
- 插画约 96–120dp。
- 使用确认文案：“开始规划一次旅行”“创建旅行后，可以收藏地点并按天安排行程”“创建旅行”。
- Loading/Error 保留页头；Error 同时提供重试和创建。

- [ ] **Step 5: 运行 GREEN**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripListContentTest

./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin
graphify update .
git diff --check
```

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListContent.kt \
  app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListCards.kt \
  app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListStates.kt \
  app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripListContentTest.kt \
  graphify-out
git commit -m "Converge trip list cards and empty state" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: 完成旅行删除确认的 busy 与失败恢复

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListScreen.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/ConfirmationDialog.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/core/ui/component/ConfirmationDialogTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripFlowTest.kt`

**Interfaces:**
- Consumes: Task 1 的 `TripDeletionUiState` 与删除 actions。
- Produces: 查询、确认、提交和失败状态完整的危险确认流程。

- [ ] **Step 1: 写 dialog 与真实删除流程 RED 测试**

```kotlin
@Test fun busyDialogBlocksConfirmDismissBackAndOutsideDismiss()
@Test fun failedDialogKeepsImpactAndAllowsRetry()
@Test fun impactLoadingAndFailureKeepDeleteTargetVisible()
@Test fun tripMenuDeleteShowsExactImpactAndDeletesOnce()
```

断言：busy 时按钮禁用、Back/外部点击不关闭；失败时保留影响摘要和错误；impact 未知时不显示伪造数字；真实流程从 `···` 菜单进入。

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.core.ui.component.ConfirmationDialogTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripFlowTest#tripMenuDeleteShowsExactImpactAndDeletesOnce
```

- [ ] **Step 3: 扩展共享 ConfirmationDialog**

保持接口：

```kotlin
fun ConfirmationDialog(
    model: ConfirmationUiModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    errorMessage: String? = null,
)
```

- `busy=true` 时禁用 confirm/dismiss。
- 使用 `DialogProperties` 禁止 Back 和 outside dismiss。
- 显示“处理中…”和进度语义。
- failure 时恢复重试并显示错误。

- [ ] **Step 4: 接入 TripListScreen**

- LoadingImpact 显示目标旅行名与查询进度。
- ImpactFailure 显示“未删除旅行”、错误和重试。
- Ready/Deleting/DeleteFailure 使用同一精确 confirmation。
- 所有操作派发 ViewModel action，不访问 repository。
- 删除成功等待 Room Flow 刷新。

- [ ] **Step 5: 运行 GREEN**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.core.ui.component.ConfirmationDialogTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripFlowTest

./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.trip.ui.TripListViewModelTest'
graphify update .
git diff --check
```

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListScreen.kt \
  app/src/main/java/com/yangchengwei/easytrip/core/ui/component/ConfirmationDialog.kt \
  app/src/androidTest/java/com/yangchengwei/easytrip/core/ui/component/ConfirmationDialogTest.kt \
  app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripFlowTest.kt \
  graphify-out
git commit -m "Lock and recover trip deletion confirmation" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 4: 强化创建草稿、请求 identity 与一次性导航

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripUiState.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripViewModel.kt`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/trip/ui/CreateTripViewModelTest.kt`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/trip/ui/CreateTripValidatorTest.kt`

**Interfaces:**
- Consumes: `validateCreateTrip`、`TripService.createTrip`、`SavedStateHandle`、现有 requestId。
- Produces: inclusive `endDate` 派生值、identity-safe 提交和一次性 `OpenWorkspace(tripId)` effect。

- [ ] **Step 1: 写创建状态 RED 测试**

```kotlin
@Test fun datedDraftComputesInclusiveEndDateWithoutChangingCommand()
@Test fun clearingOrDismissingDatePickerDoesNotEraseValidDraft()
@Test fun allMutatingActionsAreIgnoredWhileSubmitting()
@Test fun cancelledSubmitDoesNotBecomeFailure()
@Test fun staleCompletionCannotClearNewDraftOrPublishNavigation()
@Test fun savedStateRestoresDraftButDoesNotAutoSubmitOrNavigate()
@Test fun failedRetryReusesRequestIdUntilCommandChanges()
```

结束日期示例：2026-10-01 + 3 天 = 2026-10-03；创建命令仍传 startDate + dayCount。

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.trip.ui.CreateTripViewModelTest' \
  --tests 'com.yangchengwei.easytrip.trip.ui.CreateTripValidatorTest'
```

- [ ] **Step 3: 实现派生日期和提交 identity**

- `endDate` 为 UiState 计算属性，不写 Room/SavedState。
- 保留 requestId 的失败原地重试语义。
- 增加 `submitGeneration`；completion 前比较 generation、requestId 和当前状态。
- `CancellationException` 重抛。
- 成功时先验证 identity，再清 SavedState，最后发一次 `OpenWorkspace`。
- 初始化恢复只恢复草稿，不自动提交或导航。
- submitting 时所有 mutating action 和 Back 都 no-op。

- [ ] **Step 4: 运行 GREEN**

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.trip.ui.CreateTripViewModelTest' \
  --tests 'com.yangchengwei.easytrip.trip.ui.CreateTripValidatorTest'

graphify update .
git diff --check
```

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripUiState.kt \
  app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripViewModel.kt \
  app/src/test/java/com/yangchengwei/easytrip/trip/ui/CreateTripViewModelTest.kt \
  app/src/test/java/com/yangchengwei/easytrip/trip/ui/CreateTripValidatorTest.kt \
  graphify-out
git commit -m "Guard trip creation state and navigation" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 5: 收敛创建页、DatePicker、IME 与返回锁

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripContent.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripFormFields.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripRoute.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/CreateTripContentTest.kt`

**Interfaces:**
- Consumes: Task 4 的 `CreateTripUiState.endDate` 与 `CreateTripAction`。
- Produces: 44dp 返回、52dp 字段、66dp 日期行、48dp 提交按钮、Forest Sage DatePicker、IME 可达表单和提交中返回锁。

- [ ] **Step 1: 写创建页面 RED 测试**

```kotlin
@Test fun usesSpecifiedFortyFourAndFortyEightDpActions()
@Test fun datedFormShowsStartAndInclusiveEndDate()
@Test fun datePickerUsesForestSageThemeAndKeepsDraftOnDismiss()
@Test fun validationErrorsHaveNearbyTextAndErrorSemantics()
@Test fun submittingLocksBackFieldsPickerModesAndSubmit()
@Test fun systemBackIsConsumedWhileSubmitting()
@Test fun narrowLargeFontAndImeKeepFocusedFieldAndSubmitReachable()
```

测试在 280dp 宽、500dp 高、fontScale 2 下验证内容可滚动；不要求固定屏幕截图像素一致。

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.CreateTripContentTest
```

- [ ] **Step 3: 拆分并实现无状态字段组件**

- `CreateTripFormFields.kt` 承载名称、日期摘要、天数、方式、Planning Tip 和 DatePicker。
- 页面使用 safe drawing inset、滚动容器与 IME padding。
- 返回 44dp，提交 48dp，字段 52dp，日期行 66dp。
- 日期行显示开始与 inclusive 结束日期；待定时不伪造结束日期。
- DatePicker 使用 `DatePickerDefaults.colors` 显式绑定 Forest Sage token。
- dismiss picker 仅关闭本地 overlay，不清空日期。
- Planning Tip 内容驱动高度，使用 11dp × 12dp padding。
- 字段错误同时设置 `isError`、附近文字和 error semantics。

- [ ] **Step 4: 实现 Route 返回与 callback freshness**

- submitting 时 `BackHandler` 消费系统 Back。
- idle 时顶部与系统 Back 都派发 `CreateTripAction.Back`。
- effect collector 使用最新 callback 引用，避免重组后调用旧 lambda。

- [ ] **Step 5: 运行 GREEN**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.CreateTripContentTest

./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.trip.ui.CreateTripViewModelTest'
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin
graphify update .
git diff --check
```

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripContent.kt \
  app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripFormFields.kt \
  app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripRoute.kt \
  app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/CreateTripContentTest.kt \
  graphify-out
git commit -m "Converge trip creation form" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 6: 验证 Room 创建、返回、重进和删除闭环

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/V1PencilFlowTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/data/RoomTripRepositoryTest.kt`

**Interfaces:**
- Consumes: Tasks 1–5 的列表、删除、创建和导航契约。
- Produces: 真实 Room 支撑的端到端入口闭环与精确导航次数证据。

- [ ] **Step 1: 扩展真实闭环 RED 测试**

```kotlin
@Test fun createBackReopenDeleteUsesRoomAndNavigatesExactlyOncePerAction()
@Test fun roomFlowReflectsDeleteWithoutManualUiMutation()
@Test fun requestIdReplayStillCreatesOneTripAndOneNavigationTarget()
```

主测试路径：

```text
Room 空列表
→ 创建旅行
→ 新旅行工作台
→ 返回列表
→ 再次进入同一旅行
→ 返回列表
→ 卡片菜单删除
→ Room Flow 刷新为空态
```

断言创建只产生一个 trip 和一次初始工作台导航；重进只增加一次对应导航；删除不增加导航。

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1PencilFlowTest#createBackReopenDeleteUsesRoomAndNavigatesExactlyOncePerAction
```

预期：旧 selector 或未完成的删除到空态流程失败。

- [ ] **Step 3: 接通 production 导航与测试选择器**

- 使用 production `AppNavigation`、in-memory Room 和真实 impact DAO。
- 创建成功继续 `popUpTo(CREATE_TRIP_ROUTE) { inclusive = true }`。
- 不清空列表 back stack。
- 删除后不 navigate list、不手动过滤列表；等待 Room Flow。
- 测试从新卡片 `···` 菜单进入删除。
- callback freshness 仅用 `rememberUpdatedState`，不改路由结构。

- [ ] **Step 4: 运行 GREEN**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1PencilFlowTest#createBackReopenDeleteUsesRoomAndNavigatesExactlyOncePerAction

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.data.RoomTripRepositoryTest

graphify update .
git diff --check
```

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt \
  app/src/androidTest/java/com/yangchengwei/easytrip/V1PencilFlowTest.kt \
  app/src/androidTest/java/com/yangchengwei/easytrip/trip/data/RoomTripRepositoryTest.kt \
  graphify-out
git commit -m "Prove Room-backed trip entry loop" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 7: 更新 V1 场景与候选验收记录

**Files:**
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioExecutable.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioFixtures.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioCatalogTest.kt`
- Modify: `docs/testing/v1-full-ui-scenario-matrix.md`
- Create: `docs/testing/trip-entry-creation-ui-acceptance.md`
- Create: `docs/testing/trip-entry-creation-ui-conflicts.md`

**Interfaces:**
- Consumes: Tasks 1–6 的稳定 tags、文案和真实闭环。
- Produces: 场景 01、01v、07、09、13、36、47 的候选证据、冲突清单与真机状态。

- [ ] **Step 1: 写/更新场景 RED 测试**

```kotlin
@Test fun tripEntryScenariosUseProductionSelectorsAndSemantics()
@Test fun tripEntryScenarioCatalogCoversConfirmedFrames()
@Test fun tripEntryExecutablesDoNotClaimProfileNavigation()
```

要求：

- 01 使用唯一“继续规划”入口。
- 01v/13 从 `···` 进入删除。
- 07 使用生产提交 tag/文案。
- 09 验证开始与结束摘要。
- 36 验证新空态文案。
- 47 验证字段附近错误。
- 精确覆盖 frame `K9h3r`、`d1sTtb`、`dzhkC`、`xQfD0`、`oW9mK`、`zIbEu`、`yIGiQ`。

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1ScenarioCatalogTest
```

- [ ] **Step 3: 迁移 executable 并保留业务断言**

- 更新生产 selectors，不删除旅行名、日期、影响摘要等业务断言。
- 场景 13 使用精确删除影响摘要。
- 头像仅断言装饰，不声明 Profile destination。
- 不增加设置日期管理、权限或工作台内部场景。

- [ ] **Step 4: 写验收与冲突文档**

`trip-entry-creation-ui-conflicts.md` 记录：

- 头像纯装饰。
- 主卡/其他卡都用菜单承载设置和删除。
- 创建保持名称+开始日期+天数+方式。
- 日期管理、权限和工作台内部 UI 排除。
- 细小间距、字体渲染和轻微圆角差异不阻断。

`trip-entry-creation-ui-acceptance.md` 记录每条命令的实际测试数、失败数、设备与日期；Mate 60 Pro 未执行时明确写 `物理设备验收：DEFERRED`。

- [ ] **Step 5: 执行最终自动化门禁**

设备测试严格串行：

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripListContentTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.CreateTripContentTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1PencilFlowTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1ScenarioCatalogTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1FullUiAcceptanceTest
```

每组必须记录测试总数；0 tests、超时、signal 9 或 runner crash 均不算 PASS。若出现环境异常，保存 XML/UTP/logcat，最多重启 AVD 一次，再按受影响用例分组串行验证。

- [ ] **Step 6: Graphify 与最终检查**

```bash
graphify update .
git diff --check
git status --short
```

确认未包含 `diagrams/`、`.pen`、`.kotlin/` 或测试产物。

- [ ] **Step 7: 提交候选**

```bash
git add app/src/main app/src/test app/src/androidTest \
  docs/testing/v1-full-ui-scenario-matrix.md \
  docs/testing/trip-entry-creation-ui-acceptance.md \
  docs/testing/trip-entry-creation-ui-conflicts.md \
  graphify-out
git commit -m "Converge trip entry and creation UI" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## 执行顺序与审查边界

```text
Task 1 删除/列表状态机
 ├─→ Task 2 列表视觉与菜单
 └─→ Task 3 删除确认集成

Task 4 创建状态机
 └─→ Task 5 创建 UI 与 Back/IME

Tasks 2 + 3 + 4 + 5
 └─→ Task 6 Room/导航真实闭环
      └─→ Task 7 V1 场景与验收文档
```

执行时可分析并行，但实现者不得并行修改同一工作树。每个任务使用新的实现 Agent，并在任务提交后进行独立规格/质量审查；最终再对完整分支做一次跨任务审查。

审查必须拒绝：

- 可点击头像、空 Profile callback 或新 Profile route。
- 同一卡片同时拥有整卡点击和同语义“继续规划”双入口。
- 用列表 index 绑定菜单或异步删除 completion。
- 删除成功时手动从 UI state 过滤旅行。
- 把领域模型改成日期范围。
- 吞掉 `CancellationException`。
- busy dialog 仍允许 Back/外部 dismiss。
- 将模拟器结果写成 Mate 60 Pro 验收。
- 修改工作台内部、权限或旅行设置日期管理。
