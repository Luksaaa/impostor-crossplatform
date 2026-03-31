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

actual object FirebaseManager : IFirebaseManager {
    private val database: FirebaseDatabase get() = Firebase.database
    private val roomsRef: DatabaseReference get() = database.reference("rooms")
    
    private val firebaseScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun generateRoom(username: String, onComplete: (String) -> Unit) {
        firebaseScope.launch {
            try {
                val code = generateRandomCode()
                val sanitizedName = username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Igrac" }
                val now = currentPlatformMillis()
                
                val snapshot = roomsRef.child(code).valueEvents.first()
                
                if (snapshot.value != null) {
                    generateRoom(username, onComplete)
                } else {
                    val roomData = mapOf(
                        "admin" to sanitizedName,
                        "originalAdmin" to sanitizedName,
                        "status" to "waiting",
                        "players" to mapOf(sanitizedName to mapOf("name" to sanitizedName, "isReady" to false, "joinedAt" to now)),
                        "messages" to mapOf("init" to "$sanitizedName je napravio sobu")
                    )
                    roomsRef.child(code).setValue(roomData)
                    
                    withContext(Dispatchers.Main) {
                        onComplete(code)
                    }
                }
            } catch (e: Exception) {}
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
            
            val timestamp = currentPlatformMillis()
            roomRef.child("players").child(sanitizedName).setValue(mapOf("name" to sanitizedName, "isReady" to false, "joinedAt" to timestamp))
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
                roomRef.child("players").child(sanitizedName).removeValue()
                withContext(Dispatchers.Main) { onComplete() }
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
                    "discussionStartTime" to 0L,
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
                val timestamp = currentPlatformMillis()
                val chatMsg = ChatMessage(sanitizedName, message.trim(), timestamp)
                roomsRef.child(roomCode).child("chatMessages").push().setValue(chatMsg)
            } catch (e: Exception) {}
        }
    }

    override fun startDiscussion(roomCode: String, seconds: Int) {
        firebaseScope.launch {
            try {
                val updates = if (seconds > 0) {
                    val now = currentPlatformMillis()
                    val endTime = now + (seconds * 1000L)
                    mapOf(
                        "isDiscussionActive" to true,
                        "discussionStartTime" to now,
                        "discussionEndTime" to endTime
                    )
                } else {
                    mapOf(
                        "isDiscussionActive" to false,
                        "discussionStartTime" to 0L,
                        "discussionEndTime" to 0L
                    )
                }
                roomsRef.child(roomCode).updateChildren(updates)
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
                val roomRef = roomsRef.child(roomCode)
                val snapshot = roomRef.valueEvents.first()
                if (snapshot.value == null) return@launch
                
                val room = snapshot.getValueSafe<Room>()
                val updates = mutableMapOf<String, Any?>(
                    "status" to "waiting",
                    "chatMessages" to null,
                    "isDiscussionActive" to false,
                    "discussionStartTime" to 0L,
                    "discussionEndTime" to 0L,
                    "resultMessage" to ""
                )
                
                if (room.originalAdmin.isNotEmpty()) {
                    updates["admin"] = room.originalAdmin
                    updates["players/${room.originalAdmin}/name"] = room.originalAdmin
                    updates["players/${room.originalAdmin}/isReady"] = false
                    val existingJoinedAt = room.players[room.originalAdmin]?.joinedAt ?: currentPlatformMillis()
                    updates["players/${room.originalAdmin}/joinedAt"] = existingJoinedAt
                }
                
                roomRef.updateChildren(updates)
            } catch (e: Exception) {}
        }
    }
    
    override fun removePlayer(roomCode: String, playerName: String) {
        firebaseScope.launch {
            try {
                val roomRef = roomsRef.child(roomCode)
                val snapshot = roomRef.valueEvents.first()
                if (snapshot.value == null) return@launch

                val room = snapshot.getValueSafe<Room>()
                val timestamp = currentPlatformMillis()
                
                val updates = mutableMapOf<String, Any?>()
                updates["players/$playerName"] = null

                if (room.admin == playerName) {
                    val nextActiveAdmin = room.players.values
                        .filter { it.name != playerName }
                        .sortedBy { it.joinedAt }
                        .firstOrNull()?.name
                        
                    updates["admin"] = nextActiveAdmin
                    val exitMsg = "$playerName je izbačen, privremeni admin je $nextActiveAdmin"
                    updates["messages/exit_$timestamp"] = exitMsg
                    val sysMsgKey = roomRef.child("chatMessages").push().key
                    updates["chatMessages/$sysMsgKey"] = ChatMessage("Sustav", exitMsg, timestamp)
                } else {
                    val exitMsg = "$playerName je izbačen"
                    updates["messages/exit_$timestamp"] = exitMsg
                    val sysMsgKey = roomRef.child("chatMessages").push().key
                    updates["chatMessages/$sysMsgKey"] = ChatMessage("Sustav", exitMsg, timestamp)
                }
                
                roomRef.updateChildren(updates)
            } catch (e: Exception) {}
        }
    }
}
