package com.example.impostergame

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.impostergame.ui.components.AnimatedBackground
import com.example.impostergame.ui.theme.ImposterGameTheme
import com.russhwolf.settings.Settings


/**
 * Minimalna implementacija Settings za Preview okruženje
 */
private class MockSettings : Settings {
    private val data = mutableMapOf<String, Any?>()
    override val keys: Set<String> get() = data.keys
    override val size: Int get() = data.size
    override fun clear() = data.clear()
    override fun remove(key: String) { data.remove(key) }
    override fun hasKey(key: String): Boolean = data.containsKey(key)
    override fun putInt(key: String, value: Int) { data[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int = data[key] as? Int ?: defaultValue
    override fun getIntOrNull(key: String): Int? = data[key] as? Int
    override fun putLong(key: String, value: Long) { data[key] = value }
    override fun getLong(key: String, defaultValue: Long): Long = data[key] as? Long ?: defaultValue
    override fun getLongOrNull(key: String): Long? = data[key] as? Long
    override fun putString(key: String, value: String) { data[key] = value }
    override fun getString(key: String, defaultValue: String): String = data[key] as? String ?: defaultValue
    override fun getStringOrNull(key: String): String? = data[key] as? String
    override fun putFloat(key: String, value: Float) { data[key] = value }
    override fun getFloat(key: String, defaultValue: Float): Float = data[key] as? Float ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = data[key] as? Float
    override fun putDouble(key: String, value: Double) { data[key] = value }
    override fun getDouble(key: String, defaultValue: Double): Double = data[key] as? Double ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = data[key] as? Double
    override fun putBoolean(key: String, value: Boolean) { data[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = data[key] as? Boolean ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = data[key] as? Boolean
}

@Composable

fun App() {
    // Koristimo remember kako bismo izbjegli ponovno kreiranje Settings objekta na svakoj rekompoziciji.
    // Također koristimo try-catch kako bismo spriječili pucanje u Preview modu.
    val settings: Settings = remember {
        try {
            Settings()
        } catch (_: Throwable) {
            MockSettings()
        }
    }
    
    // Učitaj spremljeno stanje
    val savedUsername = remember { settings.getString("username", "") }
    val savedScreenOrdinal = remember { settings.getInt("currentScreen", Screen.ENTER_NAME.ordinal) }
    val savedRoomCode = remember { settings.getString("roomCode", "") }
    val savedIsAdmin = remember { settings.getBoolean("isAdmin", false) }
    
    var username by remember { mutableStateOf(savedUsername) }
    var currentScreen by remember { mutableStateOf(Screen.entries[savedScreenOrdinal.coerceIn(0, Screen.entries.size - 1)]) }
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
    if (currentScreen == Screen.JOIN) {
        CommonBackHandler {
            navigateTo(Screen.HOME)
        }
    } else if (currentScreen == Screen.LOBBY || currentScreen == Screen.GAME) {
        CommonBackHandler {
            // Blokira 'back' akciju
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
