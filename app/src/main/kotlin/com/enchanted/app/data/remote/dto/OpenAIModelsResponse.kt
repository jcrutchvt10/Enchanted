package com.enchanted.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OpenAIModelsResponse(
    val data: List<OpenAIModelDto>
)

@Serializable
data class OpenAIModelDto(
    val id: String
)
