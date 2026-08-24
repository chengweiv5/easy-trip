package com.yangchengwei.easytrip

import android.app.Application
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.amap.AmapPrivacyGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class EasyTripApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val container by lazy { AppContainer(this, applicationScope) }
    val database get() = container.database
    val tripRepository get() = container.tripRepository
    val tripService get() = container.tripService
    val savedPlaceRepository get() = container.savedPlaceRepository
    val itineraryRepository get() = container.itineraryRepository
    val routeLegRepository get() = container.routeLegRepository
    val networkMonitor get() = container.networkMonitor
    val mapPreferences get() = container.mapPreferences
    val locationPermissionRequestStore get() = container.locationPermissionRequestStore
    val deleteImpactProvider get() = container.deleteImpactProvider

    private val preferences by lazy { getSharedPreferences("privacy", MODE_PRIVATE) }
    private val amapPrivacyGate by lazy { AmapPrivacyGate.create(this) }
    var amapConsentToken: AmapConsentToken? = null
        private set
    var amapPrivacyShown: Boolean = false
        private set
    var amapPrivacyDecided: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        if (preferences.contains(AMAP_ACCEPTED)) {
            reportAmapPrivacyShown()
            decideAmapPrivacy(preferences.getBoolean(AMAP_ACCEPTED, false), persist = false)
            if (amapConsentToken != null) startRouteCoordinator()
        }
    }

    fun routeCoordinatorOrNull() = container.routeCoordinator(amapConsentToken)
    fun startRouteCoordinator() = container.startRouteCoordinator(amapConsentToken)

    fun reportAmapPrivacyShown() {
        if (!amapPrivacyShown) {
            amapPrivacyGate.reportPrivacyShown()
            amapPrivacyShown = true
        }
    }

    fun decideAmapPrivacy(accepted: Boolean) = decideAmapPrivacy(accepted, persist = true)

    private fun decideAmapPrivacy(accepted: Boolean, persist: Boolean) {
        check(amapPrivacyShown)
        if (!accepted) container.stopRouteCoordinator()
        amapConsentToken = amapPrivacyGate.reportUserDecision(accepted)
        amapPrivacyDecided = true
        if (persist) preferences.edit().putBoolean(AMAP_ACCEPTED, accepted).apply()
    }

    companion object { private const val AMAP_ACCEPTED = "amap.accepted" }
}
