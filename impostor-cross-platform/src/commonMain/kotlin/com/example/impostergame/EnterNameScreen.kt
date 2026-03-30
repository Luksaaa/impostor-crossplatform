package com.example.impostergame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
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
        val width = maxWidth
        val height = maxHeight
        
        val isMobile = width < 600.dp
        val isDesktop = width >= 1000.dp
        
        val titleSize = when {
            isDesktop -> 56.sp
            width >= 600.dp -> 48.sp
            else -> 36.sp
        }
        
        val cardPadding = if (isMobile) 24.dp else 48.dp
        val spacerHeight = if (height < 600.dp) 20.dp else 40.dp
        
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(spacerHeight))

            Text(
                text = "IMPOSTOR GAME",
                fontSize = titleSize,
                fontWeight = FontWeight.ExtraBold,
                color = SageGreen,
                letterSpacing = if (width > 600.dp) 6.sp else 4.sp,
                lineHeight = if (width > 600.dp) 64.sp else 42.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Tko je među nama?",
                fontSize = if (width > 600.dp) 22.sp else 16.sp,
                color = textColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(if (height < 700.dp) 40.dp else 60.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = inputContainerColor.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(cardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Unesi svoje ime",
                        fontSize = if (width > 600.dp) 24.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    
                    Spacer(modifier = Modifier.height(if (width > 600.dp) 24.dp else 16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            errorMessage = null 
                        },
                        placeholder = { 
                            Text(
                                "Username...", 
                                color = textColor.copy(alpha = 0.4f), 
                                fontSize = if (width > 600.dp) 20.sp else 16.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            ) 
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(if (width > 600.dp) 72.dp else 64.dp),
                        shape = RoundedCornerShape(12.dp),
                        isError = errorMessage != null,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = if (width > 600.dp) 20.sp else 16.sp,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
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
                            fontSize = if (width > 600.dp) 16.sp else 12.sp,
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(if (width > 600.dp) 24.dp else 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = SageGreen),
                            modifier = Modifier.scale(if (width > 600.dp) 1.5f else 1.0f)
                        )
                        Spacer(Modifier.width(if (width > 600.dp) 12.dp else 0.dp))
                        Text(
                            text = "Zapamti me",
                            color = textColor,
                            fontSize = if (width > 600.dp) 18.sp else 14.sp,
                            modifier = Modifier.clickable { rememberMe = !rememberMe }
                        )
                    }

                    Spacer(modifier = Modifier.height(if (width > 600.dp) 48.dp else 24.dp))

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
                            .height(if (width > 600.dp) 80.dp else 56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text("KRENI", fontWeight = FontWeight.Bold, fontSize = if (width > 600.dp) 24.sp else 18.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
