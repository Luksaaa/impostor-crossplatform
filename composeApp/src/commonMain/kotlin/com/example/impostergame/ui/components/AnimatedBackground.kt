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
    xOffset: Float,
    yOffset: Float,
    content: @Composable () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) DarkGray else LightGray
    
    val blueAlpha = if (isDarkTheme) 0.2f else 0.62f
    val purpleAlpha = if (isDarkTheme) 0.2f else 0.62f

    // Smanjujemo radijus krugova za tamni način rada
    val blueRadius = if (isDarkTheme) 200.dp else 350.dp
    val purpleRadius = if (isDarkTheme) 180.dp else 300.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        BlueGradient.copy(alpha = blueAlpha),
                        Color.Transparent
                    ),
                    center = Offset(xOffset.dp.toPx(), yOffset.dp.toPx()),
                    radius = blueRadius.toPx()
                ),
                radius = blueRadius.toPx(),
                center = Offset(xOffset.dp.toPx(), yOffset.dp.toPx())
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PurpleGradient.copy(alpha = purpleAlpha),
                        Color.Transparent
                    ),
                    center = Offset((400 - xOffset).dp.toPx(), (800 - yOffset).dp.toPx()),
                    radius = purpleRadius.toPx()
                ),
                radius = purpleRadius.toPx(),
                center = Offset((400 - xOffset).dp.toPx(), (800 - yOffset).dp.toPx())
            )
        }
        content()
    }
}
