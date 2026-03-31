package com.example.impostergame

import kotlin.js.Date

actual fun currentPlatformMillis(): Long {
    // Koristimo direktni JS Date kako bismo izbjegli LinkageError s kotlinx-datetime na JS-u
    return Date.now().toLong()
}
