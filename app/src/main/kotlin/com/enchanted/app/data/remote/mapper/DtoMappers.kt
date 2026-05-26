package com.enchanted.app.data.remote.mapper

import android.util.Base64
import com.enchanted.app.data.remote.dto.ChatMessageDto
import com.enchanted.app.data.remote.dto.ModelDto
import com.enchanted.app.domain.model.LanguageModel
import com.enchanted.app.domain.model.ModelProvider

fun ModelDto.toDomain(): LanguageModel = LanguageModel(
    name = name,
    provider = ModelProvider.OLLAMA,
    imageSupport = details.families?.any { it == "clip" || it == "mllama" } ?: false
)

fun List<ModelDto>.toDomain(): List<LanguageModel> = map { it.toDomain() }

fun createChatMessageDto(role: String, content: String, imageBase64: ByteArray? = null): ChatMessageDto {
    val images = if (imageBase64 != null) {
        listOf(Base64.encodeToString(imageBase64, Base64.NO_WRAP))
    } else null

    return ChatMessageDto(
        role = role,
        content = content,
        images = images
    )
}
