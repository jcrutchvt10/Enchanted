package com.enchanted.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "completion_instructions")
data class CompletionInstructionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val keyboardCharacter: String,
    val instruction: String,
    val order: Int,
    val modelTemperature: Float = 0.8f,
    val createdAt: Long = System.currentTimeMillis()
)
