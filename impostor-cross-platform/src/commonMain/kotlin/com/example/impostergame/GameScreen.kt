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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.impostergame.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameScreen(
    roomCode: String, 
    username: String, 
    isAdmin: Boolean,
    onRepeat: () -> Unit,
    onNewGame: () -> Unit
) {
    if (roomCode.isBlank()) return

    val sanitizedName = remember(username) { username.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "Igrac" } }

    var word by remember { mutableStateOf("") }
    var isRevealed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }
    var exitHoldProgress by remember { mutableStateOf(0f) }
    var currentAdmin by remember { mutableStateOf("") }
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var chatInput by remember { mutableStateOf("") }
    var players by remember { mutableStateOf<Map<String, PlayerInfo>>(emptyMap()) }
    
    var gameStatus by remember { mutableStateOf("started") }
    var showVoteDialog by remember { mutableStateOf(false) }
    
    var isDiscussionActive by remember { mutableStateOf(false) }
    var discussionStartTime by remember { mutableLongStateOf(0L) }
    var discussionEndTime by remember { mutableLongStateOf(0L) }
    var localStartTime by remember { mutableLongStateOf(0L) }
    
    var timeLeft by remember { mutableIntStateOf(0) }
    
    val showDiscussion = isDiscussionActive && timeLeft > 0
    
    // Provjera tko ima ovlasti (originalni admin ili najstariji preostali igrač)
    val isUserAdmin = remember(currentAdmin, players, sanitizedName) {
        if (currentAdmin == sanitizedName) return@remember true
        if (!players.containsKey(currentAdmin)) {
            val oldestPlayer = players.values.sortedBy { it.joinedAt }.firstOrNull()
            return@remember oldestPlayer?.name == sanitizedName
        }
        false
    }

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

    // Detekcija tipkovnice preko paddinga
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    LaunchedEffect(roomCode) {
        FirebaseManager.listenToRoom(roomCode).collectLatest { room ->
            if (room == null) return@collectLatest
            
            val prevStatus = gameStatus
            currentAdmin = room.admin
            gameStatus = room.status
            val imposterId = room.imposterId
            val mrWhiteId = room.mrWhiteId
            
            if (gameStatus == "waiting") {
                onRepeat()
            }
            
            word = when (sanitizedName) {
                mrWhiteId -> "TI SI MR. WHITE"
                imposterId -> room.imposterWord
                else -> room.mainWord
            }

            // Sortiramo po timestampu kako bi poruke (uključujući sistemske) bile u točnom kronološkom redoslijedu
            chatMessages = room.chatMessages.values.sortedBy { it.timestamp }
            players = room.players
            
            // AUTOMATSKI RESET: Ako je igra u tijeku, a ostao je samo 1 igrač (ili je impostor izbačen)
            if (gameStatus == "started") {
                // Određujeme jesmo li MI ti koji trebaju poslati reset (kako ne bi svi odjednom slali)
                val currentEffAdmin = if (room.players.containsKey(room.admin)) room.admin 
                                     else room.players.values.sortedBy { it.joinedAt }.firstOrNull()?.name
                
                if (sanitizedName == currentEffAdmin) {
                    val impostorKicked = imposterId.isNotEmpty() && !room.players.containsKey(imposterId)
                    val onlyOneLeft = room.players.size <= 1
                    
                    if (impostorKicked || onlyOneLeft) {
                        FirebaseManager.resetToLobby(roomCode)
                    }
                }
            }

            val newlyStarted = room.isDiscussionActive && !isDiscussionActive
            isDiscussionActive = room.isDiscussionActive
            discussionStartTime = room.discussionStartTime
            discussionEndTime = room.discussionEndTime

            if (newlyStarted) {
                localStartTime = currentPlatformMillis()
            } else if (!room.isDiscussionActive) {
                localStartTime = 0L
            }
        }
    }

    LaunchedEffect(players) {
        // Ako smo izbačeni iz liste igrača, a igra još traje, vratimo se u lobby
        if (gameStatus == "started" && players.isNotEmpty() && !players.containsKey(sanitizedName)) {
            onRepeat()
        }
    }

    LaunchedEffect(isDiscussionActive, discussionEndTime, localStartTime) {
        if (isDiscussionActive && discussionEndTime > 0L && discussionStartTime > 0L) {
            val duration = ((discussionEndTime - discussionStartTime) / 1000).toInt()
            
            while (true) {
                val diff = if (localStartTime > 0L) {
                    val elapsed = ((currentPlatformMillis() - localStartTime) / 1000).toInt()
                    duration - elapsed
                } else {
                    ((discussionEndTime - currentPlatformMillis()) / 1000).toInt()
                }

                if (diff <= 0) {
                    timeLeft = 0
                    if (isUserAdmin) {
                        FirebaseManager.startDiscussion(roomCode, 0)
                    }
                    break
                }
                timeLeft = diff
                delay(100)
            }
        } else { 
            timeLeft = 0 
        }
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
                                        FirebaseManager.removePlayer(roomCode, pId)
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

    val wordRevealCard = @Composable { modifier: Modifier, isWideScreen: Boolean ->
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (showDiscussion) {
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
                            Text(if (isAdmin || isUserAdmin) "Tvoja riječ:" else "Status:", color = textColor.copy(alpha = 0.6f), fontSize = if (isWideScreen) 20.sp else 14.sp)
                            Text(word, color = if (word == "TI SI MR. WHITE") MutedRose else textColor, fontSize = if (word == "TI SI MR. WHITE") (if (isWideScreen) 48.sp else 32.sp) else (if (isWideScreen) 64.sp else 42.sp), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, null, tint = accentColor, modifier = Modifier.size(if (isWideScreen) 48.dp else 32.dp))
                            Spacer(Modifier.width(16.dp))
                            Text("DODIRNI ZA OTKRIVANJE", color = textColor.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = if (isWideScreen) 20.sp else 14.sp)
                        }
                    }
                }
            }
        }
    }

    val chatCard = @Composable { modifier: Modifier, isWideScreen: Boolean ->
        Card(modifier = modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.5f))) {
            Column(modifier = Modifier.padding(if (isWideScreen) 24.dp else 12.dp).fillMaxHeight()) {
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showDiscussion) "RASPRAVA" else "CHAT", 
                        fontWeight = FontWeight.Bold, 
                        color = if (showDiscussion) MutedRose else accentColor,
                        modifier = Modifier.defaultMinSize(minWidth = 100.dp),
                        fontSize = if (isWideScreen) 20.sp else 16.sp
                    )
                    
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.CenterEnd) {
                        if (isUserAdmin && !showDiscussion) {
                            var showTimerMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showTimerMenu = true }) { Icon(Icons.Default.Timer, null, tint = accentColor) }
                                DropdownMenu(expanded = showTimerMenu, onDismissRequest = { showTimerMenu = false }) {
                                    listOf(30, 45, 60).forEach { sec ->
                                        DropdownMenuItem(text = { Text("$sec sekundi") }, onClick = {
                                            FirebaseManager.startDiscussion(roomCode, sec)
                                            showTimerMenu = false
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(vertical = 8.dp)) {
                    itemsIndexed(chatMessages) { index, msg ->
                        val isMe = msg.sender == sanitizedName
                        val isNewGroup = index == 0 || chatMessages[index - 1].sender != msg.sender
                        val verticalPadding = if (isNewGroup) 6.dp else 2.dp

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = verticalPadding), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                            if (isNewGroup) {
                                Text(
                                    text = msg.sender,
                                    fontSize = if (isWideScreen) 13.sp else 11.sp, 
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
                                Text(msg.message, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = if (isWideScreen) 18.sp else 15.sp)
                            }
                        }
                    }
                }

                fun sendMessage() {
                    if (chatInput.trim().isNotBlank()) {
                        FirebaseManager.sendMessage(roomCode, sanitizedName, chatInput.trim())
                        chatInput = ""
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = chatInput, 
                        onValueChange = { chatInput = it }, 
                        modifier = Modifier.weight(1f).onKeyEvent { event ->
                            if ((event.key == Key.Enter || event.key == Key.NumPadEnter) && event.type == KeyEventType.KeyDown) {
                                sendMessage()
                                true
                            } else {
                                false
                            }
                        },
                        placeholder = { Text("Napiši nešto...", fontSize = if (isWideScreen) 18.sp else 16.sp) }, 
                        colors = TextFieldDefaults.colors(focusedContainerColor = textColor.copy(alpha = 0.05f), unfocusedContainerColor = textColor.copy(alpha = 0.05f), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), 
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(onSend = { sendMessage() })
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { sendMessage() }, modifier = Modifier.background(accentColor, CircleShape).size(if (isWideScreen) 56.dp else 48.dp)) { Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(if (isWideScreen) 24.dp else 20.dp)) }
                }
            }
        }
    }

    val actionButtons = @Composable { modifier: Modifier, isWideScreen: Boolean ->
        Column(modifier = modifier) {
            if (isUserAdmin) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).height(if (isWideScreen) 100.dp else 50.dp).clip(RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = {
                                    holdJob = scope.launch {
                                        val start = currentPlatformMillis()
                                        while (holdProgress < 1.4f) { 
                                            holdProgress = ((currentPlatformMillis() - start) / 1000f).coerceAtMost(1.4f)
                                            delay(10) 
                                        }
                                        FirebaseManager.resetToLobby(roomCode)
                                        holdProgress = 0f
                                    }
                                    try { awaitRelease() } finally { holdJob?.cancel(); holdProgress = 0f }
                                })
                            },
                        color = secondaryBtnBg
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (holdProgress > 0f) Box(modifier = Modifier.fillMaxWidth(holdProgress / 1.4f).fillMaxHeight().background(progressColor).align(Alignment.CenterStart))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, null, tint = textColor.copy(0.6f), modifier = Modifier.size(if (isWideScreen) 32.dp else 18.dp))
                                Spacer(Modifier.width(8.dp))
                                val remaining = (1.4f - holdProgress)
                                Text(if (holdProgress > 0f) "${(remaining * 10).toInt() / 10.0}s" else "PONOVI", fontWeight = FontWeight.ExtraBold, fontSize = if (isWideScreen) 20.sp else 14.sp, color = textColor)
                            }
                        }
                    }

                    Button(
                        onClick = { showVoteDialog = true },
                        modifier = Modifier.weight(1.2f).height(if (isWideScreen) 100.dp else 50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MutedRose.copy(alpha = 0.9f))
                    ) {
                        Icon(Icons.Default.Gavel, null, modifier = Modifier.size(if (isWideScreen) 32.dp else 18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("IZBACI ULJEZA", fontWeight = FontWeight.ExtraBold, fontSize = if (isWideScreen) 20.sp else 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth().height(if (isWideScreen) 100.dp else 50.dp).clip(RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            exitHoldJob = scope.launch {
                                val start = currentPlatformMillis()
                                while (exitHoldProgress < 1.4f) { 
                                    exitHoldProgress = ((currentPlatformMillis() - start) / 1000f).coerceAtMost(1.4f)
                                    delay(10) 
                                }
                                FirebaseManager.leaveRoomWithAdminTransfer(roomCode, sanitizedName, onNewGame)
                                exitHoldProgress = 0f
                            }
                            try { awaitRelease() } finally { exitHoldJob?.cancel(); exitHoldProgress = 0f }
                        })
                    },
                color = textColor.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (exitHoldProgress > 0f) Box(modifier = Modifier.fillMaxWidth(exitHoldProgress / 1.4f).fillMaxHeight().background(MutedRose.copy(0.2f)).align(Alignment.CenterStart))
                    val remaining = (1.4f - exitHoldProgress)
                    Text(
                        text = if (exitHoldProgress > 0f) "IZAĐI ZA ${(remaining * 10).toInt() / 10.0}s" else "IZAĐI IZ SOBE",
                        color = textColor.copy(alpha = 0.5f), 
                        fontWeight = FontWeight.Bold, 
                        fontSize = if (isWideScreen) 20.sp else 14.sp
                    )
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(paddingValues).imePadding(), contentAlignment = Alignment.Center) {
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
                        wordRevealCard(Modifier.widthIn(max = 600.dp).fillMaxWidth().height(400.dp), true)
                        Spacer(Modifier.height(64.dp))
                        actionButtons(Modifier.widthIn(max = 600.dp).fillMaxWidth(), true)
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        chatCard(Modifier.fillMaxSize(), true)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = 700.dp)
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    wordRevealCard(Modifier.fillMaxWidth().height(160.dp), false)
                    Spacer(modifier = Modifier.height(16.dp))
                    chatCard(Modifier.fillMaxWidth().weight(1f), false)
                    
                    if (!isKeyboardVisible) {
                        Spacer(modifier = Modifier.height(16.dp))
                        actionButtons(Modifier.fillMaxWidth(), false)
                    }
                }
            }
        }
    }
}
