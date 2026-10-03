package com.yangchengwei.easytrip.place.amap

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.domain.PlaceCity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull

class LocationPermissionRequired : IllegalStateException("请允许定位权限，以搜索当前城市的地点")
class CurrentLocationUnavailable(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

/** In-memory application-session cache. Failed requests stay failed until an explicit retry. */
class AppLocationSession(
    private val scope: CoroutineScope,
    private val hasPermission: () -> Boolean,
    private val locate: suspend () -> GeoPoint,
    private val resolveCity: suspend (GeoPoint) -> PlaceCity?,
) {
    private val lock = Any()
    private var request: Deferred<Result<PlaceCity>>? = null
    private var revision = 0L
    private var closed = false
    private var point: GeoPoint? = null

    val location: GeoPoint? get() = synchronized(lock) { point }

    fun warmUp() {
        if (hasPermission()) synchronized(lock) { if (!closed) startRequest() }
    }

    suspend fun currentCity(): PlaceCity {
        while (true) {
            if (!hasPermission()) throw LocationPermissionRequired()
            val (generation, pending) = synchronized(lock) {
                check(!closed) { "地图服务已关闭" }
                revision to startRequest()
            }
            val result = try {
                pending.await()
            } catch (error: CancellationException) {
                currentCoroutineContext().ensureActive()
                if (synchronized(lock) { !closed && generation != revision }) continue
                throw error
            }
            if (synchronized(lock) { generation != revision }) continue
            if (!hasPermission()) throw LocationPermissionRequired()
            return result.getOrThrow()
        }
    }

    /** Explicit map locate overrides an older startup request, without moving any other map. */
    fun updateLocation(value: GeoPoint) = synchronized(lock) {
        if (closed) return@synchronized
        revision++
        request?.cancel()
        request = null
        point = value
    }

    fun retry() = synchronized(lock) {
        if (closed || request?.isActive == true) return@synchronized
        revision++
        request = null
    }

    fun close() = synchronized(lock) {
        closed = true
        revision++
        request?.cancel()
        request = null
        point = null
    }

    private fun startRequest(): Deferred<Result<PlaceCity>> {
        request?.let { return it }
        val generation = revision
        val knownPoint = point
        return scope.async(start = CoroutineStart.LAZY) {
            try {
                val city = withTimeoutOrNull(20_000) {
                    val fix = knownPoint ?: locate()
                    synchronized(lock) { if (!closed && revision == generation) point = fix }
                    resolveCity(fix)?.takeIf { it.name.isNotBlank() }
                        ?: throw CurrentLocationUnavailable("无法识别当前定位城市，请重试")
                } ?: throw CurrentLocationUnavailable("定位超时，请检查系统定位服务后重试")
                Result.success(city)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Result.failure(
                    if (error is LocationPermissionRequired || error is CurrentLocationUnavailable) error
                    else CurrentLocationUnavailable("无法获取当前城市，请检查定位和网络后重试", error),
                )
            }
        }.also { request = it; it.start() }
    }
}
