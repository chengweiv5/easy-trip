package com.yangchengwei.easytrip.assistant

object GuideEntityValidation {
    fun locate(rawQuote: String, literal: String, occurrence: Int): IntRange? {
        if (literal.isEmpty() || occurrence < 0 || occurrence > rawQuote.length) return null
        var start = 0
        repeat(occurrence + 1) { index ->
            val found = rawQuote.indexOf(literal, start)
            if (found < 0) return null
            if (index == occurrence) return found until found + literal.length
            start = found + literal.length
        }
        return null
    }

    internal fun completeBracketParts(quote: String): Pair<String, String>? {
        val match = Regex("^([^（）()]+)[（(]([^（）()]+)[）)]$").matchEntire(quote.trim()) ?: return null
        val outside = match.groupValues[1].trim()
        val inside = match.groupValues[2].trim()
        val gate = Regex("^(.+?)(?:东|南|西|北|东北|东南|西北|西南)?(?:大门|城门|门|入口|出口)$")
            .matchEntire(inside) ?: return null
        val parent = gate.groupValues[1]
        if (inside in setOf("南门", "北门", "西门", "东门", "入口", "出口") ||
            outside == parent || inside.startsWith(outside) ||
            outside.any(Char::isDigit)) return null
        return outside to inside
    }
}
