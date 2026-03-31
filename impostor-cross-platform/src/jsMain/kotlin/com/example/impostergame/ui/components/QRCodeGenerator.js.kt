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
import kotlinx.coroutines.delay

@Composable
actual fun QRCodeImage(content: String, modifier: Modifier) {
    // Spremit ćemo "qr" objekt kad se stvori
    var qrObj by remember(content) { mutableStateOf<dynamic>(null) }
    var loaded by remember(content) { mutableStateOf(false) }

    LaunchedEffect(content) {
        // Pokušaj više puta ako se skripta još skida (max 10 puta s 200ms = 2 sekunde čekanja)
        for (i in 1..10) {
            val exists = js("typeof qrcode !== 'undefined'").unsafeCast<Boolean>()
            if (exists) {
                try {
                    // qrcode-generator: qrcode(typeNumber, errorCorrectionLevel)
                    // typeNumber: 0 (auto-detect) do 40
                    // errCorrectionLevel: L, M, Q, H
                    val tempQr = js("qrcode(0, 'M')")
                    tempQr.addData(content)
                    tempQr.make()
                    
                    qrObj = tempQr
                    loaded = true
                    break // Izađi iz petlje, uspjeli smo
                } catch (e: Throwable) {
                    println("QR Code Generation Error: \${e.message}")
                    break
                }
            }
            delay(200)
        }
    }

    Box(modifier = modifier.size(200.dp).background(Color.White), contentAlignment = Alignment.Center) {
        if (loaded && qrObj != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                try {
                    val size = qrObj.getModuleCount().unsafeCast<Int>()
                    val cellSize = this.size.width / size
                    
                    for (row in 0 until size) {
                        for (col in 0 until size) {
                            val isDark = qrObj.isDark(row, col).unsafeCast<Boolean>()
                            if (isDark) {
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(col * cellSize, row * cellSize),
                                    size = Size(cellSize + 0.1f, cellSize + 0.1f) // +0.1f protiv bijelih linija
                                )
                            }
                        }
                    }
                } catch (e: Throwable) {
                    println("QR Code Canvas Error: \${e.message}")
                }
            }
        } else {
            // Sivi fallback (čekamo ili je skripta blokirana)
            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray))
        }
    }
}

actual fun generateQRCodeBitmap(content: String, size: Int): ImageBitmap? = null
