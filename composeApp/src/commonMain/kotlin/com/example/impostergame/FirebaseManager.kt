package com.example.impostergame

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface IFirebaseManager {
    val roomsRef: DatabaseReference
    fun generateRoom(username: String, onComplete: (String) -> Unit)
    suspend fun joinRoom(roomCode: String, username: String): Result<Unit>
    fun leaveRoomWithAdminTransfer(roomCode: String, username: String, onComplete: () -> Unit)
    fun listenToRoom(roomCode: String): Flow<Room?>
    fun toggleReady(roomCode: String, username: String, isReady: Boolean)
    fun startGame(roomCode: String, playersList: List<String>)
    fun sendMessage(roomCode: String, username: String, message: String)
    fun startDiscussion(roomCode: String, seconds: Int)
    fun endRound(roomCode: String, resultMessage: String)
    fun resetToLobby(roomCode: String)
    fun removePlayer(roomCode: String, playerName: String)
}

var activeFirebaseManager: IFirebaseManager? = null

object FirebaseManager : IFirebaseManager {
    private val delegate get() = activeFirebaseManager ?: GitLiveFirebaseManager

    override val roomsRef: DatabaseReference get() = delegate.roomsRef

    override fun generateRoom(username: String, onComplete: (String) -> Unit) = delegate.generateRoom(username, onComplete)
    override suspend fun joinRoom(roomCode: String, username: String): Result<Unit> = delegate.joinRoom(roomCode, username)
    override fun leaveRoomWithAdminTransfer(roomCode: String, username: String, onComplete: () -> Unit) = delegate.leaveRoomWithAdminTransfer(roomCode, username, onComplete)
    override fun listenToRoom(roomCode: String): Flow<Room?> = delegate.listenToRoom(roomCode)
    override fun toggleReady(roomCode: String, username: String, isReady: Boolean) = delegate.toggleReady(roomCode, username, isReady)
    override fun startGame(roomCode: String, playersList: List<String>) = delegate.startGame(roomCode, playersList)
    override fun sendMessage(roomCode: String, username: String, message: String) = delegate.sendMessage(roomCode, username, message)
    override fun startDiscussion(roomCode: String, seconds: Int) = delegate.startDiscussion(roomCode, seconds)
    override fun endRound(roomCode: String, resultMessage: String) = delegate.endRound(roomCode, resultMessage)
    override fun resetToLobby(roomCode: String) = delegate.resetToLobby(roomCode)
    override fun removePlayer(roomCode: String, playerName: String) = delegate.removePlayer(roomCode, playerName)
}

object GitLiveFirebaseManager : IFirebaseManager {
    private val database: FirebaseDatabase get() = Firebase.database
    override val roomsRef: DatabaseReference get() = database.reference("rooms")
    
    private val firebaseScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun generateRoom(username: String, onComplete: (String) -> Unit) {
        firebaseScope.launch {
            try {
                val code = generateRandomCode()
                val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Igrac" }
                
                // Using valueEvents.first() as it seems to be the most compatible way in this project's setup
                val snapshot = roomsRef.child(code).valueEvents.first()
                
                if (snapshot.value != null) {
                    generateRoom(username, onComplete)
                } else {
                    val roomData = mapOf(
                        "admin" to sanitizedName,
                        "status" to "waiting",
                        "players" to mapOf(sanitizedName to mapOf("name" to sanitizedName, "isReady" to false)),
                        "messages" to mapOf("init" to "$sanitizedName je napravio sobu")
                    )
                    roomsRef.child(code).setValue(roomData)
                    
                    withContext(Dispatchers.Main) {
                        onComplete(code)
                    }
                }
            } catch (e: Exception) {
                println("Firebase Error: ${e.message}")
            }
        }
    }

    private fun generateRandomCode(): String {
        val letters = ('A'..'Z').toList()
        val numbers = ('0'..'9').toList()
        return (1..3).map { letters.random() }.joinToString("") + 
               (1..3).map { numbers.random() }.joinToString("")
    }

    override suspend fun joinRoom(roomCode: String, username: String): Result<Unit> {
        return try {
            val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Gost" }
            val roomRef = roomsRef.child(roomCode)
            val snapshot = roomRef.valueEvents.first()
            
            if (snapshot.value == null) {
                return Result.failure(Exception("Soba ne postoji"))
            }
            
            val players = snapshot.child("players")
            var count = 0
            players.children.forEach { _ -> count++ }
            
            if (count >= 16) {
                return Result.failure(Exception("Soba je puna"))
            }
            
            roomRef.child("players").child(sanitizedName).setValue(mapOf("name" to sanitizedName, "isReady" to false))
            val timestamp = currentPlatformMillis()
            roomRef.child("messages").child("join_$timestamp").setValue("$sanitizedName je ušao")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun leaveRoomWithAdminTransfer(roomCode: String, username: String, onComplete: () -> Unit) {
        firebaseScope.launch {
            try {
                val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
                val roomRef = roomsRef.child(roomCode)
                val snapshot = roomRef.valueEvents.first()
                
                if (snapshot.value == null) {
                    withContext(Dispatchers.Main) { onComplete() }
                    return@launch
                }
                
                val currentAdmin = snapshot.child("admin").getValueSafe<String?>()
                val playersSnapshots = mutableListOf<DataSnapshot>()
                snapshot.child("players").children.forEach { playersSnapshots.add(it) }
                
                val timestamp = currentPlatformMillis()
                
                if (currentAdmin == sanitizedName) {
                    val nextAdmin = playersSnapshots.firstOrNull { it.key != sanitizedName }?.key
                    if (nextAdmin != null) {
                        roomRef.updateChildren(mapOf(
                            "admin" to nextAdmin,
                            "players/$sanitizedName" to null,
                            "messages/exit_$timestamp" to "$sanitizedName je izašao, novi admin je $nextAdmin"
                        ))
                    } else {
                        roomRef.removeValue()
                    }
                } else {
                    roomRef.child("players").child(sanitizedName).removeValue()
                    roomRef.child("messages").child("exit_$timestamp").setValue("$sanitizedName je izašao")
                }
                
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }

    override fun listenToRoom(roomCode: String): Flow<Room?> {
        return roomsRef.child(roomCode).valueEvents.map { it.getValueSafe<Room?>() }
    }

    override fun toggleReady(roomCode: String, username: String, isReady: Boolean) {
        firebaseScope.launch {
            try {
                val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
                roomsRef.child(roomCode).child("players").child(sanitizedName).child("isReady").setValue(isReady)
            } catch (e: Exception) {}
        }
    }

    override fun startGame(roomCode: String, playersList: List<String>) {
        firebaseScope.launch {
            try {
                val (mainWord, imposterWord) = WordManager.getNextWords()
                val shuffled = playersList.shuffled()
                val imposterId = shuffled[0]
                val mrWhiteId = if (shuffled.size >= 3 && (1..100).random() <= 20) shuffled[1] else ""
                
                val updates = mapOf(
                    "mainWord" to mainWord,
                    "imposterWord" to imposterWord,
                    "imposterId" to imposterId,
                    "mrWhiteId" to mrWhiteId,
                    "status" to "started",
                    "chatMessages" to null,
                    "isDiscussionActive" to false,
                    "discussionEndTime" to 0L,
                    "resultMessage" to ""
                )
                roomsRef.child(roomCode).updateChildren(updates)
            } catch (e: Exception) {}
        }
    }

    override fun sendMessage(roomCode: String, username: String, message: String) {
        firebaseScope.launch {
            try {
                val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }
                val chatMsg = ChatMessage(sanitizedName, message.trim(), currentPlatformMillis())
                roomsRef.child(roomCode).child("chatMessages").push().setValue(chatMsg)
            } catch (e: Exception) {}
        }
    }

    override fun startDiscussion(roomCode: String, seconds: Int) {
        firebaseScope.launch {
            try {
                if (seconds > 0) {
                    val endTime = currentPlatformMillis() + (seconds * 1000L)
                    roomsRef.child(roomCode).updateChildren(mapOf(
                        "isDiscussionActive" to true,
                        "discussionEndTime" to endTime
                    ))
                } else {
                    roomsRef.child(roomCode).updateChildren(mapOf(
                        "isDiscussionActive" to false,
                        "discussionEndTime" to 0L
                    ))
                }
            } catch (e: Exception) {}
        }
    }

    override fun endRound(roomCode: String, resultMessage: String) {
        firebaseScope.launch {
            try {
                roomsRef.child(roomCode).updateChildren(mapOf(
                    "status" to "finished",
                    "resultMessage" to resultMessage,
                    "isDiscussionActive" to false
                ))
            } catch (e: Exception) {}
        }
    }

    override fun resetToLobby(roomCode: String) {
        firebaseScope.launch {
            try {
                roomsRef.child(roomCode).updateChildren(mapOf(
                    "status" to "waiting",
                    "chatMessages" to null,
                    "isDiscussionActive" to false,
                    "discussionEndTime" to 0L,
                    "resultMessage" to ""
                ))
            } catch (e: Exception) {}
        }
    }
    
    override fun removePlayer(roomCode: String, playerName: String) {
        firebaseScope.launch {
            try {
                roomsRef.child(roomCode).child("players").child(playerName).removeValue()
            } catch (e: Exception) {}
        }
    }
}
