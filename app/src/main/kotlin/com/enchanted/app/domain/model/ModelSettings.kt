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
 *   - **Reasoning models** (DeepSeek, QwQ, GPT‑OSS) use greedy sampling
 *     (deterministic, best for coding/math) and a limited thinking‑token
 *     budget so the chain‑of‑thought doesn't run unbounded.
 *   - **Chat models** (Llama, Nemotron, Mistral, MiniMax) use moderate
 *     temperature for natural conversation and a comfortable max‑tokens cap.
 *   - **General‑purpose models** (Qwen, GLM, Kimi) use slightly higher
 *     temperature for versatility.
 *   - **Fallback** returns the server defaults.
 */
fun defaultSettingsFor(modelName: String): ModelSettings {
    val lower = modelName.lowercase()

    // Reasoning / chain‑of‑thought models (DeepSeek V4/R1, QwQ, GPT‑OSS)
    val isReasoningModel = lower.contains("deepseek") ||
            lower.contains("qwq") ||
            lower.contains("gpt-oss")

    // High‑throughput chat / instruct models
    val isChatModel = lower.contains("llama") ||
            lower.contains("nemotron") ||
            lower.contains("mistral") ||
            lower.contains("minimax")

    // General‑purpose instruct models
    val isGeneralModel = lower.contains("qwen") ||
            lower.contains("glm") ||
            lower.contains("z-ai") ||
            lower.contains("kimi") ||
            lower.contains("moonshot")

    return when {
        isReasoningModel -> ModelSettings(
            temperature = 0.0f,          // greedy — best for coding/reasoning
            maxTokens = 16384,           // reasoning needs room for CoT
            topP = 1.0f,
            thinkingTokenBudget = 4096   // cap thinking to bound response time
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
