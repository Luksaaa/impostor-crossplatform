package com.example.impostergame

import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import impostergame.impostor_cross_platform.generated.resources.Res

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
    val isReady: Boolean = false,
    val joinedAt: Long = 0L
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
    val originalAdmin: String = "",
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
    var wordPairs: List<Pair<String, String>> = listOf(
        "Jabuka" to "Kruška",
        "Automobil" to "Motor",
        "Zagreb" to "Split",
        "Sunce" to "Mjesec",
        "Kava" to "Čaj",
        "Pas" to "Mačka",
        "Ljeto" to "Zima",
        "Pizza" to "Burger"
    )

    @OptIn(ExperimentalResourceApi::class)
    suspend fun loadDictionary() {
        try {
            // Učitavanje cijelog hrvatskog rječnika iz composeResources mape
            val bytes = Res.readBytes("files/hrvatski_rijecnik.txt")
            val text = bytes.decodeToString()
            val parsed = text.lines().mapNotNull { line ->
                val parts = line.split("/")
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
            }
            if (parsed.isNotEmpty()) {
                wordPairs = parsed
                println("Uspješno učitan rječnik s \${parsed.size} parova!")
            }
        } catch (e: Exception) {
            println("Greška pri učitavanju rječnika: \${e.message}")
        }
    }

    fun getNextWords(): Pair<String, String> {
        val pair = wordPairs.random()
        return if ((0..1).random() == 0) {
            pair.first to pair.second
        } else {
            pair.second to pair.first
        }
    }
}
