package com.enchanted.app.domain.model

import java.util.UUID

data class Conversation(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val model: LanguageModel? = null,
    val messages: List<Message> = emptyList()
)
