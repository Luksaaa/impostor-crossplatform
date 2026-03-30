package com.example.impostergame

import dev.gitlive.firebase.database.DatabaseReference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL

object DesktopFirebaseManager : IFirebaseManager {
    // We throw exception if accessed because this is used only in Android
    override val roomsRef: DatabaseReference get() = throw UnsupportedOperationException("roomsRef not available on Desktop")
    
    private const val BASE_URL = "https://gameofimpostor-default-rtdb.europe-west1.firebasedatabase.app/rooms"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

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

    fun deleteData(path: String) {
        try {
            val url = URL("$BASE_URL/$path.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
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
            val now = System.currentTimeMillis()
            val room = Room(
                admin = sanitizedName,
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
            val now = System.currentTimeMillis()
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
                
                if (response == "null") {
                    withContext(Dispatchers.Main) { onComplete() }
                    return@launch
                }
                
                val room = json.decodeFromString<Room>(response)
                val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
                val timestamp = System.currentTimeMillis()
                
                if (room.players.size <= 1) {
                    deleteData(roomCode)
                } else {
                    val updates = mutableMapOf<String, JsonElement>()
                    updates["players/$sanitizedName"] = JsonNull
                    
                    val exitMsg: String
                    if (room.admin == sanitizedName) {
                        val nextActiveAdmin = room.players.values
                            .filter { it.name != sanitizedName }
                            .sortedBy { it.joinedAt }
                            .firstOrNull()?.name
                        
                        exitMsg = "$sanitizedName je izašao, privremeni admin je $nextActiveAdmin"
                    } else {
                        exitMsg = "$sanitizedName je izašao"
                    }
                    
                    updates["messages/exit_$timestamp"] = JsonPrimitive(exitMsg)
                    updates["chatMessages/sys_$timestamp"] = json.encodeToJsonElement(ChatMessage("Sustav", exitMsg, timestamp))
                    
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
            } catch (e: Exception) {
                // Ignore network errors in polling loop
            }
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
            
            val updates = buildJsonObject {
                put("mainWord", JsonPrimitive(mainWord))
                put("imposterWord", JsonPrimitive(imposterWord))
                put("imposterId", JsonPrimitive(imposterId))
                put("mrWhiteId", JsonPrimitive(mrWhiteId))
                put("status", JsonPrimitive("started"))
                put("chatMessages", JsonNull)
                put("isDiscussionActive", JsonPrimitive(false))
                put("discussionStartTime", JsonPrimitive(0L))
                put("discussionEndTime", JsonPrimitive(0L))
                put("resultMessage", JsonPrimitive(""))
            }
            patchData(roomCode, updates)
        }
    }

    override fun sendMessage(roomCode: String, username: String, message: String) {
        scope.launch {
            try {
                val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
                val timestamp = System.currentTimeMillis()
                val chatMsg = ChatMessage(sanitizedName, message.trim(), timestamp)
                
                // POST requests stvara `push()` kljuc unutar Firebasea
                val url = URL("$BASE_URL/$roomCode/chatMessages.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
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
            if (seconds > 0) {
                val now = System.currentTimeMillis()
                val endTime = now + (seconds * 1000L)
                val updates = buildJsonObject {
                    put("isDiscussionActive", JsonPrimitive(true))
                    put("discussionStartTime", JsonPrimitive(now))
                    put("discussionEndTime", JsonPrimitive(endTime))
                }
                patchData(roomCode, updates)
            } else {
                val updates = buildJsonObject {
                    put("isDiscussionActive", JsonPrimitive(false))
                    put("discussionStartTime", JsonPrimitive(0L))
                    put("discussionEndTime", JsonPrimitive(0L))
                }
                patchData(roomCode, updates)
            }
        }
    }

    override fun endRound(roomCode: String, resultMessage: String) {
        scope.launch {
            val updates = buildJsonObject {
                put("status", JsonPrimitive("finished"))
                put("resultMessage", JsonPrimitive(resultMessage))
                put("isDiscussionActive", JsonPrimitive(false))
            }
            patchData(roomCode, updates)
        }
    }

    override fun resetToLobby(roomCode: String) {
        scope.launch {
            val updates = buildJsonObject {
                put("status", JsonPrimitive("waiting"))
                put("chatMessages", JsonNull)
                put("isDiscussionActive", JsonPrimitive(false))
                put("discussionStartTime", JsonPrimitive(0L))
                put("discussionEndTime", JsonPrimitive(0L))
                put("resultMessage", JsonPrimitive(""))
            }
            patchData(roomCode, updates)
        }
    }
    
    override fun removePlayer(roomCode: String, playerName: String) {
        scope.launch {
            try {
                // Prvo dohvatiti sobu da vidimo tko je admin
                val url = URL("$BASE_URL/$roomCode.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                
                if (response == "null") return@launch
                val room = json.decodeFromString<Room>(response)
                val timestamp = System.currentTimeMillis()
                
                val updates = mutableMapOf<String, JsonElement>()
                updates["players/$playerName"] = JsonNull
                
                val exitMsg: String
                if (room.admin == playerName) {
                    val nextActiveAdmin = room.players.values
                        .filter { it.name != playerName }
                        .sortedBy { it.joinedAt }
                        .firstOrNull()?.name
                    
                    exitMsg = "$playerName je izbačen, privremeni admin je $nextActiveAdmin"
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
