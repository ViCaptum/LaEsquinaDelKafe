package com.brain.laesquinadelkafe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brain.laesquinadelkafe.data.model.OrderEntity
import com.brain.laesquinadelkafe.data.model.OrderItemEntity
import com.brain.laesquinadelkafe.data.model.OrderWithItems
import com.brain.laesquinadelkafe.data.repository.OrderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OrderViewModel(private val repository: OrderRepository) : ViewModel() {

    val pendingOrders: StateFlow<List<OrderWithItems>> = repository.pendingOrders
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val debts: StateFlow<List<OrderWithItems>> = repository.debts
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val historyOrders: StateFlow<List<OrderWithItems>> = repository.historyOrders
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addOrder(clientName: String, items: List<OrderItemEntity>, isPaid: Boolean) {
        viewModelScope.launch {
            val total = items.sumOf { it.quantity * it.pricePerUnit }
            val order = OrderEntity(
                clientName = clientName,
                timestamp = System.currentTimeMillis(),
                status = "PENDIENTE",
                isPaid = isPaid,
                totalAmount = total
            )
            repository.insertOrderWithItems(order, items)
        }
    }

    fun addItemsToExistingOrder(orderId: Int, items: List<OrderItemEntity>, additionalTotal: Double) {
        viewModelScope.launch {
            repository.addItemsToOrder(orderId, items, additionalTotal)
        }
    }

    suspend fun getActiveOrderByName(name: String) = repository.getActiveOrderByName(name)

    fun markAsPaid(order: OrderEntity) {
        viewModelScope.launch {
            repository.updatePayment(order.id, order.totalAmount, true)
        }
    }

    fun updatePayment(orderId: Int, paidAmount: Double, isPaid: Boolean) {
        viewModelScope.launch {
            repository.updatePayment(orderId, paidAmount, isPaid)
        }
    }

    fun updateItemQuantity(itemId: Int, quantity: Int) {
        viewModelScope.launch {
            repository.updateItemQuantity(itemId, quantity)
        }
    }

    fun updateStatus(order: OrderEntity, status: String) {
        viewModelScope.launch {
            repository.updateOrder(order.copy(status = status))
        }
    }

    fun deleteOrder(order: OrderEntity) {
        viewModelScope.launch {
            repository.deleteOrder(order)
        }
    }
}

class OrderViewModelFactory(private val repository: OrderRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrderViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
