package com.example.impostergame

import androidx.compose.runtime.Composable

@Composable
actual fun CommonBackHandler(onBack: () -> Unit) {
    // Desktop doesn't have a system back button in the same way
}
