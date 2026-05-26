package com.enchanted.app.domain.model

data class NotificationMessage(
    val message: String,
    val status: NotificationStatus
)

enum class NotificationStatus {
    SUCCESS, ERROR, INFO
}
