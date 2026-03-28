package com.example.impostergame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impostergame.ui.theme.*

@Composable
fun EnterNameScreen(onNameEntered: (String, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) OffWhite else DeepCharcoal
    val inputContainerColor = if (isDarkTheme) DarkInputGray else Color.White

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        val isWideScreen = maxWidth > 800.dp
        
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(if (isWideScreen) 60.dp else 40.dp))

            Text(
                text = "IMPOSTOR GAME",
                fontSize = if (isWideScreen) 56.sp else 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SageGreen,
                letterSpacing = if (isWideScreen) 6.sp else 4.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Tko je među nama?",
                fontSize = if (isWideScreen) 22.sp else 16.sp,
                color = textColor.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(if (isWideScreen) 80.dp else 60.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = inputContainerColor.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(if (isWideScreen) 48.dp else 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Unesi svoje ime",
                        fontSize = if (isWideScreen) 24.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    
                    Spacer(modifier = Modifier.height(if (isWideScreen) 24.dp else 16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            errorMessage = null 
                        },
                        placeholder = { Text("Username...", color = textColor.copy(alpha = 0.4f), fontSize = if (isWideScreen) 20.sp else 16.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(if (isWideScreen) 72.dp else 64.dp),
                        shape = RoundedCornerShape(12.dp),
                        isError = errorMessage != null,
                        textStyle = LocalTextStyle.current.copy(fontSize = if (isWideScreen) 20.sp else 16.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SageGreen,
                            unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            errorBorderColor = MutedRose
                        )
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MutedRose,
                            fontSize = if (isWideScreen) 16.sp else 12.sp,
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isWideScreen) 24.dp else 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = SageGreen),
                            modifier = Modifier.scale(if (isWideScreen) 1.5f else 1.0f)
                        )
                        Spacer(Modifier.width(if (isWideScreen) 12.dp else 0.dp))
                        Text(
                            text = "Zapamti me",
                            color = textColor,
                            fontSize = if (isWideScreen) 18.sp else 14.sp,
                            modifier = Modifier.clickable { rememberMe = !rememberMe }
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isWideScreen) 48.dp else 24.dp))

                    Button(
                        onClick = { 
                            when {
                                name.isBlank() -> errorMessage = "Ime ne smije biti prazno"
                                name.length > 8 -> errorMessage = "Ime ne smije biti duže od 8 znakova"
                                !name.all { it.isLetterOrDigit() || it == '_' } -> errorMessage = "Samo slova, brojke i _"
                                else -> onNameEntered(name, rememberMe)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isWideScreen) 80.dp else 56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text("KRENI", fontWeight = FontWeight.Bold, fontSize = if (isWideScreen) 24.sp else 18.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
