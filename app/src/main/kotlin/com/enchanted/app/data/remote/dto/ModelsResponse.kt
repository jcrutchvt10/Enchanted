package com.enchanted.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelsResponse(
    val models: List<ModelDto>
)

@Serializable
data class ModelDto(
    val name: String,
    @SerialName("modified_at") val modifiedAt: String? = null,
    val size: Long? = null,
    val digest: String? = null,
    val details: ModelDetailsDto = ModelDetailsDto()
)

@Serializable
data class ModelDetailsDto(
    @SerialName("parent_model") val parentModel: String? = null,
    val format: String? = null,
    val family: String? = null,
    val families: List<String>? = null,
    @SerialName("parameter_size") val parameterSize: String? = null,
    @SerialName("quantization_level") val quantizationLevel: String? = null
)
