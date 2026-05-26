package com.enchanted.app.domain.model

sealed class ConversationState {
    data object Completed : ConversationState()
    data object Loading : ConversationState()
    data class Error(val message: String) : ConversationState()
}
