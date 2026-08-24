package com.yangchengwei.easytrip

import android.content.Context
import androidx.room.Room
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.network.ConnectivityNetworkMonitor
import com.yangchengwei.easytrip.core.network.NetworkMonitor
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.PlaceService
import com.yangchengwei.easytrip.route.amap.AmapRouteDataSource
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.route.domain.DataSourceRoutePlanner
import com.yangchengwei.easytrip.route.domain.DefaultRouteRefreshCoordinator
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider
import com.yangchengwei.easytrip.permission.SharedPreferencesLocationPermissionRequestStore
import com.yangchengwei.easytrip.workspace.SharedPreferencesMapPreferences
import kotlinx.coroutines.CoroutineScope

class AppContainer(
    context: Context,
    private val applicationScope: CoroutineScope,
    databaseFactory: (Context) -> EasyTripDatabase = {
        Room.databaseBuilder(it, EasyTripDatabase::class.java, "easy-trip.db").build()
    },
    networkFactory: (Context, CoroutineScope) -> NetworkMonitor = ::ConnectivityNetworkMonitor,
) {
    private val context = context.applicationContext
    val database = databaseFactory(this.context)
    val networkMonitor = networkFactory(this.context, applicationScope)
    val mapPreferences = SharedPreferencesMapPreferences(this.context.getSharedPreferences("map", Context.MODE_PRIVATE))
    val locationPermissionRequestStore = SharedPreferencesLocationPermissionRequestStore(
        this.context.getSharedPreferences("permissions", Context.MODE_PRIVATE),
    )
    val tripRepository = RoomTripRepository(database.tripDao(), database = database, isOnline = { networkMonitor.isOnline.value })
    val tripService = TripService(tripRepository)
    val savedPlaceRepository = RoomSavedPlaceRepository(database, isOnline = { networkMonitor.isOnline.value })
    val placeService = PlaceService(savedPlaceRepository)
    val routeLegRepository = RoomRouteLegRepository(database.routeLegDao())
    val itineraryRepository = RoomItineraryRepository(
        database,
        database.itineraryEditingDao(),
        database.routeLegDao(),
        isOnline = { networkMonitor.isOnline.value },
    )
    val deleteImpactProvider = RoomDeleteImpactProvider(database.deleteImpactDao())

    private var routeCoordinatorToken: AmapConsentToken? = null
    private var routeCoordinatorScope: CoroutineScope? = null
    private var activeRouteCoordinator: DefaultRouteRefreshCoordinator? = null

    @Synchronized
    fun routeCoordinator(token: AmapConsentToken?): DefaultRouteRefreshCoordinator? {
        val activeToken = token?.takeIf { it.isActive() }
        if (activeToken == null) {
            stopRouteCoordinator()
            return null
        }
        if (routeCoordinatorToken !== activeToken) {
            stopRouteCoordinator()
            routeCoordinatorToken = activeToken
            routeCoordinatorScope = CoroutineScope(applicationScope.coroutineContext + kotlinx.coroutines.SupervisorJob(applicationScope.coroutineContext[kotlinx.coroutines.Job]))
            activeRouteCoordinator = DefaultRouteRefreshCoordinator(
                routeLegRepository,
                DataSourceRoutePlanner(AmapRouteDataSource(context, activeToken)),
                networkMonitor,
            )
        }
        return activeRouteCoordinator
    }

    @Synchronized
    fun startRouteCoordinator(token: AmapConsentToken?) {
        routeCoordinator(token)?.start(requireNotNull(routeCoordinatorScope))
    }

    @Synchronized
    fun stopRouteCoordinator() {
        routeCoordinatorScope?.coroutineContext?.get(kotlinx.coroutines.Job)?.cancel()
        routeCoordinatorScope = null
        routeCoordinatorToken = null
        activeRouteCoordinator = null
    }
}
