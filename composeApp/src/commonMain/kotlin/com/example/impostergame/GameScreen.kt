@file:OptIn(ExperimentalTime::class)

package com.example.impostergame

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.impostergame.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    roomCode: String, 
    username: String, 
    isAdmin: Boolean,
    onRepeat: () -> Unit,
    onNewGame: () -> Unit
) {
    if (roomCode.isBlank()) return

    val database = remember(roomCode) { 
        FirebaseManager.roomsRef.child(roomCode)
    }
    
    var word by remember { mutableStateOf("") }
    var isRevealed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var exitHoldProgress by remember { mutableFloatStateOf(0f) }
    var currentAdmin by remember { mutableStateOf("") }
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var chatInput by remember { mutableStateOf("") }
    var players by remember { mutableStateOf<Map<String, PlayerInfo>>(emptyMap()) }
    
    var gameStatus by remember { mutableStateOf("started") }
    var showVoteDialog by remember { mutableStateOf(false) }
    
    var isDiscussionActive by remember { mutableStateOf(false) }
    var discussionEndTime by remember { mutableLongStateOf(0L) }
    var timeLeft by remember { mutableIntStateOf(0) }
    
    val isUserAdmin = remember(currentAdmin, username) { currentAdmin == username }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var holdJob by remember { mutableStateOf<Job?>(null) }
    var exitHoldJob by remember { mutableStateOf<Job?>(null) }

    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) OffWhite else DeepCharcoal
    val containerColor = if (isDarkTheme) DarkInputGray else Color.White
    val accentColor = SageGreen
    
    val secondaryBtnBg = if (isDarkTheme) Color(0xFF3E3A33) else Color(0xFFFDF5E6)
    val progressColor = SageGreen.copy(alpha = 0.3f)

    LaunchedEffect(roomCode) {
        database.valueEvents.collectLatest { snapshot ->
            if (snapshot.value == null) return@collectLatest
            currentAdmin = snapshot.child("admin").getValueSafe<String?>() ?: ""
            gameStatus = snapshot.child("status").getValueSafe<String?>() ?: "started"
            val imposterId = snapshot.child("imposterId").getValueSafe<String?>() ?: ""
            val mrWhiteId = snapshot.child("mrWhiteId").getValueSafe<String?>() ?: ""
            
            // Ako admin promijeni status u "waiting", svi se vraćaju u lobby
            if (gameStatus == "waiting") {
                onRepeat()
            }
            
            word = when (username) {
                mrWhiteId -> "TI SI MR. WHITE"
                imposterId -> snapshot.child("imposterWord").getValueSafe<String?>() ?: ""
                else -> snapshot.child("mainWord").getValueSafe<String?>() ?: ""
            }

            val chatList = mutableListOf<ChatMessage>()
            snapshot.child("chatMessages").children.forEach {
                it.getValueSafe<ChatMessage?>()?.let { msg -> chatList.add(msg) }
            }
            chatMessages = chatList

            val playersMap = mutableMapOf<String, PlayerInfo>()
            snapshot.child("players").children.forEach { playerSnapshot ->
                val pInfo = playerSnapshot.getValueSafe<PlayerInfo?>()
                if (pInfo != null) {
                    playerSnapshot.key?.let { playersMap[it] = pInfo }
                }
            }
            players = playersMap
            isDiscussionActive = snapshot.child("isDiscussionActive").getValueSafe<Boolean?>() ?: false
            discussionEndTime = snapshot.child("discussionEndTime").getValueSafe<Long?>() ?: 0L
        }
    }

    LaunchedEffect(players) {
        // Ako je igra u tijeku, ali korisnika više nema u listi igrača (izbačen je)
        if (gameStatus == "started" && players.isNotEmpty() && !players.containsKey(username)) {
            onRepeat() // Vraća u lobby
        }
    }

    LaunchedEffect(isDiscussionActive, discussionEndTime) {
        if (isDiscussionActive && discussionEndTime > 0L) {
            while (true) {
                val now = Clock.System.now().toEpochMilliseconds()
                val diff = ((discussionEndTime - now) / 1000).toInt()
                if (diff <= 0) {
                    timeLeft = 0
                    if (isUserAdmin) scope.launch { database.child("isDiscussionActive").setValue(false) }
                    break
                }
                timeLeft = diff
                delay(1000)
            }
        } else { timeLeft = 0 }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) listState.animateScrollToItem(chatMessages.size - 1)
    }

    if (showVoteDialog) {
        Dialog(onDismissRequest = { showVoteDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF2D2D2D) else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("TKO JE ULJEZ?", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MutedRose)
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val playerList = players.keys.toList()
                        itemsIndexed(playerList) { _, pId ->
                            val playerName = players[pId]?.name ?: pId
                            Surface(
                                onClick = {
                                    scope.launch {
                                        // Ukloni igrača iz sobe
                                        database.child("players").child(pId).removeValue()
                                        
                                        // Dodaj poruku u chat da je izbačen
                                        database.child("chatMessages").push().setValue(
                                            ChatMessage("Sustav", "Korisnik $playerName je izbačen.", Clock.System.now().toEpochMilliseconds())
                                        )
                                        
                                        // Prekini raspravu
                                        database.child("isDiscussionActive").setValue(false)
                                    }
                                    showVoteDialog = false
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
                                border = if (isDarkTheme) null else BorderStroke(1.dp, Color.Black.copy(0.05f))
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(32.dp).background(accentColor.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                        Text(playerName.firstOrNull()?.toString()?.uppercase() ?: "?", fontWeight = FontWeight.Bold, color = accentColor, fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(playerName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = textColor)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { showVoteDialog = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("ODUSTANI", color = textColor.copy(0.4f), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isDiscussionActive) {
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(MutedRose.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, null, tint = MutedRose, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        val minutes = timeLeft / 60
                        val seconds = timeLeft % 60
                        Text("${if(minutes < 10) "0" else ""}$minutes:${if(seconds < 10) "0" else ""}$seconds", color = MutedRose, fontWeight = FontWeight.Bold)
                    }
                }
                Box(modifier = Modifier.fillMaxSize().clickable { isRevealed = !isRevealed }, contentAlignment = Alignment.Center) {
                    if (isRevealed) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (username == isAdmin.toString() || username.isNotEmpty()) "Tvoja riječ:" else "Status:", color = textColor.copy(alpha = 0.6f), fontSize = 14.sp)
                            Text(word, color = if (word == "TI SI MR. WHITE") MutedRose else textColor, fontSize = if (word == "TI SI MR. WHITE") 32.sp else 42.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, null, tint = accentColor, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("DODIRNI ZA OTKRIVANJE", color = textColor.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.5f))) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isDiscussionActive) "RASPRAVA" else "CHAT", fontWeight = FontWeight.Bold, color = if (isDiscussionActive) MutedRose else accentColor)
                    if (isUserAdmin && !isDiscussionActive) {
                        var showTimerMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showTimerMenu = true }) { Icon(Icons.Default.Timer, null, tint = accentColor) }
                            DropdownMenu(expanded = showTimerMenu, onDismissRequest = { showTimerMenu = false }) {
                                listOf(30, 45, 60).forEach { sec ->
                                    DropdownMenuItem(text = { Text("$sec sekundi") }, onClick = {
                                        scope.launch {
                                            database.child("isDiscussionActive").setValue(true)
                                            database.child("discussionEndTime").setValue(Clock.System.now().toEpochMilliseconds() + (sec * 1000L))
                                        }
                                        showTimerMenu = false
                                    })
                                }
                            }
                        }
                    }
                }
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(vertical = 8.dp)) {
                    itemsIndexed(chatMessages) { index, msg ->
                        val isMe = msg.sender == username
                        val isNewGroup = index == 0 || chatMessages[index - 1].sender != msg.sender
                        val verticalPadding = if (isNewGroup) 6.dp else 2.dp

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = verticalPadding), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                            if (isNewGroup) {
                                Text(
                                    text = msg.sender,
                                    fontSize = 11.sp, 
                                    color = textColor.copy(alpha = 0.5f), 
                                    modifier = Modifier.padding(start = if(isMe) 0.dp else 4.dp, end = if(isMe) 4.dp else 0.dp, bottom = 2.dp)
                                )
                            }
                            Surface(
                                color = if (isMe) accentColor.copy(alpha = if(isDarkTheme) 0.25f else 0.85f) 
                                        else (if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)), 
                                shape = RoundedCornerShape(
                                    topStart = if (isNewGroup || isMe) 16.dp else 4.dp, 
                                    topEnd = if (isNewGroup || !isMe) 16.dp else 4.dp, 
                                    bottomStart = if (isMe) 16.dp else 4.dp, 
                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                ), 
                                contentColor = if (isMe && !isDarkTheme) Color.White else textColor
                            ) {
                                Text(msg.message, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 15.sp)
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextField(value = chatInput, onValueChange = { chatInput = it }, modifier = Modifier.weight(1f), placeholder = { Text("Napiši nešto...") }, colors = TextFieldDefaults.colors(focusedContainerColor = textColor.copy(alpha = 0.05f), unfocusedContainerColor = textColor.copy(alpha = 0.05f), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(24.dp))
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { 
                        if (chatInput.trim().isNotBlank()) { 
                            scope.launch {
                                try {
                                    database.child("chatMessages").push().setValue(ChatMessage(username, chatInput.trim(), Clock.System.now().toEpochMilliseconds()))
                                    chatInput = "" 
                                } catch (_: Exception) {}
                            }
                        } 
                    }, modifier = Modifier.background(accentColor, CircleShape).size(48.dp)) { Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isUserAdmin && !isDiscussionActive) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                holdJob = scope.launch {
                                    val start = Clock.System.now().toEpochMilliseconds()
                                    while (holdProgress < 2f) { 
                                        holdProgress = ((Clock.System.now().toEpochMilliseconds() - start) / 1000f).coerceAtMost(2f)
                                        delay(10) 
                                    }
                                    // Resetiranje cijele sobe na "waiting" status vraća sve igrače u Lobby
                                    database.updateChildren(mapOf("status" to "waiting", "chatMessages" to null, "isDiscussionActive" to false, "discussionEndTime" to 0L, "resultMessage" to ""))
                                    holdProgress = 0f
                                }
                                try { awaitRelease() } finally { holdJob?.cancel(); holdProgress = 0f }
                            })
                        },
                    color = secondaryBtnBg
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (holdProgress > 0f) Box(modifier = Modifier.fillMaxWidth(holdProgress / 2f).fillMaxHeight().background(progressColor).align(Alignment.CenterStart))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, null, tint = textColor.copy(0.6f), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            val remaining = (2f - holdProgress)
                            Text(if (holdProgress > 0f) "${(remaining * 10).toInt() / 10.0}s" else "PONOVI", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = textColor)
                        }
                    }
                }

                Button(
                    onClick = { showVoteDialog = true },
                    modifier = Modifier.weight(1.2f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MutedRose.copy(alpha = 0.9f))
                ) {
                    Icon(Icons.Default.Gavel, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("IZBACI ULJEZA", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        exitHoldJob = scope.launch {
                            val start = Clock.System.now().toEpochMilliseconds()
                            while (exitHoldProgress < 2f) { 
                                exitHoldProgress = ((Clock.System.now().toEpochMilliseconds() - start) / 1000f).coerceAtMost(2f)
                                delay(10) 
                            }
                            FirebaseManager.leaveRoomWithAdminTransfer(roomCode, username, onNewGame)
                            exitHoldProgress = 0f
                        }
                        try { awaitRelease() } finally { exitHoldJob?.cancel(); exitHoldProgress = 0f }
                    })
                },
            color = textColor.copy(alpha = 0.05f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (exitHoldProgress > 0f) Box(modifier = Modifier.fillMaxWidth(exitHoldProgress / 2f).fillMaxHeight().background(MutedRose.copy(0.2f)).align(Alignment.CenterStart))
                val remaining = (2f - exitHoldProgress)
                Text(
                    text = if (exitHoldProgress > 0f) "IZAĐI ZA ${(remaining * 10).toInt() / 10.0}s" else "IZAĐI IZ SOBE",
                    color = textColor.copy(alpha = 0.5f), 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp
                )
            }
        }
    }
}
