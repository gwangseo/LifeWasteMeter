package com.sst.lifewastemeter.ui.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sst.lifewastemeter.LifeWasteMeterApplication
import com.sst.lifewastemeter.service.AppUsageTrackingService
import com.sst.lifewastemeter.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
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
        // 주기적으로 접근성 서비스 상태 확인
        while (true) {
            isAccessibilityEnabled = AppUsageTrackingService.isAccessibilityServiceEnabled(context)
            delay(500)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "인생낭비 측정기에 오신 것을 환영합니다!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // 닉네임 입력
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("닉네임을 입력하세요") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )
        
        // 접근성 권한 요청
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAccessibilityEnabled) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (isAccessibilityEnabled) "✓ 접근성 권한이 활성화되었습니다" else "접근성 권한이 필요합니다",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "앱 사용 시간과 스크롤 횟수를 측정하기 위해 접근성 권한이 필요합니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (!isAccessibilityEnabled) {
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("권한 설정으로 이동")
                    }
                }
            }
        }
        
        // 시작하기 버튼
        Button(
            onClick = {
                if (nickname.isNotBlank()) {
                    viewModel.updateNickname(nickname)
                }
                viewModel.setFirstLaunchComplete()
                onComplete()
            },
            enabled = isAccessibilityEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("시작하기", fontSize = 18.sp)
        }
        
        if (!isAccessibilityEnabled) {
            Text(
                text = "접근성 권한을 활성화한 후 시작할 수 있습니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

