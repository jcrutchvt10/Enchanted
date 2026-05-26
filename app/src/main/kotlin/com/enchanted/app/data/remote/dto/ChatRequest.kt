package com.enchanted.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = true,
    val options: ChatOptionsDto? = null
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String,
    val reasoning: String? = null,
    val images: List<String>? = null
)

@Serializable
data class ChatOptionsDto(
    val temperature: Float? = null,
    @SerialName("top_p") val topP: Float? = null,
    @SerialName("top_k") val topK: Int? = null,
    @SerialName("num_predict") val numPredict: Int? = null,
    val stop: List<String>? = null,
    @SerialName("frequency_penalty") val frequencyPenalty: Float? = null,
    @SerialName("presence_penalty") val presencePenalty: Float? = null,
    val seed: Int? = null
) {
    /** Returns true when at least one option is non‑null (avoids sending empty objects). */
    fun hasAny(): Boolean = temperature != null || topP != null || topK != null ||
            numPredict != null || stop != null || frequencyPenalty != null ||
            presencePenalty != null || seed != null
}
