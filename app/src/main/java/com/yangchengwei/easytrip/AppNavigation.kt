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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.amap.AmapPrivacyGate
import com.yangchengwei.easytrip.place.amap.AmapPlaceDataSource
import com.yangchengwei.easytrip.place.ui.PlacePoolSheet
import com.yangchengwei.easytrip.place.ui.PlacePoolViewModel
import com.yangchengwei.easytrip.place.ui.PlaceSearchScreen
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.route.domain.RouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator
import com.yangchengwei.easytrip.workspace.MapPreferences
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.ui.CreateTripRoute
import com.yangchengwei.easytrip.trip.ui.CreateTripViewModel
import com.yangchengwei.easytrip.trip.ui.DeleteImpactProvider
import com.yangchengwei.easytrip.trip.ui.TripListScreen
import com.yangchengwei.easytrip.trip.ui.TripListViewModel
import com.yangchengwei.easytrip.trip.ui.TripSettingsScreen
import com.yangchengwei.easytrip.trip.ui.TripSettingsViewModel
import com.yangchengwei.easytrip.itinerary.ui.DayItinerarySheet
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModel
import com.yangchengwei.easytrip.workspace.SEARCH_SELECTION_RESULT
import com.yangchengwei.easytrip.workspace.TripWorkspaceScreen
import com.yangchengwei.easytrip.workspace.TripWorkspaceViewModel
import com.yangchengwei.easytrip.workspace.consumeSearchSelection
import com.yangchengwei.easytrip.workspace.toSearchSelectionPayload

const val TRIP_LIST_ROUTE = "trips"
const val CREATE_TRIP_ROUTE = "trips/create"
const val TRIP_WORKSPACE_ROUTE = "trips/{tripId}"
const val TRIP_SETTINGS_ROUTE = "trips/{tripId}/settings"
const val TRIP_SEARCH_ROUTE = "trips/{tripId}/search"

fun tripSearchRoute(tripId: String): String = "trips/$tripId/search"

data class AppNavigationDependencies(
    val savedPlaceRepository: SavedPlaceRepository,
    val itineraryRepository: ItineraryRepository,
    val routeLegRepository: RouteLegRepository,
    val mapPreferences: MapPreferences,
    val routeCoordinator: RouteRefreshCoordinator? = null,
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
                val placeState by placeModel.state.collectAsStateWithLifecycle()
                val workspaceModel: TripWorkspaceViewModel = viewModel(factory = TripWorkspaceViewModel.Factory(id, repository, workspaceDependencies.savedPlaceRepository, workspaceDependencies.itineraryRepository, workspaceDependencies.routeLegRepository, mapPreferences = workspaceDependencies.mapPreferences))
                val workspaceState by workspaceModel.state.collectAsStateWithLifecycle()
                val searchSelection by entry.savedStateHandle
                    .getStateFlow<com.yangchengwei.easytrip.workspace.SearchSelectionPayload?>(SEARCH_SELECTION_RESULT, null)
                    .collectAsStateWithLifecycle()
                LaunchedEffect(searchSelection) {
                    if (searchSelection != null) {
                        consumeSearchSelection(entry.savedStateHandle, workspaceModel::focusSearchResult)
                    }
                }
                val token = application?.amapConsentToken?.takeIf { it.isActive() }
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
                TripWorkspaceScreen(
                    workspaceModel,
                    token,
                    navController::popBackStack,
                    { navigate("trips/$id/settings") },
                    { if (application != null) { showConsent = true; policyRead = false } },
                    { navigate(tripSearchRoute(id)) },
                    { PlacePoolSheet(placeModel, Modifier.fillMaxWidth(), showSearch = false) },
                    { DayItinerarySheet(itineraryModel, Modifier.fillMaxWidth()) },
                    isPoiSaved = workspaceState.selectedMapPoi?.poiId in placeState.savedPoiIds,
                    collectionBusyPoiIds = placeState.collectionBusyPoiIds,
                    collectionError = placeState.collectionError,
                    onTogglePoiCollection = { candidate ->
                        workspaceModel.retainViewportForPlaceCardCollection()
                        placeModel.toggleCollection(candidate)
                    },
                    mapHostFactory = mapHostFactory ?: { context -> com.yangchengwei.easytrip.workspace.RealAmapMapHost(context) },
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
            if (application == null) {
                Column {
                    Text("搜索地点")
                    TextButton(onClick = navController::popBackStack) { Text("返回") }
                }
            } else {
                val source = remember {
                    application.amapConsentToken?.takeIf { it.isActive() }?.let { AmapPlaceDataSource(application, it) }
                }
                val model: PlacePoolViewModel = viewModel(
                    factory = PlacePoolViewModel.Factory(id, application.savedPlaceRepository, source),
                )
                val state by model.state.collectAsStateWithLifecycle()
                PlaceSearchScreen(
                    state = state,
                    onQueryChange = model::setQuery,
                    onBack = {
                        model.clearSearch()
                        navController.popBackStack()
                    },
                    onSelect = { candidate ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(SEARCH_SELECTION_RESULT, candidate.toSearchSelectionPayload())
                        model.clearSearch()
                        navController.popBackStack()
                    },
                    onToggleCollection = model::toggleCollection,
                )
                state.collectionError?.let { Text(it) }
                state.pendingCollectionRemoval?.let { pending ->
                    AlertDialog(
                        onDismissRequest = model::dismissCollectionRemoval,
                        title = { Text("取消收藏 ${pending.place.name}？") },
                        text = { Text("将同时删除 ${pending.usageCount} 次行程安排及受影响路线。") },
                        confirmButton = { TextButton(model::confirmCollectionRemoval) { Text("确认取消收藏") } },
                        dismissButton = { TextButton(model::dismissCollectionRemoval) { Text("取消") } },
                    )
                }
            }
        }
        composable(TRIP_SETTINGS_ROUTE, arguments = listOf(navArgument("tripId") { type = NavType.StringType })) {
            val model: TripSettingsViewModel = viewModel(factory = TripSettingsViewModel.Factory(service, repository, impacts))
            TripSettingsScreen(model, navController::popBackStack)
        }
    }
}
