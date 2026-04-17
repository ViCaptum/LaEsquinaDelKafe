package com.brain.laesquinadelkafe.agent

import com.brain.laesquinadelkafe.data.repository.ProductRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class InventoryProvider(private val productRepository: ProductRepository) {

    suspend fun getAvailableProductsJson(): String {
        val products = productRepository.allProducts.first()
        val availableProducts = products.filter { it.isAvailable }
        
        val jsonArray = JSONArray()
        availableProducts.forEach { product ->
            val jsonObject = JSONObject().apply {
                put("id", product.id)
                put("name", product.name)
                put("price", product.price)
                put("isDrink", product.isDrink)
            }
            jsonArray.put(jsonObject)
        }
        
        return jsonArray.toString()
    }
}
