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
 *
 * **CRITICAL FOR PERFORMANCE:** Enabling `thinking` (a.k.a. chain‑of‑thought)
 * forces the model to generate internal reasoning tokens *before* any visible
 * output, which dramatically increases Time‑To‑First‑Token (TTFT) and total
 * response time.  Only set it for models that genuinely need it.
 *
 * Models that require `thinking` to produce ANY output — DeepSeek — remain
 * here.  Reasoning‑capable chat models such as MiniMax M2.x are excluded
 * because enabling thinking  makes TTFT exceed the 5‑minute NVIDIA NIM
 * gateway timeout on the free tier.  They work well as fast instruct models
 * without explicit kwargs.
 *
 * Extend this list as new model families appear.
 */
fun chatTemplateKwargsFor(model: String): Map<String, JsonElement>? {
    val lower = model.lowercase()

    // ── Models that REQUIRE thinking for correct behaviour ──────────────────
    // DeepSeek V4 / V3 / R1: does not produce valid responses without
    // "thinking": true in chat_template_kwargs.
    if (lower.contains("deepseek")) {
        return mapOf("thinking" to JsonPrimitive(true))
    }

    // GPT‑OSS (Open‑Source reasoning model)
    if (lower.contains("gpt-oss")) {
        return mapOf("thinking" to JsonPrimitive(true))
    }

    // ── Models where thinking is OPTIONAL (latency cost) ────────────────────
    // Only enable when the model name explicitly indicates a reasoning variant.

    // QwQ (Qwen reasoning model) — use "enable_thinking"
    if (lower.contains("qwq")) {
        return mapOf("enable_thinking" to JsonPrimitive(true))
    }

    // Kimi reasoning models
    if (lower.contains("kimi") && lower.contains("reasoning")) {
        return mapOf("thinking" to JsonPrimitive(true))
    }

    // Default: no kwargs — the model uses its own defaults, providing the
    // fastest possible response time for chat / instruct use.
    return null
}
