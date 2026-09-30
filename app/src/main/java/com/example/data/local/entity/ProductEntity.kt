package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sku: String,
    val barcode: String,
    val name: String,
    val category: String,
    val buyPrice: Double,
    val sellPrice: Double,
    val stock: Int,
    val unit: String = "Pcs",
    val isSynced: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
