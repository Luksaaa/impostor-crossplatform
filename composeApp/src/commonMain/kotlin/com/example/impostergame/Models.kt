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

@Serializable
data class Room(
    val admin: String = "",
    val status: String = "waiting", // "waiting", "started", "finished"
    val players: Map<String, PlayerInfo> = emptyMap(),
    val messages: Map<String, String> = emptyMap(),
    val chatMessages: Map<String, ChatMessage> = emptyMap(),
    val mainWord: String = "",
    val imposterWord: String = "",
    val imposterId: String = "",
    val mrWhiteId: String = "",
    val isDiscussionActive: Boolean = false,
    val discussionStartTime: Long = 0L,
    val discussionEndTime: Long = 0L,
    val resultMessage: String = ""
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
