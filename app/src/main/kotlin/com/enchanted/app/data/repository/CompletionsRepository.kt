package com.enchanted.app.data.repository

import com.enchanted.app.data.local.dao.CompletionInstructionDao
import com.enchanted.app.data.local.mapper.toDomain
import com.enchanted.app.data.local.mapper.toEntity
import com.enchanted.app.domain.model.CompletionInstruction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompletionsRepository @Inject constructor(
    private val completionInstructionDao: CompletionInstructionDao
) {
    val allInstructions: Flow<List<CompletionInstruction>> =
        completionInstructionDao.getAllInstructions().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun loadInstructions(): List<CompletionInstruction> {
        return completionInstructionDao.getAllInstructionsList().map { it.toDomain() }
    }

    suspend fun prepopulateIfEmpty() {
        val current = completionInstructionDao.getAllInstructionsList()
        if (current.isEmpty()) {
            val defaults = listOf(
                CompletionInstruction(
                    name = "Think",
                    prompt = "Analyze the request and explain your reasoning process step-by-step before providing the final answer. Wrap your reasoning in <think> tags."
                ),
                CompletionInstruction(
                    name = "Stream",
                    prompt = "Provide a response optimized for real-time streaming. Break content into small paragraphs and use concise language."
                ),
                CompletionInstruction(
                    name = "JSON Tooling",
                    prompt = "Respond only with a valid JSON object matching the requested schema. No conversational filler or explanations."
                ),
                CompletionInstruction(
                    name = "NVIDIA NIM",
                    prompt = "Optimize response for NVIDIA NIM high-throughput inference. Be direct, clear, and prioritize performance-oriented answers."
                )
            )
            completionInstructionDao.insertAll(defaults.map { it.toEntity() })
        }
    }

    suspend fun saveInstruction(instruction: CompletionInstruction) {
        completionInstructionDao.insert(instruction.toEntity())
    }

    suspend fun deleteInstruction(instruction: CompletionInstruction) {
        completionInstructionDao.delete(instruction.toEntity())
    }

    suspend fun updateInstructions(instructions: List<CompletionInstruction>) {
        completionInstructionDao.deleteAll()
        completionInstructionDao.insertAll(instructions.map { it.toEntity() })
    }
}
