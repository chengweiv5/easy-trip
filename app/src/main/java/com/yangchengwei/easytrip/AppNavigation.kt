package com.yangchengwei.easytrip

import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton as Button
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.amap.AmapPrivacyGate
import com.yangchengwei.easytrip.place.amap.AmapPlaceDataSource
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
import androidx.compose.foundation.layout.fillMaxWidth
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

const val TRIP_LIST_ROUTE = "trips"
const val CREATE_TRIP_ROUTE = "trips/create"
const val TRIP_WORKSPACE_ROUTE = "trips/{tripId}"
const val TRIP_SETTINGS_ROUTE = "trips/{tripId}/settings"
const val TRIP_SEARCH_ROUTE = "trips/{tripId}/search"
internal const val WORKSPACE_SEARCH_RETURN_KEY = "searchReturnPoiIds"

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
    onOpenApplicationSettings: ((android.content.Context) -> Unit)? = null,
) {
    val navController = rememberNavController()
    val navigate: (String) -> Unit = { route ->
        navigationObserver?.onNavigate(route)
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
                    navigationObserver?.onNavigate("trips/$id")
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
            val workspaceDependencies = dependencies ?: application?.let {
                AppNavigationDependencies(
                    it.savedPlaceRepository,
                    it.itineraryRepository,
                    it.routeLegRepository,
                    it.mapPreferences,
                    it.locationPermissionRequestStore,
                    it.routeCoordinatorOrNull(),
                )
            }
            if (workspaceDependencies == null) Column { Text("旅行工作区 $id"); Button(onClick = { navigate("trips/$id/settings") }) { Text("设置") } }
            else {
                var source by remember {
                    mutableStateOf(
                        application?.amapConsentToken?.takeIf { it.isActive() }?.let { AmapPlaceDataSource(application, it) },
                    )
                }
                var showConsent by remember { mutableStateOf(application?.amapPrivacyDecided == false) }
                var policyRead by remember { mutableStateOf(false) }
                var privacyReported by remember { mutableStateOf(application?.amapPrivacyShown == true) }
                val placeModel: PlacePoolViewModel = viewModel(factory = PlacePoolViewModel.Factory(id, workspaceDependencies.savedPlaceRepository, source))
                val workspaceModel: TripWorkspaceViewModel = viewModel(factory = TripWorkspaceViewModel.Factory(id, repository, workspaceDependencies.savedPlaceRepository, workspaceDependencies.itineraryRepository, workspaceDependencies.routeLegRepository, mapPreferences = workspaceDependencies.mapPreferences))
                val workspaceSearchReturnState: WorkspaceSearchReturnViewModel = viewModel(viewModelStoreOwner = entry)
                val context = androidx.compose.ui.platform.LocalContext.current
                val activity = context as? Activity
                val locationCoordinator = remember(entry) {
                    LocationPermissionCoordinator(entry.savedStateHandle, workspaceDependencies.locationPermissionRequestStore)
                }
                fun locationPermissionSnapshot(grants: Map<String, Boolean>? = null) =
                    LocationPermissionSnapshot.from(
                        permissions = setOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                        isGranted = { permission ->
                            grants?.get(permission) == true || grants == null &&
                                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                        },
                        shouldShowRationale = { permission ->
                            activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } == true
                        },
                    )
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { result ->
                    locationCoordinator.onPermissionResult(locationPermissionSnapshot(result))
                }
                val searchReturnPayload by entry.savedStateHandle.getStateFlow<Array<String>?>(WORKSPACE_SEARCH_RETURN_KEY, null).collectAsStateWithLifecycle()
                LaunchedEffect(searchReturnPayload) {
                    if (searchReturnPayload != null) workspaceSearchReturnState.show(consumeWorkspaceSearchReturn(entry.savedStateHandle))
                }
                val token = (workspaceDependencies.mapConsentToken ?: application?.amapConsentToken)
                    ?.takeIf { it.isActive() }
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
                        workspaceDependencies.routeCoordinator,
                        workspaceDependencies.savedPlaceRepository.observePlaces(id, emptySet()),
                        workspaceModel.selectedDayId,
                    ),
                )
                LaunchedEffect(source) {
                    placeModel.setSearchSource(source)
                    itineraryModel.setRouteCoordinator(workspaceDependencies.routeCoordinator)
                    if (source != null) application?.startRouteCoordinator()
                }
                TripWorkspaceRoute(
                    viewModel = workspaceModel,
                    consent = token,
                    onBack = {
                        workspaceSearchReturnState.clear()
                        navController.popBackStack()
                    },
                    onSettings = {
                        workspaceSearchReturnState.clear()
                        navigate("trips/$id/settings")
                    },
                    onPrivacySettings = { if (application != null) { showConsent = true; policyRead = false } },
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
                    locationPermissionSnapshot = ::locationPermissionSnapshot,
                    onWorkspaceEffect = { effect ->
                        when (effect) {
                            WorkspaceEffect.RequestLocationPermission -> locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                            )
                            WorkspaceEffect.ShowCurrentLocation -> Unit
                            WorkspaceEffect.OpenApplicationSettings -> if (onOpenApplicationSettings != null) {
                                onOpenApplicationSettings(context)
                            } else {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
                                )
                            }
                        }
                    },
                )
                if (showConsent && application != null) {
                    SideEffect {
                        application.reportAmapPrivacyShown()
                        privacyReported = true
                    }
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("高德服务隐私说明") },
                        text = {
                            Column {
                                Text("搜索地点会调用高德地图服务。不同意仍可使用本地点池。")
                                TextButton(onClick = {
                                    application.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://lbs.amap.com/home/privacy/")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }) { Text("阅读高德隐私权政策") }
                                androidx.compose.foundation.layout.Row {
                                    Checkbox(policyRead, { policyRead = it })
                                    Text("我已阅读高德隐私权政策")
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                enabled = privacyReported && policyRead,
                                onClick = {
                                    application.decideAmapPrivacy(true)
                                    source = application.amapConsentToken?.let { AmapPlaceDataSource(application, it) }
                                    showConsent = false
                                },
                            ) { Text("同意并启用搜索") }
                        },
                        dismissButton = {
                            TextButton(
                                enabled = privacyReported,
                                onClick = {
                                    application.decideAmapPrivacy(false)
                                    source = null
                                    showConsent = false
                                },
                            ) { Text("不同意") }
                        },
                    )
                }
            }
        }
        composable(TRIP_SEARCH_ROUTE, arguments = listOf(navArgument("tripId") { type = NavType.StringType })) { entry ->
            val id = checkNotNull(entry.arguments?.getString("tripId"))
            val savedPlaceRepository = dependencies?.savedPlaceRepository ?: application?.savedPlaceRepository
            if (savedPlaceRepository == null) {
                Column {
                    Text("搜索地点")
                    TextButton(onClick = navController::popBackStack) { Text("返回") }
                }
            } else {
                val source = remember(application, dependencies?.placeSearchDataSource) {
                    dependencies?.placeSearchDataSource
                        ?: application?.amapConsentToken
                            ?.takeIf { it.isActive() }
                            ?.let { AmapPlaceDataSource(application, it) }
                }
                val model: PlaceSearchViewModel = viewModel(
                    factory = PlaceSearchViewModel.Factory(
                        id,
                        savedPlaceRepository,
                        source,
                        entry.savedStateHandle,
                    ),
                )
                PlaceSearchRoute(model) {
                    navController.previousBackStackEntry?.savedStateHandle?.let {
                        publishWorkspaceSearchReturn(it, model.recentlyCollectedPoiIds())
                    }
                    navController.popBackStack()
                }
            }
        }
        composable(TRIP_SETTINGS_ROUTE, arguments = listOf(navArgument("tripId") { type = NavType.StringType })) {
            val model: TripSettingsViewModel = viewModel(factory = TripSettingsViewModel.Factory(service, repository, impacts))
            TripSettingsRoute(model, navController::popBackStack)
        }
    }
}
