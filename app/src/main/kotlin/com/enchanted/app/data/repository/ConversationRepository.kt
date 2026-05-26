package com.enchanted.app.data.repository

import com.enchanted.app.data.local.dao.ConversationDao
import com.enchanted.app.data.local.dao.LanguageModelDao
import com.enchanted.app.data.local.dao.MessageDao
import com.enchanted.app.data.local.mapper.toDomain
import com.enchanted.app.data.local.mapper.toEntity
import com.enchanted.app.data.remote.NimClient
import com.enchanted.app.data.remote.dto.ChatMessageDto
import com.enchanted.app.data.remote.mapper.createChatMessageDto
import com.enchanted.app.domain.model.Conversation
import com.enchanted.app.domain.model.ConversationState
import com.enchanted.app.domain.model.LanguageModel
import com.enchanted.app.domain.model.Message
import com.enchanted.app.domain.model.defaultSettingsFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val languageModelDao: LanguageModelDao,
    private val nimClient: NimClient
) {
    private val _conversationState = MutableStateFlow<ConversationState>(ConversationState.Completed)
    val conversationState: StateFlow<ConversationState> = _conversationState.asStateFlow()

    val allConversations: Flow<List<Conversation>> = conversationDao.getAllConversations().map { entities ->
        entities.map { entity ->
            val model = entity.modelName?.let { name ->
                languageModelDao.getModel(name)?.toDomain()
            }
            entity.toDomain(model)
        }
    }

    fun getMessages(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesByConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getConversation(id: String): Conversation? {
        val entity = conversationDao.getConversation(id) ?: return null
        val model = entity.modelName?.let { languageModelDao.getModel(it)?.toDomain() }
        return entity.toDomain(model)
    }

    suspend fun deleteConversation(conversation: Conversation) {
        conversationDao.deleteById(conversation.id.toString())
    }

    suspend fun deleteAllConversations() {
        conversationDao.deleteAll()
        messageDao.deleteAll()
    }

    suspend fun deleteMessages(conversationId: String) {
        messageDao.deleteByConversationId(conversationId)
    }

    suspend fun sendMessage(
        userPrompt: String,
        model: LanguageModel,
        imageData: ByteArray?,
        systemPrompt: String,
        trimmingMessageId: String?,
        currentConversationId: String?,
        onStateChange: (ConversationState) -> Unit,
        onNewConversation: (String) -> Unit,
        onMessagesUpdated: () -> Unit
    ) {
        if (userPrompt.isBlank()) return

        val conversationId = currentConversationId ?: UUID.randomUUID().toString()
        val isNew = currentConversationId == null

        // Create or update conversation
        if (isNew) {
            val conversation = Conversation(
                id = UUID.fromString(conversationId),
                name = userPrompt.take(50),
                model = model
            )
            conversationDao.insert(conversation.toEntity())
            onNewConversation(conversationId)
        } else {
            val existingConversation = conversationDao.getConversation(conversationId) ?: return
            conversationDao.update(existingConversation.copy(updatedAt = System.currentTimeMillis()))
        }

        // Add system prompt
        if (systemPrompt.isNotBlank()) {
            val existingMessages = messageDao.getMessagesByConversationList(conversationId)
            if (existingMessages.isEmpty()) {
                val systemMsg = Message(
                    content = systemPrompt,
                    role = "system",
                    conversationId = UUID.fromString(conversationId)
                )
                messageDao.insert(systemMsg.toEntity())
            }
        }

        // Add user message
        val userMessage = Message(
            content = userPrompt,
            role = "user",
            imageData = imageData,
            conversationId = UUID.fromString(conversationId)
        )
        messageDao.insert(userMessage.toEntity())

        // Add empty assistant message
        val assistantMessage = Message(
            content = "",
            role = "assistant",
            conversationId = UUID.fromString(conversationId)
        )
        messageDao.insert(assistantMessage.toEntity())

        onMessagesUpdated()
        _conversationState.value = ConversationState.Loading
        onStateChange(ConversationState.Loading)

        // Prepare message history
        val allMessages = messageDao.getMessagesByConversationList(conversationId)
        val chatMessages = mutableListOf<ChatMessageDto>()

        for (msg in allMessages) {
            if (msg.role == "system") {
                chatMessages.add(createChatMessageDto("system", msg.content))
            }
        }

        var lastUserMessageContent = userPrompt
        for (msg in allMessages) {
            if (msg.role == "user") {
                lastUserMessageContent = msg.content
                chatMessages.add(createChatMessageDto("user", msg.content))
            } else if (msg.role == "assistant" && msg.content.isNotEmpty()) {
                chatMessages.add(createChatMessageDto("assistant", msg.content))
            }
        }

        // Attach image to the last user message
        if (imageData != null && chatMessages.isNotEmpty()) {
            val lastMsg = chatMessages.removeAt(chatMessages.lastIndex)
            chatMessages.add(createChatMessageDto("user", lastMsg.content, imageData))
        }

        // Handle trimming
        if (trimmingMessageId != null) {
            val trimIndex = allMessages.indexOfFirst { it.id == trimmingMessageId }
            if (trimIndex >= 0) {
                val trimmedIds = allMessages.drop(trimIndex).map { it.id }
                for (id in trimmedIds) {
                    messageDao.delete(messageDao.getMessage(id) ?: continue)
                }
            }
        }

        // Execute streaming chat
        try {
            var accumulatedContent = ""
            var lastUpdateAt = 0L

            nimClient.chatStream(
                model = model.name,
                messages = chatMessages,
                settings = defaultSettingsFor(model.name),
                onChunk = { content, done ->
                    accumulatedContent += content
                    val now = System.currentTimeMillis()

                    // Update UI/DB at most every 150ms or when done
                    if (done || now - lastUpdateAt > 150) {
                        val lastMsg = messageDao.getLastMessage(conversationId)
                        if (lastMsg != null) {
                            val updated = lastMsg.copy(
                                content = lastMsg.content + accumulatedContent,
                                done = done
                            )
                            messageDao.update(updated)
                            accumulatedContent = ""
                            lastUpdateAt = now
                            onMessagesUpdated()
                        }

                        if (done) {
                            _conversationState.value = ConversationState.Completed
                            onStateChange(ConversationState.Completed)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            val errorMessages = messageDao.getMessagesByConversationList(conversationId)
            if (errorMessages.isNotEmpty()) {
                val lastMsg = errorMessages.last()
                messageDao.update(lastMsg.copy(error = true))
                onMessagesUpdated()
            }
            _conversationState.value = ConversationState.Error(e.message ?: "Unknown error")
            onStateChange(ConversationState.Error(e.message ?: "Unknown error"))
        }
    }

    suspend fun stopGenerating() {
        _conversationState.value = ConversationState.Completed
    }

    suspend fun updateMessage(message: Message) {
        messageDao.update(message.toEntity())
    }
}
