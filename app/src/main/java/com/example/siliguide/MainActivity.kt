package com.example.siliguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.presentation.navigation.SiliGuideNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SiliGuideTheme {
                // Initialize the Navigation Controller
                val navController = rememberNavController()

                // Launch our Navigation Graph
                SiliGuideNavGraph(navController = navController)
            }
        }
    }
}