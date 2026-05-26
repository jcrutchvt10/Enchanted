package com.enchanted.app.domain.model

import java.util.UUID

data class Message(
    val id: UUID = UUID.randomUUID(),
    val content: String,
    val role: String,
    val done: Boolean = false,
    val error: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val imageData: ByteArray? = null,
    val conversationId: UUID? = null
) {
    val think: String?
        get() {
            val thinkTags = listOf("<think>" to "</think>", "<thought>" to "</thought>")
            for ((start, end) in thinkTags) {
                if (content.contains(start)) {
                    if (content.contains(end)) {
                        val parts = content.split(end)
                        if (parts.size > 1) {
                            return parts[0].replace(start, "").trim()
                        }
                    }
                    return content.replace(start, "").trim()
                }
            }
            return null
        }

    val hasThink: Boolean get() = content.contains("<think>") || content.contains("<thought>")

    val thinkComplete: Boolean
        get() = (content.contains("<think>") && content.contains("</think>")) ||
                (content.contains("<thought>") && content.contains("</thought>"))

    val realContent: String
        get() {
            val thinkTags = listOf("<think>" to "</think>", "<thought>" to "</thought>")
            for ((start, end) in thinkTags) {
                if (content.contains(start)) {
                    if (content.contains(end)) {
                        val parts = content.split(end)
                        return if (parts.size > 1) parts[1].trim() else ""
                    }
                    return ""
                }
            }
            return content
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Message) return false
        if (id != other.id) return false
        if (content != other.content) return false
        if (role != other.role) return false
        if (done != other.done) return false
        if (error != other.error) return false
        if (createdAt != other.createdAt) return false
        if (imageData != null) {
            if (other.imageData == null) return false
            if (!imageData.contentEquals(other.imageData)) return false
        } else if (other.imageData != null) return false
        return conversationId == other.conversationId
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + done.hashCode()
        result = 31 * result + error.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (imageData?.contentHashCode() ?: 0)
        result = 31 * result + (conversationId?.hashCode() ?: 0)
        return result
    }
}
