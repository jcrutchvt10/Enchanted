package com.enchanted.app.di

import android.content.Context
import androidx.room.Room
import com.enchanted.app.data.local.AppDatabase
import com.enchanted.app.data.local.dao.CompletionInstructionDao
import com.enchanted.app.data.local.dao.ConversationDao
import com.enchanted.app.data.local.dao.LanguageModelDao
import com.enchanted.app.data.local.dao.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "enchanted_database"
        ).build()
    }

    @Provides
    fun provideConversationDao(database: AppDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideLanguageModelDao(database: AppDatabase): LanguageModelDao = database.languageModelDao()

    @Provides
    fun provideCompletionInstructionDao(database: AppDatabase): CompletionInstructionDao =
        database.completionInstructionDao()
}
