package com.example.impostergame

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun CommonBackHandler(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}
