@file:JvmName("AppMain")
package com.example.impostergame

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

fun main() {
    try {
        application {
            val windowState = rememberWindowState(width = 1000.dp, height = 800.dp)
            
            Window(
                onCloseRequest = ::exitApplication,
                title = "ImposterGame Desktop",
                state = windowState
            ) {
                App()
            }
        }
    } catch (e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        File("desktop-crash.log").writeText(sw.toString())
        throw e
    }
}
