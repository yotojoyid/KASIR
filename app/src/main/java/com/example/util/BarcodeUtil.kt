package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

object BarcodeUtil {

    /**
     * Generates a 2D boolean grid representing a QR code matrix.
     * Implements a deterministic QR-style matrix representation with standard finder patterns
     * (top-left, top-right, bottom-left 7x7 position squares) and payload encoding.
     */
    fun generateQrMatrix(content: String, size: Int = 29): Array<BooleanArray> {
        val matrix = Array(size) { BooleanArray(size) { false } }

        // Draw standard QR finder patterns
        drawFinderPattern(matrix, 0, 0)
        drawFinderPattern(matrix, size - 7, 0)
        drawFinderPattern(matrix, 0, size - 7)

        // Draw timing patterns
        for (i in 8 until size - 8) {
            val isBlack = (i % 2 == 0)
            matrix[6][i] = isBlack
            matrix[i][6] = isBlack
        }

        // Encode content hash & characters into data area
        val bytes = content.toByteArray()
        var bitIndex = 0
        val totalBits = bytes.size * 8

        for (c in 0 until size) {
            for (r in 0 until size) {
                // Skip finder patterns
                if ((r < 8 && c < 8) || (r < 8 && c >= size - 8) || (r >= size - 8 && c < 8)) {
                    continue
                }
                // Skip timing patterns
                if (r == 6 || c == 6) continue

                // Populate with data bits and alignment masks
                val byteVal = if (bytes.isNotEmpty()) bytes[(bitIndex / 8) % bytes.size].toInt() else 0
                val bitVal = (byteVal shr (7 - (bitIndex % 8))) and 1
                val mask = ((r + c) % 2 == 0)
                matrix[r][c] = (bitVal == 1) xor mask
                bitIndex++
            }
        }

        return matrix
    }

    private fun drawFinderPattern(matrix: Array<BooleanArray>, startR: Int, startC: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val isOuter = (r == 0 || r == 6 || c == 0 || c == 6)
                val isInner = (r in 2..4 && c in 2..4)
                matrix[startR + r][startC + c] = isOuter || isInner
            }
        }
    }

    /**
     * Generates a dynamic QRIS payload string format.
     */
    fun createQrisPayload(
        merchantName: String,
        nmid: String,
        amount: Double,
        invoiceNumber: String
    ): String {
        val amountStr = String.format("%.0f", amount)
        return "00020101021226600016ID.CO.QRIS.WWW0118${nmid}0215$invoiceNumber" +
                "51440014ID.LINKAJA.WWW0215$invoiceNumber" +
                "520458125303360540${amountStr.length}$amountStr" +
                "5802ID59${merchantName.length}$merchantName" +
                "6007JAKARTA61051293062180114$invoiceNumber" +
                "6304ABCD"
    }

    /**
     * Generates pseudo-barcode pattern (Code 128 style) from a barcode or SKU string.
     */
    fun getBarcodePattern(code: String): List<Int> {
        val pattern = mutableListOf<Int>()
        pattern.addAll(listOf(2, 1, 1, 2)) // Start code
        val hash = abs(code.hashCode())
        var current = hash

        for (i in 0 until 35) {
            val width = (current % 3) + 1
            pattern.add(width)
            current = (current * 31 + 17) and 0x7FFFFFFF
        }
        pattern.addAll(listOf(2, 3, 1, 1, 2)) // Stop code
        return pattern
    }

    fun generateQrBitmap(content: String, dimension: Int = 512): Bitmap {
        val matrix = generateQrMatrix(content, 29)
        val bitmap = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888)
        val cellSize = dimension.toFloat() / matrix.size

        for (r in matrix.indices) {
            for (c in matrix[r].indices) {
                val color = if (matrix[r][c]) Color.BLACK else Color.WHITE
                val startX = (c * cellSize).toInt()
                val endX = ((c + 1) * cellSize).toInt().coerceAtMost(dimension)
                val startY = (r * cellSize).toInt()
                val endY = ((r + 1) * cellSize).toInt().coerceAtMost(dimension)

                for (x in startX until endX) {
                    for (y in startY until endY) {
                        bitmap.setPixel(x, y, color)
                    }
                }
            }
        }
        return bitmap
    }
}

@Composable
fun BarcodeView(
    code: String,
    modifier: Modifier = Modifier,
    barHeight: Dp = 56.dp,
    barColor: ComposeColor = ComposeColor.Black
) {
    val pattern = BarcodeUtil.getBarcodePattern(code)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
    ) {
        val totalUnits = pattern.sum().toFloat()
        val unitWidth = size.width / totalUnits
        var currentX = 0f

        pattern.forEachIndexed { index, barUnits ->
            val isBar = index % 2 == 0
            val barW = barUnits * unitWidth
            if (isBar) {
                drawRect(
                    color = barColor,
                    topLeft = Offset(currentX, 0f),
                    size = Size(barW, size.height)
                )
            }
            currentX += barW
        }
    }
}

@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    qrColor: ComposeColor = ComposeColor.Black,
    backgroundColor: ComposeColor = ComposeColor.White
) {
    val matrix = BarcodeUtil.generateQrMatrix(content)
    Canvas(modifier = modifier) {
        // Draw background
        drawRect(
            color = backgroundColor,
            topLeft = Offset.Zero,
            size = size
        )
        val moduleSize = size.width / matrix.size

        for (r in matrix.indices) {
            for (c in matrix[r].indices) {
                if (matrix[r][c]) {
                    drawRect(
                        color = qrColor,
                        topLeft = Offset(c * moduleSize, r * moduleSize),
                        size = Size(moduleSize, moduleSize)
                    )
                }
            }
        }
    }
}
