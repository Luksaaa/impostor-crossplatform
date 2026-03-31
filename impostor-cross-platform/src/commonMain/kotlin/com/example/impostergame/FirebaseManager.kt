package com.example.impostergame

import kotlinx.coroutines.flow.Flow

// Expect deklaracija za FirebaseManager
expect object FirebaseManager {
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

// activeFirebaseManager je maknut jer sada koristimo expect/actual
// GitLiveFirebaseManager je premješten u platform-specifične implementacije (androidMain, jvmMain)
