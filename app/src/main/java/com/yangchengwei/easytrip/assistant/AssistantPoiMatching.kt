package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.explicitlyNamesCity
import java.text.Normalizer

internal fun PlaceCandidate.validPosition(): Boolean = poiId.isNotBlank() && name.isNotBlank() &&
    point?.let { it.latitude.isFinite() && it.longitude.isFinite() &&
        it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 } == true

internal fun normalizedPlace(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase().filter { it.isLetterOrDigit() }

internal fun sameCity(first: String?, second: String): Boolean =
    !first.isNullOrBlank() && normalizedPlace(first).removeSuffix("市") == normalizedPlace(second).removeSuffix("市")

internal fun PlaceIntent.hasExplicitCityIn(text: String): Boolean =
    explicitlyNamesCity(text, city)

internal fun independentPlaceQualifier(value: String): String? {
    val normalized = normalizedPlace(value).removeSuffix("城楼")
    return Regex("(东北|东南|西北|西南|东|南|西|北)(门|出入口|入口|出口)$").find(normalized)?.value
        ?: Regex("[（(]([^）)]+)[）)]").findAll(value).map { it.groupValues[1] }
            .firstOrNull { it in setOf("城门", "城楼", "入口", "出口", "出入口") }
}

internal fun matchItem(item: IntakeItem, results: List<PlaceCandidate>): IntakeItem {
    val valid = results.filter { it.validPosition() }.distinctBy { it.poiId }.take(20)
    if (valid.isEmpty()) return item.copy(status = IntakeStatus.NOT_FOUND, detail = "未找到有真实坐标的地点，请补充名称或地址。")
    val sameCity = valid.filter { sameCity(it.cityName, item.intent.city) }
    if (sameCity.isEmpty()) return item.copy(status = IntakeStatus.NEED_CITY, candidates = valid,
        detail = "结果城市缺失或与输入不一致，请逐项核对城市和地址。")
    val qualifier = independentPlaceQualifier(item.intent.query)
    val matching = if (qualifier == null) sameCity else
        sameCity.filter { independentPlaceQualifier(it.name) == qualifier }
    if (matching.isEmpty()) return item.copy(status = IntakeStatus.AMBIGUOUS, candidates = valid, poi = null,
        detail = "尚未匹配到名称中的独立入口或城门，请核对候选；不会默认选择所属景点。")
    return item.copy(status = IntakeStatus.READY, candidates = valid, poi = matching.first(), detail = null)
}
