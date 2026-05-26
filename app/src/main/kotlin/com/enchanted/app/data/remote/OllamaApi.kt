package com.enchanted.app.data.remote

import com.enchanted.app.data.remote.dto.ChatRequest
import com.enchanted.app.data.remote.dto.ChatResponse
import com.enchanted.app.data.remote.dto.ModelsResponse
import com.enchanted.app.data.remote.dto.OpenAIChatRequest
import com.enchanted.app.data.remote.dto.OpenAIChatResponse
import com.enchanted.app.data.remote.dto.OpenAIModelsResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming

interface OllamaApi {

    @GET("api/tags")
    suspend fun listModels(): ModelsResponse

    @GET("models")
    suspend fun listModelsOpenAI(): OpenAIModelsResponse

    @POST("api/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @POST("chat/completions")
    suspend fun chatOpenAI(@Body request: OpenAIChatRequest): OpenAIChatResponse

    @Streaming
    @POST("api/chat")
    suspend fun chatStream(@Body request: ChatRequest): Response<ResponseBody>

    @Streaming
    @Headers("Accept: text/event-stream")
    @POST("chat/completions")
    suspend fun chatStreamOpenAI(@Body request: OpenAIChatRequest): Response<ResponseBody>
}
