package com.sst.lifewastemeter.data.model

import java.util.Calendar

data class AppUsageData(
    val packageName: String,
    val appName: String,
    val scrollCount: Int = 0,
    val usageTimeMillis: Long = 0,
    val date: Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
)

data class DailyUsageData(
    val date: Long,
    val totalScrollCount: Int = 0,  // 레거시 호환용
    val totalScrollDistanceMeters: Double = 0.0,  // 실제 스크롤 거리 (미터)
    val totalUsageTimeMillis: Long = 0,
    val appUsages: Map<String, AppUsageData> = emptyMap()
)

data class UserSettings(
    val nickname: String = "",
    val selectedApps: Set<String> = setOf(
        "com.google.android.youtube",
        "com.instagram.android",
        "com.zhiliaoapp.musically"
    ),
    val currentMode: DisplayMode = DisplayMode.CLIMBING,
    val isFirstLaunch: Boolean = true
)

enum class DisplayMode {
    CLIMBING, // 등반 모드
    TOILET_PAPER // 휴지 낭비 모드
}





