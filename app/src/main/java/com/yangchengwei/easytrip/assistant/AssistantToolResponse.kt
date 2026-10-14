package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.*

enum class AssistantProtocolIssue(val repairable: Boolean) {
    INVALID_RESPONSE(false), INVALID_CHOICES(false), TRUNCATED(false), UNSUPPORTED_FINISH(false),
    MISSING_TOOL(true), MULTIPLE_TOOLS(false), UNEXPECTED_TOOL(false), INVALID_ARGUMENTS(true),
    REQUIRED_FIELDS(true), INVALID_FIELDS(true), SOURCE_MISMATCH(false), TARGET_EVIDENCE(false),
    INVALID_GROUPS(false),
}

internal class ProtocolViolation(val issue: AssistantProtocolIssue) : Exception()
internal inline fun <T> protocolSection(issue: AssistantProtocolIssue, block: () -> T): T = try {
    block()
} catch (e: ProtocolViolation) { throw e }
catch (e: AssistantFailure) { throw e }
catch (e: kotlinx.coroutines.CancellationException) { throw e }
catch (_: Exception) { throw ProtocolViolation(issue) }

internal fun protocolRequire(condition: Boolean, issue: AssistantProtocolIssue) {
    if (!condition) throw ProtocolViolation(issue)
}

internal fun protocolFailure(error: Exception, stage: AssistantProtocolStage, fallback: AssistantProtocolIssue): AssistantFailure {
    val issue = (error as? ProtocolViolation)?.issue ?: fallback
    val subject = when (stage) {
        AssistantProtocolStage.IMAGE -> "图片理解"
        AssistantProtocolStage.TEXT -> "文字理解"
        AssistantProtocolStage.NORMALIZATION -> "地点整理"
    }
    val detail = when (issue) {
        AssistantProtocolIssue.TRUNCATED -> "模型输出被截断"
        AssistantProtocolIssue.SOURCE_MISMATCH -> "地点名称或城市与来源不符"
        AssistantProtocolIssue.TARGET_EVIDENCE -> "地点来源证据不完整"
        AssistantProtocolIssue.INVALID_GROUPS -> "地点分组不完整或有冲突"
        AssistantProtocolIssue.MISSING_TOOL -> "未返回要求的结构化结果"
        AssistantProtocolIssue.MULTIPLE_TOOLS, AssistantProtocolIssue.UNEXPECTED_TOOL -> "返回了非预期工具调用"
        AssistantProtocolIssue.UNSUPPORTED_FINISH -> "模型未正常完成输出"
        AssistantProtocolIssue.REQUIRED_FIELDS -> "返回结果缺少必要字段"
        else -> "返回格式不完整"
    }
    val retry = if (stage == AssistantProtocolStage.NORMALIZATION) "请重试整理" else "可重试失败素材"
    return AssistantFailure("$subject：$detail；未查询地点，$retry。",
        AssistantFailureCode.PROTOCOL, SendEvidence.RESPONSE_RECEIVED, issue)
}

/** Reads data only. Unknown fields never acquire tool authority or enter domain objects. */
internal object AssistantToolResponse {
    fun arguments(response: String, tool: String): JsonObject {
        val choices = protocolSection(AssistantProtocolIssue.INVALID_RESPONSE) {
            Json.parseToJsonElement(response).jsonObject.getValue("choices").jsonArray
        }
        protocolRequire(choices.size == 1, AssistantProtocolIssue.INVALID_CHOICES)
        val choice = protocolSection(AssistantProtocolIssue.INVALID_RESPONSE) { choices.single().jsonObject }
        // Some compatible providers finish a complete call with "stop". Never accept
        // length/content_filter/unknown endings, even if the JSON happens to be parseable.
        val finish = protocolSection(AssistantProtocolIssue.UNSUPPORTED_FINISH) { choice["finish_reason"]?.jsonPrimitive?.content }
        protocolRequire(finish != "length", AssistantProtocolIssue.TRUNCATED)
        protocolRequire(finish in setOf("tool_calls", "stop"), AssistantProtocolIssue.UNSUPPORTED_FINISH)
        val calls = protocolSection(AssistantProtocolIssue.MISSING_TOOL) {
            choice.getValue("message").jsonObject.getValue("tool_calls").jsonArray
        }
        protocolRequire(calls.isNotEmpty(), AssistantProtocolIssue.MISSING_TOOL)
        protocolRequire(calls.size == 1, AssistantProtocolIssue.MULTIPLE_TOOLS)
        val function = protocolSection(AssistantProtocolIssue.UNEXPECTED_TOOL) {
            val call = calls.single().jsonObject
            require(call["type"]?.jsonPrimitive?.content == "function")
            call.getValue("function").jsonObject.also { require(it["name"]?.jsonPrimitive?.content == tool) }
        }
        return protocolSection(AssistantProtocolIssue.INVALID_ARGUMENTS) {
            val arguments = function.getValue("arguments").jsonPrimitive
            require(arguments.isString)
            Json.parseToJsonElement(arguments.content).jsonObject
        }
    }

    fun repairRequest(original: String): String {
        val root = Json.parseToJsonElement(original).jsonObject.toMutableMap()
        val messages = root.getValue("messages").jsonArray.toMutableList()
        val system = messages.first().jsonObject.toMutableMap()
        system["content"] = JsonPrimitive(system.getValue("content").jsonPrimitive.content +
            "\n上次返回未通过格式校验。本次只修正返回格式，重新核对同一份原始资料；完整返回工具要求的字段和全部项目。" +
            "不补造地点、城市或来源证据，不因格式修复省略项目。无地点也须按工具结构返回，不省略必要字段；不要输出解释文字。")
        messages[0] = JsonObject(system)
        root["messages"] = JsonArray(messages)
        return JsonObject(root).toString()
    }
}
