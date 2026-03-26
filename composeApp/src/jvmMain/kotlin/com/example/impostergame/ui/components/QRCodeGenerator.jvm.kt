package com.example.impostergame.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.Color
import java.awt.image.BufferedImage

@Composable
actual fun QRCodeImage(content: String, modifier: Modifier) {
    val bitmap = remember(content) { generateQRCodeBitmap(content, 512) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "QR Code",
            modifier = modifier
        )
    }
}

actual fun generateQRCodeBitmap(content: String, size: Int): ImageBitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                image.setRGB(x, y, if (bitMatrix[x, y]) Color.BLACK.rgb else Color.WHITE.rgb)
            }
        }
        image.toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
