package com.example.impostergame

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.withContext
import kotlin.js.Json
import kotlin.js.json

actual object FirebaseManager {
    private val firebaseScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val database: dynamic get() = firebase.database()
    private val roomsRef: dynamic get() = database.ref("rooms")

    private fun generateRandomCode(): String {
        val letters = ('A'..'Z').toList()
        val numbers = ('0'..'9').toList()
        return (1..3).map { letters.random() }.joinToString("") +
               (1..3).map { numbers.random() }.joinToString("")
    }

    actual fun generateRoom(username: String, onComplete: (String) -> Unit) {
        firebaseScope.launch {
            try {
                val code = generateRandomCode()
                val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Igrac" }
                val now = currentPlatformMillis().toDouble()

                val snapshot = (roomsRef.child(code).once("value").await() as JsDataSnapshot)
                if (snapshot.exists()) {
                    generateRoom(username, onComplete)
                } else {
                    val roomData = json(
                        "admin" to sanitizedName,
                        "status" to "waiting",
                        "players" to json(sanitizedName to json("name" to sanitizedName, "isReady" to false, "joinedAt" to now)),
                        "messages" to json("init" to "$sanitizedName je napravio sobu")
                    )
                    roomsRef.child(code).set(roomData).await()
                    withContext(Dispatchers.Main) { onComplete(code) }
                }
            } catch (e: Exception) {
                console.error("Error generating room:", e)
                withContext(Dispatchers.Main) { onComplete("ERROR") }
            }
        }
    }

    actual suspend fun joinRoom(roomCode: String, username: String): Result<Unit> {
        return try {
            val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Gost" }
            val roomRef = roomsRef.child(roomCode)
            val snapshot = (roomRef.once("value").await() as JsDataSnapshot)

            if (!snapshot.exists()) return Result.failure(Exception("Soba ne postoji"))

            val timestamp = currentPlatformMillis().toDouble()
            roomRef.child("players").child(sanitizedName).set(json("name" to sanitizedName, "isReady" to false, "joinedAt" to timestamp)).await()
            roomRef.child("messages").child("join_${currentPlatformMillis()}").set("$sanitizedName je ušao").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual fun leaveRoomWithAdminTransfer(roomCode: String, username: String, onComplete: () -> Unit) {
        firebaseScope.launch {
            try {
                val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
                val roomRef = roomsRef.child(roomCode)
                roomRef.child("players").child(sanitizedName).remove().await()
                withContext(Dispatchers.Main) { onComplete() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }

    actual fun listenToRoom(roomCode: String): Flow<Room?> = callbackFlow {
        val roomRef = roomsRef.child(roomCode)
        val callback = { snapshot: JsDataSnapshot ->
            if (!snapshot.exists()) {
                trySend(null)
            } else {
                val rawData = snapshot.`val`()
                val data = rawData.unsafeCast<RoomJsInterop>()
                
                val playersMap = mutableMapOf<String, PlayerInfo>()
                val playersJson = data.players
                if (playersJson != null) {
                    val keys = js("Object.keys(playersJson)").unsafeCast<Array<String>>()
                    for (key in keys) {
                        val p = playersJson[key].unsafeCast<PlayerInfoJsInterop>()
                        playersMap[key] = PlayerInfo(p.name, p.isReady, p.joinedAt.toLong())
                    }
                }

                val chatMap = mutableMapOf<String, ChatMessage>()
                val chatJson = data.chatMessages
                if (chatJson != null) {
                    val keys = js("Object.keys(chatJson)").unsafeCast<Array<String>>()
                    for (key in keys) {
                        val c = chatJson[key].unsafeCast<ChatMessageJsInterop>()
                        chatMap[key] = ChatMessage(c.sender, c.message, c.timestamp.toLong())
                    }
                }

                trySend(Room(
                    admin = data.admin,
                    status = data.status,
                    imposterId = data.imposterId ?: "",
                    mrWhiteId = data.mrWhiteId ?: "",
                    imposterWord = data.imposterWord ?: "",
                    mainWord = data.mainWord ?: "",
                    chatMessages = chatMap,
                    players = playersMap,
                    isDiscussionActive = data.isDiscussionActive ?: false,
                    discussionStartTime = data.discussionStartTime?.toLong() ?: 0L,
                    discussionEndTime = data.discussionEndTime?.toLong() ?: 0L,
                    resultMessage = data.resultMessage ?: ""
                ))
            }
        }
        roomRef.on("value", callback)
        awaitClose { roomRef.off("value", callback) }
    }

    actual fun toggleReady(roomCode: String, username: String, isReady: Boolean) {
        firebaseScope.launch {
            val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
            roomsRef.child(roomCode).child("players").child(sanitizedName).child("isReady").set(isReady).await()
        }
    }

    actual fun startGame(roomCode: String, playersList: List<String>) {
        firebaseScope.launch {
            val (mainWord, imposterWord) = WordManager.getNextWords()
            val shuffled = playersList.shuffled()
            val imposterId = shuffled[0]
            val mrWhiteId = if (shuffled.size >= 3 && (1..100).random() <= 20) shuffled[1] else ""

            val updateObj = json(
                "mainWord" to mainWord,
                "imposterWord" to imposterWord,
                "imposterId" to imposterId,
                "mrWhiteId" to mrWhiteId,
                "status" to "started",
                "chatMessages" to null,
                "isDiscussionActive" to false,
                "discussionStartTime" to 0,
                "discussionEndTime" to 0,
                "resultMessage" to ""
            )
            // Use update via Dynamic to avoid issues
            roomsRef.child(roomCode).asDynamic().update(updateObj)
        }
    }

    actual fun sendMessage(roomCode: String, username: String, message: String) {
        firebaseScope.launch {
            val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
            val timestamp = currentPlatformMillis().toDouble()
            val chatMsg = json("sender" to sanitizedName, "message" to message.trim(), "timestamp" to timestamp)
            roomsRef.child(roomCode).child("chatMessages").push().set(chatMsg).await()
        }
    }

    actual fun startDiscussion(roomCode: String, seconds: Int) {
        firebaseScope.launch {
            val updateObj = if (seconds > 0) {
                val now = currentPlatformMillis().toDouble()
                val endTime = now + (seconds * 1000.0)
                json(
                    "isDiscussionActive" to true,
                    "discussionStartTime" to now,
                    "discussionEndTime" to endTime
                )
            } else {
                json(
                    "isDiscussionActive" to false,
                    "discussionStartTime" to 0,
                    "discussionEndTime" to 0
                )
            }
            roomsRef.child(roomCode).asDynamic().update(updateObj)
        }
    }

    actual fun endRound(roomCode: String, resultMessage: String) {
        firebaseScope.launch {
            roomsRef.child(roomCode).asDynamic().update(json(
                "status" to "finished",
                "resultMessage" to resultMessage,
                "isDiscussionActive" to false
            ))
        }
    }

    actual fun resetToLobby(roomCode: String) {
        firebaseScope.launch {
            roomsRef.child(roomCode).asDynamic().update(json(
                "status" to "waiting",
                "chatMessages" to null,
                "isDiscussionActive" to false,
                "discussionStartTime" to 0,
                "discussionEndTime" to 0,
                "resultMessage" to ""
            ))
        }
    }

    actual fun removePlayer(roomCode: String, playerName: String) {
        firebaseScope.launch {
            roomsRef.child(roomCode).child("players").child(playerName).remove().await()
        }
    }
}
