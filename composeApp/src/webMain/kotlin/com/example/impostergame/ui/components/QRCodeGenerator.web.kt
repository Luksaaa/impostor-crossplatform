package com.example.impostergame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
actual fun QRCodeImage(content: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(100.dp)
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Text("QR", fontSize = 20.sp, color = Color.Gray)
    }
}

actual fun generateQRCodeBitmap(content: String, size: Int): ImageBitmap? = null
