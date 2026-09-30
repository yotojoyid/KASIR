package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val customerName: String = "Umum",
    val cashierName: String = "Kasir 1",
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double,
    val totalCost: Double,
    val paymentMethod: String, // CASH, QRIS, EWALLET, BANK_TRANSFER, DEBIT
    val paymentDetail: String = "", // e.g. "GOPAY 08123456789", "BCA VA 880123984"
    val cashReceived: Double = 0.0,
    val changeAmount: Double = 0.0,
    val note: String = "",
    val deviceId: String = "DEV-01",
    val isSynced: Boolean = false,
    val syncTimestamp: Long = 0L
)
