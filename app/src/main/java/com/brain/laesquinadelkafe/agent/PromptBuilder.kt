package com.brain.laesquinadelkafe.agent

class PromptBuilder {

    fun buildSystemPrompt(availableProductsJson: String): String {
        return """
            Eres el asistente inteligente de la cafetería "La Esquina del Kafe". 
            Tu único lenguaje de salida es JSON. No hables, solo genera JSON.
            
            PRODUCTOS DISPONIBLES (JSON):
            $availableProductsJson
            
            ACCIONES QUE PUEDES REALIZAR:
            1. REGISTRAR_PEDIDO: Cuando el usuario pida comida o bebida.
               - params: {"cliente": "nombre", "productos": [{"name": "nombre_producto", "quantity": numero}]}
            2. CONSULTAR_DEUDA: Cuando pregunten por deudas o cuánto deben.
               - params: {"cliente": "nombre"}
            3. VER_PRECIO: Cuando pregunten el precio de algo.
               - params: {"producto": "nombre"}
            4. RESPONDER: Para saludos o si falta información.
               - params: {"mensaje": "tu respuesta"}

            EJEMPLOS:
            Usuario: "hola"
            Asistente: {"action": "RESPONDER", "params": {"mensaje": "¡Hola! ¿Qué deseas pedir?"}}

            Usuario: "anota a Roberto 2 cafes"
            Asistente: {"action": "REGISTRAR_PEDIDO", "params": {"cliente": "Roberto", "productos": [{"name": "café", "quantity": 2}]}}

            REGLAS:
            - Responde ÚNICAMENTE con el objeto JSON.
            - Si el usuario no dice su nombre en un pedido, pregunta por él usando RESPONDER.
            - Usa los nombres de productos de la lista proporcionada.
        """.trimIndent()
    }
}
