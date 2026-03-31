package com.example.impostergame

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual object FirebaseManager {
    actual fun generateRoom(username: String, onComplete: (String) -> Unit) {
        // No-op for JS target as Firebase is not supported directly.
        // Will immediately return a dummy code or notify completion with null.
        onComplete("JSROOM")
    }

    actual suspend fun joinRoom(roomCode: String, username: String): Result<Unit> {
        // No-op for JS target.
        return Result.failure(UnsupportedOperationException("Firebase not supported on JS"))
    }

    actual fun leaveRoomWithAdminTransfer(roomCode: String, username: String, onComplete: () -> Unit) {
        // No-op for JS target.
        onComplete()
    }

    actual fun listenToRoom(roomCode: String): Flow<Room?> {
        // No-op for JS target.
        return emptyFlow()
    }

    actual fun toggleReady(roomCode: String, username: String, isReady: Boolean) {
        // No-op for JS target.
    }

    actual fun startGame(roomCode: String, playersList: List<String>) {
        // No-op for JS target.
    }

    actual fun sendMessage(roomCode: String, username: String, message: String) {
        // No-op for JS target.
    }

    actual fun startDiscussion(roomCode: String, seconds: Int) {
        // No-op for JS target.
    }

    actual fun endRound(roomCode: String, resultMessage: String) {
        // No-op for JS target.
    }

    actual fun resetToLobby(roomCode: String) {
        // No-op for JS target.
    }

    actual fun removePlayer(roomCode: String, playerName: String) {
        // No-op for JS target.
    }
}
