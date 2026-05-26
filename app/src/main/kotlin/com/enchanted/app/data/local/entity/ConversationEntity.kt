package com.enchanted.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "conversations",
    foreignKeys = [
        ForeignKey(
            entity = LanguageModelEntity::class,
            parentColumns = ["name"],
            childColumns = ["modelName"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("modelName")]
)
data class ConversationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelName: String? = null
)
