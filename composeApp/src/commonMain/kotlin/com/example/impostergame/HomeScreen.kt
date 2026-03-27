package com.example.impostergame

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Bok, $username!",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            
            Text(
                text = "Spreman za novu rundu?",
                fontSize = 16.sp,
                color = textColor.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(60.dp))

            AestheticButton(
                text = "Napravi sobu",
                subText = "Postani admin i započni igru",
                icon = Icons.Default.Add,
                onClick = onCreateRoom,
                color = SageGreen
            )

            Spacer(modifier = Modifier.height(20.dp))

            AestheticButton(
                text = "Pridruži se",
                subText = "Uđi u postojeću sobu",
                icon = Icons.Default.Group,
                onClick = onJoinRoom,
                color = MutedRose
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Surface(
                onClick = { showRules = true },
                color = Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Pravila",
                        tint = textColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    if (showRules) {
        ModalBottomSheet(
            onDismissRequest = { showRules = false },
            containerColor = if (isDarkTheme) DarkInputGray else OffWhite,
            dragHandle = { BottomSheetDefaults.DragHandle(color = textColor.copy(alpha = 0.2f)) }
        ) {
            RulesContent(textColor)
        }
    }
}

@Composable
fun RulesContent(textColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Kako igrati?",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        RuleSection(
            "1. Dodjela uloga", 
            "Svi igrači dobivaju tajnu riječ. Većina dobiva istu, dok Imposter dobiva sličan pojam koji ga može zbuniti.", 
            textColor
        )
        
        RuleSection(
            "2. Opisivanje", 
            "Svaki igrač u krugu kaže točno JEDNU riječ (asocijaciju) koja opisuje njegov pojam.\n\n",
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = textColor.copy(alpha = 0.1f))

        Text("🏆 Cilj igre", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(Modifier.height(8.dp))
        Text(
            "Većina: Razotkriti Impostera prije nego što on shvati vašu riječ.",
            fontSize = 15.sp, color = textColor.copy(alpha = 0.8f)
        )
        Text(
            "Imposter: Preživjeti blefiranjem i uklopiti se u ekipu do samog kraja.",
            fontSize = 15.sp, color = textColor.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SageGreen.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("💡 Savjeti", fontWeight = FontWeight.Bold, color = SageGreen)
                Spacer(Modifier.height(8.dp))
                Text(
                    "• Ne budi previše očigledan: Ako kažeš previše, Imposter će lako pogoditi vašu riječ i pobijediti.",
                    fontSize = 14.sp, color = textColor.copy(alpha = 0.8f)
                )
                Text(
                    "• Pažljivo slušaj: Imposter često griješi u nijansama – prati tko se \"čudno\" nadovezuje.",
                    fontSize = 14.sp, color = textColor.copy(alpha = 0.8f), modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "• Blefiraj do kraja: Čak i ako si otkriven, uvjeri ih da je netko drugi zapravo uljez!",
                    fontSize = 14.sp, color = textColor.copy(alpha = 0.8f), modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RuleSection(title: String, description: String, textColor: Color, isWarning: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = if (isWarning) MutedRose else textColor, fontSize = 17.sp)
        Spacer(Modifier.height(4.dp))
        Text(description, color = textColor.copy(alpha = 0.7f), fontSize = 15.sp, lineHeight = 20.sp)
    }
}

@Composable
fun AestheticButton(text: String, subText: String, icon: ImageVector, onClick: () -> Unit, color: Color) {
    val isDarkTheme = isSystemInDarkTheme()
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = if (isDarkTheme) 0.15f else 0.9f),
            contentColor = if (isDarkTheme) color else Color.White
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp), 
                shape = RoundedCornerShape(14.dp), 
                color = if (isDarkTheme) color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) { 
                    Icon(icon, null, modifier = Modifier.size(24.dp)) 
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text.uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(
                    text = subText, 
                    fontSize = 12.sp, 
                    color = (if (isDarkTheme) color else Color.White).copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
