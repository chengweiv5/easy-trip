package com.yangchengwei.easytrip.amap

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.location.AMapLocation
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.CurrentLocationUnavailable
import com.yangchengwei.easytrip.place.amap.LocationPermissionRequired
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

fun hasDeviceLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/** One foreground request, never a continuous/background location service. */
internal class AmapCurrentLocation(
    context: Context,
    private val consent: AmapConsentToken,
) {
    private val context = context.applicationContext

    suspend fun locate(): GeoPoint = withContext(Dispatchers.Main.immediate) {
        consent.validateActive()
        if (!hasDeviceLocationPermission(context)) throw LocationPermissionRequired()
        // Restore only the previously accepted SDK privacy decision, before constructing the client.
        AmapPrivacyGate.create(context).apply { reportShown(); reportDecision(true) }
        consent.validateActive()
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTPS)
        val client = AMapLocationClient(context)
        try {
            client.setLocationOption(AMapLocationClientOption().apply {
                isOnceLocation = true
                isNeedAddress = false
                isLocationCacheEnable = false
                httpTimeOut = 10_000
            })
            val location = withTimeoutOrNull(15_000) {
                awaitSdkCallback("CURRENT_LOCATION", object : CallbackBoundary<AMapLocation?> {
                    private var locationListener: AMapLocationListener? = null
                    override fun install(listener: (Result<AMapLocation?>) -> Unit) {
                        locationListener = AMapLocationListener { location ->
                            listener(Result.success(location))
                        }.also(client::setLocationListener)
                    }
                    override fun start() {
                        consent.validateActive()
                        client.startLocation()
                    }
                    override fun clear() {
                        locationListener?.let(client::unRegisterLocationListener)
                        locationListener = null
                    }
                }) { it }
            } ?: throw CurrentLocationUnavailable("定位超时，请检查系统定位服务后重试")
            consent.validateActive()
            if (!hasDeviceLocationPermission(context)) throw LocationPermissionRequired()
            if (location.errorCode != 0 ||
                !location.latitude.isFinite() || !location.longitude.isFinite() ||
                location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0 ||
                (location.latitude == 0.0 && location.longitude == 0.0)
            ) throw CurrentLocationUnavailable("定位失败（${location.errorCode}），请检查系统定位服务后重试")
            GeoPoint(location.latitude, location.longitude)
        } finally {
            try { client.stopLocation() } finally { client.onDestroy() }
        }
    }
}
