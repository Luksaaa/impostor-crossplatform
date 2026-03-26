package com.example.impostergame.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun CameraScanner(
    modifier: Modifier,
    onResult: (String) -> Unit
) {
    // Nema podrške za skeniranje kamere direktno na JVM Desktopu bez kompliciranih C biblioteka
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("Skeniranje QR koda nije podržano na računalu.")
    }
}