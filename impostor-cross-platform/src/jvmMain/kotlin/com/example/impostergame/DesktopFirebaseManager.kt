package com.example.impostergame

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

object DesktopFirebaseManager : IFirebaseManager {
    private const val BASE_URL = "https://gameofimpostor-default-rtdb.europe-west1.firebasedatabase.app/rooms"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    private val lastTimestamp = AtomicLong(0)

    private fun getUniqueTimestamp(): Long {
        val now = System.currentTimeMillis()
        while (true) {
            val last = lastTimestamp.get()
            val next = if (now > last) now else last + 1
            if (lastTimestamp.compareAndSet(last, next)) return next
        }
    }

    private fun generateRandomCode(): String {
        val letters = ('A'..'Z').toList()
        val numbers = ('0'..'9').toList()
        return (1..3).map { letters.random() }.joinToString("") + 
               (1..3).map { numbers.random() }.joinToString("")
    }

    private inline fun <reified T> putData(path: String, data: T) {
        try {
            val url = URL("$BASE_URL/$path.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.write(json.encodeToString(data).toByteArray())
            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {
            println("Firebase Desktop Error: ${e.message}")
        }
    }
    
    fun patchData(roomCode: String, updates: JsonObject) {
        try {
            val url = URL("$BASE_URL/$roomCode.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.write(json.encodeToString(updates).toByteArray())
            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {
            println("Firebase Desktop Error: ${e.message}")
        }
    }

    override fun generateRoom(username: String, onComplete: (String) -> Unit) {
        scope.launch {
            val code = generateRandomCode()
            val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Igrac" }
            val now = getUniqueTimestamp()
            val room = Room(
                admin = sanitizedName,
                originalAdmin = sanitizedName,
                status = "waiting",
                players = mapOf(sanitizedName to PlayerInfo(sanitizedName, false, now)),
                messages = mapOf("init" to "$sanitizedName je napravio sobu")
            )
            putData(code, room)
            withContext(Dispatchers.Main) { onComplete(code) }
        }
    }

    override suspend fun joinRoom(roomCode: String, username: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/$roomCode.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            
            if (response == "null") return@withContext Result.failure(Exception("Soba ne postoji"))
            
            val room = json.decodeFromString<Room>(response)
            if (room.players.size >= 16) return@withContext Result.failure(Exception("Soba je puna"))
            
            val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Gost" }
            val now = getUniqueTimestamp()
            putData("$roomCode/players/$sanitizedName", PlayerInfo(sanitizedName, false, now))
            putData("$roomCode/messages/join_$now", "$sanitizedName je ušao")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun leaveRoomWithAdminTransfer(roomCode: String, username: String, onComplete: () -> Unit) {
        scope.launch {
            try {
                val url = URL("$BASE_URL/$roomCode.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                if (response != "null") {
                    val room = json.decodeFromString<Room>(response)
                    val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
                    val timestamp = getUniqueTimestamp()
                    
                    val updates = mutableMapOf<String, JsonElement>()
                    updates["players/$sanitizedName"] = JsonNull
                    
                    if (room.admin == sanitizedName) {
                        val nextAdmin = room.players.values
                            .filter { it.name != sanitizedName }
                            .sortedBy { it.joinedAt }
                            .firstOrNull()?.name
                        
                        if (nextAdmin != null) {
                            updates["admin"] = JsonPrimitive(nextAdmin)
                            updates["originalAdmin"] = JsonPrimitive(nextAdmin) // STALNI ADMIN (kad sam izađe)
                            val msg = "$sanitizedName je izašao, novi admin je $nextAdmin"
                            updates["messages/exit_$timestamp"] = JsonPrimitive(msg)
                            updates["chatMessages/sys_$timestamp"] = json.encodeToJsonElement(ChatMessage("Sustav", msg, timestamp))
                        }
                    } else {
                        val msg = "$sanitizedName je izašao"
                        updates["messages/exit_$timestamp"] = JsonPrimitive(msg)
                        updates["chatMessages/sys_$timestamp"] = json.encodeToJsonElement(ChatMessage("Sustav", msg, timestamp))
                    }
                    
                    patchData(roomCode, JsonObject(updates))
                }
            } catch (e: Exception) {
                println("Firebase Desktop Error: ${e.message}")
            }
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    override fun listenToRoom(roomCode: String): Flow<Room?> = flow {
        while (true) {
            try {
                val url = URL("$BASE_URL/$roomCode.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                
                if (response != "null") {
                    emit(json.decodeFromString<Room>(response))
                } else {
                    emit(null)
                }
            } catch (e: Exception) { }
            delay(1500)
        }
    }.flowOn(Dispatchers.IO)

    override fun toggleReady(roomCode: String, username: String, isReady: Boolean) {
        scope.launch {
            val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
            putData("$roomCode/players/$sanitizedName/isReady", isReady)
        }
    }
    
    override fun startGame(roomCode: String, playersList: List<String>) {
        scope.launch {
            val (mainWord, imposterWord) = WordManager.getNextWords()
            val shuffled = playersList.shuffled()
            val imposterId = shuffled.getOrNull(0) ?: ""
            val mrWhiteId = if (shuffled.size >= 3 && (1..100).random() <= 20) shuffled.getOrNull(1) ?: "" else ""
            
            val updates = mutableMapOf<String, JsonElement>(
                "mainWord" to JsonPrimitive(mainWord),
                "imposterWord" to JsonPrimitive(imposterWord),
                "imposterId" to JsonPrimitive(imposterId),
                "mrWhiteId" to JsonPrimitive(mrWhiteId),
                "status" to JsonPrimitive("started"),
                "chatMessages" to JsonNull,
                "isDiscussionActive" to JsonPrimitive(false),
                "discussionStartTime" to JsonPrimitive(0L),
                "discussionEndTime" to JsonPrimitive(0L),
                "resultMessage" to JsonPrimitive("")
            )
            patchData(roomCode, JsonObject(updates))
        }
    }

    override fun sendMessage(roomCode: String, username: String, message: String) {
        scope.launch {
            try {
                val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
                val timestamp = getUniqueTimestamp()
                val chatMsg = ChatMessage(sanitizedName, message.trim(), timestamp)
                
                val url = URL("$BASE_URL/$roomCode/chatMessages/msg_$timestamp.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.write(json.encodeToString(chatMsg).toByteArray())
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                println("Firebase Desktop Error: ${e.message}")
            }
        }
    }

    override fun startDiscussion(roomCode: String, seconds: Int) {
        scope.launch {
            val now = getUniqueTimestamp()
            if (seconds > 0) {
                val endTime = now + (seconds * 1000L)
                val updates = mutableMapOf<String, JsonElement>(
                    "isDiscussionActive" to JsonPrimitive(true),
                    "discussionStartTime" to JsonPrimitive(now),
                    "discussionEndTime" to JsonPrimitive(endTime)
                )
                patchData(roomCode, JsonObject(updates))
            } else {
                val updates = mutableMapOf<String, JsonElement>("isDiscussionActive" to JsonPrimitive(false))
                patchData(roomCode, JsonObject(updates))
            }
        }
    }

    override fun endRound(roomCode: String, resultMessage: String) {
        scope.launch {
            val updates = mutableMapOf<String, JsonElement>(
                "status" to JsonPrimitive("finished"),
                "resultMessage" to JsonPrimitive(resultMessage),
                "isDiscussionActive" to JsonPrimitive(false)
            )
            patchData(roomCode, JsonObject(updates))
        }
    }

    override fun resetToLobby(roomCode: String) {
        scope.launch {
            try {
                val url = URL("$BASE_URL/$roomCode.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                
                if (response == "null") return@launch
                val room = json.decodeFromString<Room>(response)
                
                val updates = mutableMapOf<String, JsonElement>(
                    "status" to JsonPrimitive("waiting"),
                    "chatMessages" to JsonNull,
                    "isDiscussionActive" to JsonPrimitive(false),
                    "discussionStartTime" to JsonPrimitive(0L),
                    "discussionEndTime" to JsonPrimitive(0L),
                    "resultMessage" to JsonPrimitive("")
                )
                
                // UVIJEK VRAĆAMO ADMINA NA ORIGINALNOG KREATORA BEZ UVJETA
                if (room.originalAdmin.isNotEmpty()) {
                    updates["admin"] = JsonPrimitive(room.originalAdmin)
                    if (room.players.containsKey(room.originalAdmin)) {
                        updates["players/${room.originalAdmin}/isReady"] = JsonPrimitive(false)
                    }
                }
                
                patchData(roomCode, JsonObject(updates))
            } catch (e: Exception) {
                println("Firebase Desktop Error: ${e.message}")
            }
        }
    }
    
    override fun removePlayer(roomCode: String, playerName: String) {
        scope.launch {
            try {
                val url = URL("$BASE_URL/$roomCode.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                
                if (response == "null") return@launch
                val room = json.decodeFromString<Room>(response)
                val timestamp = getUniqueTimestamp()
                
                val updates = mutableMapOf<String, JsonElement>()
                updates["players/$playerName"] = JsonNull
                
                val exitMsg: String
                if (room.admin == playerName) {
                    val nextActiveAdmin = room.players.values
                        .filter { it.name != playerName }
                        .sortedBy { it.joinedAt }
                        .firstOrNull()?.name
                    
                    if (nextActiveAdmin != null) {
                        exitMsg = "$playerName je izbačen, privremeni admin je $nextActiveAdmin"
                        updates["admin"] = JsonPrimitive(nextActiveAdmin)
                        // ORIGINALADMIN OSTAJE KREATOR (PRIVREMENI PRIJENOS)
                    } else {
                        exitMsg = "$playerName je izbačen"
                    }
                } else {
                    exitMsg = "$playerName je izbačen"
                }
                
                updates["messages/exit_$timestamp"] = JsonPrimitive(exitMsg)
                updates["chatMessages/sys_$timestamp"] = json.encodeToJsonElement(ChatMessage("Sustav", exitMsg, timestamp))

                patchData(roomCode, JsonObject(updates))
            } catch (e: Exception) {
                println("Firebase Desktop Error: ${e.message}")
            }
        }
    }
}
