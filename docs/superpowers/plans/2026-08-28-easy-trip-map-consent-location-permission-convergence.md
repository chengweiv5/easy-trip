# Easy Trip 地图授权与定位权限收敛实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将地图服务授权和 Android 定位权限分别收敛为单一权威状态流，并实现撤回授权即时降级、永久拒绝页内恢复及系统设置返回后的一次性定位。

**Architecture:** `AmapConsentStore` 统一持久化决定、高德隐私 API、token generation 和可观察状态；`LocationPermissionCoordinator` 保持纯逻辑状态机，通过带 generation 的 effect 与 Route/Navigation 协调 Android launcher、系统设置和生命周期。工作台、搜索和路线只消费当前 consent generation，Room 旅行、地点池和行程始终独立。

**Tech Stack:** Kotlin、Jetpack Compose、Navigation Compose、StateFlow、Coroutines、Activity Result API、Lifecycle、SharedPreferences、JUnit、kotlinx-coroutines-test、Compose instrumentation、Android emulator、Graphify。

**Spec:** `docs/superpowers/specs/2026-08-28-easy-trip-map-consent-location-permission-convergence-design.md`

## Global Constraints

- 地图服务授权是全应用持久化决定，同意和拒绝都跨旅行、跨页面和冷启动保持。
- 地图服务授权与 Android 定位权限分别建模，不引入通用 permission engine。
- 尚未决定时只在首次进入旅行工作台自动展示说明；拒绝后只能由用户主动重新打开。
- 定位权限只在用户点击定位后请求，系统 launcher 确实发起后才记录“曾请求”。
- 大致或精确位置任一获批即可定位，不追问精确权限。
- 永久拒绝使用地图区域内联提示，不反复显示阻断式弹窗。
- 只有由“前往系统设置”发起的返回才自动复检；授权后最多恢复一次原定位请求。
- 撤回地图授权立即停用地图、路线和远程地点搜索，但不删除或阻断 Room 旅行、地点池和行程。
- 地图未授权、地图加载失败、定位永久拒绝按依赖顺序显示各自恢复动作。
- 不重做地图视觉，不处理 395/396dp Sheet 锚点，不扩展路线领域模型。
- 生产行为变更严格执行 RED → GREEN；每个新测试必须先观察到因目标行为缺失而失败。
- Connected tests 只在单一 emulator 上全局互斥并串行执行；基础设施异常最多重试一次。
- 不清除用户物理设备上的现有旅行数据；物理设备状态准备必须使用专用测试安装/fixture，或另行取得明确授权。
- 每个任务完成后运行 `graphify update .`；Graphify 既有解析警告不在本批顺手修复。
- 执行期间不主动 commit；只有用户在执行阶段明确授权时，才按任务 checkpoint 提交。

---

## 文件结构与串行依赖

| Task | 产物 | 依赖 |
|---|---|---|
| 1 | `AmapConsentStore`、持久化与 token generation | 无 |
| 2 | Application/Container/路线统一消费 consent state | Task 1 |
| 3 | 纯逻辑定位权限状态机 | Task 1 |
| 4 | Activity Result、系统设置和 lifecycle 接线 | Task 3 |
| 5 | Workspace 地图状态优先级与降级 UI | Tasks 1、2、3、4 |
| 6 | 地点搜索随 consent generation 收敛 | Tasks 1、2、5 |
| 7 | 全量门禁、物理设备验收与验收记录 | Tasks 1–6 |

实施严格按 Task 1 → 2 → 3 → 4 → 5 → 6 → 7 串行。Task 2 与 Task 3 虽可分别编码，但都修改应用装配接口，不并行实施，避免 `AppNavigation` 和依赖构造冲突。

---

### Task 1：建立唯一 `AmapConsentStore`

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/amap/AmapConsentStore.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/amap/AmapPrivacyGate.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/EasyTripApplication.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/amap/AmapConsentStoreTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/amap/AmapPrivacyStateMachineTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/amap/ConsentRevocationTest.kt`

**Interfaces:**

```kotlin
sealed interface AmapConsentFact {
    val generation: Long

    data class Undecided(override val generation: Long) : AmapConsentFact
    data class Accepted(
        override val generation: Long,
        val token: AmapConsentToken,
    ) : AmapConsentFact
    data class Declined(override val generation: Long) : AmapConsentFact
}

data class AmapConsentState(
    val fact: AmapConsentFact,
    val updating: Boolean = false,
    val error: String? = null,
)

interface AmapConsentPersistence {
    fun readDecision(): Boolean?
    fun writeDecision(accepted: Boolean)
}

interface AmapPrivacyReporter {
    suspend fun reportShown()
    suspend fun reportDecision(accepted: Boolean)
}

class AmapConsentStore(
    private val persistence: AmapConsentPersistence,
    private val reporter: AmapPrivacyReporter,
    private val registry: ConsentRegistry,
) {
    val state: StateFlow<AmapConsentState>
    suspend fun reportShown(): Result<Unit>
    suspend fun decide(accepted: Boolean)
}
```

- `AmapConsentFact` 是所有业务调用方的唯一地图授权事实。
- `generation` 每次 `decide()` 发起时递增；旧完成必须在写持久化、token 和 state 前再次校验 generation。
- `AmapPrivacyGate` 实现 `AmapPrivacyReporter`，只包装高德隐私 API，不持有业务决定。
- SharedPreferences 实现放在 `AmapConsentStore.kt`，key 继续兼容现有 `privacy/amap.accepted`。
- `ConsentRegistry` 只负责 token 激活与失效，不对外代表业务决定。

- [ ] **Step 1: 写首次状态与持久化 RED**

在 `AmapConsentStoreTest` 使用内存 persistence、可控 reporter 和真实 `ConsentRegistry`，加入：

```kotlin
@Test
fun initialStateWithoutPersistedDecisionIsUndecided() {
    val store = createStore(decision = null)
    assertThat(store.state.value.fact).isInstanceOf(AmapConsentFact.Undecided::class.java)
}

@Test
fun acceptedAndDeclinedDecisionsRestoreAcrossStoreInstances() = runTest {
    val persistence = MemoryConsentPersistence()
    createStore(persistence).decide(true)
    assertThat(createStore(persistence).state.value.fact)
        .isInstanceOf(AmapConsentFact.Accepted::class.java)

    createStore(persistence).decide(false)
    assertThat(createStore(persistence).state.value.fact)
        .isInstanceOf(AmapConsentFact.Declined::class.java)
}
```

- [ ] **Step 2: 运行 RED**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.amap.AmapConsentStoreTest.initialStateWithoutPersistedDecisionIsUndecided' \
  --tests 'com.yangchengwei.easytrip.amap.AmapConsentStoreTest.acceptedAndDeclinedDecisionsRestoreAcrossStoreInstances'
```

Expected: 编译失败，因为 `AmapConsentStore` 和状态类型尚不存在。

- [ ] **Step 3: 实现最小状态、持久化兼容和冷启动恢复**

实现 `AmapConsentStore` 初始读取规则：缺 key → `Undecided(0)`；`true` → 通过 registry 签发当前 active token 后发布 `Accepted`；`false` → registry inactive 并发布 `Declined`。不得通过调用 UI 层回放决定。

- [ ] **Step 4: 运行 GREEN**

Run: Step 2 同一命令。

Expected: 2 tests PASS。

- [ ] **Step 5: 写 token 撤回、失败保持和 stale generation RED**

加入：

```kotlin
@Test
fun withdrawalInvalidatesPreviouslyIssuedToken() = runTest {
    val store = createStore()
    store.decide(true)
    val token = (store.state.value.fact as AmapConsentFact.Accepted).token

    store.decide(false)

    assertThat(token.isActive()).isFalse()
    assertThat(store.state.value.fact).isInstanceOf(AmapConsentFact.Declined::class.java)
}

@Test
fun privacyApiFailureKeepsPreviousConsentFact() = runTest {
    val reporter = ControllablePrivacyReporter()
    val store = createStore(reporter = reporter)
    store.decide(true)
    val accepted = store.state.value.fact
    reporter.failNext(IllegalStateException("privacy update failed"))

    store.decide(false)

    assertThat(store.state.value.fact).isEqualTo(accepted)
    assertThat(store.state.value.error).isEqualTo("地图授权更新失败，请重试")
}

@Test
fun staleGenerationCompletionCannotOverwriteNewerDecision() = runTest {
    val reporter = ControllablePrivacyReporter(pauseDecisions = true)
    val store = createStore(reporter = reporter)
    val accept = async { store.decide(true) }
    reporter.awaitDecision(true)
    val decline = async { store.decide(false) }
    reporter.completeDecision(false)
    decline.await()
    reporter.completeDecision(true)
    accept.await()

    assertThat(store.state.value.fact).isInstanceOf(AmapConsentFact.Declined::class.java)
}
```

- [ ] **Step 6: 运行 RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.amap.AmapConsentStoreTest'
```

Expected: 撤回、异常或 stale completion 至少一项失败，证明旧 registry/直接 gate 行为不能满足 store 契约。

- [ ] **Step 7: 实现 generation、更新失败和 token 注销**

`decide()` 顺序固定为：生成 request generation → state 标记 updating → 调 reporter → 校验仍为当前 generation → persistence 写入 → registry 决定并生成/注销 token → 发布事实。异常只在当前 generation 写 `error`，保持旧 fact；`CancellationException` 继续抛出。

- [ ] **Step 8: 收敛 `EasyTripApplication`**

删除 `amapConsentToken`、`amapPrivacyShown`、`amapPrivacyDecided` 三个业务变量以及直接 `decideAmapPrivacy()` 状态同步。Application 创建并公开：

```kotlin
lateinit var amapConsentStore: AmapConsentStore
    private set
```

冷启动只创建 store；不在 Application 中维护第二份决定。

- [ ] **Step 9: 运行 Task 1 GREEN 与回归**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.amap.AmapConsentStoreTest' \
  --tests 'com.yangchengwei.easytrip.amap.AmapPrivacyStateMachineTest' \
  --tests 'com.yangchengwei.easytrip.amap.ConsentRevocationTest'
```

Expected: 全部 PASS，0 skipped。

- [ ] **Step 10: 更新 Graphify 并检查差异**

```bash
graphify update .
git diff --check
```

Expected: 两条命令退出码 0；仅允许既有 Graphify 解析警告。

---

### Task 2：让应用装配与路线能力统一消费 consent state

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppContainer.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/EasyTripApplication.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/route/domain/RouteRefreshCoordinator.kt` only if a current-generation guard cannot remain at container/session boundary
- Create: `app/src/test/java/com/yangchengwei/easytrip/AppContainerTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/route/domain/RouteRefreshCoordinatorTest.kt`

**Interfaces:**

```kotlin
data class AmapRuntimeSession(
    val generation: Long,
    val token: AmapConsentToken,
    val placeSearchDataSource: PlaceSearchDataSource,
    val routeRefreshCoordinator: RouteRefreshCoordinator,
)

sealed interface MapHostState {
    data object Loading : MapHostState
    data object Ready : MapHostState
    data class Failed(val message: String) : MapHostState
}

class AppContainer {
    fun runtimeSession(fact: AmapConsentFact.Accepted): AmapRuntimeSession
    fun stopRuntimeSession()
}
```

- `AppContainer` 按 consent generation 缓存一份运行时 session，而不是分别缓存静态 token/source/coordinator。
- `Declined/Undecided` 必须调用 `stopRuntimeSession()`，取消 route scope 并释放 session。
- 新 `Accepted` generation 必须创建新 session，不复用旧 token 对应 source/coordinator。
- Route 结果若已进入 repository 写入边界，继续使用现有 RouteLeg version 校验；consent generation 的职责是取消旧 session，不修改路线领域模型。

- [ ] **Step 1: 写 session 生命周期 RED**

测试：同一 generation 两次请求得到同一 session；切换 `Declined` 后旧 token inactive、旧 coordinator scope 被取消；新 generation 得到不同 session。

```kotlin
@Test
fun declinedStateStopsActiveRuntimeSessionAndNewGenerationDoesNotReuseIt() {
    val first = container.runtimeSession(acceptedFact(generation = 1))
    container.stopRuntimeSession()
    val second = container.runtimeSession(acceptedFact(generation = 2))

    assertThat(first.token.isActive()).isFalse()
    assertThat(second).isNotSameInstanceAs(first)
    assertThat(second.generation).isEqualTo(2)
}
```

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.*AppContainer*Test' \
  --tests 'com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinatorTest'
```

Expected: 新 session API 缺失或旧 coordinator 未被停止。

- [ ] **Step 3: 实现单 session 装配**

将现有 `routeCoordinator(token)`、`startRouteCoordinator(token)`、静态 `placeSearchDataSource` 装配收敛到 `AmapRuntimeSession`。同一 generation 返回同一 session；generation 或 token 改变前先停止旧 session。

- [ ] **Step 4: 写 Navigation 不重建 Room/ViewModel RED**

通过现有 Navigation test seam 记录 workspace ViewModel/repository identity：consent 从 `Accepted` → `Declined` → 新 `Accepted` 时，运行时 SDK session 变化，但同一 nav entry 的 trip/workspace 数据状态和当前 Sheet/Tab 保持。

测试名：

```text
consentChangeReplacesAmapRuntimeWithoutResettingWorkspaceState
```

- [ ] **Step 5: 运行 RED**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.*AppContainer*Test'
```

若该行为仅能通过 Compose navigation 验证，则在本 Task 先完成可控依赖 seam，RED 放到 Task 5 的 `WorkspaceFlowTest`，不得用无断言测试代替。

- [ ] **Step 6: 让 `AppNavigationDependencies` 接收 store/session factory**

移除静态 `mapConsentToken` 与静态 `placeSearchDataSource` 作为生产事实的职责。测试依赖可注入 `AmapConsentStore` 或等价 `StateFlow<AmapConsentState>` 与 runtime session factory；生产路径从 Application 的 store 收集状态。

- [ ] **Step 7: 运行 Task 2 GREEN**

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.amap.*' \
  --tests 'com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinatorTest' \
  --tests 'com.yangchengwei.easytrip.*AppContainer*Test'
```

Expected: 全部 PASS。

- [ ] **Step 8: 更新 Graphify 并检查差异**

```bash
graphify update .
git diff --check
```

---

### Task 3：重写纯逻辑定位权限状态机

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/permission/LocationPermissionCoordinator.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/permission/PermissionExplanationContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceOverlay.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/permission/LocationPermissionCoordinatorTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/permission/LocationPermissionSourceTest.kt`

**Interfaces:**

```kotlin
data class LocationPermissionSnapshot(
    val granted: Boolean,
    val shouldShowRationale: Boolean,
)

data class LocationPermissionUiState(
    val explanationVisible: Boolean = false,
    val permanentlyDenied: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
)

sealed interface WorkspaceEffect {
    data class RequestLocationPermission(val generation: Long) : WorkspaceEffect
    data class ShowCurrentLocation(val generation: Long) : WorkspaceEffect
    data class OpenApplicationSettings(
        val generation: Long,
        val workspaceId: String,
    ) : WorkspaceEffect
}

class LocationPermissionCoordinator(
    private val requestStore: LocationPermissionRequestStore,
) {
    val uiState: StateFlow<LocationPermissionUiState>
    val effectFlow: Flow<WorkspaceEffect>

    fun attachWorkspace(workspaceId: String)
    fun detachWorkspace(workspaceId: String)
    fun onLocateClick(snapshot: LocationPermissionSnapshot)
    fun confirmExplanation()
    fun dismissExplanation()
    fun onPermissionLaunchStarted(generation: Long)
    fun onPermissionLaunchFailed(generation: Long)
    fun onPermissionResult(generation: Long, snapshot: LocationPermissionSnapshot)
    fun requestApplicationSettings()
    fun onSettingsLaunchStarted(generation: Long)
    fun onSettingsLaunchFailed(generation: Long)
    fun onWorkspaceResumed(workspaceId: String, snapshot: LocationPermissionSnapshot)
}
```

- Coordinator 不接收 `Context`、launcher、LifecycleOwner 或 `SavedStateHandle`。
- 删除 `SavedStateLocationPermissionRequestStore`。
- 删除 `PermissionKind.MAP_SERVICE_CONSENT`。
- `PermissionExplanationContent` 只渲染 `DEVICE_LOCATION` 用途说明；永久拒绝由 Workspace 内联状态渲染。
- `generation` 对每次用户定位意图递增；所有平台回调都必须匹配 active generation。

- [ ] **Step 1: 写 launcher started 才持久化的 RED**

```kotlin
@Test
fun confirmExplanationEmitsLaunchEffectWithoutPersistingRequested() = runTest {
    coordinator.attachWorkspace("trip-1")
    coordinator.onLocateClick(deniedFirstRequest)
    coordinator.confirmExplanation()

    assertThat(store.hasRequested).isFalse()
    assertThat(coordinator.effectFlow.first())
        .isInstanceOf(WorkspaceEffect.RequestLocationPermission::class.java)
}

@Test
fun permissionLaunchStartedPersistsRequestedExactlyOnce() = runTest {
    val generation = requestPermissionAndReadGeneration()
    coordinator.onPermissionLaunchStarted(generation)
    coordinator.onPermissionLaunchStarted(generation)

    assertThat(store.writeCount).isEqualTo(1)
    assertThat(store.hasRequested).isTrue()
}
```

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.permission.LocationPermissionCoordinatorTest.confirmExplanationEmitsLaunchEffectWithoutPersistingRequested' \
  --tests 'com.yangchengwei.easytrip.permission.LocationPermissionCoordinatorTest.permissionLaunchStartedPersistsRequestedExactlyOnce'
```

Expected: 旧实现会在 confirm 时提前写入或新 API 不存在。

- [ ] **Step 3: 实现最小 generation 与 launch 回执**

`confirmExplanation()` 只发 effect；`onPermissionLaunchStarted()` 才写 store。launch failed 清 busy、保留未请求事实并写“无法打开系统权限请求，请重试”。重复 started 不重复写。

- [ ] **Step 4: 写授权、普通拒绝和永久拒绝 RED**

加入测试：

```text
coarseGrantEmitsLocateOnce
fineGrantEmitsLocateOnce
ordinaryDenialAllowsAnotherExplanation
permanentDenialPublishesInlineSettingsState
stalePermissionCallbackIsIgnored
```

普通拒绝 fixture：`granted=false, shouldShowRationale=true`。永久拒绝 fixture：store 已请求且 `granted=false, shouldShowRationale=false`。

- [ ] **Step 5: 运行 RED**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.permission.LocationPermissionCoordinatorTest'
```

Expected: 至少永久拒绝内联状态、generation 或 coarse grant 用例失败。

- [ ] **Step 6: 实现拒绝分类和一次性定位 effect**

授权时仅对当前 generation 发出一次 `ShowCurrentLocation`；普通拒绝退出 busy，不常驻提示；永久拒绝设置 `permanentlyDenied=true`。不得用 result map 单独判定，输入必须是 Route 重新采样后的 snapshot。

- [ ] **Step 7: 写 settings recovery RED**

加入：

```text
openSettingsCreatesRecoveryRequestForWorkspaceAndGeneration
settingsLaunchFailureKeepsPermanentDenialAndShowsError
settingsResumeWithGrantLocatesExactlyOnce
settingsResumeWithoutGrantKeepsPermanentDenial
ordinaryResumeDoesNotLocate
workspaceChangeInvalidatesSettingsRecovery
```

`settingsResumeWithGrantLocatesExactlyOnce` 必须连续调用两次 `onWorkspaceResumed()`，只收到一个定位 effect。

- [ ] **Step 8: 运行 RED**

Run: Step 5 同一命令。

Expected: settings recovery API 缺失或重复 resume 导致重复定位。

- [ ] **Step 9: 实现 recovery request 与 workspace identity**

只有 `onSettingsLaunchStarted()` 后才允许一次 resume 检查；attach 新 workspace 或 detach 当前 workspace 使旧 recovery 失效。未授权 resume 消费本次 resume 资格但保留永久拒绝 UI；再次点击设置创建新 recovery generation。

- [ ] **Step 10: 删除双状态和阻断式永久拒绝 dialog**

删除 SavedState request store、`MAP_SERVICE_CONSENT` 和 `DEVICE_LOCATION_SETTINGS` 的 dialog 分支。保留既有定位用途说明文案和可访问语义。

- [ ] **Step 11: 运行 Task 3 GREEN**

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.permission.LocationPermissionCoordinatorTest' \
  --tests 'com.yangchengwei.easytrip.permission.LocationPermissionSourceTest'
```

Expected: 全部 PASS，0 skipped。

- [ ] **Step 12: 更新 Graphify 并检查差异**

```bash
graphify update .
git diff --check
```

---

### Task 4：接通 Activity Result、系统设置和 lifecycle 恢复

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceRoute.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/permission/LocationPermissionCoordinator.kt` only for defects exposed by platform wiring tests
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspacePermissionFlowTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/permission/LocationPermissionSourceTest.kt`

**Interfaces:**

`AppNavigation` 的可测试平台 seam 扩展为：

```kotlin
onLaunchLocationPermission: (Array<String>) -> Result<Unit>
onOpenApplicationSettings: () -> Result<Unit>
locationPermissionSnapshot: () -> LocationPermissionSnapshot
```

生产默认实现：

- permission seam 调用 `ActivityResultLauncher.launch()`，返回后才是 `Result.success(Unit)`；同步异常返回 failure；
- settings seam 调用 app details settings Intent，`startActivity()` 返回后才是 started；
- destination `LifecycleEventObserver` 在 `ON_RESUME` 时调用 coordinator，coordinator 自行判断是否存在有效 recovery。

- [ ] **Step 1: 写 launcher 成功/失败接线 RED**

在 `WorkspacePermissionFlowTest` 通过可控 seam 验证：

```text
launcherStartedIsReportedOnlyAfterLaunchReturnsSuccessfully
launcherFailureDoesNotMarkPermissionRequested
```

失败 seam 返回 `Result.failure(IllegalStateException("launcher unavailable"))`；断言 store 未写、用途说明可再次打开、错误可见。

- [ ] **Step 2: 运行 RED**

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspacePermissionFlowTest
```

Expected: 新 seam 或回执 API 不存在，或者旧实现提前写 requested。

- [ ] **Step 3: 实现 permission launcher 接线**

Activity Result callback 收到结果后重新调用 `locationPermissionSnapshot()`，将 snapshot 和 effect generation 回传。不得把 `Map<String, Boolean>` 直接作为永久拒绝结论。

- [ ] **Step 4: 写 settings return RED**

加入：

```text
settingsIntentStartedCreatesResumeEligibility
settingsReturnWithGrantTriggersOneLocateRequest
consecutiveResumeDoesNotRepeatLocate
ordinaryForegroundResumeDoesNotLocate
leavingWorkspaceBeforeResumeInvalidatesLocate
switchingTripBeforeResumeInvalidatesLocate
```

测试使用可控 lifecycle owner 和 snapshot，不能依靠固定 sleep；通过状态条件等待 effect/节点。

- [ ] **Step 5: 运行 RED**

Run: Step 2 同一命令。

Expected: 返回授权不自动定位、重复 resume 重复定位，或旧 workspace 未失效。

- [ ] **Step 6: 实现 destination-scoped lifecycle observer**

observer 绑定当前 workspace nav entry；`ON_RESUME` 时传入当前 `tripId` 和最新 snapshot。DisposableEffect 移除 observer 并调用 `detachWorkspace(tripId)`。settings Intent 失败回传 `onSettingsLaunchFailed()`。

- [ ] **Step 7: 运行 JVM 与 instrumentation GREEN**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.permission.*'
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspacePermissionFlowTest
```

Expected: 两条命令 PASS，instrumentation 0 skipped。

- [ ] **Step 8: 更新 Graphify 并检查差异**

```bash
graphify update .
git diff --check
```

---

### Task 5：实现 Workspace 地图状态优先级和独立恢复动作

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceUiModels.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceRoute.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceMapFallback.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/AmapComposeMap.kt` only if existing token-key disposal test exposes a defect
- Test: `app/src/test/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentStateTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspacePermissionFlowTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/amap/AmapComposeMapTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt`

**Interfaces:**

```kotlin
sealed interface WorkspaceMapState {
    data object Loading : WorkspaceMapState
    data object Ready : WorkspaceMapState
    data object ConsentRequired : WorkspaceMapState
    data class Failed(val message: String) : WorkspaceMapState
    data object LocationPermanentlyDenied : WorkspaceMapState
}

fun resolveWorkspaceMapState(
    consentFact: AmapConsentFact,
    mapHostState: MapHostState,
    locationPermanentlyDenied: Boolean,
): WorkspaceMapState
```

优先级固定为：`ConsentRequired` → `Failed` → `LocationPermanentlyDenied` → `Loading/Ready`。

`WorkspaceMapFallback` 使用独立 callback：

```kotlin
onOpenConsent: () -> Unit
onRetryMap: () -> Unit
onOpenLocationSettings: () -> Unit
```

- [ ] **Step 1: 写纯状态优先级 RED**

在 `TripWorkspaceContentStateTest` 加入：

```kotlin
@Test
fun consentRequiredTakesPriorityOverFailureAndPermanentLocationDenial() {
    val state = resolveWorkspaceMapState(
        consentFact = declinedFact(),
        mapHostState = MapHostState.Failed("offline"),
        locationPermanentlyDenied = true,
    )
    assertThat(state).isEqualTo(WorkspaceMapState.ConsentRequired)
}

@Test
fun mapFailureTakesPriorityOverPermanentLocationDenial() {
    val state = resolveWorkspaceMapState(
        consentFact = acceptedFact(),
        mapHostState = MapHostState.Failed("offline"),
        locationPermanentlyDenied = true,
    )
    assertThat(state).isInstanceOf(WorkspaceMapState.Failed::class.java)
}
```

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.workspace.TripWorkspaceContentStateTest'
```

Expected: resolver 或定位永久拒绝状态不存在。

- [ ] **Step 3: 实现纯 resolver 并运行 GREEN**

Run: Step 2 同一命令。

Expected: 新优先级测试和既有 workspace state 测试全部 PASS。

- [ ] **Step 4: 写三种恢复 UI RED**

在 Compose 测试覆盖：

```text
declinedConsentShowsMapServiceDisabledRecovery
mapFailureShowsRetryInsteadOfConsentAction
permanentLocationDenialShowsInlineSettingsAction
declinedConsentKeepsPlacePoolAndItineraryInteractive
```

稳定语义节点：

```text
map-consent-required
map-consent-open
map-load-failed
map-retry
location-permission-denied
location-open-settings
```

测试必须点击地点池/行程关键入口证明非阻断，不能只断言文案存在。

- [ ] **Step 5: 运行 RED**

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.TripWorkspaceContentTest,com.yangchengwei.easytrip.workspace.WorkspacePermissionFlowTest
```

Expected: 定位内联节点或独立 callback 不存在。

- [ ] **Step 6: 实现降级面板和回调边界**

- `ConsentRequired`：显示“地图服务未启用”“本地地点与行程仍可使用”“查看并授权”。
- `Failed`：显示“地图加载失败”“重试地图”。
- `LocationPermanentlyDenied`：显示“定位权限未开启”“前往系统设置”。
- 不把定位永久拒绝重新映射到 `PermissionExplanationContent`。

- [ ] **Step 7: 写撤回立即销毁与 map retry 隔离 RED**

`AmapComposeMapTest`：active token 构造 host 后发布 Declined/失活 token，断言 listener 移除、controller destroy、旧 callback 不更新 UI。`WorkspaceFlowTest`：点击 map retry 只创建新 map host，trip、Sheet、Tab 和 Room 内容 identity 不变。

测试名：

```text
withdrawalDisposesActiveMapHostAndIgnoresOldCallbacks
mapRetryRecreatesOnlyMapHostAndKeepsWorkspaceContext
consentChangeKeepsWorkspaceViewModelAndSelectedSection
```

- [ ] **Step 8: 运行 RED**

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.amap.AmapComposeMapTest,com.yangchengwei.easytrip.workspace.WorkspaceFlowTest
```

Expected: 至少 workspace context 或 consent state 动态切换行为失败；若现有 host disposal 已满足，保留首次即 PASS 作为既有行为证明，不修改 `AmapComposeMap.kt`。

- [ ] **Step 9: 实现动态 consent 消费与首次说明规则**

工作台收集 `AmapConsentStore.state`：

- `Undecided` 且当前进入尚未消费自动展示资格 → 打开说明；
- `Declined` → 不自动重开；
- 点击 `map-consent-open` → 显式打开说明；
- `Accepted` → 使用当前 runtime session token；
- 决定更新失败时保留旧地图事实并展示 store error。

Compose 本地状态只保存“弹层当前是否展开”，不得复制授权决定。

- [ ] **Step 10: 运行 Task 5 GREEN**

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.workspace.TripWorkspaceContentStateTest'

ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.TripWorkspaceContentTest

ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspacePermissionFlowTest

ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.amap.AmapComposeMapTest

ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceFlowTest
```

Expected: 所有 suite PASS，0 skipped；严格串行，不合并启动多个 instrumentation。

- [ ] **Step 11: 更新 Graphify 并检查差异**

```bash
graphify update .
git diff --check
```

---

### Task 6：地点搜索随 consent generation 收敛

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchReducer.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchRoute.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContent.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/place/ui/PlaceSearchReducerTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModelTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContentTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt`

**Interfaces:**

```kotlin
class PlaceSearchViewModel {
    fun setRemoteSearchSession(
        generation: Long,
        source: PlaceSearchDataSource?,
    )
}

@Composable
fun PlaceSearchRoute(
    viewModel: PlaceSearchViewModel,
    consentState: AmapConsentState,
    onOpenConsent: () -> Unit,
    ...
)
```

- ViewModel 实例绑定 nav entry，不随 consent 变化重建。
- `setRemoteSearchSession()` 委托 reducer 更新 source 与 generation；旧 generation 响应无效。
- source 为 null 时清远程结果和远程详情，但保留 Room SavedPlace、标签和当前 query。
- 新 Accepted generation 恢复当前 query 的远程搜索，不需要用户重新输入。

- [ ] **Step 1: 写 reducer 撤回与 stale response RED**

```kotlin
@Test
fun revokingConsentCancelsRemoteSearchClearsRemoteResultsAndKeepsSavedPlaces() = runTest {
    reducer.setSavedPlaces(listOf(savedPlace))
    reducer.setRemoteSearchSession(1, firstSource)
    reducer.updateQuery("西湖")
    firstSource.awaitRequest()

    reducer.setRemoteSearchSession(2, null)
    firstSource.complete(listOf(remoteResult))

    assertThat(reducer.state.value.savedPlaces).containsExactly(savedPlace)
    assertThat(reducer.state.value.results).isEmpty()
}

@Test
fun staleSearchResponseFromRevokedGenerationIsIgnored() = runTest {
    // generation 1 request completes after generation 2 becomes current
    assertThat(reducer.state.value.results).doesNotContain(oldResult)
}
```

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.place.ui.PlaceSearchReducerTest'
```

Expected: 新 session API 缺失或旧结果重新进入 state。

- [ ] **Step 3: 实现 generation-aware source 替换**

复用现有 search job cancellation，但 completion 同时检查 reducer query generation 与 consent generation。`source=null` 使用明确未授权 phase，不伪装普通网络失败；SavedPlace 收集流不变。

- [ ] **Step 4: 写 ViewModel 不重建与自动恢复 RED**

测试：同一 ViewModel 从 generation 1 accepted → declined → generation 3 accepted；SavedPlace 和 query 保持，新 source 自动收到当前 query 一次，旧 source completion 无效。

测试名：

```text
reauthorizingRetriesCurrentQueryWithoutRecreatingViewModel
newAcceptedGenerationCreatesFreshSearchSession
```

- [ ] **Step 5: 运行 RED**

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest'
```

Expected: ViewModel 没有动态 source API，或重新授权不恢复 query。

- [ ] **Step 6: 实现 ViewModel 与 Navigation 动态 session 接线**

Navigation 收集 store state，将当前 runtime session 的 generation/source 传给既有 ViewModel；不得通过 consent key 重建 ViewModel。Declined/Undecided 传 null source。

- [ ] **Step 7: 写搜索无授权 UI RED**

Compose 测试稳定节点：

```text
search-consent-required
search-consent-open
saved-place-list
```

`declinedSearchShowsAuthorizeActionAndKeepsSavedPlaces` 同时断言 SavedPlace 行与授权按钮可见；点击按钮调用 `onOpenConsent`。Map detail 无 consent 时不得只渲染 Spacer。

- [ ] **Step 8: 运行 RED**

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest
```

Expected: 授权节点或 callback 缺失。

- [ ] **Step 9: 实现搜索降级与显式重新授权入口**

无授权时显示“地图服务未启用”“在线地点搜索暂不可用，本地收藏仍可使用”“查看并授权”。授权弹层仍由同一个 store/Navigation owner 打开，搜索页不维护第二份授权决定。

- [ ] **Step 10: 写首次自动说明和显式重开端到端 RED**

`WorkspaceFlowTest` 加入：

```text
firstWorkspaceEntryShowsConsentExplanationOnce
persistedDeclineDoesNotAutoPromptOnReentry
explicitAuthorizeActionReopensConsentExplanation
withdrawalStopsSearchAndMapWithoutResettingWorkspace
```

使用内存 persistence/store，不能改真实 SharedPreferences 或依赖测试顺序。

- [ ] **Step 11: 运行 RED**

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceFlowTest
```

Expected: 自动说明或动态撤回路径失败。

- [ ] **Step 12: 运行 Task 6 GREEN**

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.place.ui.PlaceSearchReducerTest' \
  --tests 'com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest'

ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest

ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceFlowTest
```

Expected: 全部 PASS，0 skipped。

- [ ] **Step 13: 更新 Graphify 并检查差异**

```bash
graphify update .
git diff --check
```

---

### Task 7：候选门禁、物理设备验收与记录

**Files:**
- Create: `docs/testing/map-consent-location-permission-acceptance.md`
- Modify: `docs/testing/v1-device-checklist.md`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioExecutable.kt` only when existing scenario selectors or product copy legitimately change
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioCatalogTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/V1FullUiAcceptanceTest.kt`

**Interfaces:**

验收记录必须包含：commit/工作树身份、设备、每个 suite 的 XML 实际计数、失败/错误/跳过、唯一 infra 重试、物理设备边界和未执行项。不得把 focused suite 表述为全量 connected。

- [ ] **Step 1: 运行 focused JVM**

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.amap.*' \
  --tests 'com.yangchengwei.easytrip.permission.*' \
  --tests 'com.yangchengwei.easytrip.workspace.*' \
  --tests 'com.yangchengwei.easytrip.place.ui.*' \
  --tests 'com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinatorTest'
```

Expected: 0 failures/errors/skipped。

- [ ] **Step 2: 运行全量 JVM**

```bash
./gradlew :app:testDebugUnitTest
```

完成后从 `app/build/test-results/testDebugUnitTest/*.xml` 汇总实际 tests/failures/errors/skipped，不从控制台估算。

- [ ] **Step 3: 确认 emulator 与 instrumentation 空闲**

```bash
adb devices -l
adb -s emulator-5554 shell pidof androidx.test.services || true
adb -s emulator-5554 shell pidof com.yangchengwei.easytrip.test || true
```

Expected: `emulator-5554` online，且没有其他 instrumentation。若 serial 不同，选择唯一专用 emulator 并在后续命令一致使用；不得误用物理设备运行会清 app data 的 connected tests。

- [ ] **Step 4: 串行运行权限与地图 focused connected suites**

每条单独执行，前一条结束后才开始下一条：

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspacePermissionFlowTest

ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.TripWorkspaceContentTest

ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.amap.AmapComposeMapTest

ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest

ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceFlowTest
```

Expected: 每项 0 failures/errors/skipped。runner crash、设备退出或 signal 9 可重试同 suite 一次，并记录为 infra；产品断言失败不得直接重试掩盖。

- [ ] **Step 5: 串行运行候选场景门禁**

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1ScenarioCatalogTest

ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1FullUiAcceptanceTest
```

Expected: Catalog 47/47、Full UI 47/47，除非仓库在实施期间有经批准的场景数变更；任何变更都必须在验收记录解释新增/删除的业务场景。

- [ ] **Step 6: 运行 lint 与 APK 构建**

```bash
./gradlew :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Expected: `BUILD SUCCESSFUL`，lint 无 error，两个 APK 均生成。

- [ ] **Step 7: 更新 Graphify 并执行最终静态检查**

```bash
git diff --check
graphify update .
git diff --check
git status --short
```

Expected: diff check 前后均退出码 0；status 只包含本批设计、计划、生产、测试、验收和 Graphify 产物。

- [ ] **Step 8: 编写自动化验收记录**

`docs/testing/map-consent-location-permission-acceptance.md` 记录：

- JVM XML 实际计数；
- 每个 connected suite 实际计数；
- Catalog 和 Full UI 计数；
- lint/build 结果；
- infra 失败与唯一重试；
- Graphify 结果与既有警告；
- 尚未执行的物理设备项必须明确写 `NOT-RUN`，不得推断为 PASS。

- [ ] **Step 9: 在候选物理设备执行权限关键路径**

使用专用测试安装/fixture，不清用户现有旅行数据。依次验证：

1. 未决定时首次工作台自动说明；
2. 拒绝后重新进入不自动弹出；
3. 地图降级时地点池与行程仍可操作；
4. 主动重新授权后地图恢复；
5. 撤回授权后地图、路线和在线搜索立即停用；
6. 点击定位先应用内说明，再真实系统弹窗；
7. 仅授予大致位置时可定位；
8. 普通拒绝后可再次请求；
9. 永久拒绝后页内“前往系统设置”可达；
10. 设置返回授权后自动定位一次；
11. 再次 resume 不重复定位；
12. 地图加载失败重试不影响 Room 内容、Sheet 和 Tab。

保留必要截图/日志作为诊断证据，但不要求逐屏截图或像素对比。只有功能/状态错误、数据不一致、崩溃、流程不可达、严重裁切和关键交互失效阻断批次。

- [ ] **Step 10: 更新长期验收清单并做最终复审**

更新 `docs/testing/v1-device-checklist.md`，引用本批验收文档。对 `git diff` 做全分支审查，重点确认：

- 不存在第二个地图授权业务状态源；
- 不存在 SavedState 与 SharedPreferences 双写 `hasRequested`；
- launcher 未 started 不写请求历史；
- settings recovery 只消费一次；
- consent 撤回不清 Room 数据；
- 搜索和路线旧 generation 不能回写；
- 没有混入响应式或视觉重做。

Critical/Important 必须修复并重新执行受影响 focused gate；任何生产修改都会使此前相关门禁 stale，最终提交前必须刷新对应证据。

- [ ] **Step 11: 记录 checkpoint，不主动提交**

```bash
git status --short
git diff --stat
git diff --check
```

只有执行阶段用户明确要求 commit/push 时，才显式 `git add` 本批文件并按仓库风格提交；不得使用 `git add -A`、`--no-verify` 或 `--force`。

---

## 计划自检矩阵

| Spec 要求 | 覆盖 Task |
|---|---|
| 地图授权单一可观察状态源、同意/拒绝持久化 | Task 1 |
| 隐私 API 失败保持旧事实、stale generation 隔离 | Task 1 |
| Application/Container/路线统一 runtime session | Task 2 |
| 撤回授权立即停止路线与 SDK 会话 | Tasks 2、5 |
| 定位曾请求单一持久化来源 | Task 3 |
| launcher started 后才持久化 | Tasks 3、4 |
| 大致/精确任一可定位 | Task 3 |
| 永久拒绝内联提示 | Tasks 3、5 |
| settings 返回来源明确、最多定位一次 | Tasks 3、4 |
| 普通 resume 不定位、workspace 切换失效 | Tasks 3、4 |
| 地图未授权/失败/永久拒绝优先级 | Task 5 |
| Room 地点池和行程始终可操作 | Tasks 5、6 |
| 搜索撤回清远程结果、保留 SavedPlace | Task 6 |
| 首次自动说明、拒绝后不自动打扰、主动重开 | Tasks 5、6 |
| 自动化、物理设备与证据边界 | Task 7 |
