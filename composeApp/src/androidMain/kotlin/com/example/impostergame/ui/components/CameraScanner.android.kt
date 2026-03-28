package com.example.impostergame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
actual fun CameraScanner(
    modifier: Modifier,
    onResult: (String) -> Unit
) {
    val context = LocalContext.current
    var hasLaunched by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasLaunched) {
            hasLaunched = true
            val scanner = GmsBarcodeScanning.getClient(context)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    barcode.rawValue?.let { value ->
                        onResult(value)
                    }
                }
                .addOnFailureListener {
                    // Ignoriraj, korisnik je možda odustao
                }
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Otvara se Googleov skener...",
            color = Color.White
        )
    }
}
