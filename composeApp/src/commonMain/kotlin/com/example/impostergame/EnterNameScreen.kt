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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "IMPOSTOR GAME",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SageGreen,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Tko je među nama?",
                fontSize = 16.sp,
                color = textColor.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(60.dp))

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
                        text = "Unesi svoje ime",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            errorMessage = null 
                        },
                        placeholder = { Text("Username...", color = textColor.copy(alpha = 0.4f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = errorMessage != null,
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
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = SageGreen)
                        )
                        Text(
                            text = "Zapamti me",
                            color = textColor,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { rememberMe = !rememberMe }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

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
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text("KRENI", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
