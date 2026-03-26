package com.example.impostergame

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.impostergame.ui.components.AnimatedBackground
import com.example.impostergame.ui.theme.ImposterGameTheme
import com.russhwolf.settings.Settings
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AppBackHandler(onBack: () -> Unit) {
    // Platform-specific implementation needed for back button
}

@Composable
@Preview
fun App() {
    val settings: Settings = Settings()
    
    // Učitaj spremljeno stanje
    val savedUsername = remember { settings.getString("username", "") }
    val savedScreenOrdinal = remember { settings.getInt("currentScreen", Screen.ENTER_NAME.ordinal) }
    val savedRoomCode = remember { settings.getString("roomCode", "") }
    val savedIsAdmin = remember { settings.getBoolean("isAdmin", false) }
    
    var username by remember { mutableStateOf(savedUsername) }
    var currentScreen by remember { mutableStateOf(Screen.entries[savedScreenOrdinal]) }
    var roomCode by remember { mutableStateOf(savedRoomCode) }
    var isAdmin by remember { mutableStateOf(savedIsAdmin) }

    // Funkcija za promjenu ekrana uz spremanje stanja
    fun navigateTo(screen: Screen) {
        currentScreen = screen
        settings.putInt("currentScreen", screen.ordinal)
        settings.putString("roomCode", roomCode)
        settings.putBoolean("isAdmin", isAdmin)
    }

    // Logika za animiranu pozadinu
    val infiniteTransition = rememberInfiniteTransition()
    val xOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Upravljanje natrag gestom
    CommonBackHandler {
        when (currentScreen) {
            Screen.JOIN -> navigateTo(Screen.HOME)
            Screen.LOBBY -> {
                FirebaseManager.leaveRoomWithAdminTransfer(roomCode, username) {
                    navigateTo(Screen.HOME)
                }
            }
            Screen.GAME -> {
                // Možda potvrda izlaska? Za sada samo u Home
                navigateTo(Screen.HOME)
            }
            else -> {} // Na HOME i ENTER_NAME nek sustav odradi svoje (izlaz)
        }
    }

    ImposterGameTheme {
        AnimatedBackground(xOffset = xOffset, yOffset = yOffset) {
            Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Transparent) {
                when (currentScreen) {
                    Screen.ENTER_NAME -> {
                        EnterNameScreen(onNameEntered = { name, rememberMe ->
                            username = name
                            if (rememberMe) {
                                settings.putString("username", name)
                            } else {
                                settings.remove("username")
                            }
                            navigateTo(Screen.HOME)
                        })
                    }
                    Screen.HOME -> {
                        HomeScreen(
                            username = username,
                            onCreateRoom = {
                                isAdmin = true
                                FirebaseManager.generateRoom(username) { code ->
                                    roomCode = code
                                    navigateTo(Screen.LOBBY)
                                }
                            },
                            onJoinRoom = {
                                navigateTo(Screen.JOIN)
                            }
                        )
                    }
                    Screen.JOIN -> {
                        JoinRoomScreen(
                            username = username,
                            onJoined = { code ->
                                roomCode = code
                                isAdmin = false
                                navigateTo(Screen.LOBBY)
                            },
                            onBack = {
                                navigateTo(Screen.HOME)
                            }
                        )
                    }
                    Screen.LOBBY -> {
                        LobbyScreen(
                            roomCode = roomCode,
                            username = username,
                            isAdmin = isAdmin,
                            onLeaveRoom = {
                                navigateTo(Screen.HOME)
                            },
                            onGameStarted = {
                                navigateTo(Screen.GAME)
                            }
                        )
                    }
                    Screen.GAME -> {
                        GameScreen(
                            roomCode = roomCode,
                            username = username,
                            isAdmin = isAdmin,
                            onRepeat = {
                                navigateTo(Screen.LOBBY)
                            },
                            onNewGame = {
                                navigateTo(Screen.HOME)
                            }
                        )
                    }
                }
            }
        }
    }
}
