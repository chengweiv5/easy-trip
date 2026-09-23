package com.yangchengwei.easytrip

import android.app.Application
import com.yangchengwei.easytrip.amap.AmapConsentStore
import com.yangchengwei.easytrip.amap.AmapPrivacyGate
import com.yangchengwei.easytrip.amap.ConsentRegistry
import com.yangchengwei.easytrip.amap.SharedPreferencesAmapConsentPersistence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal suspend fun decideAmapPrivacy(
    store: AmapConsentStore,
    accepted: Boolean,
): Result<Unit> = store.decide(accepted)

class EasyTripApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val themePreferences by lazy {
        com.yangchengwei.easytrip.core.ui.theme.ThemePreferenceStore(
            com.yangchengwei.easytrip.core.ui.theme.AtomicThemePersistence(java.io.File(filesDir, "theme-preference")),
        )
    }
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

    lateinit var amapConsentStore: AmapConsentStore
        private set

    override fun onCreate() {
        super.onCreate()
        amapConsentStore = AmapConsentStore(
            SharedPreferencesAmapConsentPersistence(getSharedPreferences("privacy", MODE_PRIVATE)),
            AmapPrivacyGate.create(this),
            ConsentRegistry(),
        )
    }
}
