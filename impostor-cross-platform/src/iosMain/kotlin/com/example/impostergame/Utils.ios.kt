package com.example.impostergame

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun currentPlatformMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}