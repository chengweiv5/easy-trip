package com.yangchengwei.easytrip.assistant

import java.util.Locale
import kotlinx.serialization.json.*

/** Transient model evidence. Only filtered PlaceIntent values enter existing caches/journals. */
internal object GuidePlaceClassification {
    // Structured classification and quotations cost more than a name-only
    // response. Keep the 100-place input limit without silently truncating it.
    const val MAX_OUTPUT_TOKENS = 32768
    const val RULES = """每个候选附classification，分类而不是用名称关键词决定保留：
purpose取SIGHTSEEING（独立游览）、LODGING（酒店/客栈/民宿/旅舍等住宿，即使明确推荐）、DINING（餐饮/食品/餐饮地址片段）、TRANSPORT（仅抵离换乘集合）、OTHER或UNKNOWN。
granularity取DESTINATION（整篇攻略独立一站）、INTERNAL_DETAIL（园内舞台/布景/展厅/普通入口/服务设施）、REFERENCE（仅方位距离/住宿区域参考）、BACKGROUND（底图背景）或UNKNOWN。
independentVisit只在资料将该实体本身作为独立游览目的地时为true；作者编号/图钉/推荐不能单独证明独立性。普通出入口与可独立参观的历史建筑须按本次用途区分。
先复制包含名称和紧邻用途说明的完整原句作为sourceSpan，再把同一原句原样复制到evidenceQuote。保留换行和emoji；禁止删词、拼接不同位置或用自己的标点连接。不能用自己生成的“独立景点”等结论代替。图片只引用本图可读文字，不能引用其他图或城市常识。
按本候选自身分类，不能借用其父景点的游览价值。带方向的普通入口标签即使配有古城/公园的介绍，仍是INTERNAL_DETAIL；父景点另外提取，只能用本图/原文中真实出现的父名，不能把普通门作为父景点别名。介绍古街体验、城墙夜景不等于介绍这座门自身的历史建筑价值。
住宿区域介绍中“附近/近/离某处不远/步行可达”等只证明位置便利，该段的周边地点是REFERENCE，不是推荐游览，不能因为该地点别处值得游览而改写本段用途。
应用只保留SIGHTSEEING+DESTINATION+independentVisit=true；住宿、餐饮、交通、内部细节及背景均剔除；信息不足返回UNKNOWN交由核对，不猜测为景点。"""

    enum class Decision { KEEP, DROP, REVIEW }
    private val purposes = setOf("SIGHTSEEING", "LODGING", "DINING", "TRANSPORT", "OTHER", "UNKNOWN")
    private val levels = setOf("DESTINATION", "INTERNAL_DETAIL", "REFERENCE", "BACKGROUND", "UNKNOWN")

    fun schema() = buildJsonObject {
        put("type", "object"); put("additionalProperties", false)
        putJsonArray("required") { listOf("purpose", "granularity", "independentVisit", "evidenceQuote").forEach(::add) }
        putJsonObject("properties") {
            for ((key, values) in listOf("purpose" to purposes, "granularity" to levels)) putJsonObject(key) {
                put("type", "string"); put("enum", JsonArray(values.map(::JsonPrimitive)))
                put("description", if (key == "purpose")
                    "本段为何提到本名称，不是常识里的地点类型：推荐住宿=LODGING，吃喝=DINING，独立参观=SIGHTSEEING"
                    else "住宿方位/周边便利=REFERENCE；普通出入口/园内细节=INTERNAL_DETAIL；底图文字=BACKGROUND；只有本名称自身是独立游览一站才是DESTINATION")
            }
            putJsonObject("independentVisit") {
                put("type", "boolean")
                put("description", "只依据本名称自身的独立参观安排；普通入口不能借用父景点介绍，住宿周边参照也填false")
            }
            putJsonObject("evidenceQuote") {
                put("type", "string"); put("maxLength", 500)
                put("description", "原样复制本项sourceSpan；整理阶段原样复制组内一个已有excerpt，不能拼接改写")
            }
        }
    }

    fun decide(item: JsonObject, evidence: List<String>): Decision {
        protocolRequire(item["classification"] is JsonObject, AssistantProtocolIssue.REQUIRED_FIELDS)
        val c = item.getValue("classification").jsonObject
        protocolRequire(c.keys.containsAll(setOf("purpose", "granularity", "independentVisit", "evidenceQuote")),
            AssistantProtocolIssue.REQUIRED_FIELDS)
        require(listOf("purpose", "granularity", "evidenceQuote").all { c.getValue(it).jsonPrimitive.isString })
        require(!c.getValue("independentVisit").jsonPrimitive.isString)
        val purpose = c.getValue("purpose").jsonPrimitive.content.trim().uppercase(Locale.ROOT)
        val level = c.getValue("granularity").jsonPrimitive.content.trim().uppercase(Locale.ROOT)
        val independent = c.getValue("independentVisit").jsonPrimitive.boolean
        val quote = c.getValue("evidenceQuote").jsonPrimitive.content.trim()
        require(quote.length <= 500)
        // A known excluded use never acquires an exception through a recommendation,
        // a missing quotation, a conflicting flag, or an unrecognised finer category.
        if (purpose in purposes - setOf("SIGHTSEEING", "UNKNOWN") ||
            level in levels - setOf("DESTINATION", "UNKNOWN")) return Decision.DROP
        // A parenthesized directional entrance is a navigation qualifier, not
        // evidence about a separately named historical gate. Do not strip it
        // and manufacture the parent as an alias.
        val name = item["query"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (isNavigationEntrance(name)) return Decision.DROP
        if (purpose != "SIGHTSEEING" || level != "DESTINATION" || !independent ||
            quote.isBlank() || evidence.none { originalQuote(it, quote) != null }) return Decision.REVIEW
        return Decision.KEEP
    }

    fun apply(intent: PlaceIntent, item: JsonObject, evidence: List<String>): PlaceIntent? =
        when (decide(item, evidence)) {
            Decision.DROP -> null
            Decision.REVIEW -> intent.copy(needsReview = true)
            Decision.KEEP -> if (Regex("[\\p{IsHan}]{2}[./／、→][\\p{IsHan}]{2}")
                    .containsMatchIn(intent.query)) intent.copy(needsReview = true) else intent
        }

    fun quote(item: JsonObject): String =
        item["classification"]?.jsonObject?.get("evidenceQuote")?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

    /** Labels and captions are separate on a map. Accept only a quoted label,
     * never an inferred name embedded in the model's explanation. */
    fun quotedImageLabel(item: JsonObject, name: String): String? {
        val detail = item["targetDetail"]?.jsonPrimitive?.contentOrNull ?: return null
        return Regex("[“「『\"]([^“”「」『』\"]{1,100})[”」』\"]").findAll(detail)
            .map { it.groupValues[1] }.firstOrNull { normalizedPlace(it) == normalizedPlace(name) }
    }

    /** Recover a nearby name/caption pair from the original text, without
     * joining unrelated paragraphs or manufacturing a quotation. */
    fun nearbyNamedQuote(source: String, name: String, quote: String): String? {
        if (name.isBlank() || quote.isBlank()) return null
        val exact = originalQuote(source, quote) ?: return null
        val quoteStart = source.indexOf(exact)
        val nameStart = source.lastIndexOf(name, quoteStart)
        if (nameStart < 0) return null
        val end = quoteStart + exact.length
        val span = source.substring(nameStart, end)
        return span.takeIf { it.length <= 500 && it.count { c -> c == '\n' } <= 1 &&
            !it.contains("\n\n") }
    }

    fun isNavigationEntrance(name: String): Boolean =
        Regex("[（(][^（）()]{0,50}(?:(?:东北|东南|西北|西南|东|南|西|北)门|入口|出口|出入口)[）)]$")
            .containsMatchIn(name)

    /** Ignore layout whitespace only; return the actual source, never a rewritten quotation. */
    fun originalQuote(source: String, quote: String): String? {
        val needle = quote.filterNot(Char::isWhitespace)
        if (needle.isEmpty()) return null
        val positions = source.indices.filter { !source[it].isWhitespace() }
        val offset = positions.map { source[it] }.joinToString("").indexOf(needle)
        if (offset < 0) return null
        return source.substring(positions[offset], positions[offset + needle.length - 1] + 1)
    }
}
