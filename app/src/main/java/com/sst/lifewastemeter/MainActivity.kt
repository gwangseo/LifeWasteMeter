package com.sst.lifewastemeter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.sst.lifewastemeter.data.repository.UsageRepository
import com.sst.lifewastemeter.ui.navigation.NavGraph
import com.sst.lifewastemeter.ui.navigation.Screen
import com.sst.lifewastemeter.ui.theme.LifeWasteMeterTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeWasteMeterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val app = applicationContext as LifeWasteMeterApplication
                    val repository = app.repository
                    
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    
                    // 첫 실행 여부 확인
                    LaunchedEffect(Unit) {
                        val settings = repository.userSettings.first()
                        startDestination = if (settings.isFirstLaunch) {
                            Screen.Onboarding.route
                        } else {
                            Screen.Home.route
                        }
                    }
                    
                    startDestination?.let { destination ->
                        NavGraph(
                            navController = navController,
                            startDestination = destination
                        )
                    }
                }
            }
        }
    }
}