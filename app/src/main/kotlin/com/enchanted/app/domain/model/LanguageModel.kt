package com.enchanted.app.domain.model

data class LanguageModel(
    val name: String,
    val provider: ModelProvider,
    val imageSupport: Boolean
)
