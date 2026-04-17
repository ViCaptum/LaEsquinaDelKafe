package com.brain.laesquinadelkafe.data.dao

import androidx.room.*
import com.brain.laesquinadelkafe.data.model.OrderEntity
import com.brain.laesquinadelkafe.data.model.OrderItemEntity
import com.brain.laesquinadelkafe.data.model.OrderWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItemEntity)

    @Transaction
    @Query("SELECT * FROM orders WHERE isPaid = 0 AND status = 'PENDIENTE' ORDER BY timestamp DESC")
    fun getPendingOrders(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE isPaid = 0 AND status = 'ENVIADO' ORDER BY timestamp DESC")
    fun getShippedUnpaidOrders(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE isPaid = 1 ORDER BY timestamp DESC")
    fun getPaidOrders(): Flow<List<OrderWithItems>>

    @Query("UPDATE orders SET paidAmount = :paidAmount, isPaid = :isPaid WHERE id = :orderId")
    suspend fun updatePayment(orderId: Int, paidAmount: Double, isPaid: Boolean)

    @Query("UPDATE order_items SET quantity = :quantity WHERE id = :itemId")
    suspend fun updateItemQuantity(itemId: Int, quantity: Int)

    @Transaction
    suspend fun insertOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
        val orderId = insertOrder(order)
        items.forEach { item ->
            insertOrderItem(item.copy(orderId = orderId.toInt()))
        }
    }

    @Transaction
    @Query("SELECT * FROM orders WHERE LOWER(clientName) = LOWER(:clientName) AND isPaid = 0 LIMIT 1")
    suspend fun getActiveOrderByName(clientName: String): OrderWithItems?

    @Transaction
    suspend fun addItemsToOrder(orderId: Int, items: List<OrderItemEntity>, additionalTotal: Double) {
        items.forEach { item ->
            insertOrderItem(item.copy(orderId = orderId))
        }
        val currentOrder = getOrderById(orderId)
        currentOrder?.let {
            updateOrder(it.copy(totalAmount = it.totalAmount + additionalTotal))
        }
    }

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Int): OrderEntity?

    @Delete
    suspend fun deleteOrder(order: OrderEntity)
    
    @Update
    suspend fun updateOrder(order: OrderEntity)
}
