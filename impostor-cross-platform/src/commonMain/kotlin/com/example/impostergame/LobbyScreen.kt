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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impostergame.ui.components.QRCodeImage
import com.example.impostergame.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    val sanitizedName = remember(username) { username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Gost" } }

    // Detekcija tipkovnice preko paddinga
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    if (roomCode.isBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SageGreen)
        }
        return
    }

    var messages by remember { mutableStateOf(listOf<String>()) }
    var playerCount by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("waiting") }
    var currentAdmin by remember { mutableStateOf("") }
    var players by remember { mutableStateOf<Map<String, PlayerInfo>>(emptyMap()) }
    
    // U lobbyu je admin UVIJEK onaj tko je zapisan u bazi (stalan je)
    val isUserAdmin = sanitizedName == currentAdmin
    
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(roomCode) {
        FirebaseManager.listenToRoom(roomCode).collectLatest { room ->
            if (room == null) return@collectLatest
            
            status = room.status
            currentAdmin = room.admin
            players = room.players
            
            var isInRoom = false
            var count = 0
            
            room.players.forEach { (key, _) ->
                count++
                if (key == sanitizedName) isInRoom = true
            }
            playerCount = count
            
            if (status == "started" && isInRoom) {
                onGameStarted()
            }

            if (status == "waiting" && !isInRoom) {
                FirebaseManager.joinRoom(roomCode, sanitizedName)
            }

            // Sortiramo poruke tako da najnovija bude na vrhu popisa
            val sortedMsgs = room.messages.entries
                .sortedByDescending { it.key } 
                .map { it.value }
            
            messages = sortedMsgs
        }
    }

    val headerInfo = @Composable { modifier: Modifier, isWideScreen: Boolean ->
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
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
                        fontSize = if (isWideScreen) 50.sp else 32.sp, 
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        style = if (isWideScreen) TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = Offset(4f, 4f),
                                blurRadius = 8f
                            )
                        ) else LocalTextStyle.current
                    )
                    Text(
                        text = "(Klikni za kopiranje)",
                        fontSize = if (isWideScreen) 16.sp else 12.sp,
                        color = textColor.copy(alpha = 0.3f)
                    )
                }
                
                Spacer(modifier = Modifier.width(if (isWideScreen) 32.dp else 16.dp))
                
                Surface(
                    modifier = Modifier
                        .requiredSize(if (isWideScreen) 180.dp else 80.dp)
                        .aspectRatio(1f),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                        QRCodeImage(
                            content = "impostergame://join?code=$roomCode", 
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = if (isUserAdmin) "Ti si ADMIN" else "Admin je: $currentAdmin",
                color = if (isUserAdmin) Gold else textColor.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                fontSize = if (isWideScreen) 24.sp else 14.sp
            )
        }
    }

    val playersCard = @Composable { modifier: Modifier, isWideScreen: Boolean ->
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.9f))
        ) {
            Column(modifier = Modifier.padding(if (isWideScreen) 32.dp else 20.dp).fillMaxHeight()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Igrači", fontSize = if (isWideScreen) 28.sp else 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("$playerCount / 16", color = SageGreen, fontWeight = FontWeight.Bold, fontSize = if (isWideScreen) 20.sp else 16.sp)
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = textColor.copy(alpha = 0.1f))
                
                Text("Događaji:", fontSize = if (isWideScreen) 18.sp else 14.sp, fontWeight = FontWeight.Medium, color = textColor.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(messages) { msg ->
                        Surface(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = textColor.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = msg,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                fontSize = if (isWideScreen) 18.sp else 14.sp,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }

    val actionButtons = @Composable { modifier: Modifier, isWideScreen: Boolean ->
        Column(modifier = modifier) {
            if (isUserAdmin) {
                val isGameRunning = status == "started" || status == "finished"
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                if (!isGameRunning) {
                                    val room = FirebaseManager.listenToRoom(roomCode).first()
                                    if (room != null) {
                                        val playersList = room.players.keys.toList()
                                        FirebaseManager.startGame(roomCode, playersList)
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(if (isWideScreen) 100.dp else 60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGameRunning) Color.Gray.copy(alpha = 0.5f) else (if (playerCount >= 2) primaryBtnBg else Color.Gray),
                        contentColor = if (isGameRunning) textColor.copy(alpha = 0.5f) else primaryBtnText
                    ),
                    enabled = !isGameRunning && playerCount >= 2
                ) {
                    Text(
                        text = when {
                            isGameRunning -> "IGRA JE U TIJEKU..."
                            playerCount < 2 -> "MIN 2 IGRAČA"
                            else -> "POKRENI IGRU"
                        },
                        fontSize = if (isWideScreen) 24.sp else 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().height(if (isWideScreen) 100.dp else 60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(if (isWideScreen) 32.dp else 24.dp), strokeWidth = 3.dp, color = Gold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Čekamo admina...", color = textColor, fontWeight = FontWeight.Bold, fontSize = if (isWideScreen) 20.sp else 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    FirebaseManager.leaveRoomWithAdminTransfer(roomCode, username, onLeaveRoom)
                },
                modifier = Modifier.fillMaxWidth().height(if (isWideScreen) 80.dp else 60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.1f))
            ) {
                Text("IZAĐI IZ SOBE", color = textColor, fontWeight = FontWeight.Bold, fontSize = if (isWideScreen) 18.sp else 16.sp)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            val isWideScreen = maxWidth > 1100.dp

            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 48.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 48.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        headerInfo(Modifier.widthIn(max = 600.dp).fillMaxWidth(), true)
                        Spacer(modifier = Modifier.height(64.dp))
                        actionButtons(Modifier.widthIn(max = 600.dp).fillMaxWidth(), true)
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        playersCard(Modifier.fillMaxSize(), true)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize()
                        .padding(24.dp)
                        .statusBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    headerInfo(Modifier.fillMaxWidth(), false)
                    Spacer(modifier = Modifier.height(32.dp))
                    playersCard(Modifier.fillMaxWidth().weight(1f), false)
                    
                    if (!isKeyboardVisible) {
                        Spacer(modifier = Modifier.height(32.dp))
                        actionButtons(Modifier.fillMaxWidth(), false)
                    }
                }
            }
        }
    }
}
