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
    
    // Completely dynamic access to avoid mapping crashes
    private fun getRoomsRef(): dynamic {
        return try {
            val fb: dynamic = js("typeof firebase !== 'undefined' ? firebase : null")
            fb?.database()?.ref("rooms")
        } catch (e: Exception) {
            null
        }
    }

    private fun generateRandomCode(): String {
        val letters = ('A'..'Z').toList()
        val numbers = ('0'..'9').toList()
        return (1..3).map { letters.random() }.joinToString("") +
               (1..3).map { numbers.random() }.joinToString("")
    }

    actual fun generateRoom(username: String, onComplete: (String) -> Unit) {
        val ref = getRoomsRef()
        if (ref == null) {
            onComplete("")
            return
        }

        firebaseScope.launch {
            try {
                val code = generateRandomCode()
                val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Igrac" }
                val now = currentPlatformMillis().toDouble()

                val snapshot: dynamic = ref.child(code).once("value").await()
                if (snapshot.exists().unsafeCast<Boolean>()) {
                    generateRoom(username, onComplete)
                } else {
                    val roomData = json(
                        "admin" to sanitizedName,
                        "status" to "waiting",
                        "imposterId" to "",
                        "mrWhiteId" to "",
                        "imposterWord" to "",
                        "mainWord" to "",
                        "isDiscussionActive" to false,
                        "discussionStartTime" to 0.0,
                        "discussionEndTime" to 0.0,
                        "resultMessage" to "",
                        "players" to json(sanitizedName to json("name" to sanitizedName, "isReady" to false, "joinedAt" to now)),
                        "messages" to json("init" to "$sanitizedName je napravio sobu")
                    )
                    ref.child(code).set(roomData).await()
                    withContext(Dispatchers.Main) { onComplete(code) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete("") }
            }
        }
    }

    actual suspend fun joinRoom(roomCode: String, username: String): Result<Unit> {
        val ref = getRoomsRef() ?: return Result.failure(Exception("Firebase unavailable"))
        return try {
            val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Gost" }
            val timestamp = currentPlatformMillis().toDouble()
            val playerUpdate = json("name" to sanitizedName, "isReady" to false, "joinedAt" to timestamp)
            
            ref.child(roomCode).child("players").child(sanitizedName).set(playerUpdate).await()
            ref.child(roomCode).child("messages").child("join_${currentPlatformMillis()}").set("$sanitizedName je ušao").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual fun leaveRoomWithAdminTransfer(roomCode: String, username: String, onComplete: () -> Unit) {
        val ref = getRoomsRef()
        if (ref == null) { onComplete(); return }
        firebaseScope.launch {
            try {
                val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
                ref.child(roomCode).child("players").child(sanitizedName).remove().await()
                withContext(Dispatchers.Main) { onComplete() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }

    actual fun listenToRoom(roomCode: String): Flow<Room?> = callbackFlow {
        val ref = getRoomsRef()
        if (ref == null) { trySend(null); close(); return@callbackFlow }
        
        val roomRef = ref.child(roomCode)
        val callback = { snapshot: dynamic ->
            try {
                if (!snapshot.exists().unsafeCast<Boolean>()) {
                    trySend(null)
                } else {
                    val data = snapshot.`val`()
                    
                    val playersMap = mutableMapOf<String, PlayerInfo>()
                    val pJson = data.players
                    if (pJson != null) {
                        val keys = js("Object.keys(pJson)").unsafeCast<Array<String>>()
                        for (k in keys) {
                            val p = pJson[k]
                            playersMap[k] = PlayerInfo(
                                p.name?.toString() ?: "", 
                                p.isReady?.unsafeCast<Boolean>() ?: false, 
                                (p.joinedAt?.unsafeCast<Double>() ?: 0.0).toLong()
                            )
                        }
                    }

                    val chatMap = mutableMapOf<String, ChatMessage>()
                    val cJson = data.chatMessages
                    if (cJson != null) {
                        val keys = js("Object.keys(cJson)").unsafeCast<Array<String>>()
                        for (k in keys) {
                            val c = cJson[k]
                            chatMap[k] = ChatMessage(
                                c.sender?.toString() ?: "", 
                                c.message?.toString() ?: "", 
                                (c.timestamp?.unsafeCast<Double>() ?: 0.0).toLong()
                            )
                        }
                    }
                    
                    val msgMap = mutableMapOf<String, String>()
                    val mJson = data.messages
                    if (mJson != null) {
                        val keys = js("Object.keys(mJson)").unsafeCast<Array<String>>()
                        for (k in keys) {
                            msgMap[k] = mJson[k]?.toString() ?: ""
                        }
                    }

                    trySend(Room(
                        admin = data.admin?.toString() ?: "",
                        status = data.status?.toString() ?: "waiting",
                        imposterId = data.imposterId?.toString() ?: "",
                        mrWhiteId = data.mrWhiteId?.toString() ?: "",
                        imposterWord = data.imposterWord?.toString() ?: "",
                        mainWord = data.mainWord?.toString() ?: "",
                        chatMessages = chatMap,
                        players = playersMap,
                        messages = msgMap,
                        isDiscussionActive = data.isDiscussionActive?.unsafeCast<Boolean>() ?: false,
                        discussionStartTime = (data.discussionStartTime?.unsafeCast<Double>() ?: 0.0).toLong(),
                        discussionEndTime = (data.discussionEndTime?.unsafeCast<Double>() ?: 0.0).toLong(),
                        resultMessage = data.resultMessage?.toString() ?: ""
                    ))
                }
            } catch (e: Exception) {
                console.error("Parse error", e)
            }
        }
        roomRef.on("value", callback)
        awaitClose { roomRef.off("value", callback) }
    }

    actual fun toggleReady(roomCode: String, username: String, isReady: Boolean) {
        val ref = getRoomsRef() ?: return
        firebaseScope.launch {
            val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
            ref.child(roomCode).child("players").child(sanitizedName).child("isReady").set(isReady).await()
        }
    }

    actual fun startGame(roomCode: String, playersList: List<String>) {
        val ref = getRoomsRef() ?: return
        firebaseScope.launch {
            val (main, imp) = WordManager.getNextWords()
            val shuffled = playersList.shuffled()
            val update = json(
                "mainWord" to main, "imposterWord" to imp,
                "imposterId" to shuffled[0], "status" to "started"
            )
            ref.child(roomCode).asDynamic().update(update)
        }
    }

    actual fun sendMessage(roomCode: String, username: String, message: String) {
        val ref = getRoomsRef() ?: return
        firebaseScope.launch {
            val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
            val chatMsg = json("sender" to sanitizedName, "message" to message.trim(), "timestamp" to currentPlatformMillis().toDouble())
            ref.child(roomCode).child("chatMessages").push().set(chatMsg).await()
        }
    }

    actual fun startDiscussion(roomCode: String, seconds: Int) {
        val ref = getRoomsRef() ?: return
        firebaseScope.launch {
            val now = currentPlatformMillis().toDouble()
            val update = if (seconds > 0) json("isDiscussionActive" to true, "discussionStartTime" to now, "discussionEndTime" to (now + seconds * 1000))
                         else json("isDiscussionActive" to false)
            ref.child(roomCode).asDynamic().update(update)
        }
    }

    actual fun endRound(roomCode: String, resultMessage: String) {
        val ref = getRoomsRef() ?: return
        firebaseScope.launch {
            ref.child(roomCode).asDynamic().update(json("status" to "finished", "resultMessage" to resultMessage, "isDiscussionActive" to false))
        }
    }

    actual fun resetToLobby(roomCode: String) {
        val ref = getRoomsRef() ?: return
        firebaseScope.launch {
            ref.child(roomCode).asDynamic().update(json("status" to "waiting", "chatMessages" to null, "isDiscussionActive" to false))
        }
    }

    actual fun removePlayer(roomCode: String, playerName: String) {
        val ref = getRoomsRef() ?: return
        firebaseScope.launch { ref.child(roomCode).child("players").child(playerName).remove().await() }
    }
}
