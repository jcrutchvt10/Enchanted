package com.enchanted.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.enchanted.app.data.local.dao.CompletionInstructionDao
import com.enchanted.app.data.local.dao.ConversationDao
import com.enchanted.app.data.local.dao.LanguageModelDao
import com.enchanted.app.data.local.dao.MessageDao
import com.enchanted.app.data.local.entity.CompletionInstructionEntity
import com.enchanted.app.data.local.entity.ConversationEntity
import com.enchanted.app.data.local.entity.LanguageModelEntity
import com.enchanted.app.data.local.entity.MessageEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        LanguageModelEntity::class,
        CompletionInstructionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun languageModelDao(): LanguageModelDao
    abstract fun completionInstructionDao(): CompletionInstructionDao
}
