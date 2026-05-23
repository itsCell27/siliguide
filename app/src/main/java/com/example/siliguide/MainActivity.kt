package com.example.siliguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.presentation.MainViewModel
import com.example.siliguide.presentation.navigation.SiliGuideNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Inject the MainViewModel directly into the Activity
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SiliGuideTheme {
                // Collect the DataStore state (null = loading, string = route)
                val startDest by mainViewModel.startDestination.collectAsState()

                if (startDest != null) {
                    val navController = rememberNavController()
                    SiliGuideNavGraph(
                        navController = navController,
                        startDestination = startDest!! // Pass the dynamic route
                    )
                }
            }
        }
    }
}