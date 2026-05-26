package com.enchanted.app.data.repository

import com.enchanted.app.data.local.dao.LanguageModelDao
import com.enchanted.app.data.local.mapper.toDomain
import com.enchanted.app.data.local.mapper.toEntity
import com.enchanted.app.data.remote.NimClient
import com.enchanted.app.data.remote.mapper.toDomain
import com.enchanted.app.domain.model.LanguageModel
import com.enchanted.app.domain.model.ModelProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageModelRepository @Inject constructor(
    private val languageModelDao: LanguageModelDao,
    private val nimClient: NimClient
) {
    val allModels: Flow<List<LanguageModel>> = languageModelDao.getAllModels().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun refreshModels(): List<LanguageModel> {
        return try {
            val response = nimClient.listModels()
            val models = response.models.toDomain().map { model ->
                // The DTO mapper defaults to OLLAMA, but if the active endpoint is
                // OpenAI-compatible (e.g. NVIDIA NIM), correct the provider.
                if (nimClient.isOpenAIEndpoint()) {
                    model.copy(provider = ModelProvider.NVIDIA_NIM)
                } else {
                    model
                }
            }

            // Save to local database
            languageModelDao.deleteAll()
            languageModelDao.insertAll(models.map { it.toEntity() })

            models
        } catch (e: Exception) {
            // Return cached models if network fails
            languageModelDao.getAllModelsList().map { it.toDomain() }
        }
    }

    suspend fun loadCachedModels(): List<LanguageModel> {
        return languageModelDao.getAllModelsList().map { it.toDomain() }
    }

    suspend fun getModel(name: String): LanguageModel? {
        return languageModelDao.getModel(name)?.toDomain()
    }
}
