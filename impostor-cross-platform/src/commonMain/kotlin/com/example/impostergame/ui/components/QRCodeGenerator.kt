package com.example.impostergame.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

@Composable
expect fun QRCodeImage(content: String, modifier: Modifier = Modifier)

expect fun generateQRCodeBitmap(content: String, size: Int): ImageBitmap?
