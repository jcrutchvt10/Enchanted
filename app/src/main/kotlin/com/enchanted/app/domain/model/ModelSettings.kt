package com.enchanted.app.domain.model

/**
 * Settings that control model generation behaviour and can significantly
 * impact both response quality and response time.
 *
 * All fields default to values that match the OpenAI‑compatible server
 * defaults (temperature=1.0, top_p=1.0, etc.) so the behaviour is
 * identical to the current hardcoded path when no customisation is applied.
 *
 * @property temperature           Sampling temperature (0 = greedy/deterministic).
 * @property maxTokens             Maximum output tokens (hard stop).
 * @property topP                  Nucleus sampling threshold.
 * @property topK                  Top‑K token limit; -1 = disabled.
 * @property frequencyPenalty      Penalise tokens by their frequency [-2, 2].
 * @property presencePenalty       Penalise tokens by their presence [-2, 2].
 * @property stop                  Stop sequences where generation halts.
 * @property seed                  Reproducible sampling seed.
 * @property thinkingTokenBudget   Max reasoning/CoT tokens (vLLM extra param).
 *                                 Only relevant for reasoning models.
 *                                 Default = unlimited (null).
 */
data class ModelSettings(
    val temperature: Float = 1.0f,
    val maxTokens: Int = 8192,
    val topP: Float = 1.0f,
    val topK: Int = -1,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val stop: List<String>? = null,
    val seed: Int? = null,
    val thinkingTokenBudget: Int? = null
)

/**
 * Returns optimal [ModelSettings] for the given model name based on
 * known model‑family characteristics.
 *
 * The goal is to balance response quality with response time:
 *   - **Deep reasoning models** (DeepSeek, GPT‑OSS) use greedy sampling and
 *     a limited thinking‑token budget (vLLM‑supported).
 *   - **Chat reasoning models** (MiniMax M2.x, QwQ) use moderate temperature
 *     for conversational output; they do NOT receive CoT kwargs (see
 *     [chatTemplateKwargsFor]) because the latency exceeds NVIDIA NIM's
 *     5‑minute gateway timeout on free‑tier accounts.
 *   - **Chat models** (Llama, Nemotron, Mistral, MiniMax M1) use moderate
 *     temperature for natural conversation.
 *   - **General‑purpose models** (Qwen, GLM, Kimi) use slightly higher
 *     temperature for versatility.
 *   - **Fallback** returns the server defaults.
 */
fun defaultSettingsFor(modelName: String): ModelSettings {
    val lower = modelName.lowercase()

    // Deep reasoning / chain‑of‑thought models — vLLM‑based, support
    // thinking_token_budget (DeepSeek, GPT‑OSS).  Greedy temperature is
    // appropriate for code / math.
    val isDeepReasoningModel = lower.contains("deepseek") ||
            lower.contains("gpt-oss")

    // Light reasoning / chat‑reasoning models — do NOT support
    // thinking_token_budget on NVIDIA NIM (MiniMax M2.x, QwQ).
    // Use moderate temperature for conversational reasoning.
    val isLightReasoningModel = lower.contains("qwq") ||
            (lower.contains("minimax") && lower.contains("m2."))

    // High‑throughput chat / instruct models (MiniMax M1 is a chat model)
    val isChatModel = lower.contains("llama") ||
            lower.contains("nemotron") ||
            lower.contains("mistral") ||
            // MiniMax M1 — non-reasoning chat model
            (lower.contains("minimax") && !lower.contains("m2."))

    // General‑purpose instruct models
    val isGeneralModel = lower.contains("qwen") ||
            lower.contains("glm") ||
            lower.contains("z-ai") ||
            lower.contains("kimi") ||
            lower.contains("moonshot")

    return when {
        isDeepReasoningModel -> ModelSettings(
            temperature = 0.0f,          // greedy — best for coding/reasoning
            maxTokens = 16384,           // reasoning needs room for CoT
            topP = 1.0f,
            thinkingTokenBudget = 6144   // vLLM param; cap thinking for bounded latency
        )
        isLightReasoningModel -> ModelSettings(
            temperature = 0.6f,          // moderate — conversational reasoning
            maxTokens = 4096,            // generous but keeps TTFT manageable
            topP = 0.95f
            // NO thinkingTokenBudget — MiniMax M2.x / QwQ don't support it
            // NO chat_template_kwargs / reasoning_effort — those are set
            // separately in the request pipeline.
        )
        isChatModel -> ModelSettings(
            temperature = 0.6f,
            maxTokens = 4096,
            topP = 0.95f
        )
        isGeneralModel -> ModelSettings(
            temperature = 0.7f,
            maxTokens = 4096,
            topP = 0.95f
        )
        else -> ModelSettings()
    }
}
