package com.example.siliguide.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.features.dashboard.DashboardScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SiliGuideTheme {
                DashboardScreen()
            }
        }
    }
}
