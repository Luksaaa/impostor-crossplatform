package com.example.impostergame

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlin.js.Json
import kotlin.js.json

actual object FirebaseManager : IFirebaseManager {
    private val firebaseScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private fun isFirebaseReady(): Boolean {
        return js("typeof firebase !== 'undefined' && typeof firebase.database === 'function'").unsafeCast<Boolean>()
    }

    private fun generateRandomCode(): String {
        val letters = ('A'..'Z').toList()
        val numbers = ('0'..'9').toList()
        return (1..3).map { letters.random() }.joinToString("") +
               (1..3).map { numbers.random() }.joinToString("")
    }

    override fun generateRoom(username: String, onComplete: (String) -> Unit) {
        if (!isFirebaseReady()) {
            console.error("Firebase NOT READY")
            onComplete("")
            return
        }

        firebaseScope.launch {
            try {
                val code = generateRandomCode()
                val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Igrac" }
                val now = currentPlatformMillis().toDouble()
                
                console.log("Attempting to create room: $code")
                val db = firebase.database()
                
                db.ref("rooms/$code").once("value", { snapshot: dynamic ->
                    if (snapshot.exists().unsafeCast<Boolean>()) {
                        generateRoom(username, onComplete)
                    } else {
                        val roomData = json(
                            "admin" to sanitizedName,
                            "status" to "waiting",
                            "players" to json(sanitizedName to json("name" to sanitizedName, "isReady" to false, "joinedAt" to now)),
                            "messages" to json("init" to "$sanitizedName je napravio sobu")
                        )
                        db.ref("rooms/$code").set(roomData, { err: dynamic ->
                            if (err == null) {
                                console.log("Room created: $code")
                                onComplete(code)
                            } else {
                                console.error("Set error", err)
                                onComplete("")
                            }
                        })
                    }
                }, { err: dynamic ->
                    console.error("Once error", err)
                    onComplete("")
                })
            } catch (e: Exception) {
                console.error("generateRoom crash", e)
                onComplete("")
            }
        }
    }

    override suspend fun joinRoom(roomCode: String, username: String): Result<Unit> {
        if (!isFirebaseReady()) return Result.failure(Exception("Firebase not ready"))
        
        return try {
            val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Gost" }
            val timestamp = currentPlatformMillis().toDouble()
            val db = firebase.database()
            
            console.log("Joining room: $roomCode as $sanitizedName")
            
            db.ref("rooms/$roomCode/players/$sanitizedName").set(json(
                "name" to sanitizedName, "isReady" to false, "joinedAt" to timestamp
            ))
            db.ref("rooms/$roomCode/messages/join_${currentPlatformMillis()}").set("$sanitizedName je ušao")
            
            Result.success(Unit)
        } catch (e: Exception) {
            console.error("Join error", e)
            Result.failure(e)
        }
    }

    override fun leaveRoomWithAdminTransfer(roomCode: String, username: String, onComplete: () -> Unit) {
        if (!isFirebaseReady()) { onComplete(); return }
        try {
            val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
            firebase.database().ref("rooms/$roomCode/players/$sanitizedName").remove()
            onComplete()
        } catch (e: Exception) {
            onComplete()
        }
    }

    override fun listenToRoom(roomCode: String): Flow<Room?> = callbackFlow {
        if (!isFirebaseReady()) { 
            trySend(null)
            close()
            return@callbackFlow 
        }
        
        val db = firebase.database()
        val ref = db.ref("rooms/$roomCode")
        val callback = { snapshot: dynamic ->
            try {
                if (snapshot == null || !snapshot.exists().unsafeCast<Boolean>()) {
                    trySend(null)
                } else {
                    val data = snapshot.`val`()
                    if (data == null) {
                        trySend(null)
                    } else {
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

                        trySend(Room(
                            admin = data.admin?.toString() ?: "",
                            status = data.status?.toString() ?: "waiting",
                            imposterId = data.imposterId?.toString() ?: "",
                            chatMessages = chatMap,
                            players = playersMap,
                            isDiscussionActive = data.isDiscussionActive?.unsafeCast<Boolean>() ?: false,
                            resultMessage = data.resultMessage?.toString() ?: ""
                        ))
                    }
                }
            } catch (e: Exception) {
                console.error("Listen parse error", e)
            }
        }
        
        ref.on("value", callback)
        awaitClose { ref.off("value", callback) }
    }

    override fun toggleReady(roomCode: String, username: String, isReady: Boolean) {
        if (!isFirebaseReady()) return
        val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
        firebase.database().ref("rooms/$roomCode/players/$sanitizedName/isReady").set(isReady)
    }

    override fun startGame(roomCode: String, playersList: List<String>) {
        if (!isFirebaseReady()) return
        val (main, imp) = WordManager.getNextWords()
        val shuffled = playersList.shuffled()
        val update = json(
            "mainWord" to main, "imposterWord" to imp,
            "imposterId" to shuffled[0], "status" to "started"
        )
        firebase.database().ref("rooms/$roomCode").asDynamic().update(update)
    }

    override fun sendMessage(roomCode: String, username: String, message: String) {
        if (!isFirebaseReady()) return
        val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
        val chatMsg = json("sender" to sanitizedName, "message" to message.trim(), "timestamp" to currentPlatformMillis().toDouble())
        firebase.database().ref("rooms/$roomCode/chatMessages").push().set(chatMsg)
    }

    override fun startDiscussion(roomCode: String, seconds: Int) {
        if (!isFirebaseReady()) return
        val now = currentPlatformMillis().toDouble()
        val update = if (seconds > 0) json("isDiscussionActive" to true, "discussionStartTime" to now, "discussionEndTime" to (now + seconds * 1000))
                     else json("isDiscussionActive" to false)
        firebase.database().ref("rooms/$roomCode").asDynamic().update(update)
    }

    override fun endRound(roomCode: String, resultMessage: String) {
        if (!isFirebaseReady()) return
        firebase.database().ref("rooms/$roomCode").asDynamic().update(json("status" to "finished", "resultMessage" to resultMessage, "isDiscussionActive" to false))
    }

    override fun resetToLobby(roomCode: String) {
        if (!isFirebaseReady()) return
        firebase.database().ref("rooms/$roomCode").asDynamic().update(json("status" to "waiting", "chatMessages" to null, "isDiscussionActive" to false))
    }

    override fun removePlayer(roomCode: String, playerName: String) {
        if (!isFirebaseReady()) return
        firebase.database().ref("rooms/$roomCode/players/$playerName").remove()
    }
}

private external val firebase: dynamic
