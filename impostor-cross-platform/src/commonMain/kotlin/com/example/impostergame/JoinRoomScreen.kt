package com.example.impostergame

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impostergame.ui.components.CameraScanner
import com.example.impostergame.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JoinRoomScreen(username: String, onJoined: (String) -> Unit, onBack: () -> Unit) {
    var inputCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var isJoining by remember { mutableStateOf(false) }
    
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) OffWhite else DeepCharcoal
    val inputContainerColor = if (isDarkTheme) DarkInputGray else Color.White
    
    val accentColor = MutedRose
    val scope = rememberCoroutineScope()

    // Detekcija tipkovnice preko paddinga
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    // Funkcija za zajedničku logiku ulaska u sobu
    fun attemptJoin(code: String) {
        if (isJoining) return
        val upperCode = code.uppercase()
        isJoining = true
        errorMessage = ""
        
        scope.launch {
            val result = FirebaseManager.joinRoom(upperCode, username)
            if (result.isSuccess) {
                onJoined(upperCode)
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Nepoznata greška"
                showScanner = false
            }
            isJoining = false
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val width = maxWidth
        val height = maxHeight
        val isMobile = width < 600.dp
        val isWide = width >= 600.dp
        
        // Back Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(if (isWide) 32.dp else 16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Surface(
                onClick = onBack,
                color = if (isWide) textColor.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(if (isWide) 64.dp else 48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Back", 
                        tint = textColor,
                        modifier = Modifier.size(if (isWide) 32.dp else 24.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isWide) 48.dp else 24.dp, vertical = 24.dp), // POVEĆAN HORIZONTAL PADDING ZA WIDE
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(if (height < 600.dp) 20.dp else 60.dp))
            
            Text(
                text = "Pridruži se",
                fontSize = if (isWide) 56.sp else 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            
            Spacer(modifier = Modifier.height(if (isWide) 64.dp else 48.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = if (isWide) 32.dp else 0.dp), // PADDING DA CARD NE DIRA RUBOVE
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = inputContainerColor.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(if (isWide) 48.dp else 24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Unesi kod sobe",
                        fontSize = if (isWide) 24.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    
                    Spacer(modifier = Modifier.height(if (isWide) 24.dp else 16.dp))

                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { 
                            if (it.length <= 6) inputCode = it.uppercase() 
                            errorMessage = ""
                        },
                        placeholder = { 
                            Text(
                                "ABC 123", 
                                color = textColor.copy(alpha = 0.4f), 
                                fontSize = if (isWide) 20.sp else 16.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            ) 
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isWide) 32.dp else 0.dp) // PADDING UNUTAR CARDA
                            .height(if (isWide) 80.dp else 64.dp)
                            .onKeyEvent { event ->
                                if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                    if (inputCode.length == 6) {
                                        attemptJoin(inputCode)
                                    } else {
                                        errorMessage = "Kod mora imati 6 znakova"
                                    }
                                    true
                                } else {
                                    false
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = if (isWide) 24.sp else 16.sp, // VEĆA SLOVA KODA
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        isError = errorMessage.isNotEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        enabled = !isJoining
                    )
                    
                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage, 
                            color = MaterialTheme.colorScheme.error, 
                            fontSize = if (isWide) 16.sp else 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isWide) 48.dp else 24.dp)) // VEĆI RAZMAK
                    
                    Button(
                        onClick = {
                            if (inputCode.length == 6) {
                                attemptJoin(inputCode)
                            } else {
                                errorMessage = "Kod mora imati 6 znakova"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isWide) 32.dp else 0.dp) // PADDING ZA GUMB
                            .height(if (isWide) 80.dp else 56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (inputCode.length == 6 && !isJoining) accentColor else accentColor.copy(alpha = 0.5f),
                            contentColor = Color.White
                        ),
                        enabled = !isJoining
                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(modifier = Modifier.size(if (isWide) 32.dp else 24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("PRIDRUŽI SE", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontSize = if (isWide) 20.sp else 16.sp)
                        }
                    }
                }
            }

            if (!isKeyboardVisible) {
                Spacer(modifier = Modifier.height(if (isWide) 48.dp else 32.dp))

                Surface(
                    modifier = Modifier
                        .size(if (isWide) 100.dp else 64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showScanner = true },
                    color = inputContainerColor.copy(alpha = 0.9f),
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Skeniraj QR kod",
                            modifier = Modifier.size(if (isWide) 48.dp else 32.dp),
                            tint = textColor
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
