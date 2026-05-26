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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OllamaClient @Inject constructor(
    private val ollamaApi: OllamaApi,
    private val settingsDataStore: SettingsDataStore
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var isOpenAI: Boolean? = null

    /**
     * Ensures we know whether the configured endpoint speaks the OpenAI API or the native Ollama API.
     * If the endpoint type is unknown, we perform a lightweight reachability probe.
     * Additionally, for OpenAI‑compatible endpoints we verify that an auth token is configured –
     * otherwise the request will inevitably fail with 401. The token check is defensive; the
     * interceptor will still send the request without a token, but we surface a clear log entry.
     */
    private suspend fun ensureEndpointType() {
        if (isOpenAI == null) {
            Log.d("OllamaClient", "Endpoint type unknown, probing...")
            reachable()
        }
        // If we have determined the endpoint is OpenAI‑compatible, make sure a bearer token exists.
        if (isOpenAI == true) {
            val token = settingsDataStore.getBearerToken()
            if (token.isNullOrBlank()) {
                Log.w("OllamaClient", "OpenAI‑compatible endpoint detected but no bearer token is set. Calls may fail with 401.")
            }
        }
    }

    /**
     * Fetch models from the configured endpoint (Ollama or OpenAI).
     * Throws if unreachable.
     */
    suspend fun listModels(): ModelsResponse {
        ensureEndpointType()
        return try {
            if (isOpenAI == true) {
                val openAIResponse = ollamaApi.listModelsOpenAI()
                ModelsResponse(
                    models = openAIResponse.data.map {
                        ModelDto(name = it.id)
                    }
                )
            } else {
                ollamaApi.listModels()
            }
        } catch (e: Exception) {
            Log.e("OllamaClient", "Failed to list models", e)
            throw e
        }
    }

    /**
     * Non-streaming chat (used for simple calls).
     */
    suspend fun chat(
        model: String,
        messages: List<ChatMessageDto>,
        temperature: Float? = null
    ): ChatResponse {
        ensureEndpointType()
        return if (isOpenAI == true) {
            val request = OpenAIChatRequest(
                model = model,
                messages = messages,
                stream = false,
                temperature = temperature,
                maxTokens = 4096
            )
            val response = ollamaApi.chatOpenAI(request)
            ChatResponse(
                model = model,
                message = response.choices.firstOrNull()?.message?.let {
                    ChatResponseMessageDto(role = it.role, content = it.content)
                },
                done = true
            )
        } else {
            val request = ChatRequest(
                model = model,
                messages = messages,
                stream = false,
                options = temperature?.let { ChatOptionsDto(it) }
            )
            ollamaApi.chat(request)
        }
    }

    /**
     * Streaming chat. Calls [onChunk] for each content delta.
     * [onChunk] receives (content: String, isDone: Boolean).
     */
    suspend fun chatStream(
        model: String,
        messages: List<ChatMessageDto>,
        temperature: Float? = null,
        onChunk: suspend (String, Boolean) -> Unit
    ) {
        ensureEndpointType()
        Log.d("OllamaClient", "chatStream: model=$model, isOpenAI=$isOpenAI")
        var isThinking = false
        
        withContext(Dispatchers.IO) {
            try {
                val response: Response<ResponseBody> = if (isOpenAI == true) {
                    val request = OpenAIChatRequest(
                        model = model,
                        messages = messages,
                        stream = true,
                        temperature = temperature ?: 1.0f,
                        maxTokens = 1024,
                        top_p = 0.95f,
                        frequency_penalty = 0.0f,
                        presence_penalty = 0.0f,
                        n = 1
                    )
                    Log.d("OllamaClient", "Sending OpenAI request: ${json.encodeToString(OpenAIChatRequest.serializer(), request)}")
                    ollamaApi.chatStreamOpenAI(request)
                } else {
                    val request = ChatRequest(
                        model = model,
                        messages = messages,
                        stream = true,
                        options = temperature?.let { ChatOptionsDto(it) }
                    )
                    ollamaApi.chatStream(request)
                }

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: ""
                    Log.e("OllamaClient", "Stream error response: ${response.code()} $errorBody")
                    
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
                    Log.d("OllamaClient", "Raw line: $currentLine")
                    if (currentLine.isBlank()) continue
                    
                    if (isOpenAI == true) {
                        if (currentLine.startsWith("data: ")) {
                            val data = currentLine.removePrefix("data: ").trim()
                            if (data == "[DONE]") {
                                if (isThinking) {
                                    onChunk("\n</think>\n\n", false)
                                    isThinking = false
                                }
                                onChunk("", true)
                                break
                            }
                            try {
                                val chatResponse = json.decodeFromString<OpenAIChatResponse>(data)
                                val delta = chatResponse.choices.firstOrNull()?.delta
                                val reasoning = delta?.reasoning_content ?: delta?.reasoning ?: delta?.thought ?: delta?.thinking
                                val content = delta?.content

                                if (!reasoning.isNullOrEmpty()) {
                                    if (!isThinking) {
                                        onChunk("<think>\n", false)
                                        isThinking = true
                                    }
                                    onChunk(reasoning, false)
                                } else if (!content.isNullOrEmpty()) {
                                    if (isThinking) {
                                        onChunk("\n</think>\n\n", false)
                                        isThinking = false
                                    }
                                    onChunk(content, false)
                                }
                            } catch (e: Exception) {
                                // Ignore parsing errors for individual chunks
                            }
                        }
                    } else {
                        try {
                            val chatResponse = json.decodeFromString<ChatResponse>(currentLine)
                            onChunk(
                                chatResponse.message?.content ?: "",
                                chatResponse.done
                            )
                        } catch (e: Exception) {
                            Log.e("OllamaClient", "Error parsing Ollama chunk: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("OllamaClient", "Stream error: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Quick reachability check against the configured endpoint.
     * Tries to be smart about whether it's Ollama or OpenAI.
     */
    suspend fun reachable(): Boolean {
        val uri = settingsDataStore.getOllamaUri()
        
        // Optimization: if URL contains nvidia or ends in v1, prioritize OpenAI
        val looksLikeOpenAI = uri.contains("nvidia", ignoreCase = true) || 
                              uri.contains("openai", ignoreCase = true) ||
                              uri.endsWith("/v1")
        
        Log.d("OllamaClient", "Reachable check: uri=$uri, looksLikeOpenAI=$looksLikeOpenAI")

        return try {
            if (looksLikeOpenAI) {
                try {
                    ollamaApi.listModelsOpenAI()
                    isOpenAI = true
                    Log.d("OllamaClient", "Detected OpenAI-compatible endpoint")
                    true
                } catch (e: Exception) {
                    Log.d("OllamaClient", "OpenAI check failed, trying Ollama: ${e.message}")
                    ollamaApi.listModels()
                    isOpenAI = false
                    Log.d("OllamaClient", "Detected Ollama endpoint")
                    true
                }
            } else {
                try {
                    ollamaApi.listModels()
                    isOpenAI = false
                    Log.d("OllamaClient", "Detected Ollama endpoint")
                    true
                } catch (e: Exception) {
                    Log.d("OllamaClient", "Ollama check failed, trying OpenAI: ${e.message}")
                    ollamaApi.listModelsOpenAI()
                    isOpenAI = true
                    Log.d("OllamaClient", "Detected OpenAI-compatible endpoint")
                    true
                }
            }
        } catch (e: Exception) {
            Log.w("OllamaClient", "Endpoint unreachable: ${e.message}")
            false
        }
    }

    /**
     * Resets the endpoint type detection.
     * Call this when the server URL or API Key changes.
     */
    fun resetEndpointType() {
        isOpenAI = null
    }
}
