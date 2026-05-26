package com.enchanted.app.data.local.mapper

import com.enchanted.app.data.local.entity.CompletionInstructionEntity
import com.enchanted.app.data.local.entity.ConversationEntity
import com.enchanted.app.data.local.entity.LanguageModelEntity
import com.enchanted.app.data.local.entity.MessageEntity
import com.enchanted.app.domain.model.CompletionInstruction
import com.enchanted.app.domain.model.Conversation
import com.enchanted.app.domain.model.LanguageModel
import com.enchanted.app.domain.model.Message
import com.enchanted.app.domain.model.ModelProvider
import java.util.UUID

// ── LanguageModel ──

fun LanguageModelEntity.toDomain(): LanguageModel = LanguageModel(
    name = name,
    provider = try {
        ModelProvider.valueOf(modelProvider)
    } catch (_: Exception) {
        ModelProvider.OLLAMA
    },
    imageSupport = imageSupport
)

fun LanguageModel.toEntity(): LanguageModelEntity = LanguageModelEntity(
    name = name,
    isAvailable = true,
    imageSupport = imageSupport,
    modelProvider = provider.name
)

// ── Conversation ──

fun ConversationEntity.toDomain(model: LanguageModel? = null): Conversation = Conversation(
    id = UUID.fromString(id),
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    model = model
)

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id.toString(),
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    modelName = model?.name
)

// ── Message ──

fun MessageEntity.toDomain(): Message = Message(
    id = UUID.fromString(id),
    content = content,
    role = role,
    done = done,
    error = error,
    createdAt = createdAt,
    imageData = imageData,
    conversationId = conversationId?.let { UUID.fromString(it) }
)

fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id.toString(),
    content = content,
    role = role,
    done = done,
    error = error,
    createdAt = createdAt,
    imageData = imageData,
    conversationId = conversationId?.toString()
)

// ── CompletionInstruction ──

fun CompletionInstructionEntity.toDomain(): CompletionInstruction = CompletionInstruction(
    id = UUID.fromString(id),
    name = name,
    prompt = instruction,
    order = order,
    createdAt = createdAt
)

fun CompletionInstruction.toEntity(): CompletionInstructionEntity = CompletionInstructionEntity(
    id = id.toString(),
    name = name,
    keyboardCharacter = name.firstOrNull()?.lowercase() ?: "x",
    instruction = prompt,
    order = order,
    modelTemperature = 0.8f
)
