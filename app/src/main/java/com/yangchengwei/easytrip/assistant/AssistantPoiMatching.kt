package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import java.text.Normalizer

internal fun PlaceCandidate.validPosition(): Boolean = poiId.isNotBlank() && name.isNotBlank() &&
    point?.let { it.latitude.isFinite() && it.longitude.isFinite() &&
        it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 } == true

internal fun normalizedPlace(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase().filter { it.isLetterOrDigit() }

internal fun sameCity(first: String?, second: String): Boolean =
    !first.isNullOrBlank() && normalizedPlace(first).removeSuffix("市") == normalizedPlace(second).removeSuffix("市")

internal fun matchItem(item: IntakeItem, results: List<PlaceCandidate>): IntakeItem {
    val valid = results.filter { it.validPosition() }.distinctBy { it.poiId }.take(20)
    if (valid.isEmpty()) return item.copy(status = IntakeStatus.NOT_FOUND, detail = "未找到有真实坐标的地点，请补充名称或地址。")
    val sameCity = valid.filter { sameCity(it.cityName, item.intent.city) }
    if (sameCity.isEmpty()) return item.copy(status = IntakeStatus.NEED_CITY, candidates = valid,
        detail = "结果城市缺失或与输入不一致，请逐项核对城市和地址。")
    val exact = sameCity.filter { normalizedPlace(it.name) == normalizedPlace(item.intent.query) }
    return if (exact.size == 1) item.copy(status = IntakeStatus.READY, candidates = valid, poi = exact.single(), detail = null)
    else item.copy(status = IntakeStatus.AMBIGUOUS, candidates = valid, detail = "请选择具体位置，不会默认采用第一条结果。")
}
