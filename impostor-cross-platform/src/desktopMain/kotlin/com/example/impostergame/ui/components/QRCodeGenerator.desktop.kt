package com.example.impostergame.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage

@Composable
actual fun QRCodeImage(content: String, modifier: Modifier) {
    val bitmap = remember(content) {
        generateQRCodeBitmap(content, 512)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "QR Code for $content",
            modifier = modifier.fillMaxSize()
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No QR")
        }
    }
}

actual fun generateQRCodeBitmap(content: String, size: Int): ImageBitmap? {
    if (content.isBlank()) return null
    return try {
        val hints = mapOf(com.google.zxing.EncodeHintType.MARGIN to 1)
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bufferedImage.setRGB(x, y, if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        
        bufferedImage.toComposeImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}