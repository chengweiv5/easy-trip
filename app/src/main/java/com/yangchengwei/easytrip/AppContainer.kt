package com.yangchengwei.easytrip

import android.content.Context
import androidx.room.Room
import com.yangchengwei.easytrip.amap.AmapConsentFact
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.network.ConnectivityNetworkMonitor
import com.yangchengwei.easytrip.core.network.NetworkMonitor
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.permission.SharedPreferencesLocationPermissionRequestStore
import com.yangchengwei.easytrip.place.amap.AmapPlaceDataSource
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.PlaceService
import com.yangchengwei.easytrip.route.amap.AmapRouteDataSource
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.route.data.FlexibleRouteDefaultsUpdate
import com.yangchengwei.easytrip.route.domain.DataSourceRoutePlanner
import com.yangchengwei.easytrip.route.domain.DefaultRouteRefreshCoordinator
import com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider
import com.yangchengwei.easytrip.workspace.SharedPreferencesMapPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

data class AmapRuntimeSession(
    val generation: Long,
    val token: AmapConsentToken,
    val placeSearchDataSource: PlaceSearchDataSource,
    val routeRefreshCoordinator: RouteRefreshCoordinator,
)

data class AmapRuntimeDependencies(
    val token: AmapConsentToken? = null,
    val placeSearchDataSource: PlaceSearchDataSource? = null,
    val routeRefreshCoordinator: RouteRefreshCoordinator? = null,
)

internal fun resolveAmapRuntimeDependencies(
    consentFact: AmapConsentFact?,
    session: AmapRuntimeSession?,
    legacy: AmapRuntimeDependencies,
): AmapRuntimeDependencies = when (consentFact) {
    is AmapConsentFact.Accepted -> session
        ?.takeIf { it.generation == consentFact.generation && it.token === consentFact.token }
        ?.let { AmapRuntimeDependencies(it.token, it.placeSearchDataSource, it.routeRefreshCoordinator) }
        ?: AmapRuntimeDependencies()
    is AmapConsentFact.Declined, is AmapConsentFact.Undecided -> AmapRuntimeDependencies()
    null -> legacy
}

internal class AmapRuntimeSessionManager(
    private val applicationScope: CoroutineScope,
    private val factory: (Long, AmapConsentToken, CoroutineScope) -> AmapRuntimeSession,
) {
    private var activeScope: CoroutineScope? = null
    private var activeSession: AmapRuntimeSession? = null

    @Synchronized
    fun runtimeSession(fact: AmapConsentFact.Accepted): AmapRuntimeSession {
        activeSession?.takeIf { it.generation == fact.generation && it.token === fact.token }?.let { return it }
        stopRuntimeSession()
        val scope = CoroutineScope(
            applicationScope.coroutineContext + SupervisorJob(applicationScope.coroutineContext[Job]),
        )
        return factory(fact.generation, fact.token, scope).also {
            activeScope = scope
            activeSession = it
        }
    }

    @Synchronized
    fun stopRuntimeSession() {
        activeScope?.coroutineContext?.get(Job)?.cancel()
        activeScope = null
        activeSession = null
    }
}

class AppContainer(
    context: Context,
    applicationScope: CoroutineScope,
    databaseFactory: (Context) -> EasyTripDatabase = {
        Room.databaseBuilder(it, EasyTripDatabase::class.java, "easy-trip.db")
            .addMigrations(EasyTripDatabase.MIGRATION_1_2, EasyTripDatabase.MIGRATION_2_3, EasyTripDatabase.MIGRATION_3_4, EasyTripDatabase.MIGRATION_4_5, EasyTripDatabase.MIGRATION_5_6)
            .addCallback(FlexibleRouteDefaultsUpdate)
            .build()
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

    private val runtimeSessions = AmapRuntimeSessionManager(applicationScope) { generation, token, scope ->
        AmapRuntimeSession(
            generation = generation,
            token = token,
            placeSearchDataSource = AmapPlaceDataSource(this.context, token),
            routeRefreshCoordinator = DefaultRouteRefreshCoordinator(
                routeLegRepository,
                DataSourceRoutePlanner(AmapRouteDataSource(this.context, token)),
                networkMonitor,
            ).also { it.start(scope) },
        )
    }

    fun runtimeSession(fact: AmapConsentFact.Accepted): AmapRuntimeSession = runtimeSessions.runtimeSession(fact)

    fun stopRuntimeSession() = runtimeSessions.stopRuntimeSession()
}
