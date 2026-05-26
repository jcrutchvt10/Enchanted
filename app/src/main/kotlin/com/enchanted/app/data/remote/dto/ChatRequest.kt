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
    val images: List<String>? = null
)

@Serializable
data class ChatOptionsDto(
    val temperature: Float? = null
)
