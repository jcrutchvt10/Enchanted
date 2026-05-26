package com.enchanted.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "language_models")
data class LanguageModelEntity(
    @PrimaryKey
    val name: String,
    val isAvailable: Boolean = false,
    val imageSupport: Boolean = false,
    val modelProvider: String = "OLLAMA"
)
