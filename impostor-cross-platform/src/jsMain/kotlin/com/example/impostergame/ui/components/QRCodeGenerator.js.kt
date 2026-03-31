package com.example.impostergame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp

// External JS declaration for the qrcode library - Case sensitive!
// Standard browser global for this library is 'QRCode'
@JsName("QRCode")
external object QRCodeJs {
    fun create(content: String, options: dynamic): dynamic
}

@Composable
actual fun QRCodeImage(content: String, modifier: Modifier) {
    // Generiramo QR kod kao matricu modula u JS-u i crtamo ga na Compose Canvasu
    val modules = remember(content) {
        try {
            // Check if global QRCode is defined to avoid crash
            if (js("typeof QRCode !== 'undefined'").unsafeCast<Boolean>()) {
                val qr = QRCodeJs.create(content, json("errorCorrectionLevel" to "H"))
                val count = qr.modules.size.unsafeCast<Int>()
                val result = Array(count) { x ->
                    BooleanArray(count) { y ->
                        qr.modules.get(x).get(y).unsafeCast<Boolean>()
                    }
                }
                result
            } else {
                null
            }
        } catch (e: Exception) {
            console.error("QR Code Generation error:", e)
            null
        }
    }

    if (modules != null) {
        Canvas(modifier = modifier.size(200.dp).background(Color.White)) {
            val count = modules.size
            val cellSize = size.width / count
            for (x in 0 until count) {
                for (y in 0 until count) {
                    if (modules[x][y]) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(x * cellSize, y * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f) // Small overlap to avoid lines
                        )
                    }
                }
            }
        }
    } else {
        Box(modifier = modifier.size(200.dp).background(Color.LightGray), contentAlignment = Alignment.Center) {
            // Fallback placeholder when library is not loaded yet
        }
    }
}

actual fun generateQRCodeBitmap(content: String, size: Int): ImageBitmap? = null

private fun json(vararg pairs: Pair<String, Any?>): dynamic {
    val res = js("({})")
    for (pair in pairs) res[pair.first] = pair.second
    return res
}
