package com.example.impostergame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        AndroidContext.context = this

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AndroidContext.context = null
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    // Initializing AndroidContext for Preview environment
    AndroidContext.context = LocalContext.current
    App()
}
