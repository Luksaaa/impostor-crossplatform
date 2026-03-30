package com.example.impostergame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun CameraScanner(
    modifier: Modifier,
    onResult: (String) -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text("Scanner not supported on Web yet", color = Color.White)
    }
}
