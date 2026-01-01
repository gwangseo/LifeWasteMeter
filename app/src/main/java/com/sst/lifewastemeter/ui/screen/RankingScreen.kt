package com.sst.lifewastemeter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.sst.lifewastemeter.ui.viewmodel.MainViewModel
import com.sst.lifewastemeter.util.ConversionUtil
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

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
    val dailyHistory by viewModel.dailyUsageHistory.collectAsState()
    
    // 화면 진입 시 일별 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.loadDailyUsageHistory(30) // 최근 30일
    }
    
    // 통계 계산 (30일치 합산)
    val totalScrollCount = dailyHistory.sumOf { it.totalScrollCount.toLong() }.toInt()
    val totalScrollDistance = dailyHistory.sumOf { it.totalScrollDistanceMeters }
    
    // 앱별 사용 시간 계산 (오늘 데이터만 사용 - 디지털 웰빙 앱과 동일하게)
    val todayData = dailyHistory.lastOrNull()
    val appUsageMap = mutableMapOf<String, Long>()
    if (todayData != null) {
        todayData.appUsages.forEach { (packageName, appData) ->
            appUsageMap[packageName] = appData.usageTimeMillis
        }
    }
    
    // 총 사용 시간 계산 (오늘의 앱별 사용 시간 합계)
    val totalUsageTime = appUsageMap.values.sum()
    
    // 최대 스크롤 거리 계산 (동적 조정)
    val maxActualDistance = dailyHistory.maxOfOrNull { it.totalScrollDistanceMeters } ?: 0.0
    val chartMaxDistance = when {
        maxActualDistance <= 0 -> 250.0
        maxActualDistance <= 250 -> 250.0
        maxActualDistance <= 500 -> 500.0
        maxActualDistance <= 750 -> 750.0
        maxActualDistance <= 1000 -> 1000.0
        maxActualDistance <= 1250 -> 1250.0
        maxActualDistance <= 1500 -> 1500.0
        maxActualDistance <= 1750 -> 1750.0
        maxActualDistance <= 2000 -> 2000.0
        maxActualDistance <= 10000 -> {
            // 250m 단위로 올림
            ceil(maxActualDistance / 250.0) * 250.0
        }
        else -> 10000.0 // 10000m 이상은 모두 10000m로 고정
    }
    
    // 최근 7일과 전체 30일 데이터
    val recent7Days = dailyHistory.takeLast(7)
    val all30Days = dailyHistory.takeLast(30)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("나의 사용 통계") },
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 전체 통계 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "전체 통계",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "스크롤 횟수",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${totalScrollCount}회",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "총 거리",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${String.format("%.1f", totalScrollDistance)}m",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "총 사용 시간: ${String.format("%.1f", totalUsageTime / 1000.0 / 60.0)}분",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // 일별 스크롤 거리 차트
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "일별 스크롤 거리",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "최근 7일 (좌우 스크롤로 30일 보기)",
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (dailyHistory.isEmpty()) {
                        Text(
                            text = "데이터가 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 32.dp)
                        )
                    } else {
                        // Y축 최대값 표시
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "최대: ${String.format("%.0f", chartMaxDistance)}m",
                                fontSize = 10.sp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 바 차트 (좌우 스크롤 가능)
                        val scrollState = rememberScrollState()
                        
                        // 최근 7일이 먼저 보이도록 스크롤 위치 조정
                        LaunchedEffect(all30Days.size) {
                            if (all30Days.size > 7) {
                                // 최근 7일 이후로 스크롤 (7일 * 44dp = 308dp, 여유를 두고 300dp)
                                kotlinx.coroutines.delay(100)
                                scrollState.animateScrollTo((all30Days.size - 7) * 44)
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(350.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState)
                                    .height(300.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                all30Days.forEach { day ->
                                    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                                    val dateStr = dateFormat.format(Date(day.date))
                                    
                                    Column(
                                        modifier = Modifier
                                            .width(40.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        // 거리 표시 (바 위에)
                                        if (day.totalScrollDistanceMeters > 0) {
                                            Text(
                                                text = "${String.format("%.2f", day.totalScrollDistanceMeters)}m",
                                                fontSize = 9.sp,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                        
                                        // 바
                                        val barHeight = if (chartMaxDistance > 0) {
                                            val heightRatio = if (day.totalScrollDistanceMeters > chartMaxDistance) {
                                                1.0 // 10000m 이상은 최대 높이
                                            } else {
                                                day.totalScrollDistanceMeters / chartMaxDistance
                                            }
                                            (heightRatio * 250).coerceAtLeast(2.0).dp
                                        } else {
                                            2.dp
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(barHeight)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(
                                                    if (day.totalScrollDistanceMeters > 0) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                )
                                        )
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // 날짜 라벨
                                        Text(
                                            text = dateStr,
                                            fontSize = 8.sp,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 앱별 사용 시간
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "앱별 사용 시간",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // 선택된 앱 목록 가져오기 (없으면 기본값)
                    val selectedApps = setOf(
                        "com.google.android.youtube",
                        "com.instagram.android",
                        "com.zhiliaoapp.musically"
                    )
                    
                    selectedApps.forEach { packageName ->
                        val timeMillis = appUsageMap[packageName] ?: 0L
                        val appName = when (packageName) {
                            "com.google.android.youtube" -> "YouTube"
                            "com.instagram.android" -> "Instagram"
                            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> "TikTok"
                            else -> packageName
                        }
                        
                        val minutes = timeMillis / 1000.0 / 60.0
                        val hours = minutes / 60.0
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (hours >= 1) {
                                    "${String.format("%.1f", hours)}시간"
                                } else {
                                    "${String.format("%.1f", minutes)}분"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Divider()
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 총 사용 시간
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "총 사용 시간",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${String.format("%.1f", totalUsageTime / 1000.0 / 60.0)}분",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
