package com.example.impostergame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp

@Composable
actual fun QRCodeImage(content: String, modifier: Modifier) {
    var modules by remember(content) { mutableStateOf<Pair<Int, dynamic>?>(null) }

    LaunchedEffect(content) {
        try {
            if (js("typeof QRCode !== 'undefined'").unsafeCast<Boolean>()) {
                val options = js("({ errorCorrectionLevel: 'H' })")
                val qr = js("QRCode.create(content, options)")
                val size = qr.modules.size.unsafeCast<Int>()
                val data = qr.modules.data // dynamic (Uint8Array)
                modules = size to data
            }
        } catch (e: Exception) {
            console.error("QR Error:", e)
        }
    }

    Box(modifier = modifier.size(200.dp).background(Color.White), contentAlignment = Alignment.Center) {
        val current = modules
        if (current != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val size = current.first
                val data = current.second
                val cellSize = this.size.width / size
                
                for (row in 0 until size) {
                    for (col in 0 until size) {
                        // Accessing Uint8Array elements via dynamic access
                        val isBlack = js("data[row * size + col] === 1").unsafeCast<Boolean>()
                        if (isBlack) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(col * cellSize, row * cellSize),
                                size = Size(cellSize + 0.5f, cellSize + 0.5f)
                            )
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray))
        }
    }
}

actual fun generateQRCodeBitmap(content: String, size: Int): ImageBitmap? = null
