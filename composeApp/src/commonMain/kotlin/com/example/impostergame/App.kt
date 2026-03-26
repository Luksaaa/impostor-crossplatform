package com.example.impostergame

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.impostergame.ui.theme.ImposterGameTheme
import com.russhwolf.settings.Settings
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val settings: Settings = Settings()
    
    // Provjeri je li korisnik već upisao ime ranije
    val savedUsername = remember { settings.getString("username", "") }
    
    var username by remember { mutableStateOf(savedUsername) }
    var currentScreen by remember { mutableStateOf(if (username.isNotBlank()) Screen.HOME else Screen.ENTER_NAME) }
    var roomCode by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }

    ImposterGameTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                Screen.ENTER_NAME -> {
                    EnterNameScreen(onNameEntered = { name, rememberMe ->
                        username = name
                        if (rememberMe) {
                            settings.putString("username", name)
                        } else {
                            settings.remove("username")
                        }
                        currentScreen = Screen.HOME
                    })
                }
                Screen.HOME -> {
                    HomeScreen(
                        username = username,
                        onCreateRoom = {
                            isAdmin = true
                            FirebaseManager.generateRoom(username) { code ->
                                roomCode = code
                                currentScreen = Screen.LOBBY
                            }
                        },
                        onJoinRoom = {
                            currentScreen = Screen.JOIN
                        }
                    )
                }
                Screen.JOIN -> {
                    JoinRoomScreen(
                        username = username,
                        onJoined = { code ->
                            roomCode = code
                            isAdmin = false
                            currentScreen = Screen.LOBBY
                        },
                        onBack = {
                            currentScreen = Screen.HOME
                        }
                    )
                }
                Screen.LOBBY -> {
                    LobbyScreen(
                        roomCode = roomCode,
                        username = username,
                        isAdmin = isAdmin,
                        onLeaveRoom = {
                            currentScreen = Screen.HOME
                        },
                        onGameStarted = {
                            currentScreen = Screen.GAME
                        }
                    )
                }
                Screen.GAME -> {
                    GameScreen(
                        roomCode = roomCode,
                        username = username,
                        isAdmin = isAdmin,
                        onRepeat = {
                            // Reset logic if needed
                        },
                        onNewGame = {
                            currentScreen = Screen.HOME
                        }
                    )
                }
            }
        }
    }
}
