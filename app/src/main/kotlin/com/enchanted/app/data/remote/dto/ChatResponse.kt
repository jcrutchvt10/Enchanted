package com.enchanted.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val model: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val message: ChatResponseMessageDto? = null,
    @SerialName("done") val done: Boolean = false
)

@Serializable
data class ChatResponseMessageDto(
    val role: String = "assistant",
    val content: String = ""
)
