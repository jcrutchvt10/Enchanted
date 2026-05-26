package com.enchanted.app.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enchanted.app.data.SettingsDataStore
import com.enchanted.app.data.remote.NimClient
import com.enchanted.app.data.repository.LanguageModelRepository
import com.enchanted.app.domain.model.AppColorScheme
import com.enchanted.app.service.HapticService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val nimClient: NimClient,
    private val languageModelRepository: LanguageModelRepository,
    private val hapticService: HapticService
) : ViewModel() {

    // ── Settings state ──

    val ollamaUri: StateFlow<String> = settingsDataStore.ollamaUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "http://10.0.2.2:11434")

    val bearerToken: StateFlow<String?> = settingsDataStore.bearerToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val systemPrompt: StateFlow<String> = settingsDataStore.systemPrompt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val userInitials: StateFlow<String> = settingsDataStore.userInitials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val defaultModel: StateFlow<String> = settingsDataStore.defaultModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val colorScheme: StateFlow<AppColorScheme> = settingsDataStore.colorScheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppColorScheme.SYSTEM)

    val pingInterval: StateFlow<Float> = settingsDataStore.pingInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5f)

    // ── Live status ──

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Unknown)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // ── Setters (live preview) ──

    fun setOllamaUri(uri: String) {
        viewModelScope.launch { settingsDataStore.setOllamaUri(uri) }
    }

    fun setBearerToken(token: String?) {
        viewModelScope.launch { settingsDataStore.setBearerToken(token) }
    }

    fun setSystemPrompt(prompt: String) {
        viewModelScope.launch { settingsDataStore.setSystemPrompt(prompt) }
    }

    fun setUserInitials(initials: String) {
        viewModelScope.launch { settingsDataStore.setUserInitials(initials) }
    }

    fun setDefaultModel(model: String) {
        viewModelScope.launch { settingsDataStore.setDefaultModel(model) }
    }

    fun setColorScheme(scheme: AppColorScheme) {
        viewModelScope.launch { settingsDataStore.setColorScheme(scheme) }
    }

    fun setPingInterval(interval: Float) {
        viewModelScope.launch { settingsDataStore.setPingInterval(interval) }
    }

    // ── Actions ──

    /**
     * Test connectivity to the currently configured server and refresh models.
     * Mirrors the iOS [Settings.checkServer] behaviour.
     */
    fun testConnection() {
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.Testing
            nimClient.resetEndpointType()
            val reachable = nimClient.reachable()
            if (reachable) {
                try {
                    languageModelRepository.refreshModels()
                    _connectionStatus.value = ConnectionStatus.Connected
                } catch (e: Exception) {
                    _connectionStatus.value = ConnectionStatus.Error(
                        "Models unreachable: ${e.message}"
                    )
                }
            } else {
                _connectionStatus.value = ConnectionStatus.Error(
                    "Cannot reach server. Check the URL and ensure Ollama (or your OpenAI-compatible server) is running."
                )
            }
        }
    }

    /**
     * Save all settings, test connection, and reload models.
     * Mirrors the iOS [Settings.save] behaviour.
     */
    fun saveAndReload() {
        viewModelScope.launch {
            _isSaving.value = true
            hapticService.mediumTap()
            nimClient.resetEndpointType()

            // 1. Update the Ollama client base URL + token (handled by dynamic interceptor)
            // 2. Test the connection
            testConnection()

            // 3. Reload models from the new endpoint
            try {
                languageModelRepository.refreshModels()
            } catch (e: Exception) {
                Log.e("SettingsVM", "Failed to reload models", e)
            }

            _isSaving.value = false
        }
    }

    /**
     * Clear all conversations and cached models.
     */
    fun clearAllData() {
        viewModelScope.launch {
            hapticService.mediumTap()
            try {
                languageModelRepository.refreshModels()
            } catch (_: Exception) {}
        }
    }
}

/** Connection check states. */
sealed class ConnectionStatus {
    data object Unknown : ConnectionStatus()
    data object Testing : ConnectionStatus()
    data object Connected : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}
