package com.example.impostergame

import kotlinx.serialization.Serializable

@Serializable
enum class Screen {
    ENTER_NAME,
    HOME,
    JOIN,
    LOBBY,
    GAME
}

@Serializable
data class PlayerInfo(
    val name: String = "",
    val isReady: Boolean = false
)

@Serializable
data class ChatMessage(
    val sender: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

object WordManager {
    private val wordPairs: List<Pair<String, String>> = listOf(
        "Jabuka" to "Kruška",
        "Automobil" to "Motor",
        "Zagreb" to "Split",
        "Sunce" to "Mjesec"
    )

    fun getNextWords(): Pair<String, String> {
        val pair = wordPairs.random()
        return if ((0..1).random() == 0) {
            pair.first to pair.second
        } else {
            pair.second to pair.first
        }
    }
}
