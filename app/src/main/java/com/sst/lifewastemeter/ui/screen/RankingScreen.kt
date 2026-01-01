package com.sst.lifewastemeter.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.sst.lifewastemeter.ui.viewmodel.MainViewModel
import com.sst.lifewastemeter.util.ConversionUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
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
    val uiState by viewModel.uiState.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    
    // 랭킹 계산 (실제로는 서버에서 가져와야 하지만, 여기서는 모의 데이터)
    val percentile = remember(uiState.scrollCount) {
        // 간단한 모의 계산: 스크롤이 많을수록 상위 퍼센트
        when {
            uiState.scrollCount >= 10000 -> 1
            uiState.scrollCount >= 5000 -> 5
            uiState.scrollCount >= 2000 -> 10
            uiState.scrollCount >= 1000 -> 25
            uiState.scrollCount >= 500 -> 50
            else -> 75
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("랭킹") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 나의 백분위
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "나의 순위",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "상위 ${percentile}%",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (percentile <= 1) {
                            "축하합니다! 상위 1%의 도파민 중독자입니다. 🏆"
                        } else if (percentile <= 5) {
                            "당신은 진정한 스크롤 마스터입니다!"
                        } else if (percentile <= 10) {
                            "인생을 충분히 낭비하고 계시네요."
                        } else {
                            "아직 여유가 있습니다. 더 스크롤해보세요!"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            // 통계 정보
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "오늘의 통계",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "스크롤 횟수",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${uiState.scrollCount}회",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Column {
                            Text(
                                text = "총 거리",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${String.format("%.1f", ConversionUtil.calculateDistance(uiState.scrollDistanceMeters))}m",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "사용 시간: ${String.format("%.1f", uiState.usageTimeMillis / 1000.0 / 60.0)}분",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 명예의 전당 (모의 데이터)
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "명예의 전당",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "오늘 가장 많이 내린 사람",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 실제로는 서버에서 가져온 데이터를 표시
                    Text(
                        text = "1위: 스크롤킹 (15,234회)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "2위: 인생낭비러 (12,456회)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "3위: 도파민중독자 (10,789회)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

