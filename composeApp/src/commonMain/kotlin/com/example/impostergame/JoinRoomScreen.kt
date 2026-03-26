package com.example.impostergame

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impostergame.ui.components.CameraScanner
import com.example.impostergame.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun JoinRoomScreen(username: String, onJoined: (String) -> Unit, onBack: () -> Unit) {
    var inputCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) OffWhite else DeepCharcoal
    val inputContainerColor = if (isDarkTheme) DarkInputGray else Color.White
    
    val accentColor = MutedRose
    val scope = rememberCoroutineScope()

    // Funkcija za zajedničku logiku ulaska u sobu
    fun attemptJoin(code: String) {
        val upperCode = code.uppercase()
        scope.launch {
            try {
                val snapshot = FirebaseManager.roomsRef.child(upperCode).valueEvents.first()
                if (snapshot.value != null) {
                    val players = snapshot.child("players")
                    var count = 0
                    players.children.forEach { _ -> count++ }
                    
                    if (count < 16) {
                        val playerRef = FirebaseManager.roomsRef.child(upperCode).child("players").child(username)
                        playerRef.setValue(mapOf("name" to username, "isReady" to false))
                        val msgRef = FirebaseManager.roomsRef.child(upperCode).child("messages").child("join_${username}")
                        msgRef.setValue("$username je ušao")
                        onJoined(upperCode)
                    } else {
                        errorMessage = "Soba je puna"
                        showScanner = false // Ugasi skener ako je pun
                    }
                } else {
                    errorMessage = "Soba ne postoji"
                    showScanner = false // Ugasi skener ako ne postoji
                }
            } catch (e: Exception) {
                errorMessage = "Greška: ${e.message}"
                showScanner = false
            }
        }
    }

    if (showScanner) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            CameraScanner(
                modifier = Modifier.fillMaxSize(),
                onResult = { result ->
                    val code = result.split("code=").lastOrNull()?.take(6)
                    if (code != null && code.length == 6) {
                        attemptJoin(code)
                    }
                }
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(24.dp)
            ) {
                IconButton(
                    onClick = { showScanner = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                
                Text(
                    text = "Usmjeri kameru prema QR kodu",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
    ) {
        IconButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pridruži se",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = inputContainerColor.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Unesi kod sobe",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = inputCode,
                            onValueChange = { 
                                if (it.length <= 6) inputCode = it.uppercase() 
                                errorMessage = ""
                            },
                            placeholder = { Text("ABC 123", color = textColor.copy(alpha = 0.4f)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            isError = errorMessage.isNotEmpty(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )
                    }
                    
                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage, 
                            color = MaterialTheme.colorScheme.error, 
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            if (inputCode.length == 6) {
                                attemptJoin(inputCode)
                            } else {
                                errorMessage = "Kod mora imati 6 znakova"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (inputCode.length == 6) accentColor else accentColor.copy(alpha = 0.5f),
                            contentColor = Color.White
                        )
                    ) {
                        Text("PRIDRUŽI SE", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showScanner = true },
                color = inputContainerColor.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Skeniraj QR kod",
                        modifier = Modifier.size(32.dp),
                        tint = textColor
                    )
                }
            }
        }
    }
}
