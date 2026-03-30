package com.example.impostergame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.impostergame.ui.theme.*

@Composable
fun AnimatedBackground(
    xOffset: Float, // Očekuje se 0..1f progress
    yOffset: Float, // Očekuje se 0..1f progress
    content: @Composable () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F5F5)
    
    // Povećavamo vidljivost boja
    val blueAlpha = if (isDarkTheme) 0.25f else 0.4f
    val purpleAlpha = if (isDarkTheme) 0.25f else 0.4f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Plavi krug koji se kreće po cijelom ekranu
            val blueRadius = (width.coerceAtMost(height) * 0.6f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        BlueGradient.copy(alpha = blueAlpha),
                        Color.Transparent
                    ),
                    center = Offset(width * xOffset, height * yOffset),
                    radius = blueRadius
                ),
                radius = blueRadius,
                center = Offset(width * xOffset, height * yOffset)
            )
            
            // Ljubičasti krug koji se kreće suprotno
            val purpleRadius = (width.coerceAtMost(height) * 0.5f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PurpleGradient.copy(alpha = purpleAlpha),
                        Color.Transparent
                    ),
                    center = Offset(width * (1f - xOffset), height * (1f - yOffset)),
                    radius = purpleRadius
                ),
                radius = purpleRadius,
                center = Offset(width * (1f - xOffset), height * (1f - yOffset))
            )
        }
        content()
    }
}
