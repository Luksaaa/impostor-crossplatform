package com.example.impostergame

import androidx.compose.runtime.Composable

@Composable
actual fun CommonBackHandler(onBack: () -> Unit) {
    // iOS gesture back is usually handled via navigation controllers
}
