package com.enchanted.app.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enchanted.app.data.SettingsDataStore
import com.enchanted.app.data.repository.CompletionsRepository
import com.enchanted.app.data.repository.ConversationRepository
import com.enchanted.app.data.repository.LanguageModelRepository
import com.enchanted.app.domain.model.CompletionInstruction
import com.enchanted.app.domain.model.Conversation
import com.enchanted.app.domain.model.ConversationState
import com.enchanted.app.domain.model.LanguageModel
import com.enchanted.app.domain.model.Message
import com.enchanted.app.service.HapticService
import com.enchanted.app.service.ReachabilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val languageModelRepository: LanguageModelRepository,
    private val completionsRepository: CompletionsRepository,
    private val settingsDataStore: SettingsDataStore,
    private val reachabilityService: ReachabilityService,
    private val hapticService: HapticService
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = conversationRepository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val models: StateFlow<List<LanguageModel>> = languageModelRepository.allModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completionInstructions: StateFlow<List<CompletionInstruction>> =
        completionsRepository.allInstructions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isReachable: StateFlow<Boolean> = reachabilityService.isReachable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val systemPrompt: StateFlow<String> = settingsDataStore.systemPrompt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val userInitials: StateFlow<String> = settingsDataStore.userInitials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val defaultModel: StateFlow<String> = settingsDataStore.defaultModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _conversationState = MutableStateFlow<ConversationState>(ConversationState.Completed)
    val conversationState: StateFlow<ConversationState> = _conversationState.asStateFlow()

    private val _selectedConversation = MutableStateFlow<Conversation?>(null)
    val selectedConversation: StateFlow<Conversation?> = _selectedConversation.asStateFlow()

    private val _selectedModel = MutableStateFlow<LanguageModel?>(null)
    val selectedModel: StateFlow<LanguageModel?> = _selectedModel.asStateFlow()

    private val _showMenu = MutableStateFlow(false)
    val showMenu: StateFlow<Boolean> = _showMenu.asStateFlow()

    private var currentGenerationJob: Job? = null

    init {
        reachabilityService.startChecking(3600000L)
        viewModelScope.launch {
            loadInitialData()
        }
    }

    private suspend fun loadInitialData() {
        try {
            completionsRepository.prepopulateIfEmpty()
            languageModelRepository.refreshModels()
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to refresh models or prepopulate", e)
        }
    }

    fun refreshModels() {
        viewModelScope.launch {
            try {
                val refreshed = languageModelRepository.refreshModels()
                if (_selectedModel.value == null && refreshed.isNotEmpty()) {
                    val defaultName = defaultModel.value
                    val model = if (defaultName.isNotBlank()) {
                        refreshed.find { it.name == defaultName }
                    } else null
                    _selectedModel.value = model ?: refreshed.first()
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to refresh models", e)
            }
        }
    }

    fun selectModel(model: LanguageModel?) {
        _selectedModel.value = model
    }

    fun toggleMenu() {
        _showMenu.value = !_showMenu.value
        hapticService.mediumTap()
    }

    fun dismissMenu() {
        _showMenu.value = false
    }

    fun selectConversation(conversation: Conversation) {
        viewModelScope.launch {
            _selectedConversation.value = conversation
            _selectedModel.value = conversation.model
            loadMessages(conversation.id.toString())
            hapticService.mediumTap()
            _showMenu.value = false
        }
    }

    fun newConversation() {
        _selectedConversation.value = null
        _messages.value = emptyList()
        _conversationState.value = ConversationState.Completed
        hapticService.mediumTap()
        refreshModels()
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            conversationRepository.getMessages(conversationId).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun sendMessage(prompt: String, imageData: ByteArray? = null, trimmingMessageId: String? = null) {
        Log.d("ChatViewModel", "sendMessage: prompt=$prompt, model=${_selectedModel.value?.name}")
        val model = _selectedModel.value ?: return
        val currentConvId = _selectedConversation.value?.id?.toString()
        val sysPrompt = systemPrompt.value

        currentGenerationJob = viewModelScope.launch {
            conversationRepository.sendMessage(
                userPrompt = prompt,
                model = model,
                imageData = imageData,
                systemPrompt = sysPrompt,
                trimmingMessageId = trimmingMessageId,
                currentConversationId = currentConvId,
                onStateChange = { state ->
                    _conversationState.value = state
                },
                onNewConversation = { convId ->
                    viewModelScope.launch {
                        val conv = conversationRepository.getConversation(convId)
                        _selectedConversation.value = conv
                        loadMessages(convId)
                    }
                },
                onMessagesUpdated = {
                    // Messages are updated via Flow from Room
                }
            )
        }
    }

    fun stopGenerating() {
        currentGenerationJob?.cancel()
        viewModelScope.launch {
            conversationRepository.stopGenerating()
            _conversationState.value = ConversationState.Completed
        }
    }

    fun deleteConversation(conversation: Conversation) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(conversation)
            if (_selectedConversation.value?.id == conversation.id) {
                _selectedConversation.value = null
                _messages.value = emptyList()
            }
            hapticService.mediumTap()
        }
    }

    fun deleteAllConversations() {
        viewModelScope.launch {
            conversationRepository.deleteAllConversations()
            _selectedConversation.value = null
            _messages.value = emptyList()
        }
    }

    fun copyMessagesAsJson() {
        // Implementation depends on how you want to handle clipboard
    }

    fun copyMessagesAsText() {
        // Implementation depends on how you want to handle clipboard
    }

    override fun onCleared() {
        super.onCleared()
        reachabilityService.stopChecking()
    }
}
