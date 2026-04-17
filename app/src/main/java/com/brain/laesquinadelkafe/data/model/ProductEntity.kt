package com.brain.laesquinadelkafe.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val isAvailable: Boolean = true,
    val isDrink: Boolean = false
)
