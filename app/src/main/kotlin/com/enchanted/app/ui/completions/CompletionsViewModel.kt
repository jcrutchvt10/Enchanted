package com.enchanted.app.ui.completions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enchanted.app.data.repository.CompletionsRepository
import com.enchanted.app.domain.model.CompletionInstruction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompletionsViewModel @Inject constructor(
    private val completionsRepository: CompletionsRepository
) : ViewModel() {

    val instructions: StateFlow<List<CompletionInstruction>> =
        completionsRepository.allInstructions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showEditor = MutableStateFlow(false)
    val showEditor: StateFlow<Boolean> = _showEditor.asStateFlow()

    private val _editingInstruction = MutableStateFlow<CompletionInstruction?>(null)
    val editingInstruction: StateFlow<CompletionInstruction?> = _editingInstruction.asStateFlow()

    fun showAddDialog() {
        _editingInstruction.value = null
        _showEditor.value = true
    }

    fun showEditDialog(instruction: CompletionInstruction) {
        _editingInstruction.value = instruction
        _showEditor.value = true
    }

    fun dismissEditor() {
        _showEditor.value = false
        _editingInstruction.value = null
    }

    fun saveInstruction(name: String, prompt: String, instruction: CompletionInstruction? = null) {
        viewModelScope.launch {
            if (instruction != null) {
                val updated = instruction.copy(name = name, prompt = prompt)
                completionsRepository.saveInstruction(updated)
            } else {
                val current = instructions.value
                val newInstruction = CompletionInstruction(
                    name = name,
                    prompt = prompt,
                    order = current.size
                )
                completionsRepository.saveInstruction(newInstruction)
            }
            dismissEditor()
        }
    }

    fun deleteInstruction(instruction: CompletionInstruction) {
        viewModelScope.launch {
            completionsRepository.deleteInstruction(instruction)
        }
    }
}
