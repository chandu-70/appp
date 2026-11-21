package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ai.configureAppCheck
import com.example.ui.SmartSplitApp
import com.example.ui.theme.SmartSplitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge and render Compose UI
        enableEdgeToEdge()
        setContent {
            SmartSplitTheme {
                SmartSplitApp()
            }
        }
    }
}
