package com.brain.laesquinadelkafe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brain.laesquinadelkafe.agent.CafeteriaAgent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel(
    private val cafeteriaAgent: CafeteriaAgent
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("¡Hola! Soy tu asistente de La Esquina del Kafe. ¿En qué puedo ayudarte hoy?", false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(text: String, modelInference: suspend (String) -> String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // Añadir mensaje del usuario
            val userMsg = ChatMessage(text, true)
            _messages.value = _messages.value + userMsg
            
            _isLoading.value = true
            
            try {
                // Procesar con el Agente
                val response = cafeteriaAgent.processInput(text) { prompt ->
                    modelInference(prompt)
                }
                
                // Añadir respuesta del agente
                _messages.value = _messages.value + ChatMessage(response, false)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("Error: ${e.message}", false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class ChatViewModelFactory(private val cafeteriaAgent: CafeteriaAgent) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(cafeteriaAgent) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
