package com.enchanted.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OpenAIChatResponse(
    val choices: List<OpenAIChoiceDto>
)

@Serializable
data class OpenAIChoiceDto(
    val message: ChatMessageDto? = null,
    val delta: OpenAIDeltaDto? = null
)

@Serializable
data class OpenAIDeltaDto(
    val content: String? = null,
    val reasoning_content: String? = null,
    val reasoning: String? = null,
    val thought: String? = null,
    val thinking: String? = null
)
