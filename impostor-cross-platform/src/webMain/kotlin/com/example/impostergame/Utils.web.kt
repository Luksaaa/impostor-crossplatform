package com.example.impostergame

import kotlinx.datetime.Clock

actual fun currentPlatformMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
}
