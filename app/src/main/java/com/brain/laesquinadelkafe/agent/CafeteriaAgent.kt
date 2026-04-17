package com.brain.laesquinadelkafe.agent

import com.brain.laesquinadelkafe.data.model.OrderEntity
import com.brain.laesquinadelkafe.data.model.OrderItemEntity
import com.brain.laesquinadelkafe.data.repository.OrderRepository
import com.brain.laesquinadelkafe.data.repository.ProductRepository
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.util.Locale

class CafeteriaAgent(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val inventoryProvider: InventoryProvider,
    private val promptBuilder: PromptBuilder
) {

    suspend fun processInput(userInput: String, modelInference: suspend (String) -> String): String {
        // 1. Obtener inventario actualizado
        val availableProductsJson = inventoryProvider.getAvailableProductsJson()
        
        // 2. Construir prompt
        val systemPrompt = promptBuilder.buildSystemPrompt(availableProductsJson)
        val fullPrompt = "$systemPrompt\n\nUsuario: $userInput\nAsistente (JSON):"
        
        // 3. Llamar al modelo (Inferencia)
        val modelResponse = modelInference(fullPrompt)
        
        // 4. Dispatcher de acciones
        return try {
            val jsonResponse = JSONObject(modelResponse)
            val action = jsonResponse.optString("action")
            val params = jsonResponse.optJSONObject("params") ?: JSONObject()
            
            dispatchAction(action, params)
        } catch (e: Exception) {
            "Error al procesar la respuesta del modelo: ${e.message}"
        }
    }

    private suspend fun dispatchAction(action: String, params: JSONObject): String {
        return when (action) {
            "REGISTRAR_PEDIDO" -> {
                val cliente = params.optString("cliente", "Cliente Genérico")
                val productosArray = params.optJSONArray("productos") 
                    ?: params.optJSONArray("items") // Flexibilidad por si el modelo cambia el nombre
                    ?: return "No entendí qué productos quieres registrar. ¿Podrías repetirlo?"
                
                val availableProducts = productRepository.allProducts.first()
                val itemsToOrder = mutableListOf<OrderItemEntity>()
                val missingProducts = mutableListOf<String>()
                
                for (i in 0 until productosArray.length()) {
                    val p = productosArray.optJSONObject(i) ?: continue
                    val name = p.optString("name", p.optString("producto", ""))
                    val qty = p.optInt("quantity", p.optInt("cantidad", 1))
                    
                    if (name.isEmpty()) continue

                    val productMatch = availableProducts.find { 
                        it.name.contains(name, ignoreCase = true) || name.contains(it.name, ignoreCase = true)
                    }
                    
                    if (productMatch != null) {
                        itemsToOrder.add(
                            OrderItemEntity(
                                orderId = 0,
                                productName = productMatch.name,
                                quantity = qty,
                                pricePerUnit = productMatch.price,
                                isToTakeAway = !productMatch.isDrink
                            )
                        )
                    } else {
                        missingProducts.add(name)
                    }
                }
                
                if (itemsToOrder.isEmpty()) {
                    return "No encontré los productos que mencionaste en nuestro menú."
                }

                if (missingProducts.isNotEmpty()) {
                    return "Puedo anotar lo demás, pero no tenemos: ${missingProducts.joinToString(", ")}"
                }
                
                val total = itemsToOrder.sumOf { it.quantity * it.pricePerUnit }
                val order = OrderEntity(
                    clientName = cliente,
                    timestamp = System.currentTimeMillis(),
                    status = "PENDIENTE",
                    isPaid = false,
                    totalAmount = total
                )
                
                orderRepository.insertOrderWithItems(order, itemsToOrder)
                "Pedido registrado para $cliente por un total de S/. ${String.format(Locale.getDefault(), "%.2f", total)}."
            }

            "CONSULTAR_DEUDA" -> {
                val cliente = params.optString("cliente", "")
                if (cliente.isEmpty()) return "¿A qué nombre busco la deuda?"

                val unpaidOrders = orderRepository.debts.first().filter { 
                    it.order.clientName.contains(cliente, ignoreCase = true) 
                }
                
                if (unpaidOrders.isEmpty()) {
                    "No encontré deudas pendientes para $cliente."
                } else {
                    val totalDeuda = unpaidOrders.sumOf { it.order.totalAmount - it.order.paidAmount }
                    "El cliente $cliente tiene una deuda total de S/. ${String.format(Locale.getDefault(), "%.2f", totalDeuda)}."
                }
            }

            "VER_PRECIO" -> {
                val productoNombre = params.optString("producto", params.optString("name", ""))
                if (productoNombre.isEmpty()) return "¿De qué producto quieres saber el precio?"

                val product = productRepository.allProducts.first().find { 
                    it.name.contains(productoNombre, ignoreCase = true) 
                }
                
                if (product != null) {
                    "El precio de ${product.name} es S/. ${String.format(Locale.getDefault(), "%.2f", product.price)}."
                } else {
                    "Lo siento, no encontré '$productoNombre' en nuestra carta."
                }
            }

            "RESPONDER" -> {
                params.optString("mensaje", "Lo siento, no pude entender la solicitud.")
            }

            else -> "Acción no reconocida por el sistema."
        }
    }
}
