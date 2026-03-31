package com.example.impostergame

import kotlinx.coroutines.flow.Flow

actual object FirebaseManager : IFirebaseManager {
    private val delegate: IFirebaseManager get() = activeFirebaseManager ?: throw IllegalStateException("FirebaseManager not initialized on JVM")

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
