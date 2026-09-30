package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.StoreConfig
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    data class ReportData(
        val periodTitle: String,
        val startDate: Long,
        val endDate: Long,
        val transactions: List<TransactionEntity>,
        val itemsMap: Map<Long, List<TransactionItemEntity>>,
        val totalRevenue: Double,
        val totalProfit: Double,
        val storeConfig: StoreConfig
    )

    fun generateSalesReportPdf(context: Context, data: ReportData): File {
        val document = PdfDocument()
        val pageWidth = 595 // Standard A4 width (points)
        val pageHeight = 842 // Standard A4 height (points)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var yPos = 36f

        // 1. Header Banner
        paint.color = Color.rgb(30, 58, 138) // Deep POS Blue
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

        // Store Name & Title
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        canvas.drawText(data.storeConfig.storeName.uppercase(), 36f, 40f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(219, 234, 254)
        canvas.drawText("${data.storeConfig.storeAddress} | Telp: ${data.storeConfig.storePhone}", 36f, 56f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("LAPORAN PENJUALAN RESMI", 36f, 80f, paint)

        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        val printedText = "Dicetak: $dateStr"
        val printedWidth = paint.measureText(printedText)
        canvas.drawText(printedText, pageWidth - 36f - printedWidth, 80f, paint)

        yPos = 115f

        // 2. Periode Laporan Subtitle
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PERIODE: ${data.periodTitle.uppercase()}", 36f, yPos, paint)

        yPos += 18f

        // 3. Metric Summary Cards (4 Cards)
        val cardWidth = (pageWidth - 72f - 24f) / 4f
        val cardHeight = 54f
        val metrics = listOf(
            Triple("TOTAL OMSET", CurrencyFormatter.formatRupiah(data.totalRevenue), Color.rgb(219, 234, 254)),
            Triple("LABA BERSIH", CurrencyFormatter.formatRupiah(data.totalProfit), Color.rgb(204, 251, 241)),
            Triple("TRANSAKSI", "${data.transactions.size} Order", Color.rgb(254, 243, 199)),
            Triple(
                "RATA-RATA",
                if (data.transactions.isNotEmpty()) CurrencyFormatter.formatRupiah(data.totalRevenue / data.transactions.size) else "Rp 0",
                Color.rgb(241, 245, 249)
            )
        )

        for (i in metrics.indices) {
            val cardLeft = 36f + i * (cardWidth + 8f)
            val rect = RectF(cardLeft, yPos, cardLeft + cardWidth, yPos + cardHeight)

            paint.color = metrics[i].third
            canvas.drawRoundRect(rect, 6f, 6f, paint)

            paint.color = Color.rgb(51, 65, 85)
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(metrics[i].first, cardLeft + 8f, yPos + 18f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(metrics[i].second, cardLeft + 8f, yPos + 38f, paint)
        }

        yPos += cardHeight + 20f

        // 4. Payment Method Breakdown Row
        paint.color = Color.rgb(248, 250, 252)
        val pmRect = RectF(36f, yPos, pageWidth - 36f, yPos + 32f)
        canvas.drawRoundRect(pmRect, 4f, 4f, paint)

        val cashCount = data.transactions.count { it.paymentMethod == "CASH" }
        val qrisCount = data.transactions.count { it.paymentMethod == "QRIS" }
        val ewalletCount = data.transactions.count { it.paymentMethod == "EWALLET" }
        val otherCount = data.transactions.size - (cashCount + qrisCount + ewalletCount)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        val breakdownText = "Metode Pembayaran:  Tunai ($cashCount)   |   QRIS ($qrisCount)   |   E-Wallet ($ewalletCount)   |   Bank / Lainnya ($otherCount)"
        canvas.drawText(breakdownText, 48f, yPos + 20f, paint)

        yPos += 45f

        // 5. Table Header
        paint.color = Color.rgb(30, 41, 59)
        val headerRect = RectF(36f, yPos, pageWidth - 36f, yPos + 22f)
        canvas.drawRect(headerRect, paint)

        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val colX = floatArrayOf(42f, 65f, 150f, 240f, 375f, 455f)
        canvas.drawText("NO", colX[0], yPos + 15f, paint)
        canvas.drawText("WAKTU & FAKTUR", colX[1], yPos + 15f, paint)
        canvas.drawText("PELANGGAN", colX[2], yPos + 15f, paint)
        canvas.drawText("ITEMS / RINCIAN", colX[3], yPos + 15f, paint)
        canvas.drawText("METODE", colX[4], yPos + 15f, paint)
        canvas.drawText("TOTAL", colX[5], yPos + 15f, paint)

        yPos += 22f

        // 6. Transactions Table Rows
        val rowHeight = 24f
        for ((index, tx) in data.transactions.withIndex()) {
            // Check if page overflow
            if (yPos + rowHeight > pageHeight - 60f) {
                // Draw footer for this page
                drawPageFooter(canvas, pageNumber, pageWidth, pageHeight, paint)
                document.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                // Header on new page
                yPos = 36f
                paint.color = Color.rgb(30, 41, 59)
                val newHRect = RectF(36f, yPos, pageWidth - 36f, yPos + 22f)
                canvas.drawRect(newHRect, paint)
                paint.color = Color.WHITE
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("NO", colX[0], yPos + 15f, paint)
                canvas.drawText("WAKTU & FAKTUR", colX[1], yPos + 15f, paint)
                canvas.drawText("PELANGGAN", colX[2], yPos + 15f, paint)
                canvas.drawText("ITEMS / RINCIAN", colX[3], yPos + 15f, paint)
                canvas.drawText("METODE", colX[4], yPos + 15f, paint)
                canvas.drawText("TOTAL", colX[5], yPos + 15f, paint)
                yPos += 22f
            }

            // Alternating row background
            if (index % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(36f, yPos, pageWidth - 36f, yPos + rowHeight, paint)
            }

            // Row border line
            paint.color = Color.rgb(226, 232, 240)
            paint.strokeWidth = 0.5f
            canvas.drawLine(36f, yPos + rowHeight, pageWidth - 36f, yPos + rowHeight, paint)

            // Content
            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8f

            canvas.drawText("${index + 1}", colX[0], yPos + 16f, paint)

            val timeStr = SimpleDateFormat("dd/MM HH:mm", Locale.US).format(Date(tx.timestamp))
            canvas.drawText("$timeStr | ${tx.invoiceNumber}", colX[1], yPos + 16f, paint)

            val cust = if (tx.customerName.length > 15) tx.customerName.take(13) + ".." else tx.customerName
            canvas.drawText(cust, colX[2], yPos + 16f, paint)

            val items = data.itemsMap[tx.id]
            val itemsSummary = if (!items.isNullOrEmpty()) {
                val summary = items.joinToString(", ") { "${it.quantity}x ${it.productName}" }
                if (summary.length > 25) summary.take(23) + ".." else summary
            } else {
                "Item penjualan"
            }
            canvas.drawText(itemsSummary, colX[3], yPos + 16f, paint)

            val methodLabel = when (tx.paymentMethod) {
                "CASH" -> "TUNAI"
                "QRIS" -> "QRIS"
                "EWALLET" -> "E-WALLET"
                else -> tx.paymentMethod
            }
            canvas.drawText(methodLabel, colX[4], yPos + 16f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val totalStr = CurrencyFormatter.formatRupiah(tx.totalAmount)
            canvas.drawText(totalStr, colX[5], yPos + 16f, paint)

            yPos += rowHeight
        }

        // 7. Signature & Summary Section on last page
        if (yPos > pageHeight - 120f) {
            drawPageFooter(canvas, pageNumber, pageWidth, pageHeight, paint)
            document.finishPage(page)

            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            yPos = 50f
        }

        yPos += 20f
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Catatan: Laporan ini dihasilkan secara otomatis oleh sistem Kasir POS KasirKilat.", 36f, yPos, paint)

        // Signature box right side
        val sigX = pageWidth - 180f
        canvas.drawText("Penanggung Jawab / Kasir:", sigX, yPos + 15f, paint)
        canvas.drawLine(sigX, yPos + 60f, pageWidth - 36f, yPos + 60f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText(data.storeConfig.cashierName, sigX, yPos + 72f, paint)

        drawPageFooter(canvas, pageNumber, pageWidth, pageHeight, paint)
        document.finishPage(page)

        // Save PDF to cache directory
        val fileName = "Laporan_Penjualan_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    private fun drawPageFooter(canvas: Canvas, page: Int, width: Int, height: Int, paint: Paint) {
        paint.color = Color.rgb(148, 163, 184)
        paint.strokeWidth = 0.5f
        canvas.drawLine(36f, height - 32f, width - 36f, height - 32f, paint)

        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("KasirKilat POS - Cloud Realtime Synchronization", 36f, height - 18f, paint)

        val pageStr = "Halaman $page"
        val pW = paint.measureText(pageStr)
        canvas.drawText(pageStr, width - 36f - pW, height - 18f, paint)
    }

    fun openPdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Buka Laporan PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun sharePdfFile(context: Context, file: File, title: String = "Laporan Penjualan") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Berikut adalah lampiran laporan penjualan PDF dari KasirKilat POS.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Laporan PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
