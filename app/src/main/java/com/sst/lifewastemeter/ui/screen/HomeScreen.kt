package com.sst.lifewastemeter.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sst.lifewastemeter.LifeWasteMeterApplication
import com.sst.lifewastemeter.data.model.DisplayMode
import com.sst.lifewastemeter.ui.viewmodel.MainViewModel
import com.sst.lifewastemeter.util.ConversionUtil
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRanking: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as LifeWasteMeterApplication
    val viewModel: MainViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(app.repository) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    var lastScrollDistance by remember { mutableStateOf(uiState.scrollDistanceMeters) }
    var showNewScrollNotification by remember { mutableStateOf(false) }
    
    // 스크롤 거리가 증가할 때 알림 표시
    LaunchedEffect(uiState.scrollDistanceMeters) {
        if (uiState.scrollDistanceMeters > lastScrollDistance) {
            val added = uiState.scrollDistanceMeters - lastScrollDistance
            showNewScrollNotification = true
            lastScrollDistance = uiState.scrollDistanceMeters
            delay(2000)
            showNewScrollNotification = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("인생낭비 측정기") },
                actions = {
                    IconButton(onClick = onNavigateToRanking) {
                        Icon(Icons.Default.Face, contentDescription = "랭킹")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // 이 줄 추가!
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 현재 추적 중인 앱 표시
            val appName = when (uiState.currentTrackingApp) {
                "com.google.android.youtube" -> "YouTube"
                "com.instagram.android" -> "Instagram"
                "com.zhiliaoapp.musically" -> "TikTok"
                "com.ss.android.ugc.trill" -> "TikTok"
                "" -> "앱이 실행되지 않음"
                else -> uiState.currentTrackingApp
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.currentTrackingApp.isNotEmpty()) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = if (uiState.currentTrackingApp.isNotEmpty()) {
                        "현재 추적 중: $appName"
                    } else {
                        "추적 중인 앱 없음"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // 모드 토글
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = uiState.displayMode == DisplayMode.CLIMBING,
                        onClick = { viewModel.updateDisplayMode(DisplayMode.CLIMBING) },
                        label = { Text("등산 모드") }
                    )
                    FilterChip(
                        selected = uiState.displayMode == DisplayMode.TOILET_PAPER,
                        onClick = { viewModel.updateDisplayMode(DisplayMode.TOILET_PAPER) },
                        label = { Text("휴지 낭비 모드") }
                    )
                }
            }
            
            // 메인 비주얼
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (uiState.displayMode) {
                        DisplayMode.CLIMBING -> {
                            // 등반 모드 비주얼
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "⛰️",
                                    fontSize = 80.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                val distance = ConversionUtil.calculateDistance(uiState.scrollDistanceMeters)
                                val progress = (distance / 8848.0).coerceAtMost(1.0)
                                
                                LinearProgressIndicator(
                                    progress = { progress.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${String.format("%.1f", distance)}m",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        DisplayMode.TOILET_PAPER -> {
                            // 휴지 낭비 모드 비주얼
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🧻",
                                    fontSize = 80.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                val sheets = ConversionUtil.calculateToiletPaperSheets(uiState.scrollDistanceMeters)
                                Text(
                                    text = "${sheets}칸",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            // 메인 메시지
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = uiState.message,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // 팩트 폭력 문구
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = uiState.factMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // 실시간 알림
            if (showNewScrollNotification) {
                val added = uiState.scrollDistanceMeters - lastScrollDistance
                AnimatedVisibility(
                    visible = showNewScrollNotification,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Text(
                            text = "방금 ${String.format("%.2f", added)}m 추가!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

