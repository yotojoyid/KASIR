package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.local.StoreConfig
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ReceiptPrinter {

    /**
     * Formats receipt as monospace text suitable for 58mm/80mm thermal printers or WhatsApp.
     */
    fun formatReceiptText(
        tx: TransactionEntity,
        items: List<TransactionItemEntity>,
        config: StoreConfig
    ): String {
        val width = 32 // 58mm typical character width
        val divider = "-".repeat(width)
        val doubleDivider = "=".repeat(width)

        fun center(text: String): String {
            if (text.length >= width) return text
            val padding = (width - text.length) / 2
            return " ".repeat(padding) + text
        }

        fun row(left: String, right: String): String {
            val spaceNeeded = width - left.length - right.length
            return if (spaceNeeded > 0) {
                left + " ".repeat(spaceNeeded) + right
            } else {
                "$left $right"
            }
        }

        val sb = StringBuilder()
        sb.appendLine(center(config.storeName.uppercase()))
        sb.appendLine(center(config.storeAddress))
        sb.appendLine(center("Telp: ${config.storePhone}"))
        sb.appendLine(doubleDivider)
        sb.appendLine(row("No Faktur :", tx.invoiceNumber))
        sb.appendLine(row("Tanggal   :", CurrencyFormatter.formatDateTime(tx.timestamp)))
        sb.appendLine(row("Kasir     :", tx.cashierName))
        sb.appendLine(row("Pelanggan :", tx.customerName))
        sb.appendLine(divider)

        for (item in items) {
            sb.appendLine(item.productName)
            val left = "  ${item.quantity} x ${CurrencyFormatter.formatRupiah(item.unitPrice)}"
            val right = CurrencyFormatter.formatRupiah(item.subtotal)
            sb.appendLine(row(left, right))
        }

        sb.appendLine(divider)
        sb.appendLine(row("Subtotal  :", CurrencyFormatter.formatRupiah(tx.subtotal)))
        if (tx.discountAmount > 0) {
            sb.appendLine(row("Diskon    :", "-${CurrencyFormatter.formatRupiah(tx.discountAmount)}"))
        }
        if (tx.taxAmount > 0) {
            sb.appendLine(row("PPN       :", CurrencyFormatter.formatRupiah(tx.taxAmount)))
        }
        sb.appendLine(doubleDivider)
        sb.appendLine(row("TOTAL     :", CurrencyFormatter.formatRupiah(tx.totalAmount)))

        val methodLabel = when (tx.paymentMethod) {
            "CASH" -> "TUNAI"
            "QRIS" -> "QRIS"
            "EWALLET" -> "E-WALLET"
            "BANK_TRANSFER" -> "TRANSFER BANK"
            else -> tx.paymentMethod
        }
        sb.appendLine(row("Metode    :", methodLabel))

        if (tx.paymentMethod == "CASH") {
            sb.appendLine(row("Tunai     :", CurrencyFormatter.formatRupiah(tx.cashReceived)))
            sb.appendLine(row("Kembalian :", CurrencyFormatter.formatRupiah(tx.changeAmount)))
        } else if (tx.paymentDetail.isNotBlank()) {
            sb.appendLine(row("Ref       :", tx.paymentDetail))
        }

        sb.appendLine(doubleDivider)
        for (line in config.receiptFooter.lines()) {
            sb.appendLine(center(line))
        }

        return sb.toString()
    }

    /**
     * Prints receipt via Android PrintManager (Standard Print Dialog, WiFi/Bluetooth/Thermal/PDF).
     */
    fun printReceipt(
        context: Context,
        tx: TransactionEntity,
        items: List<TransactionItemEntity>,
        config: StoreConfig
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Struk_${tx.invoiceNumber}"

        val adapter = object : PrintDocumentAdapter() {
            private var pdfDocument: PdfDocument? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val pdi = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()

                callback?.onLayoutFinished(pdi, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }

                pdfDocument = createReceiptPdf(tx, items, config)
                try {
                    destination?.fileDescriptor?.let { fd ->
                        FileOutputStream(fd).use { out ->
                            pdfDocument?.writeTo(out)
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    }
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    pdfDocument?.close()
                    pdfDocument = null
                }
            }
        }

        printManager.print(
            jobName,
            adapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .build()
        )
    }

    /**
     * Generates a PDF receipt suitable for thermal 58mm/80mm or standard viewing.
     */
    fun createReceiptPdf(
        tx: TransactionEntity,
        items: List<TransactionItemEntity>,
        config: StoreConfig
    ): PdfDocument {
        val document = PdfDocument()
        val pageWidth = 280 // ~58mm/80mm thermal width
        // Calculate needed height dynamically
        val neededHeight = (280 + items.size * 32 + 180).coerceAtLeast(450)
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, neededHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }

        var y = 30f

        fun drawCentered(text: String, size: Float, isBold: Boolean = false) {
            paint.textSize = size
            paint.typeface = if (isBold) Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) else Typeface.MONOSPACE
            val w = paint.measureText(text)
            canvas.drawText(text, (pageWidth - w) / 2, y, paint)
            y += size + 4f
        }

        fun drawDashedLine() {
            paint.strokeWidth = 1f
            paint.pathEffect = null
            var x = 16f
            while (x < pageWidth - 16f) {
                canvas.drawLine(x, y, x + 4f, y, paint)
                x += 8f
            }
            y += 10f
        }

        fun drawRow(left: String, right: String, size: Float = 10f, isBold: Boolean = false) {
            paint.textSize = size
            paint.typeface = if (isBold) Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) else Typeface.MONOSPACE
            canvas.drawText(left, 16f, y, paint)
            val rightW = paint.measureText(right)
            canvas.drawText(right, pageWidth - 16f - rightW, y, paint)
            y += size + 4f
        }

        // Header
        drawCentered(config.storeName.uppercase(), 14f, true)
        drawCentered(config.storeAddress, 9f)
        drawCentered("Telp: ${config.storePhone}", 9f)
        y += 4f
        drawDashedLine()

        // Metadata
        drawRow("Faktur:", tx.invoiceNumber, 9f)
        drawRow("Waktu :", CurrencyFormatter.formatDateTime(tx.timestamp), 9f)
        drawRow("Kasir :", tx.cashierName, 9f)
        drawRow("Pelanggan:", tx.customerName, 9f)
        drawDashedLine()

        // Items
        for (item in items) {
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            canvas.drawText(item.productName, 16f, y, paint)
            y += 13f

            val left = " ${item.quantity} x ${CurrencyFormatter.formatRupiah(item.unitPrice)}"
            val right = CurrencyFormatter.formatRupiah(item.subtotal)
            drawRow(left, right, 9f)
        }

        drawDashedLine()

        // Totals
        drawRow("Subtotal", CurrencyFormatter.formatRupiah(tx.subtotal), 10f)
        if (tx.discountAmount > 0) {
            drawRow("Diskon", "-${CurrencyFormatter.formatRupiah(tx.discountAmount)}", 10f)
        }
        if (tx.taxAmount > 0) {
            drawRow("PPN", CurrencyFormatter.formatRupiah(tx.taxAmount), 10f)
        }

        y += 2f
        drawRow("TOTAL", CurrencyFormatter.formatRupiah(tx.totalAmount), 13f, true)

        val methodLabel = when (tx.paymentMethod) {
            "CASH" -> "TUNAI"
            "QRIS" -> "QRIS"
            "EWALLET" -> "E-WALLET"
            "BANK_TRANSFER" -> "TRANSFER BANK"
            else -> tx.paymentMethod
        }
        drawRow("Metode Bayar", methodLabel, 10f)

        if (tx.paymentMethod == "CASH") {
            drawRow("Bayar Tunai", CurrencyFormatter.formatRupiah(tx.cashReceived), 10f)
            drawRow("Kembalian", CurrencyFormatter.formatRupiah(tx.changeAmount), 10f, true)
        } else if (tx.paymentDetail.isNotBlank()) {
            drawRow("Keterangan", tx.paymentDetail, 9f)
        }

        y += 4f
        drawDashedLine()

        // Footer
        for (line in config.receiptFooter.lines()) {
            drawCentered(line, 8.5f)
        }

        y += 6f
        drawCentered("* KasirKilat POS *", 8f)

        document.finishPage(page)
        return document
    }

    fun shareReceiptAsText(context: Context, tx: TransactionEntity, items: List<TransactionItemEntity>, config: StoreConfig) {
        val text = formatReceiptText(tx, items, config)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Struk Belanja - ${tx.invoiceNumber}")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Struk Belanja").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
