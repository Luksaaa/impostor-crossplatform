package com.example.impostergame

import kotlinx.coroutines.flow.Flow

interface IFirebaseManager {
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

// Globalni manager koji se koristi u aplikaciji
expect object FirebaseManager : IFirebaseManager

// Omogućuje dinamičku zamjenu (važno za Desktop/REST verziju)
var activeFirebaseManager: IFirebaseManager? = null
