package com.example.siliguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
                val startDest by mainViewModel.startDestination.collectAsState()
                var resolvedStartDest by rememberSaveable { mutableStateOf<String?>(null) }

                LaunchedEffect(startDest) {
                    if (resolvedStartDest == null && startDest != null) {
                        resolvedStartDest = startDest
                    }
                }

                val initialRoute = resolvedStartDest ?: startDest

                if (initialRoute != null) {
                    val navController = rememberNavController()
                    SiliGuideNavGraph(
                        navController = navController,
                        startDestination = initialRoute
                    )
                }
            }
        }
    }
}