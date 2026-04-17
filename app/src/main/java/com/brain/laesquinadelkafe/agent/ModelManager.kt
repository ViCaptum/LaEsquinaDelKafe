package com.brain.laesquinadelkafe.agent

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ModelManager(private val context: Context) {

    private var llmInference: LlmInference? = null
    private val modelFileName = "brain.task"

    suspend fun initialize(): LlmInference = withContext(Dispatchers.IO) {
        if (llmInference == null) {
            val modelFile = File(context.filesDir, modelFileName)
            
            // Si el modelo no está en la memoria interna, lo copiamos desde assets
            if (!modelFile.exists()) {
                context.assets.open(modelFileName).use { inputStream ->
                    FileOutputStream(modelFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            // Nota: En versiones recientes de MediaPipe, setTopK y setTemperature 
            // se han movido o renombrado. Usamos la configuración base compatible.
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(512)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
        }
        llmInference!!
    }

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val inference = initialize()
            inference.generateResponse(prompt) ?: "Error: El modelo no devolvió una respuesta."
        } catch (e: Exception) {
            "Error en inferencia: ${e.localizedMessage}"
        }
    }
}
