package com.example.impostergame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impostergame.ui.components.QRCodeImage
import com.example.impostergame.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    roomCode: String, 
    username: String, 
    isAdmin: Boolean,
    onLeaveRoom: () -> Unit,
    onGameStarted: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) OffWhite else DeepCharcoal
    val containerColor = if (isDarkTheme) DarkInputGray else Color.White
    
    val primaryBtnBg = if (isDarkTheme) DarkEarthy else SoftCream
    val primaryBtnText = if (isDarkTheme) SoftCream else DeepCharcoal

    if (roomCode.isBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SageGreen)
        }
        return
    }

    // Sanitizirano ime koje se sigurno podudara sa bazom
    val sanitizedName = remember(username) { username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Gost" } }

    var messages by remember { mutableStateOf(listOf<String>()) }
    var playerCount by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("waiting") }
    var currentAdmin by remember { mutableStateOf("") }
    val isUserAdmin = currentAdmin == sanitizedName
    
    // U starom Compose-u ovo je de facto način za Clipboard tekst iako je deprecated za ClipEntry
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(roomCode) {
        FirebaseManager.listenToRoom(roomCode).collectLatest { room ->
            if (room == null) return@collectLatest
            
            status = room.status
            currentAdmin = room.admin
            
            var isInGame = false
            var count = 0
            
            room.players.forEach { (key, _) ->
                count++
                if (key == sanitizedName) isInGame = true
            }
            playerCount = count
            
            // Igrač ide u igru samo ako je igra "started" i on NIJE izbačen
            if (status == "started" && isInGame) {
                onGameStarted()
            }

            // Ako je igra završila ili je admin resetirao i vratili smo se u "waiting" status
            // a igrač nije u igračima (jer je npr. izbačen ranije), tada automatski vraća igrača u sobu
            if (status == "waiting" && !isInGame) {
                FirebaseManager.joinRoom(roomCode, sanitizedName)
            }

            val msgList = mutableListOf<String>()
            room.messages.values.forEach { msg ->
                msgList.add(msg)
            }
            messages = msgList.reversed()
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                clipboardManager.setText(AnnotatedString(roomCode))
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SOBA: $roomCode", 
                            fontSize = 32.sp, 
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                        Text(
                            text = "(Klikni za kopiranje)",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.3f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Surface(
                        modifier = Modifier.size(80.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(modifier = Modifier.padding(4.dp)) {
                            QRCodeImage(content = "impostergame://join?code=$roomCode", modifier = Modifier.fillMaxSize())
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isUserAdmin) "Ti si ADMIN" else "Admin je: $currentAdmin", 
                    color = if (isUserAdmin) Gold else textColor.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.9f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Igrači", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Text("$playerCount / 16", color = SageGreen, fontWeight = FontWeight.Bold)
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = textColor.copy(alpha = 0.1f))
                        
                        Text("Događaji:", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor.copy(alpha = 0.5f))
                        
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(messages) { msg ->
                                Surface(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = textColor.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = msg,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        fontSize = 14.sp,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(32.dp))

                    if (isUserAdmin) {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val room = FirebaseManager.listenToRoom(roomCode).first()
                                        if (room != null) {
                                            val playersList = room.players.keys.toList()
                                            FirebaseManager.startGame(roomCode, playersList)
                                        }
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (playerCount >= 2) primaryBtnBg else Color.Gray,
                                contentColor = primaryBtnText
                            ),
                            enabled = playerCount >= 2
                        ) {
                            Text(
                                text = if (playerCount < 2) "MIN 2 IGRAČA" else "POKRENI IGRU",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = Gold)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Čekamo admina...", color = textColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            FirebaseManager.leaveRoomWithAdminTransfer(roomCode, username, onLeaveRoom)
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.1f))
                    ) {
                        Text("IZAĐI IZ SOBE", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
