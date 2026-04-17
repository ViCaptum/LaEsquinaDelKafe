package com.brain.laesquinadelkafe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.brain.laesquinadelkafe.agent.ModelManager
import com.brain.laesquinadelkafe.viewmodel.ChatViewModel

@Composable
fun ChatScreen(viewModel: ChatViewModel, modelManager: ModelManager) {
    var message by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()

    // Scroll al último mensaje automáticamente
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Color del botón de enviar
    val sendButtonColor = if (message.isNotBlank() && !isLoading) {
        MaterialTheme.colorScheme.primary 
    } else {
        Color.Gray
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Area de Mensajes
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(
                    text = msg.text,
                    isUser = msg.isUser
                )
            }
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterStart) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input de Chat
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { if (!it.contains("\n")) message = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje...") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val currentMsg = message
                    message = ""
                    viewModel.sendMessage(currentMsg) { prompt ->
                        try {
                            val rawResponse = modelManager.generateResponse(prompt)
                            // Limpiar el formato Markdown si el modelo lo incluye
                            rawResponse
                                .replace("```json", "")
                                .replace("```", "")
                                .trim()
                        } catch (e: Exception) {
                            // Envolver el error en un JSON de respuesta para el Agente
                            "{\"action\": \"RESPONDER\", \"params\": {\"mensaje\": \"Error de IA: ${e.localizedMessage}\"}}"
                        }
                    }
                },
                enabled = message.isNotBlank() && !isLoading,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = sendButtonColor,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
            }
        }
    }
}

@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 16.dp
            )
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
