package com.enchanted.app.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enchanted.app.data.repository.ConversationRepository
import com.enchanted.app.data.repository.LanguageModelRepository
import com.enchanted.app.domain.model.ConversationState
import com.enchanted.app.domain.model.LanguageModel
import com.enchanted.app.service.SpeechService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val speechService: SpeechService,
    private val conversationRepository: ConversationRepository,
    private val languageModelRepository: LanguageModelRepository
) : ViewModel() {

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    val models: StateFlow<List<LanguageModel>> = languageModelRepository.allModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedModel = MutableStateFlow<LanguageModel?>(null)
    val selectedModel: StateFlow<LanguageModel?> = _selectedModel.asStateFlow()

    fun startListening() {
        _isListening.value = true
        viewModelScope.launch {
            speechService.startListening().collect { text ->
                _transcribedText.value = text
            }
            _isListening.value = false
        }
    }

    fun stopListening() {
        speechService.stopListening()
        _isListening.value = false
    }

    fun selectModel(model: LanguageModel) {
        _selectedModel.value = model
    }

    fun sendVoiceMessage(text: String) {
        val model = _selectedModel.value ?: return
        viewModelScope.launch {
            conversationRepository.sendMessage(
                userPrompt = text,
                model = model,
                imageData = null,
                systemPrompt = "",
                trimmingMessageId = null,
                currentConversationId = null,
                onStateChange = {},
                onNewConversation = {},
                onMessagesUpdated = {}
            )
            _transcribedText.value = ""
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechService.stopListening()
    }
}
