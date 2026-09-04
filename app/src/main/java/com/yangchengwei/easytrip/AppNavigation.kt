package com.yangchengwei.easytrip

import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton as Button
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.amap.AmapConsentFact
import com.yangchengwei.easytrip.amap.AmapConsentStore
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.ui.PlacePoolSheet
import com.yangchengwei.easytrip.place.ui.PlacePoolViewModel
import com.yangchengwei.easytrip.place.ui.PlaceSearchRoute
import com.yangchengwei.easytrip.place.ui.PlaceSearchViewModel
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.route.domain.RouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator
import com.yangchengwei.easytrip.workspace.MapPreferences
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yangchengwei.easytrip.permission.LocationPermissionCoordinator
import com.yangchengwei.easytrip.permission.LocationPermissionRequestStore
import com.yangchengwei.easytrip.permission.LocationPermissionSnapshot
import com.yangchengwei.easytrip.permission.WorkspaceEffect
import androidx.compose.ui.Modifier
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.ui.CreateTripRoute
import com.yangchengwei.easytrip.trip.ui.CreateTripViewModel
import com.yangchengwei.easytrip.trip.ui.DeleteImpactProvider
import com.yangchengwei.easytrip.trip.ui.TripListScreen
import com.yangchengwei.easytrip.trip.ui.TripListViewModel
import com.yangchengwei.easytrip.trip.ui.TripSettingsRoute
import com.yangchengwei.easytrip.trip.ui.TripSettingsViewModel
import com.yangchengwei.easytrip.itinerary.ui.DayItinerarySheet
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModel
import com.yangchengwei.easytrip.workspace.TripWorkspaceRoute
import com.yangchengwei.easytrip.workspace.TripWorkspaceViewModel
import kotlinx.coroutines.launch

const val TRIP_LIST_ROUTE = "trips"
const val CREATE_TRIP_ROUTE = "trips/create"
const val TRIP_WORKSPACE_ROUTE = "trips/{tripId}"
const val TRIP_SETTINGS_ROUTE = "trips/{tripId}/settings"
const val TRIP_SEARCH_ROUTE = "trips/{tripId}/search"
internal const val WORKSPACE_SEARCH_RETURN_KEY = "searchReturnPoiIds"

private class LocationPermissionLaunchBridge(
    val generation: Long,
    private val coordinator: LocationPermissionCoordinator,
) {
    private var active = true
    val isActive: Boolean
        get() = active
    private var launchSucceeded = false
    private var resultDelivered = false
    private var bufferedSnapshot: LocationPermissionSnapshot? = null

    fun onResult(snapshot: LocationPermissionSnapshot) {
        if (!active || resultDelivered) return
        if (!launchSucceeded) {
            bufferedSnapshot = snapshot
            return
        }
        deliver(snapshot)
    }

    fun onLaunchFinished(result: Result<Unit>) {
        if (!active) return
        result.onSuccess {
            launchSucceeded = true
            coordinator.onPermissionLaunchStarted(generation)
            bufferedSnapshot?.let { snapshot ->
                bufferedSnapshot = null
                deliver(snapshot)
            }
        }.onFailure {
            active = false
            bufferedSnapshot = null
            coordinator.onPermissionLaunchFailed(generation)
        }
    }

    fun invalidate() {
        active = false
        bufferedSnapshot = null
    }

    private fun deliver(snapshot: LocationPermissionSnapshot) {
        if (!active || resultDelivered) return
        resultDelivered = true
        coordinator.onPermissionResult(generation, snapshot)
    }
}

internal fun dispatchApplicationSettingsRequest(
    generation: Long,
    launcher: () -> Result<Unit>,
    coordinator: LocationPermissionCoordinator,
) {
    launcher().onSuccess {
        coordinator.onSettingsLaunchStarted(generation)
    }.onFailure {
        coordinator.onSettingsLaunchFailed(generation)
    }
}

internal fun applyConsentDecision(
    result: Result<Unit>,
    error: String?,
    onSuccess: () -> Unit,
    onFailure: (String?) -> Unit,
) {
    if (result.isSuccess) onSuccess() else onFailure(error)
}

@Composable
internal fun AmapConsentDialog(
    store: AmapConsentStore,
    policyRead: Boolean,
    onPolicyReadChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onDecisionSuccess: () -> Unit,
    context: android.content.Context,
) {
    val privacyReported by store.shown.collectAsStateWithLifecycle()
    var shownAttempt by remember { mutableStateOf(0) }
    var shownError by remember { mutableStateOf<String?>(null) }
    var consentError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(shownAttempt) {
        store.reportShown().onSuccess {
            shownError = null
        }.onFailure {
            shownError = "隐私说明展示失败，请重试"
        }
    }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {},
        text = {
            AmapConsentBody(
                policyRead = policyRead,
                onPolicyReadChange = onPolicyReadChange,
                onOpenPolicy = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://lbs.amap.com/home/privacy/")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                onAllow = {
                    coroutineScope.launch {
                        applyConsentDecision(
                            result = store.decide(true),
                            error = store.state.value.error,
                            onSuccess = onDecisionSuccess,
                            onFailure = { consentError = it },
                        )
                    }
                },
                onDecline = {
                    coroutineScope.launch {
                        applyConsentDecision(
                            result = store.decide(false),
                            error = store.state.value.error,
                            onSuccess = onDecisionSuccess,
                            onFailure = { consentError = it },
                        )
                    }
                },
                allowEnabled = privacyReported && policyRead,
                declineEnabled = privacyReported,
                shownError = shownError,
                consentError = consentError,
                onRetryShown = { shownAttempt++ },
            )
        },
    )
}

@Composable
internal fun AmapConsentBody(
    policyRead: Boolean,
    onPolicyReadChange: (Boolean) -> Unit,
    onOpenPolicy: () -> Unit,
    onAllow: () -> Unit,
    onDecline: () -> Unit,
    allowEnabled: Boolean,
    declineEnabled: Boolean,
    modifier: Modifier = Modifier,
    shownError: String? = null,
    consentError: String? = null,
    onRetryShown: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState())
                .testTag("map-consent-scroll"),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            Text("允许 Easy Trip 使用地图", Modifier.semantics { heading() })
            Text("地图用于展示收藏地点、每天的路线和交通距离。暂不允许也可以继续编辑地点池和行程。")
            shownError?.let {
                Text(it)
                TextButton(onClick = onRetryShown) { Text("重试") }
            }
            consentError?.let { Text(it) }
            TextButton(onClick = onOpenPolicy) { Text("阅读高德隐私权政策") }
            Row(
                Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {}
                    .toggleable(
                        value = policyRead,
                        role = Role.Checkbox,
                        onValueChange = onPolicyReadChange,
                    )
                    .testTag("map-consent-policy-confirmation"),
            ) {
                Checkbox(checked = policyRead, onCheckedChange = null)
                Text("我已阅读高德隐私权政策")
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 330.dp) {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAllow, enabled = allowEnabled, modifier = Modifier.fillMaxWidth()) { Text("允许使用地图") }
                    TextButton(onClick = onDecline, enabled = declineEnabled, modifier = Modifier.fillMaxWidth()) { Text("暂不允许") }
                }
            } else {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDecline, enabled = declineEnabled, modifier = Modifier.weight(1f)) { Text("暂不允许") }
                    Button(onClick = onAllow, enabled = allowEnabled, modifier = Modifier.weight(1f)) { Text("允许使用地图") }
                }
            }
        }
    }
}

fun tripSearchRoute(tripId: String): String = "trips/$tripId/search"

internal fun publishWorkspaceSearchReturn(handle: androidx.lifecycle.SavedStateHandle, poiIds: Set<String>) {
    handle[WORKSPACE_SEARCH_RETURN_KEY] = poiIds.takeIf { it.isNotEmpty() }?.toTypedArray()
}

internal class WorkspaceSearchReturnViewModel : androidx.lifecycle.ViewModel() {
    var value by mutableStateOf<com.yangchengwei.easytrip.workspace.WorkspaceSearchReturn?>(null)
        private set

    fun show(searchReturn: com.yangchengwei.easytrip.workspace.WorkspaceSearchReturn?) {
        value = searchReturn
    }

    fun clear() {
        value = null
    }
}

internal fun consumeWorkspaceSearchReturn(handle: androidx.lifecycle.SavedStateHandle): com.yangchengwei.easytrip.workspace.WorkspaceSearchReturn? {
    val poiIds = handle.get<Array<String>>(WORKSPACE_SEARCH_RETURN_KEY)?.toSet().orEmpty()
    handle[WORKSPACE_SEARCH_RETURN_KEY] = null
    return poiIds.takeIf { it.isNotEmpty() }?.let { com.yangchengwei.easytrip.workspace.WorkspaceSearchReturn(it) }
}

data class AppNavigationDependencies(
    val savedPlaceRepository: SavedPlaceRepository,
    val itineraryRepository: ItineraryRepository,
    val routeLegRepository: RouteLegRepository,
    val mapPreferences: MapPreferences,
    val locationPermissionRequestStore: LocationPermissionRequestStore,
    val consentStore: AmapConsentStore? = null,
    val runtimeSessionFactory: ((AmapConsentFact.Accepted) -> AmapRuntimeSession)? = null,
    val stopRuntimeSession: () -> Unit = {},
    val routeCoordinator: RouteRefreshCoordinator? = null,
    val placeSearchDataSource: PlaceSearchDataSource? = null,
    val mapConsentToken: AmapConsentToken? = null,
)

fun interface AppNavigationObserver {
    fun onNavigate(route: String)
}

@Composable
fun AppNavigation() {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as EasyTripApplication
    AppNavigation(app.tripService, app.tripRepository, app.deleteImpactProvider, application = app)
}

@Composable
fun AppNavigation(
    service: TripService,
    repository: TripRepository,
    impacts: DeleteImpactProvider,
    initialDateMillis: Long? = null,
    application: EasyTripApplication? = null,
    dependencies: AppNavigationDependencies? = null,
    navigationObserver: AppNavigationObserver? = null,
    mapHostFactory: ((android.content.Context) -> com.yangchengwei.easytrip.workspace.AmapMapHost)? = null,
    onLaunchLocationPermission: ((Array<String>, (LocationPermissionSnapshot) -> Unit) -> Result<Unit>)? = null,
    onOpenApplicationSettings: (() -> Result<Unit>)? = null,
    locationPermissionSnapshot: (() -> LocationPermissionSnapshot)? = null,
) {
    val navController = rememberNavController()
    val effectiveDependencies = remember(dependencies, application) {
        dependencies ?: application?.let {
            AppNavigationDependencies(
                savedPlaceRepository = it.savedPlaceRepository,
                itineraryRepository = it.itineraryRepository,
                routeLegRepository = it.routeLegRepository,
                mapPreferences = it.mapPreferences,
                locationPermissionRequestStore = it.locationPermissionRequestStore,
                consentStore = it.amapConsentStore,
                runtimeSessionFactory = it.container::runtimeSession,
                stopRuntimeSession = it.container::stopRuntimeSession,
            )
        }
    }
    val currentNavigationObserver = rememberUpdatedState(navigationObserver)
    val navigate: (String) -> Unit = { route ->
        currentNavigationObserver.value?.onNavigate(route)
        navController.navigate(route)
    }
    NavHost(navController, startDestination = TRIP_LIST_ROUTE) {
        composable(TRIP_LIST_ROUTE) {
            val model: TripListViewModel = viewModel(factory = TripListViewModel.Factory(service, repository, impacts))
            TripListScreen(
                model,
                { navigate(CREATE_TRIP_ROUTE) },
                { navigate("trips/$it") },
                { navigate("trips/$it/settings") },
            )
        }
        composable(CREATE_TRIP_ROUTE) {
            val model: CreateTripViewModel = viewModel(factory = CreateTripViewModel.Factory(service))
            CreateTripRoute(
                onBack = navController::popBackStack,
                onOpenWorkspace = { id ->
                    currentNavigationObserver.value?.onNavigate("trips/$id")
                    navController.navigate("trips/$id") {
                        popUpTo(CREATE_TRIP_ROUTE) { inclusive = true }
                    }
                },
                viewModel = model,
                initialDateMillis = initialDateMillis,
            )
        }
        composable(TRIP_WORKSPACE_ROUTE, arguments = listOf(navArgument("tripId") { type = NavType.StringType })) { entry ->
            val id = checkNotNull(entry.arguments?.getString("tripId"))
            val workspaceDependencies = effectiveDependencies
            if (workspaceDependencies == null) Column { Text("旅行工作区 $id"); Button(onClick = { navigate("trips/$id/settings") }) { Text("设置") } }
            else {
                val consentStore = workspaceDependencies.consentStore
                val consentState = consentStore?.state?.collectAsStateWithLifecycle()?.value
                val consentFact = consentState?.fact
                var runtimeSession by remember { mutableStateOf<AmapRuntimeSession?>(null) }
                var showConsent by remember { mutableStateOf(consentFact is AmapConsentFact.Undecided) }
                var policyRead by remember { mutableStateOf(false) }
                val openConsentRequest by entry.savedStateHandle.getStateFlow("openConsent", false).collectAsStateWithLifecycle()
                LaunchedEffect(openConsentRequest) {
                    if (openConsentRequest) {
                        entry.savedStateHandle["openConsent"] = false
                        showConsent = true
                        policyRead = false
                    }
                }
                var consentError by remember { mutableStateOf<String?>(null) }
                val coroutineScope = rememberCoroutineScope()
                LaunchedEffect(consentFact) {
                    runtimeSession = when (val fact = consentFact) {
                        is AmapConsentFact.Accepted -> workspaceDependencies.runtimeSessionFactory?.invoke(fact)
                        is AmapConsentFact.Declined, is AmapConsentFact.Undecided -> {
                            workspaceDependencies.stopRuntimeSession()
                            null
                        }
                        null -> null
                    }
                }
                val runtime = resolveAmapRuntimeDependencies(
                    consentFact = consentFact,
                    session = runtimeSession,
                    legacy = AmapRuntimeDependencies(
                        workspaceDependencies.mapConsentToken?.takeIf { it.isActive() },
                        workspaceDependencies.placeSearchDataSource,
                        workspaceDependencies.routeCoordinator,
                    ),
                )
                val source = runtime.placeSearchDataSource
                val routeCoordinator = runtime.routeRefreshCoordinator
                val token = runtime.token
                val placeModel: PlacePoolViewModel = viewModel(factory = PlacePoolViewModel.Factory(id, workspaceDependencies.savedPlaceRepository, source))
                val workspaceModel: TripWorkspaceViewModel = viewModel(factory = TripWorkspaceViewModel.Factory(id, repository, workspaceDependencies.savedPlaceRepository, workspaceDependencies.itineraryRepository, workspaceDependencies.routeLegRepository, mapPreferences = workspaceDependencies.mapPreferences))
                val workspaceSearchReturnState: WorkspaceSearchReturnViewModel = viewModel(viewModelStoreOwner = entry)
                val context = androidx.compose.ui.platform.LocalContext.current
                val activity = context as? Activity
                val locationCoordinator = remember(entry) {
                    LocationPermissionCoordinator(workspaceDependencies.locationPermissionRequestStore)
                }
                fun readLocationPermissionSnapshot() = locationPermissionSnapshot?.invoke() ?: LocationPermissionSnapshot.from(
                    permissions = setOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                    isGranted = { permission ->
                        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                    },
                    shouldShowRationale = { permission ->
                        activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } == true
                    },
                )
                var pendingPermissionGeneration by remember { mutableStateOf<Long?>(null) }
                var activePermissionBridge by remember { mutableStateOf<LocationPermissionLaunchBridge?>(null) }
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) {
                    val generation = pendingPermissionGeneration ?: return@rememberLauncherForActivityResult
                    pendingPermissionGeneration = null
                    activePermissionBridge?.takeIf { it.generation == generation }
                        ?.onResult(readLocationPermissionSnapshot())
                }
                DisposableEffect(entry, id, locationCoordinator) {
                    locationCoordinator.attachWorkspace(id)
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            locationCoordinator.onWorkspaceResumed(id, readLocationPermissionSnapshot())
                        }
                    }
                    entry.lifecycle.addObserver(observer)
                    onDispose {
                        entry.lifecycle.removeObserver(observer)
                        activePermissionBridge?.invalidate()
                        activePermissionBridge = null
                        pendingPermissionGeneration = null
                        locationCoordinator.detachWorkspace(id)
                    }
                }
                val searchReturnPayload by entry.savedStateHandle.getStateFlow<Array<String>?>(WORKSPACE_SEARCH_RETURN_KEY, null).collectAsStateWithLifecycle()
                LaunchedEffect(searchReturnPayload) {
                    if (searchReturnPayload != null) workspaceSearchReturnState.show(consumeWorkspaceSearchReturn(entry.savedStateHandle))
                }
                val addToItineraryModel: com.yangchengwei.easytrip.itinerary.ui.AddToItineraryViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryViewModel.Factory(
                        id,
                        com.yangchengwei.easytrip.itinerary.domain.AddPlacesToDayUseCase(workspaceDependencies.itineraryRepository),
                        com.yangchengwei.easytrip.itinerary.domain.UndoAddedItemsUseCase(workspaceDependencies.itineraryRepository),
                    ),
                )
                val itineraryModel: DayItineraryViewModel = viewModel(
                    factory = DayItineraryViewModel.Factory(
                        id,
                        repository,
                        workspaceDependencies.itineraryRepository,
                        workspaceDependencies.routeLegRepository,
                        routeCoordinator,
                        workspaceDependencies.savedPlaceRepository.observePlaces(id, emptySet()),
                        workspaceModel.selectedDayId,
                    ),
                )
                LaunchedEffect(source, routeCoordinator) {
                    placeModel.setSearchSource(source)
                    itineraryModel.setRouteCoordinator(routeCoordinator)
                }
                TripWorkspaceRoute(
                    viewModel = workspaceModel,
                    consent = token,
                    consentFact = consentFact,
                    onBack = {
                        workspaceSearchReturnState.clear()
                        navController.popBackStack()
                    },
                    onSettings = {
                        workspaceSearchReturnState.clear()
                        navigate("trips/$id/settings")
                    },
                    onPrivacySettings = { if (consentStore != null) { showConsent = true; policyRead = false } },
                    onOpenSearch = {
                        workspaceSearchReturnState.clear()
                        navigate(tripSearchRoute(id))
                    },
                    placeViewModel = placeModel,
                    itineraryViewModel = itineraryModel,
                    addToItineraryViewModel = addToItineraryModel,
                    mapHostFactory = mapHostFactory ?: { context -> com.yangchengwei.easytrip.workspace.RealAmapMapHost(context) },
                    searchReturn = workspaceSearchReturnState.value,
                    onConsumeSearchReturn = workspaceSearchReturnState::clear,
                    locationPermissionCoordinator = locationCoordinator,
                    locationPermissionSnapshot = ::readLocationPermissionSnapshot,
                    onWorkspaceEffect = { effect ->
                        when (effect) {
                            is WorkspaceEffect.RequestLocationPermission -> {
                                if (onLaunchLocationPermission != null) {
                                    activePermissionBridge?.invalidate()
                                    LocationPermissionLaunchBridge(
                                        generation = effect.generation,
                                        coordinator = locationCoordinator,
                                    ).also { bridge ->
                                        activePermissionBridge = bridge
                                        bridge.onLaunchFinished(
                                            onLaunchLocationPermission(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                                ),
                                                bridge::onResult,
                                            ),
                                        )
                                    }
                                } else {
                                    if (pendingPermissionGeneration != null) return@TripWorkspaceRoute
                                    activePermissionBridge?.invalidate()
                                    LocationPermissionLaunchBridge(
                                        generation = effect.generation,
                                        coordinator = locationCoordinator,
                                    ).also { bridge ->
                                        activePermissionBridge = bridge
                                        pendingPermissionGeneration = effect.generation
                                        bridge.onLaunchFinished(
                                            runCatching {
                                                locationPermissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                                    ),
                                                )
                                            },
                                        )
                                        if (!bridge.isActive) pendingPermissionGeneration = null
                                    }
                                }
                            }
                            is WorkspaceEffect.ShowCurrentLocation -> Unit
                            is WorkspaceEffect.OpenApplicationSettings -> {
                                dispatchApplicationSettingsRequest(
                                    generation = effect.generation,
                                    launcher = onOpenApplicationSettings ?: {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                    Uri.parse("package:${context.packageName}"),
                                                ),
                                            )
                                        }
                                    },
                                    coordinator = locationCoordinator,
                                )
                            }
                        }
                    },
                )
                if (showConsent && consentStore != null) {
                    AmapConsentDialog(
                        store = consentStore,
                        policyRead = policyRead,
                        onPolicyReadChange = { policyRead = it },
                        onClose = { showConsent = false },
                        onDecisionSuccess = { showConsent = false },
                        context = context,
                    )
                }
            }
        }
        composable(TRIP_SEARCH_ROUTE, arguments = listOf(navArgument("tripId") { type = NavType.StringType })) { entry ->
            val id = checkNotNull(entry.arguments?.getString("tripId"))
            val savedPlaceRepository = effectiveDependencies?.savedPlaceRepository
            if (savedPlaceRepository == null) {
                Column {
                    Text("搜索地点")
                    TextButton(onClick = navController::popBackStack) { Text("返回") }
                }
            } else {
                val consentFact = effectiveDependencies?.consentStore?.state?.collectAsStateWithLifecycle()?.value?.fact
                var session by remember { mutableStateOf<AmapRuntimeSession?>(null) }
                LaunchedEffect(consentFact) {
                    session = when (val fact = consentFact) {
                        is AmapConsentFact.Accepted -> effectiveDependencies.runtimeSessionFactory?.invoke(fact)
                        is AmapConsentFact.Declined, is AmapConsentFact.Undecided -> {
                            effectiveDependencies.stopRuntimeSession()
                            null
                        }
                        null -> null
                    }
                }
                val runtime = resolveAmapRuntimeDependencies(
                    consentFact = consentFact,
                    session = session,
                    legacy = AmapRuntimeDependencies(
                        effectiveDependencies?.mapConsentToken?.takeIf { it.isActive() },
                        effectiveDependencies?.placeSearchDataSource,
                    ),
                )
                val model: PlaceSearchViewModel = viewModel(
                    factory = PlaceSearchViewModel.Factory(
                        id,
                        savedPlaceRepository,
                        null,
                        entry.savedStateHandle,
                    ),
                )
                val remoteSearchGeneration = (consentFact as? AmapConsentFact.Accepted)?.generation ?: -1L
                LaunchedEffect(remoteSearchGeneration, runtime.placeSearchDataSource) {
                    model.setRemoteSearchSession(remoteSearchGeneration, runtime.placeSearchDataSource)
                }
                PlaceSearchRoute(
                    viewModel = model,
                    consent = runtime.token,
                    mapHostFactory = mapHostFactory
                        ?: { context -> com.yangchengwei.easytrip.workspace.RealAmapMapHost(context) },
                    onOpenConsent = {
                        navController.popBackStack()
                        navController.currentBackStackEntry?.savedStateHandle?.set("openConsent", true)
                    },
                    onBack = {
                        navController.previousBackStackEntry?.savedStateHandle?.let {
                            publishWorkspaceSearchReturn(it, model.recentlyCollectedPoiIds())
                        }
                        navController.popBackStack()
                    },
                )
            }
        }
        composable(TRIP_SETTINGS_ROUTE, arguments = listOf(navArgument("tripId") { type = NavType.StringType })) {
            val model: TripSettingsViewModel = viewModel(factory = TripSettingsViewModel.Factory(service, repository, impacts))
            TripSettingsRoute(
                viewModel = model,
                onBack = navController::popBackStack,
                onTripDeleted = {
                    navController.navigate(TRIP_LIST_ROUTE) {
                        popUpTo(TRIP_LIST_ROUTE) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
