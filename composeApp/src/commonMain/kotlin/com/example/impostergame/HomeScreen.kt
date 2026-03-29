package com.example.impostergame

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impostergame.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(username: String, onCreateRoom: () -> Unit, onJoinRoom: () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) OffWhite else DeepCharcoal
    
    var showRules by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 800.dp
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = if (isWideScreen) 800.dp else 600.dp)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(if (isWideScreen) 100.dp else 60.dp))

                Text(
                    text = "Bok, $username!",
                    fontSize = if (isWideScreen) 80.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                
                Text(
                    text = "Spreman za novu rundu?",
                    fontSize = if (isWideScreen) 32.sp else 16.sp,
                    color = textColor.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(if (isWideScreen) 100.dp else 60.dp))

                AestheticButton(
                    text = "Napravi sobu",
                    subText = "Postani admin i započni igru",
                    icon = Icons.Default.Add,
                    onClick = onCreateRoom,
                    color = SageGreen,
                    isWideScreen = isWideScreen
                )

                Spacer(modifier = Modifier.height(if (isWideScreen) 40.dp else 24.dp))

                AestheticButton(
                    text = "Pridruži se",
                    subText = "Uđi u postojeću sobu",
                    icon = Icons.Default.Group,
                    onClick = onJoinRoom,
                    color = MutedRose,
                    isWideScreen = isWideScreen
                )
                
                Spacer(modifier = Modifier.height(60.dp))
            }

            // Info gumb u kutu (prikazuje se samo ako panel nije otvoren)
            if (!showRules || !isWideScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(24.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Surface(
                        onClick = { showRules = true },
                        color = Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(if (isWideScreen) 80.dp else 48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Pravila",
                                tint = textColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(if (isWideScreen) 56.dp else 32.dp)
                            )
                        }
                    }
                }
            }
        }

        // Responzivni prikaz pravila (Side Panel za Desktop/Web/Mac)
        if (isWideScreen) {
            // Prozirna pozadina koja zatvara panel na klik izvan njega
            if (showRules) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showRules = false }
                )
            }

            AnimatedVisibility(
                visible = showRules,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(520.dp),
                    color = (if (isDarkTheme) Color(0xFF1E1E1E) else Color.White).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 24.dp
                ) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 32.dp, vertical = 40.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Kako igrati?",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textColor
                                    )
                                    IconButton(onClick = { showRules = false }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Zatvori",
                                            tint = textColor,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            }
                            // Koristimo boju teksta za pravila unutar prozirnog panela
                            RulesContent(textColor, isSidePanel = true)
                        }
                    }
                }
            }
        } else {
            // ModalBottomSheet za mobitele
            if (showRules) {
                ModalBottomSheet(
                    onDismissRequest = { showRules = false },
                    containerColor = if (isDarkTheme) DarkInputGray else OffWhite,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = textColor.copy(alpha = 0.2f)) },
                    modifier = Modifier.statusBarsPadding()
                ) {
                    RulesContent(textColor, isSidePanel = false)
                }
            }
        }
    }
}

@Composable
fun RulesContent(textColor: Color, isSidePanel: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() 
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (isSidePanel) 32.dp else 24.dp)
            .padding(bottom = 32.dp)
    ) {
        RuleSection(
            "1. Dodjela uloga", 
            "Svi igrači dobivaju tajnu riječ. Većina dobiva istu, dok Imposter dobiva sličan pojam koji ga može zbuniti.", 
            textColor
        )
        
        RuleSection(
            "2. Opisivanje", 
            "Svaki igrač u krugu kaže točno JEDNU riječ (asocijaciju) koja opisuje njegov pojam.",
            textColor
        )
        
        RuleSection(
            "3. Zabranjene riječi", 
            "Strogo je zabranjeno izgovoriti samu tajnu riječ, njezine korijene ili njezine prijevode na druge jezike.", 
            textColor, 
            isWarning = true
        )
        
        RuleSection(
            "4. Glasanje", 
            "Nakon kratke rasprave, svi na signal prstom upiru u sumnjivca. Osoba s najviše glasova ispada.", 
            textColor
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = textColor.copy(alpha = 0.2f))

        Text("🏆 Cilj igre", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(Modifier.height(12.dp))
        Text(
            "Većina: Razotkriti Impostera prije nego što on shvati vašu riječ.",
            fontSize = 16.sp, color = textColor.copy(alpha = 0.9f)
        )
        Text(
            "Imposter: Preživjeti blefiranjem i uklopiti se u ekipu do samog kraja.",
            fontSize = 16.sp, color = textColor.copy(alpha = 0.9f), modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SageGreen.copy(alpha = if (isSidePanel) 0.3f else 0.15f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("💡 Savjeti", fontWeight = FontWeight.Bold, color = if (isSidePanel) textColor else SageGreen, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "• Ne budi previše očigledan: Ako kažeš previše, Imposter će lako pogoditi vašu riječ i pobijediti.",
                    fontSize = 15.sp, color = textColor.copy(alpha = 0.9f)
                )
                Text(
                    "• Pažljivo slušaj: Imposter često griješi u nijansama – prati tko se \"čudno\" nadovezuje.",
                    fontSize = 15.sp, color = textColor.copy(alpha = 0.9f), modifier = Modifier.padding(top = 10.dp)
                )
                Text(
                    "• Blefiraj do kraja: Čak i ako si otkriven, uvjeri ih da je netko drugi zapravo uljez!",
                    fontSize = 15.sp, color = textColor.copy(alpha = 0.9f), modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun RuleSection(title: String, description: String, textColor: Color, isWarning: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = if (isWarning) MutedRose else textColor, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        Text(description, color = textColor.copy(alpha = 0.8f), fontSize = 16.sp, lineHeight = 22.sp)
    }
}

@Composable
fun AestheticButton(text: String, subText: String, icon: ImageVector, onClick: () -> Unit, color: Color, isWideScreen: Boolean = false) {
    val isDarkTheme = isSystemInDarkTheme()
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(if (isWideScreen) 180.dp else 100.dp).padding(vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = if (isDarkTheme) 0.15f else 0.9f),
            contentColor = if (isDarkTheme) color else Color.White
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.fillMaxSize().padding(horizontal = if (isWideScreen) 48.dp else 24.dp)
        ) {
            Surface(
                modifier = Modifier.size(if (isWideScreen) 100.dp else 48.dp), 
                shape = RoundedCornerShape(24.dp), 
                color = if (isDarkTheme) color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) { 
                    Icon(icon, null, modifier = Modifier.size(if (isWideScreen) 56.dp else 24.dp)) 
                }
            }
            Spacer(Modifier.width(if (isWideScreen) 32.dp else 16.dp))
            Column {
                Text(text.uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = if (isWideScreen) 36.sp else 18.sp)
                Text(
                    text = subText, 
                    fontSize = if (isWideScreen) 20.sp else 12.sp,
                    color = (if (isDarkTheme) color else Color.White).copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
