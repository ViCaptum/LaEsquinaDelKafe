package com.brain.laesquinadelkafe.data.repository

import com.brain.laesquinadelkafe.data.dao.OrderDao
import com.brain.laesquinadelkafe.data.model.OrderEntity
import com.brain.laesquinadelkafe.data.model.OrderItemEntity
import com.brain.laesquinadelkafe.data.model.OrderWithItems
import kotlinx.coroutines.flow.Flow

class OrderRepository(private val orderDao: OrderDao) {

    val pendingOrders: Flow<List<OrderWithItems>> = orderDao.getPendingOrders()
    val debts: Flow<List<OrderWithItems>> = orderDao.getShippedUnpaidOrders()
    val historyOrders: Flow<List<OrderWithItems>> = orderDao.getPaidOrders()

    suspend fun insertOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) = orderDao.insertOrderWithItems(order, items)

    suspend fun getActiveOrderByName(name: String) = orderDao.getActiveOrderByName(name)

    suspend fun addItemsToOrder(orderId: Int, items: List<OrderItemEntity>, additionalTotal: Double) =
        orderDao.addItemsToOrder(orderId, items, additionalTotal)

    suspend fun updatePayment(orderId: Int, paidAmount: Double, isPaid: Boolean) {
        orderDao.updatePayment(orderId, paidAmount, isPaid)
    }

    suspend fun updateItemQuantity(itemId: Int, quantity: Int) {
        orderDao.updateItemQuantity(itemId, quantity)
    }

    suspend fun updateOrder(order: OrderEntity) {
        orderDao.updateOrder(order)
    }

    suspend fun deleteOrder(order: OrderEntity) {
        orderDao.deleteOrder(order)
    }
}
