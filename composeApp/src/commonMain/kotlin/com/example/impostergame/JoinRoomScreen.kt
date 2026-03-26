package com.example.impostergame

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impostergame.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun JoinRoomScreen(username: String, onJoined: (String) -> Unit, onBack: () -> Unit) {
    var inputCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) OffWhite else DeepCharcoal
    val inputContainerColor = if (isDarkTheme) DarkInputGray else Color.White
    
    val accentColor = MutedRose
    val scope = rememberCoroutineScope()

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

                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { 
                            if (it.length <= 6) inputCode = it.uppercase() 
                            errorMessage = ""
                        },
                        placeholder = { Text("ABC 123", color = textColor.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
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
                                scope.launch {
                                    try {
                                        val snapshot = FirebaseManager.roomsRef.child(inputCode).valueEvents.first()
                                        if (snapshot.value != null) {
                                            val players = snapshot.child("players")
                                            var count = 0
                                            players.children.forEach { _ -> count++ }
                                            
                                            if (count < 16) {
                                                val playerRef = FirebaseManager.roomsRef.child(inputCode).child("players").child(username)
                                                playerRef.setValue(mapOf("name" to username, "isReady" to false))
                                                val msgRef = FirebaseManager.roomsRef.child(inputCode).child("messages").child("join_${username}")
                                                msgRef.setValue("$username je ušao")
                                                onJoined(inputCode)
                                            } else {
                                                errorMessage = "Soba je puna"
                                            }
                                        } else {
                                            errorMessage = "Soba ne postoji"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Greška: ${e.message}"
                                    }
                                }
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
        }
    }
}
