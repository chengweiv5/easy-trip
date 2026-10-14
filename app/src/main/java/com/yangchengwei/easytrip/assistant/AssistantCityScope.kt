package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.*

/** Confirmed destination scope; multiple cities deliberately have no implicit first-city default. */
data class AssistantCityScope(val names: List<String>) {
    init {
        require(names.isNotEmpty() && names.size <= 30)
        require(names.all { it == it.trim() && it.length in 1..50 && it.none(Char::isISOControl) })
        require(names.distinct().size == names.size)
    }
    val defaultCity get() = names.singleOrNull()
    fun encode() = JsonArray(names.map(::JsonPrimitive)).toString()
    companion object {
        fun parse(text: String) = AssistantCityScope(text.split(Regex("[、,，;；\\n]"))
            .map(String::trim).filter(String::isNotEmpty).distinct())
        fun decode(text: String) = AssistantCityScope(Json.parseToJsonElement(text).jsonArray.map { it.jsonPrimitive.content })
    }
}

/** Context belongs in its own user message, never in the author's quoted material. */
fun withCityScope(request: String, scope: AssistantCityScope?): String {
    if (scope == null) return request
    val root = Json.parseToJsonElement(request).jsonObject
    return JsonObject(root + ("messages" to JsonArray(root.getValue("messages").jsonArray + buildJsonObject {
        put("role", "user")
        put("content", "已确认目的地城市范围：${scope.encode()}。这是上下文，不是作者原文或地点来源。单城可作为缺省城市；多城按各地点证据分别归属，无法确定则city为空，不得全部套用第一城。sourceSpan仍只能引用原素材。")
    }))).toString()
}
