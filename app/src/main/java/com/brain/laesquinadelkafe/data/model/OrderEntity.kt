package com.brain.laesquinadelkafe.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientName: String,
    val timestamp: Long,
    val status: String, // "PENDIENTE", "ENVIADO"
    val isPaid: Boolean,
    val totalAmount: Double,
    val paidAmount: Double = 0.0 // Para pagos parciales
)
