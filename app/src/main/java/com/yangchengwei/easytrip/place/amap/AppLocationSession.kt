package com.yangchengwei.easytrip.place.amap

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.domain.PlaceCity
import com.yangchengwei.easytrip.place.domain.LocatedPosition
import com.yangchengwei.easytrip.place.domain.isUsableLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationPermissionRequired : IllegalStateException("请允许定位权限，以搜索当前城市的地点")
class CurrentLocationUnavailable(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

/** In-memory cache. Explicit search/retry clears failures; typing reuses the current attempt. */
class AppLocationSession(
    private val scope: CoroutineScope,
    private val hasPermission: () -> Boolean,
    private val locate: suspend () -> GeoPoint,
    private val resolveCity: suspend (GeoPoint) -> PlaceCity?,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val lock = Any()
    private var request: Deferred<Result<PlaceCity>>? = null
    private var requestFailed = false
    private var revision = 0L
    private var closed = false
    private var point: GeoPoint? = null
    private var locatedAt: Long? = null
    private val mutablePosition = MutableStateFlow<LocatedPosition?>(null)
    val position = mutablePosition.asStateFlow()

    val location: GeoPoint? get() = synchronized(lock) { point }

    fun warmUp() {
        synchronized(lock) {
            if (closed) return
            if (!hasPermission()) clearLocation() else startRequest()
        }
    }

    suspend fun currentCity(): PlaceCity {
        while (true) {
            if (!hasPermission()) {
                synchronized(lock) { clearLocation() }
                throw LocationPermissionRequired()
            }
            val (generation, pending) = synchronized(lock) {
                check(!closed) { "地图服务已关闭" }
                val active = startRequest()
                revision to active
            }
            val result = try {
                pending.await()
            } catch (error: CancellationException) {
                currentCoroutineContext().ensureActive()
                if (synchronized(lock) { !closed && generation != revision }) continue
                throw error
            }
            if (synchronized(lock) { generation != revision }) continue
            if (!hasPermission()) {
                synchronized(lock) { clearLocation() }
                throw LocationPermissionRequired()
            }
            return result.getOrThrow()
        }
    }

    /** Explicit map locate overrides an older startup request, without moving any other map. */
    fun updateLocation(value: GeoPoint) = synchronized(lock) {
        if (closed || !hasPermission() || !value.isUsableLocation()) return@synchronized
        revision++
        request?.cancel()
        request = null
        point = value
        locatedAt = nowMillis()
        mutablePosition.value = LocatedPosition(value)
        startRequest()
    }

    /** Keep successful fixes and active requests; only a completed failure can be retried. */
    fun retry() = synchronized(lock) {
        if (closed || request?.isActive == true || !requestFailed) return@synchronized
        revision++
        request = null
        requestFailed = false
    }

    fun close() = synchronized(lock) {
        closed = true
        clearLocation()
    }

    private fun clearLocation() {
        revision++
        request?.cancel()
        request = null
        requestFailed = false
        point = null
        locatedAt = null
        mutablePosition.value = null
    }

    private fun startRequest(): Deferred<Result<PlaceCity>> {
        // Refresh on foreground entry/search after two minutes; never start a background loop.
        if (request?.isActive != true && locatedAt?.let { nowMillis() - it !in 0..120_000 } == true) {
            clearLocation()
        }
        request?.let { return it }
        requestFailed = false
        val generation = revision
        val knownPoint = point
        return scope.async(start = CoroutineStart.LAZY) {
            try {
                val city = withTimeoutOrNull(20_000) {
                    val fix = knownPoint ?: locate()
                    if (!fix.isUsableLocation()) throw CurrentLocationUnavailable("无法获取有效当前位置，请重试")
                    synchronized(lock) {
                        if (!closed && revision == generation && hasPermission()) {
                            point = fix
                            locatedAt = nowMillis()
                            mutablePosition.value = LocatedPosition(fix)
                        }
                    }
                    val resolved = resolveCity(fix)?.takeIf { it.name.isNotBlank() }
                        ?: throw CurrentLocationUnavailable("无法识别当前定位城市，请重试")
                    synchronized(lock) {
                        if (!closed && revision == generation && hasPermission()) mutablePosition.value = LocatedPosition(fix, resolved)
                    }
                    resolved
                } ?: throw CurrentLocationUnavailable("定位超时，请检查系统定位服务后重试")
                Result.success(city)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                synchronized(lock) {
                    if (!closed && revision == generation) requestFailed = true
                }
                Result.failure(
                    if (error is LocationPermissionRequired || error is CurrentLocationUnavailable) error
                    else CurrentLocationUnavailable("无法获取当前城市，请检查定位和网络后重试", error),
                )
            }
        }.also { request = it; it.start() }
    }
}
