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
    
    private var lastTimestamp: Double = 0.0

    private fun getUniqueTimestamp(): Double {
        var now = currentPlatformMillis().toDouble()
        if (now <= lastTimestamp) {
            now = lastTimestamp + 1.0
        }
        lastTimestamp = now
        return now
    }

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
        if (!isFirebaseReady()) { onComplete(""); return }
        firebaseScope.launch {
            try {
                val code = generateRandomCode()
                val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Igrac" }
                val now = getUniqueTimestamp()
                val db = firebase.database()
                
                db.ref("rooms/$code").once("value", { snapshot: dynamic ->
                    if (snapshot.exists().unsafeCast<Boolean>()) {
                        generateRoom(username, onComplete)
                    } else {
                        val roomData = json(
                            "admin" to sanitizedName,
                            "originalAdmin" to sanitizedName,
                            "status" to "waiting",
                            "players" to json(sanitizedName to json("name" to sanitizedName, "isReady" to false, "joinedAt" to now)),
                            "messages" to json("init" to "$sanitizedName je napravio sobu")
                        )
                        db.ref("rooms/$code").set(roomData, { err: dynamic ->
                            if (err == null) onComplete(code) else onComplete("")
                        })
                    }
                })
            } catch (e: Exception) { onComplete("") }
        }
    }

    override suspend fun joinRoom(roomCode: String, username: String): Result<Unit> {
        if (!isFirebaseReady()) return Result.failure(Exception("Firebase not ready"))
        return try {
            val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Gost" }
            val timestamp = getUniqueTimestamp()
            firebase.database().ref("rooms/$roomCode/players/$sanitizedName").set(json(
                "name" to sanitizedName, "isReady" to false, "joinedAt" to timestamp
            ))
            firebase.database().ref("rooms/$roomCode/messages/join_${timestamp.toLong()}").set("$sanitizedName je ušao")
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override fun leaveRoomWithAdminTransfer(roomCode: String, username: String, onComplete: () -> Unit) {
        if (!isFirebaseReady()) { onComplete(); return }
        val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
        val roomRef = firebase.database().ref("rooms/$roomCode")
        
        roomRef.once("value", { snapshot: dynamic ->
            val data = snapshot.`val`()
            if (data != null) {
                val timestamp = getUniqueTimestamp()
                val updates = json()
                updates["players/$sanitizedName"] = null
                
                val currentAdmin = data.admin?.toString() ?: ""
                if (currentAdmin == sanitizedName) {
                    val pKeys = js("Object.keys(data.players)").unsafeCast<Array<String>>()
                    val otherPlayers = mutableListOf<PlayerInfo>()
                    for (k in pKeys) {
                        if (k != sanitizedName) {
                            val p = js("data.players[k]")
                            otherPlayers.add(PlayerInfo(k, false, (p.joinedAt?.unsafeCast<Double>() ?: 0.0).toLong()))
                        }
                    }
                    val next = otherPlayers.sortedBy { it.joinedAt }.firstOrNull()?.name
                    if (next != null) {
                        updates["admin"] = next
                        updates["originalAdmin"] = next // Postaje trajni vlasnik
                        val msg = "$sanitizedName je izašao, novi admin je $next"
                        updates["messages/exit_${timestamp.toLong()}"] = msg
                        roomRef.child("chatMessages").child("sys_${timestamp.toLong()}").set(json("sender" to "Sustav", "message" to msg, "timestamp" to timestamp))
                    } else {
                        // Nema drugih igrača, soba će biti prazna
                        val msg = "$sanitizedName je izašao"
                        updates["messages/exit_${timestamp.toLong()}"] = msg
                        roomRef.child("chatMessages").child("sys_${timestamp.toLong()}").set(json("sender" to "Sustav", "message" to msg, "timestamp" to timestamp))
                    }
                } else {
                    val msg = "$sanitizedName je izašao"
                    updates["messages/exit_${timestamp.toLong()}"] = msg
                    roomRef.child("chatMessages").child("sys_${timestamp.toLong()}").set(json("sender" to "Sustav", "message" to msg, "timestamp" to timestamp))
                }
                roomRef.update(updates, { _ -> onComplete() })
            } else {
                onComplete()
            }
        })
    }

    override fun listenToRoom(roomCode: String): Flow<Room?> = callbackFlow {
        if (!isFirebaseReady()) { trySend(null); close(); return@callbackFlow }
        val ref = firebase.database().ref("rooms/$roomCode")
        val callback = { snapshot: dynamic ->
            try {
                if (snapshot == null || !snapshot.exists().unsafeCast<Boolean>()) {
                    trySend(null)
                } else {
                    val data = snapshot.`val`()
                    val playersMap = mutableMapOf<String, PlayerInfo>()
                    
                    if (data != null && data.players != null) {
                        val keys = js("Object.keys(data.players)").unsafeCast<Array<String>>()
                        for (k in keys) {
                            val p = js("data.players[k]")
                            if (p != null) {
                                playersMap[k] = PlayerInfo(
                                    name = p.name?.toString() ?: "",
                                    isReady = p.isReady?.unsafeCast<Boolean>() ?: false,
                                    joinedAt = (p.joinedAt?.unsafeCast<Double>() ?: 0.0).toLong()
                                )
                            }
                        }
                    }

                    val chatMap = mutableMapOf<String, ChatMessage>()
                    if (data != null && data.chatMessages != null) {
                        val keys = js("Object.keys(data.chatMessages)").unsafeCast<Array<String>>()
                        for (k in keys) {
                            val c = js("data.chatMessages[k]")
                            if (c != null) {
                                chatMap[k] = ChatMessage(
                                    sender = c.sender?.toString() ?: "",
                                    message = c.message?.toString() ?: "",
                                    timestamp = (c.timestamp?.unsafeCast<Double>() ?: 0.0).toLong()
                                )
                            }
                        }
                    }

                    val eventMsgs = mutableMapOf<String, String>()
                    if (data != null && data.messages != null) {
                        val keys = js("Object.keys(data.messages)").unsafeCast<Array<String>>()
                        for (k in keys) {
                            val msg = js("data.messages[k]")
                            eventMsgs[k] = msg?.toString() ?: ""
                        }
                    }

                    trySend(Room(
                        admin = data.admin?.toString() ?: "",
                        originalAdmin = data.originalAdmin?.toString() ?: "",
                        status = data.status?.toString() ?: "waiting",
                        imposterId = data.imposterId?.toString() ?: "",
                        mrWhiteId = data.mrWhiteId?.toString() ?: "",
                        imposterWord = data.imposterWord?.toString() ?: "",
                        mainWord = data.mainWord?.toString() ?: "",
                        isDiscussionActive = data.isDiscussionActive?.unsafeCast<Boolean>() ?: false,
                        discussionStartTime = (data.discussionStartTime?.unsafeCast<Double>() ?: 0.0).toLong(),
                        discussionEndTime = (data.discussionEndTime?.unsafeCast<Double>() ?: 0.0).toLong(),
                        resultMessage = data.resultMessage?.toString() ?: "",
                        chatMessages = chatMap,
                        players = playersMap,
                        messages = eventMsgs
                    ))
                }
            } catch (e: Exception) { }
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
        val update = json("mainWord" to main, "imposterWord" to imp, "imposterId" to shuffled[0], "status" to "started", "chatMessages" to null, "isDiscussionActive" to false, "resultMessage" to "")
        firebase.database().ref("rooms/$roomCode").update(update)
    }

    override fun sendMessage(roomCode: String, username: String, message: String) {
        if (!isFirebaseReady()) return
        val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
        val timestamp = getUniqueTimestamp()
        val chatMsg = json("sender" to sanitizedName, "message" to message.trim(), "timestamp" to timestamp)
        firebase.database().ref("rooms/$roomCode/chatMessages").child("msg_${timestamp.toLong()}").set(chatMsg)
    }

    override fun startDiscussion(roomCode: String, seconds: Int) {
        if (!isFirebaseReady()) return
        val now = getUniqueTimestamp()
        val update = if (seconds > 0) json("isDiscussionActive" to true, "discussionStartTime" to now, "discussionEndTime" to (now + seconds * 1000))
                     else json("isDiscussionActive" to false)
        firebase.database().ref("rooms/$roomCode").update(update)
    }

    override fun endRound(roomCode: String, resultMessage: String) {
        if (!isFirebaseReady()) return
        firebase.database().ref("rooms/$roomCode").update(json("status" to "finished", "resultMessage" to resultMessage, "isDiscussionActive" to false))
    }

    override fun resetToLobby(roomCode: String) {
        if (!isFirebaseReady()) return
        val roomRef = firebase.database().ref("rooms/$roomCode")
        roomRef.once("value", { snapshot: dynamic ->
            val data = snapshot.`val`()
            if (data != null) {
                val original = data.originalAdmin?.toString() ?: ""
                val update = json("status" to "waiting", "chatMessages" to null, "isDiscussionActive" to false, "resultMessage" to "")
                
                // POPRAVAK: Vraćamo admina originalnom kreatoru SAMO ako je on još uvijek u sobi
                if (original.isNotEmpty() && js("data.players && data.players[original] !== undefined").unsafeCast<Boolean>()) {
                    update["admin"] = original
                    update["players/$original/isReady"] = false
                } else if (original.isNotEmpty() && !js("data.players && data.players[original] !== undefined").unsafeCast<Boolean>()) {
                    // Ako originalni admin NIJE u sobi, a POSTOJE drugi igrači, postaviti najstarijeg kao admina i originalAdmina
                    val nextActiveAdmin = js("Object.values(data.players)").unsafeCast<Array<dynamic>>()
                        .filter { p -> p.name?.toString() != original }
                        .sortedBy { p -> p.joinedAt?.unsafeCast<Double>() ?: 0.0 }
                        .firstOrNull()?.name?.toString()

                    if (nextActiveAdmin != null) {
                        update["admin"] = nextActiveAdmin
                        update["originalAdmin"] = nextActiveAdmin // Postaje trajni vlasnik
                        update["players/$nextActiveAdmin/isReady"] = false
                    }
                }
                
                roomRef.update(update)
            }
        })
    }

    override fun removePlayer(roomCode: String, playerName: String) {
        if (!isFirebaseReady()) return
        val roomRef = firebase.database().ref("rooms/$roomCode")
        roomRef.once("value", { snapshot: dynamic ->
            val data = snapshot.`val`()
            if (data != null) {
                val currentAdmin = data.admin?.toString() ?: ""
                val updates = json()
                updates["players/$playerName"] = null

                val timestamp = getUniqueTimestamp()
                var msg = "$playerName je izbačen"
                if (currentAdmin == playerName) {
                    val pKeys = js("Object.keys(data.players)").unsafeCast<Array<String>>()
                    val otherPlayers = mutableListOf<PlayerInfo>()
                    for (k in pKeys) {
                        if (k != playerName) {
                            val p = js("data.players[k]")
                            otherPlayers.add(PlayerInfo(k, false, (p.joinedAt?.unsafeCast<Double>() ?: 0.0).toLong()))
                        }
                    }
                    val next = otherPlayers.sortedBy { it.joinedAt }.firstOrNull()?.name
                    if (next != null) {
                        updates["admin"] = next
                        updates["originalAdmin"] = next // Trajna promjena
                        msg = "$playerName je izbačen, novi admin je $next"
                    } else {
                        // Nema drugih igrača, soba će biti prazna
                        msg = "$playerName je izbačen"
                    }
                } else {
                    // Igrač koji nije admin je izbačen - admin se ne mijenja, originalAdmin se ne mijenja
                    msg = "$playerName je izbačen"
                }

                updates["messages/exit_${timestamp.toLong()}"] = msg
                roomRef.update(updates, { _ ->
                    roomRef.child("chatMessages").child("sys_${timestamp.toLong()}").set(json("sender" to "Sustav", "message" to msg, "timestamp" to timestamp))
                })
            }
        })
    }
}
