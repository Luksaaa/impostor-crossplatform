package com.example.impostergame

import androidx.compose.runtime.Composable

@Composable
actual fun CommonBackHandler(onBack: () -> Unit) {
    // Na desktopu nema "Back" tipke kao na Androidu, 
    // pa BackHandler ne radi ništa po defaultu.
}