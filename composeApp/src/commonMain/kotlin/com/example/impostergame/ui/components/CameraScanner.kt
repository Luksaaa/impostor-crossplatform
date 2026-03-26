package com.example.impostergame.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CameraScanner(
    modifier: Modifier = Modifier,
    onResult: (String) -> Unit
)
