package com.sst.lifewastemeter.ui.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sst.lifewastemeter.LifeWasteMeterApplication
import com.sst.lifewastemeter.service.AppUsageTrackingService
import com.sst.lifewastemeter.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
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
    val userSettings by viewModel.userSettings.collectAsState()
    var nickname by remember { mutableStateOf(userSettings.nickname) }
    var isAccessibilityEnabled by remember {
        mutableStateOf(AppUsageTrackingService.isAccessibilityServiceEnabled(context))
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityEnabled = AppUsageTrackingService.isAccessibilityServiceEnabled(context)
            delay(500)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
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
        ) {
            // 닉네임 설정
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "닉네임",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("닉네임") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.updateNickname(nickname) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("저장")
                    }
                }
            }
            
            // 감지할 앱 선택
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "감지할 앱 선택",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    val apps = listOf(
                        "YouTube" to "com.google.android.youtube",
                        "Instagram" to "com.instagram.android",
                        "TikTok" to "com.zhiliaoapp.musically"
                    )
                    
                    apps.forEach { (appName, packageName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = appName)
                            Switch(
                                checked = userSettings.selectedApps.contains(packageName),
                                onCheckedChange = { checked ->
                                    val newApps = if (checked) {
                                        userSettings.selectedApps + packageName
                                    } else {
                                        userSettings.selectedApps - packageName
                                    }
                                    viewModel.updateSelectedApps(newApps)
                                }
                            )
                        }
                    }
                }
            }
            
            // 접근성 권한 설정
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "접근성 권한",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = if (isAccessibilityEnabled) {
                            "✓ 접근성 권한이 활성화되었습니다"
                        } else {
                            "접근성 권한이 비활성화되어 있습니다"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("권한 설정으로 이동")
                    }
                }
            }
        }
    }
}

