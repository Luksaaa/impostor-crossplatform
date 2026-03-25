package com.example.impostergame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QRCodeImage(content: String, modifier: Modifier = Modifier) {
    // Placeholder za QR kod u Multiplatformi
    // Može se implementirati koristeći KMP biblioteku ili expect/actual
    Box(
        modifier = modifier
            .size(100.dp)
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Text("QR", fontSize = 20.sp, color = Color.Gray)
    }
}
