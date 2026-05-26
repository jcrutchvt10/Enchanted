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

/**
 * Client for OpenAI‑compatible endpoints such as NVIDIA NIM.
 * It reuses the existing logic from the former OllamaClient but is renamed to avoid any
 * reference to Ollama. All functionality remains identical – the client automatically
 * detects whether the configured base URL speaks the OpenAI API and handles streaming
 * responses, model listing, and reachability checks.
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
                message = response.choices.firstOrNull()?.message?.let { ChatResponseMessageDto(role = it.role, content = it.content) },
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

    suspend fun chatStream(
        model: String,
        messages: List<ChatMessageDto>,
        temperature: Float? = null,
        onChunk: suspend (String, Boolean) -> Unit
    ) {
        ensureEndpointType()
        Log.d("NimClient", "chatStream: model=$model, isOpenAI=$isOpenAI")
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
                    Log.d("NimClient", "Sending OpenAI request: ${json.encodeToString(OpenAIChatRequest.serializer(), request)}")
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
                                // ignore malformed chunk
                            }
                        }
                    } else {
                        try {
                            val chatResponse = json.decodeFromString<ChatResponse>(currentLine)
                            onChunk(chatResponse.message?.content ?: "", chatResponse.done)
                        } catch (e: Exception) {
                            Log.e("NimClient", "Error parsing Ollama chunk: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NimClient", "Stream error: ${e.message}")
                throw e
            }
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

    fun resetEndpointType() {
        isOpenAI = null
    }
}
