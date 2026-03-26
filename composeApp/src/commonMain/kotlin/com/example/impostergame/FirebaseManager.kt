package com.example.impostergame

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

object FirebaseManager {
    private val database: FirebaseDatabase get() = Firebase.database
    val roomsRef: DatabaseReference get() = database.reference("rooms")

    fun generateRoom(username: String, onComplete: (String) -> Unit) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            val code = generateRandomCode()
            try {
                // Using valueEvents.first() as a more reliable way to get a snapshot if get() is failing to resolve
                val snapshot = roomsRef.child(code).valueEvents.first()
                if (snapshot.value != null) {
                    generateRoom(username, onComplete)
                } else {
                    val roomData = mapOf(
                        "admin" to username,
                        "status" to "waiting",
                        "players" to mapOf(username to mapOf("name" to username, "isReady" to false)),
                        "messages" to mapOf("init" to "$username je napravio sobu")
                    )
                    roomsRef.child(code).setValue(roomData)
                    onComplete(code)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun generateRandomCode(): String {
        val letters = ('A'..'Z').toList()
        val numbers = ('0'..'9').toList()
        return (1..3).map { letters.random() }.joinToString("") + 
               (1..3).map { numbers.random() }.joinToString("")
    }

    suspend fun joinRoom(roomCode: String, username: String): Result<Unit> {
        return try {
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
            
            roomRef.child("players").child(username).setValue(mapOf("name" to username, "isReady" to false))
            val timestamp = Clock.System.now().toEpochMilliseconds()
            roomRef.child("messages").child("join_$timestamp").setValue("$username je ušao")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun leaveRoomWithAdminTransfer(roomCode: String, username: String, onComplete: () -> Unit) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                val roomRef = roomsRef.child(roomCode)
                val snapshot = roomRef.valueEvents.first()
                if (snapshot.value == null) {
                    onComplete()
                    return@launch
                }
                
                val currentAdmin = snapshot.child("admin").getValueSafe<String?>()
                val playersSnapshots = mutableListOf<DataSnapshot>()
                snapshot.child("players").children.forEach { playersSnapshots.add(it) }
                
                val timestamp = Clock.System.now().toEpochMilliseconds()
                
                if (currentAdmin == username) {
                    val nextAdmin = playersSnapshots.firstOrNull { it.key != username }?.key
                    if (nextAdmin != null) {
                        roomRef.updateChildren(mapOf(
                            "admin" to nextAdmin,
                            "players/$username" to null,
                            "messages/exit_$timestamp" to "$username je izašao, novi admin je $nextAdmin"
                        ))
                    } else {
                        roomRef.removeValue()
                    }
                } else {
                    roomRef.child("players").child(username).removeValue()
                    roomRef.child("messages").child("exit_$timestamp").setValue("$username je izašao")
                }
                onComplete()
            } catch (e: Exception) {
                onComplete()
            }
        }
    }

    fun listenToRoom(roomCode: String): Flow<Room?> {
        return roomsRef.child(roomCode).valueEvents.map { it.getValueSafe<Room?>() }
    }

    fun toggleReady(roomCode: String, username: String, isReady: Boolean) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                roomsRef.child(roomCode).child("players").child(username).child("isReady").setValue(isReady)
            } catch (e: Exception) {}
        }
    }

    fun startGame(roomCode: String, playersList: List<String>) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
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

    fun sendMessage(roomCode: String, username: String, message: String) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                val chatMsg = ChatMessage(username, message.trim(), Clock.System.now().toEpochMilliseconds())
                roomsRef.child(roomCode).child("chatMessages").push().setValue(chatMsg)
            } catch (e: Exception) {}
        }
    }

    fun startDiscussion(roomCode: String, seconds: Int) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                val endTime = Clock.System.now().toEpochMilliseconds() + (seconds * 1000L)
                roomsRef.child(roomCode).updateChildren(mapOf(
                    "isDiscussionActive" to true,
                    "discussionEndTime" to endTime
                ))
            } catch (e: Exception) {}
        }
    }

    fun endRound(roomCode: String, resultMessage: String) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                roomsRef.child(roomCode).updateChildren(mapOf(
                    "status" to "finished",
                    "resultMessage" to resultMessage,
                    "isDiscussionActive" to false
                ))
            } catch (e: Exception) {}
        }
    }

    fun resetToLobby(roomCode: String) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
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
}
