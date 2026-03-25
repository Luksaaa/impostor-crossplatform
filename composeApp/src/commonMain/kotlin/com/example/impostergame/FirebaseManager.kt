package com.example.impostergame

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

object FirebaseManager {
    private val database by lazy { Firebase.database }
    val roomsRef by lazy { database.reference("rooms") }

    fun generateRoom(username: String, onComplete: (String) -> Unit) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            val code = generateRandomCode()
            try {
                val snapshot = roomsRef.child(code).get()
                if (snapshot.exists) {
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

    fun leaveRoomWithAdminTransfer(roomCode: String, username: String, onComplete: () -> Unit) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                val roomRef = roomsRef.child(roomCode)
                val snapshot = roomRef.get()
                if (!snapshot.exists) {
                    onComplete()
                    return@launch
                }
                
                val currentAdmin = snapshot.child("admin").getValueSafe<String?>()
                val players = snapshot.child("players").children.toList()
                
                val timestamp = Clock.System.now().toEpochMilliseconds()
                
                if (currentAdmin == username) {
                    val nextAdmin = players.firstOrNull { it.key != username }?.key
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
}
