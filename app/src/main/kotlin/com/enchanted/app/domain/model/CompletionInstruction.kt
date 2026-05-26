package com.enchanted.app.domain.model

import java.util.UUID

data class CompletionInstruction(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val prompt: String,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
