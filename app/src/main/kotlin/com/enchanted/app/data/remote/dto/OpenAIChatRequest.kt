package com.enchanted.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Standard usage for reasoning on NVIDIA NIM:
 *
 *   chat_template_kwargs = { "thinking": true }
 *   reasoning_effort = "high"
 *
 * Different model families use different key names:
 * - DeepSeek V4/V3:   "thinking" (and optionally "reasoning_effort")
 * - GLM-5/4.7:        "enable_thinking"
 * - Qwen3/QwQ:        "enable_thinking"
 * - Nemotron 3:       "enable_thinking"
 * - MiniMax M2.5:     "thinking"
 *
 * Helper [thinkingKwargs] provides the most common default.
 */
@Serializable
data class OpenAIChatRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean,
    val temperature: Float? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val top_p: Float? = null,
    val frequency_penalty: Float? = null,
    val presence_penalty: Float? = null,
    val top_k: Int? = null,
    val n: Int? = null,
    val seed: Int? = null,
    val stop: List<String>? = null,
    @SerialName("reasoning_effort") val reasoningEffort: String? = null,
    @SerialName("chat_template_kwargs") val chatTemplateKwargs: Map<String, JsonElement>? = null,
    @SerialName("thinking_token_budget") val thinkingTokenBudget: Int? = null
) {
    companion object {
        /**
         * Returns the typical [chatTemplateKwargs] that enables thinking/reasoning
         * for the most common NVIDIA NIM model families.
         *
         * Override by supplying a different map when constructing the request.
         */
        fun thinkingKwargs(enable: Boolean = true): Map<String, JsonElement> =
            mapOf("thinking" to JsonPrimitive(enable))
    }
}

/**
 * Build [chatTemplateKwargs] that match a model prefix.
 * Extend this list as new model families appear.
 */
fun chatTemplateKwargsFor(model: String): Map<String, JsonElement>? {
    val lower = model.lowercase()
    val enable = mapOf("thinking" to JsonPrimitive(true))

    return when {
        // DeepSeek V4 / V3 / R1 — use "thinking" key
        lower.contains("deepseek") -> enable

        // GLM — use "enable_thinking"
        lower.contains("glm") || lower.contains("z-ai") ->
            mapOf("enable_thinking" to JsonPrimitive(true))

        // Kimi / Moonshot
        lower.contains("kimi") || lower.contains("moonshot") -> enable

        // Qwen / QwQ — use "enable_thinking"
        lower.contains("qwen") || lower.contains("qwq") ->
            mapOf("enable_thinking" to JsonPrimitive(true))

        // Nemotron — use "enable_thinking"
        lower.contains("nemotron") ->
            mapOf("enable_thinking" to JsonPrimitive(true))

        // MiniMax — use "thinking"
        lower.contains("minimax") -> enable

        // GPT-OSS
        lower.contains("gpt-oss") -> enable

        // Default: no kwargs; the model uses its own defaults
        else -> null
    }
}
