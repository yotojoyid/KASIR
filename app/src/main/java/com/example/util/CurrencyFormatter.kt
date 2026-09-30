package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {
    private val localeId = Locale("id", "ID")
    private val numberFormat = NumberFormat.getCurrencyInstance(localeId).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    fun formatRupiah(amount: Double): String {
        return try {
            numberFormat.format(amount).replace("Rp", "Rp ")
        } catch (e: Exception) {
            "Rp " + String.format(Locale.US, "%,.0f", amount).replace(',', '.')
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", localeId)
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", localeId)
        return sdf.format(Date(timestamp))
    }

    fun formatFullDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm:ss", localeId)
        return sdf.format(Date(timestamp))
    }

    fun generateInvoiceNumber(): String {
        val datePart = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val randomPart = (1000..9999).random()
        return "INV/$datePart/$randomPart"
    }
}
