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
            // Sigurna provjera objekta QRCode iz qrcode.min.js
            if (js("typeof QRCode !== 'undefined'").unsafeCast<Boolean>()) {
                val options = js("({ errorCorrectionLevel: 'M' })")
                // qrcode biblioteka (v1.5.3) ima QRCode.create metodu
                val qr = js("QRCode.create(content, options)")
                if (qr != null && qr.modules != null) {
                    val size = qr.modules.size.unsafeCast<Int>()
                    val data = qr.modules.data
                    modules = size to data
                }
            }
        } catch (e: Exception) { 
            // Ne dozvoljavamo da greška sruši UI
            println("QR Error: ${e.message}")
        }
    }

    Box(modifier = modifier.size(200.dp).background(Color.White), contentAlignment = Alignment.Center) {
        val current = modules
        if (current != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                try {
                    val size = current.first
                    val data = current.second
                    val cellSize = this.size.width / size
                    for (row in 0 until size) {
                        for (col in 0 until size) {
                            // Provjera crne boje (vrijednost različita od nule je crna)
                            val isBlack = js("data[row * size + col] !== 0").unsafeCast<Boolean>()
                            if (isBlack) {
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(col * cellSize, row * cellSize),
                                    size = Size(cellSize + 0.1f, cellSize + 0.1f)
                                )
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
        } else {
            // Sivi kvadrat dok se podaci ne učitaju
            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray))
        }
    }
}

actual fun generateQRCodeBitmap(content: String, size: Int): ImageBitmap? = null
