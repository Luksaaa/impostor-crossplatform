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
import kotlinx.browser.window

@Composable
actual fun QRCodeImage(content: String, modifier: Modifier) {
    var modules by remember(content) { mutableStateOf<Pair<Int, dynamic>?>(null) }

    LaunchedEffect(content) {
        // Čekamo da se window.QRCode objekt učita, ako već nije
        if (js("typeof QRCode === 'undefined'").unsafeCast<Boolean>()) {
            // Mala odgoda, ako skripta nije odmah dostupna
            kotlinx.coroutines.delay(100)
        }

        try {
            // Sigurna provjera objekta QRCode iz qrcode.min.js
            val qrCodeExists = js("typeof QRCode !== 'undefined'").unsafeCast<Boolean>()
            if (qrCodeExists) {
                val options = js("({ errorCorrectionLevel: 'M' })")
                // Biblioteka qrcode (npm qrcode) ima metodu 'create'
                val qr = js("QRCode.create(content, options)")
                if (qr != null && qr.modules != null) {
                    val size = qr.modules.size.unsafeCast<Int>()
                    val data = qr.modules.data
                    modules = size to data
                }
            }
        } catch (e: Throwable) {
            // Ne dopuštamo da greška sruši aplikaciju (Script error)
            println("QR Code Generator failed silently: ${e.message}")
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
                } catch (e: Throwable) {
                    println("QR Code Canvas drawing error: ${e.message}")
                }
            }
        } else {
            // Sivi kvadrat kao fallback
            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray))
        }
    }
}

actual fun generateQRCodeBitmap(content: String, size: Int): ImageBitmap? = null
