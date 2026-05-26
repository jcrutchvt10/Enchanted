package com.enchanted.app.data.remote

import android.util.Log
import com.enchanted.app.data.SettingsDataStore
import com.enchanted.app.data.remote.dto.ChatMessageDto
import com.enchanted.app.data.remote.dto.ChatOptionsDto
import com.enchanted.app.data.remote.dto.ChatRequest
import com.enchanted.app.data.remote.dto.ChatResponse
import com.enchanted.app.data.remote.dto.ChatResponseMessageDto
import com.enchanted.app.data.remote.dto.ModelDto
import com.enchanted.app.data.remote.dto.ModelsResponse
import com.enchanted.app.data.remote.dto.OpenAIChatRequest
import com.enchanted.app.data.remote.dto.OpenAIChatResponse
import com.enchanted.app.data.remote.dto.OpenAIModelsResponse
import com.enchanted.app.data.remote.dto.chatTemplateKwargsFor
import com.enchanted.app.domain.model.ModelSettings
import com.enchanted.app.domain.model.defaultSettingsFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client for OpenAI‑compatible endpoints such as NVIDIA NIM.
 *
 * Automatically detects whether the configured base URL speaks the OpenAI API
 * (e.g. NVIDIA NIM, any /v1-style endpoint) or the native Ollama API, and
 * dispatches requests accordingly.
 *
 * Streaming:
 * - Standard SSE (`data: {json} … data: [DONE]`).
 * - Reasoning/thinking content is detected in the delta under these field names
 *   (checked in order): [reasoning_content], [reasoning], [thought], [thinking].
 * - Inline `<think>` / `</think>` tags inside the `content` field are also
 *   detected and rendered with the same think-block wrapping.
 *
 * Reasoning models (DeepSeek V4, Qwen3, Nemotron, GLM, etc.) typically require
 * a `chat_template_kwargs` object and sometimes `reasoning_effort`. The client
 * infers sensible defaults from the model name — see [chatTemplateKwargsFor].
 */
@Singleton
class NimClient @Inject constructor(
    private val ollamaApi: OllamaApi,
    private val settingsDataStore: SettingsDataStore
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var isOpenAI: Boolean? = null

    private suspend fun ensureEndpointType() {
        if (isOpenAI == null) {
            Log.d("NimClient", "Endpoint type unknown, probing...")
            reachable()
        }
        if (isOpenAI == true) {
            val token = settingsDataStore.getBearerToken()
            if (token.isNullOrBlank()) {
                Log.w("NimClient", "OpenAI‑compatible endpoint detected but no bearer token is set. Calls may fail with 401.")
            }
        }
    }

    suspend fun listModels(): ModelsResponse {
        ensureEndpointType()
        return try {
            if (isOpenAI == true) {
                val openAIResponse = ollamaApi.listModelsOpenAI()
                ModelsResponse(models = openAIResponse.data.map { ModelDto(name = it.id) })
            } else {
                ollamaApi.listModels()
            }
        } catch (e: Exception) {
            Log.e("NimClient", "Failed to list models", e)
            throw e
        }
    }

    /**
     * Non-streaming chat (simple round-trip).
     * Preserves [reasoning] content from the response message object when
     * the model returns it alongside [content].
     */
    suspend fun chat(
        model: String,
        messages: List<ChatMessageDto>,
        settings: ModelSettings? = null
    ): ChatResponse {
        ensureEndpointType()
        val s = settings ?: defaultSettingsFor(model)
        return if (isOpenAI == true) {
            val request = OpenAIChatRequest(
                model = model,
                messages = messages,
                stream = false,
                temperature = s.temperature,
                maxTokens = s.maxTokens,
                top_p = s.topP,
                top_k = s.topK,
                frequency_penalty = s.frequencyPenalty,
                presence_penalty = s.presencePenalty,
                stop = s.stop,
                seed = s.seed,
                chatTemplateKwargs = resolveTemplateKwargs(model),
                thinkingTokenBudget = s.thinkingTokenBudget
            )
            val response = ollamaApi.chatOpenAI(request)
            val choice = response.choices.firstOrNull()
            val reasoningContent = choice?.message?.reasoning
            ChatResponse(
                model = model,
                message = choice?.message?.let {
                    val fullContent = if (!reasoningContent.isNullOrBlank()) {
                        "<think>\n${reasoningContent}\n</think>\n\n${it.content}"
                    } else {
                        it.content
                    }
                    ChatResponseMessageDto(role = it.role, content = fullContent)
                },
                done = true
            )
        } else {
            val request = ChatRequest(
                model = model,
                messages = messages,
                stream = false,
                options = ChatOptionsDto(
                    temperature = s.temperature,
                    topP = s.topP,
                    topK = s.topK,
                    numPredict = s.maxTokens,
                    stop = s.stop,
                    frequencyPenalty = s.frequencyPenalty,
                    presencePenalty = s.presencePenalty,
                    seed = s.seed
                ).takeIf { it.hasAny() }
            )
            ollamaApi.chat(request)
        }
    }

    /**
     * Streaming chat. Calls [onChunk] for each content delta.
     * [onChunk] receives (content: String, isDone: Boolean).
     *
     * Reasoning/thinking handling:
     * 1. If the delta contains a dedicated reasoning field
     *    ([reasoning_content], [reasoning], [thought], [thinking]),
     *    the text is wrapped in `<think>` … `</think>` tags.
     * 2. If the delta content contains literal `<think>` / `</think>` markers
     *    (used by some models such as MiniMax or Nemotron), the markers are
     *    forwarded as-is so the downstream UI can render them appropriately.
     */
    suspend fun chatStream(
        model: String,
        messages: List<ChatMessageDto>,
        settings: ModelSettings? = null,
        onChunk: suspend (String, Boolean) -> Unit
    ) {
        ensureEndpointType()
        Log.d("NimClient", "chatStream: model=$model, isOpenAI=$isOpenAI")
        var isThinking = false
        var doneSent = false
        val s = settings ?: defaultSettingsFor(model)
        val templateKwargs = resolveTemplateKwargs(model)
        val reasoningEffort = resolveReasoningEffort(model)
        withContext(Dispatchers.IO) {
            try {
                val response: Response<ResponseBody> = if (isOpenAI == true) {
                    val request = OpenAIChatRequest(
                        model = model,
                        messages = messages,
                        stream = true,
                        temperature = s.temperature,
                        maxTokens = s.maxTokens,
                        top_p = s.topP,
                        top_k = s.topK,
                        frequency_penalty = s.frequencyPenalty,
                        presence_penalty = s.presencePenalty,
                        stop = s.stop,
                        seed = s.seed,
                        n = 1,
                        chatTemplateKwargs = templateKwargs,
                        reasoningEffort = reasoningEffort,
                        thinkingTokenBudget = s.thinkingTokenBudget
                    )
                    Log.d("NimClient", "OpenAI request: model=$model maxTokens=${s.maxTokens} temp=${s.temperature} topP=${s.topP} thinkingBudget=${s.thinkingTokenBudget} reasoningEffort=$reasoningEffort templateKwargs=$templateKwargs")
                    ollamaApi.chatStreamOpenAI(request)
                } else {
                    val request = ChatRequest(
                        model = model,
                        messages = messages,
                        stream = true,
                        options = ChatOptionsDto(
                            temperature = s.temperature,
                            topP = s.topP,
                            topK = s.topK,
                            numPredict = s.maxTokens,
                            stop = s.stop,
                            frequencyPenalty = s.frequencyPenalty,
                            presencePenalty = s.presencePenalty,
                            seed = s.seed
                        ).takeIf { it.hasAny() }
                    )
                    ollamaApi.chatStream(request)
                }

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: ""
                    Log.e("NimClient", "Stream error response: ${response.code()} $errorBody")
                    val userMessage = if (response.code() == 404) {
                        "Model not found. The model '$model' might be unavailable or requires a different account level on NVIDIA NIM."
                    } else {
                        "HTTP ${response.code()}: $errorBody"
                    }
                    throw Exception(userMessage)
                }

                val responseBody = response.body() ?: throw Exception("Empty response body")
                val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    Log.d("NimClient", "Raw line: $currentLine")
                    if (currentLine.isBlank()) continue
                    if (isOpenAI == true) {
                        if (currentLine.startsWith("data: ")) {
                            val data = currentLine.removePrefix("data: ").trim()
                            if (data == "[DONE]") {
                                closeThinkBlock(onChunk, isThinking).also { isThinking = false }
                                onChunk("", true)
                                doneSent = true
                                break
                            }
                            try {
                                val chatResponse = json.decodeFromString<OpenAIChatResponse>(data)
                                val delta = chatResponse.choices.firstOrNull()?.delta
                                val reasoning = delta?.reasoning_content
                                    ?: delta?.reasoning
                                    ?: delta?.thought
                                    ?: delta?.thinking
                                val content = delta?.content

                                // ── Handle separate reasoning field ──
                                if (!reasoning.isNullOrBlank()) {
                                    if (!isThinking) {
                                        onChunk("<think>\n", false)
                                        isThinking = true
                                    }
                                    onChunk(reasoning, false)
                                }
                                // ── Handle content field ──
                                else if (!content.isNullOrBlank()) {
                                    // Check for inline <think> / </think> markers in content.
                                    val (thinkStatus, processed) = processInlineThink(content, isThinking)
                                    isThinking = thinkStatus
                                    if (processed.isNotEmpty()) {
                                        onChunk(processed, false)
                                    }
                                }
                            } catch (e: Exception) {
                                // Malformed JSON chunk – ignore but continue streaming.
                                Log.d("NimClient", "Skipping malformed chunk: ${e.message}")
                            }
                        }
                    } else {
                        try {
                            val chatResponse = json.decodeFromString<ChatResponse>(currentLine)
                            onChunk(chatResponse.message?.content ?: "", chatResponse.done)
                            if (chatResponse.done) doneSent = true
                        } catch (e: Exception) {
                            Log.e("NimClient", "Error parsing Ollama chunk: ${e.message}")
                        }
                    }
                }
                // Ensure a final DONE signal if the server closed the stream without emitting [DONE].
                if (!doneSent) {
                    closeThinkBlock(onChunk, isThinking).also { isThinking = false }
                    onChunk("", true)
                }
            } catch (e: Exception) {
                Log.e("NimClient", "Stream error: ${e.message}")
                throw e
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * If [isThinking] is true, emit a closing `</think>` tag.
     * Called when an OpenAI stream terminates or when the model switches to content.
     */
    private suspend fun closeThinkBlock(
        onChunk: suspend (String, Boolean) -> Unit,
        isThinking: Boolean
    ) {
        if (isThinking) {
            onChunk("\n</think>\n\n", false)
        }
    }

    /**
     * Scans [text] for inline `<think>` / `</think>` markers and returns
     * a pair (updatedIsThinking, processedText).
     *
     * This handles models (MiniMax, Nemotron, etc.) that embed thinking
     * markers directly in the `content` field rather than using a dedicated
     * delta reasoning key.
     */
    private fun processInlineThink(text: String, currentlyThinking: Boolean): Pair<Boolean, String> {
        var isThinking = currentlyThinking
        val result = StringBuilder()

        // Simple stateful scan: track open/close tags in the current chunk.
        var remaining = text
        while (remaining.isNotEmpty()) {
            val openIdx = remaining.indexOf("<think>")
            val closeIdx = remaining.indexOf("</think>")

            when {
                // Whichever tag comes first wins.
                openIdx != -1 && (closeIdx == -1 || openIdx < closeIdx) -> {
                    // Text before the tag
                    if (openIdx > 0) result.append(remaining.substring(0, openIdx))
                    if (!isThinking) {
                        result.append("<think>\n")
                        isThinking = true
                    }
                    remaining = remaining.substring(openIdx + "<think>".length)
                }
                closeIdx != -1 -> {
                    // Text before the tag
                    if (closeIdx > 0) result.append(remaining.substring(0, closeIdx))
                    if (isThinking) {
                        result.append("\n</think>\n\n")
                        isThinking = false
                    }
                    remaining = remaining.substring(closeIdx + "</think>".length)
                }
                else -> {
                    result.append(remaining)
                    remaining = ""
                }
            }
        }
        return isThinking to result.toString()
    }

    /**
     * Returns the [chatTemplateKwargs] appropriate for [model] (or null if
     * the model does not need them).
     *
     * Some NVIDIA NIM models (DeepSeek V4 in particular) **require** this
     * field; without it the server hangs indefinitely.
     */
    private fun resolveTemplateKwargs(model: String): Map<String, JsonElement>? {
        return chatTemplateKwargsFor(model)
    }

    /**
     * Returns a [reasoningEffort] hint when the model is known to support it.
     * Standard values: "low", "medium", "high", "max".
     */
    private fun resolveReasoningEffort(model: String): String? {
        val lower = model.lowercase()
        return when {
            lower.contains("deepseek") -> "high"
            lower.contains("gpt-oss") -> "high"
            else -> null
        }
    }

    suspend fun reachable(): Boolean {
        val uri = settingsDataStore.getOllamaUri()
        val looksLikeOpenAI = uri.contains("nvidia", ignoreCase = true) ||
                uri.contains("openai", ignoreCase = true) ||
                uri.endsWith("/v1")
        Log.d("NimClient", "Reachable check: uri=$uri, looksLikeOpenAI=$looksLikeOpenAI")
        return try {
            if (looksLikeOpenAI) {
                try {
                    ollamaApi.listModelsOpenAI()
                    isOpenAI = true
                    Log.d("NimClient", "Detected OpenAI-compatible endpoint")
                    true
                } catch (e: Exception) {
                    Log.d("NimClient", "OpenAI check failed, trying Ollama: ${e.message}")
                    ollamaApi.listModels()
                    isOpenAI = false
                    Log.d("NimClient", "Detected Ollama endpoint")
                    true
                }
            } else {
                try {
                    ollamaApi.listModels()
                    isOpenAI = false
                    Log.d("NimClient", "Detected Ollama endpoint")
                    true
                } catch (e: Exception) {
                    Log.d("NimClient", "Ollama check failed, trying OpenAI: ${e.message}")
                    ollamaApi.listModelsOpenAI()
                    isOpenAI = true
                    Log.d("NimClient", "Detected OpenAI-compatible endpoint")
                    true
                }
            }
        } catch (e: Exception) {
            Log.w("NimClient", "Endpoint unreachable: ${e.message}")
            false
        }
    }

    /** Returns true when the configured endpoint speaks the OpenAI-compatible API (e.g. NVIDIA NIM). */
    fun isOpenAIEndpoint(): Boolean = isOpenAI == true

    fun resetEndpointType() {
        isOpenAI = null
    }
}
