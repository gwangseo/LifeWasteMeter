package com.sst.lifewastemeter.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.*
import kotlin.math.max
import kotlin.math.min

object UsageStatsUtil {
    
    /**
     * UsageStats 권한이 있는지 확인
     */
    fun isUsageStatsPermissionGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    /**
     * UsageStats 설정 화면으로 이동하는 Intent 반환
     */
    fun getUsageStatsSettingsIntent(): android.content.Intent {
        return android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }
    
    /**
     * 특정 날짜의 앱별 사용 시간 가져오기
     * queryEvents를 사용하여 원본 이벤트를 직접 계산
     * @param context Context
     * @param packageNames 추적할 앱 패키지 이름 목록
     * @param date 날짜 (Calendar 인스턴스, 시간은 00:00:00으로 설정되어 있어야 함)
     * @return Map<패키지명, 사용 시간(밀리초)>
     */
    fun getAppUsageTimeForDate(
        context: Context,
        packageNames: Set<String>,
        date: Calendar
    ): Map<String, Long> {
        if (!isUsageStatsPermissionGranted(context)) {
            return emptyMap()
        }
        
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        // 해당 날짜의 시작 시간과 끝 시간 설정
        val startTime = date.clone() as Calendar
        startTime.set(Calendar.HOUR_OF_DAY, 0)
        startTime.set(Calendar.MINUTE, 0)
        startTime.set(Calendar.SECOND, 0)
        startTime.set(Calendar.MILLISECOND, 0)
        
        val endTime = date.clone() as Calendar
        endTime.add(Calendar.DAY_OF_YEAR, 1)
        endTime.set(Calendar.HOUR_OF_DAY, 0)
        endTime.set(Calendar.MINUTE, 0)
        endTime.set(Calendar.SECOND, 0)
        endTime.set(Calendar.MILLISECOND, 0)
        
        // queryEvents를 사용하여 원본 이벤트 가져오기
        val usageEvents = usageStatsManager.queryEvents(
            startTime.timeInMillis,
            endTime.timeInMillis
        )
        
        val result = mutableMapOf<String, Long>()
        packageNames.forEach { packageName ->
            result[packageName] = 0L
        }
        
        // 각 앱별로 포그라운드 시간 계산
        val appForegroundStart = mutableMapOf<String, Long>()
        
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            
            val eventPackageName = event.packageName
            if (!packageNames.contains(eventPackageName)) {
                continue
            }
            
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    // 앱이 포그라운드로 이동한 시간 기록
                    appForegroundStart[eventPackageName] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    // 앱이 백그라운드로 이동한 시간 기록
                    val foregroundStart = appForegroundStart[eventPackageName]
                    if (foregroundStart != null) {
                        // 포그라운드에 있었던 시간 계산
                        val dayStartTime = startTime.timeInMillis
                        val dayEndTime = endTime.timeInMillis
                        
                        // 포그라운드 기간이 해당 날짜 범위와 겹치는 부분 계산
                        val overlapStart = maxOf(foregroundStart, dayStartTime)
                        val overlapEnd = minOf(event.timeStamp, dayEndTime)
                        
                        if (overlapStart < overlapEnd) {
                            val overlapDuration = overlapEnd - overlapStart
                            result[eventPackageName] = (result[eventPackageName] ?: 0L) + overlapDuration
                        }
                        
                        // 포그라운드 시작 시간 초기화
                        appForegroundStart.remove(eventPackageName)
                    }
                }
            }
        }
        
        // 아직 포그라운드에 있는 앱들 처리
        appForegroundStart.forEach { (packageName, foregroundStart) ->
            val dayStartTime = startTime.timeInMillis
            val dayEndTime = endTime.timeInMillis
            
            val overlapStart = maxOf(foregroundStart, dayStartTime)
            val overlapEnd = dayEndTime
            
            if (overlapStart < overlapEnd) {
                val overlapDuration = overlapEnd - overlapStart
                result[packageName] = (result[packageName] ?: 0L) + overlapDuration
            }
        }
        
        return result
    }
    
    /**
     * 여러 날짜의 앱별 사용 시간 가져오기
     * queryEvents를 사용하여 원본 이벤트를 직접 계산 (디지털 웰빙 앱과 가장 근접한 결과)
     * @param context Context
     * @param packageNames 추적할 앱 패키지 이름 목록
     * @param days 가져올 일수 (오늘부터 과거로)
     * @return Map<날짜(밀리초), Map<패키지명, 사용 시간(밀리초)>>
     */
    fun getAppUsageTimeForDates(
        context: Context,
        packageNames: Set<String>,
        days: Int = 30
    ): Map<Long, Map<String, Long>> {
        if (!isUsageStatsPermissionGranted(context)) {
            return emptyMap()
        }
        
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val result = mutableMapOf<Long, Map<String, Long>>()
        
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        
        // 각 날짜별로 개별 조회
        for (i in 0 until days) {
            val date = today.clone() as Calendar
            date.add(Calendar.DAY_OF_YEAR, -i)
            val dateMillis = date.timeInMillis
            
            // 해당 날짜의 시작 시간 (00:00:00.000)
            val startTime = date.clone() as Calendar
            startTime.set(Calendar.HOUR_OF_DAY, 0)
            startTime.set(Calendar.MINUTE, 0)
            startTime.set(Calendar.SECOND, 0)
            startTime.set(Calendar.MILLISECOND, 0)
            
            // 다음 날 자정 (00:00:00.000)
            val endTime = date.clone() as Calendar
            endTime.add(Calendar.DAY_OF_YEAR, 1)
            endTime.set(Calendar.HOUR_OF_DAY, 0)
            endTime.set(Calendar.MINUTE, 0)
            endTime.set(Calendar.SECOND, 0)
            endTime.set(Calendar.MILLISECOND, 0)
            
            // queryEvents를 사용하여 원본 이벤트 가져오기
            val usageEvents = usageStatsManager.queryEvents(
                startTime.timeInMillis,
                endTime.timeInMillis
            )
            
            val dayUsage = mutableMapOf<String, Long>()
            packageNames.forEach { packageName ->
                dayUsage[packageName] = 0L
            }
            
            // 각 앱별로 포그라운드 시간 계산
            val appForegroundStart = mutableMapOf<String, Long>()
            
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                
                val eventPackageName = event.packageName
                if (!packageNames.contains(eventPackageName)) {
                    continue
                }
                
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        // 앱이 포그라운드로 이동한 시간 기록
                        appForegroundStart[eventPackageName] = event.timeStamp
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        // 앱이 백그라운드로 이동한 시간 기록
                        val foregroundStart = appForegroundStart[eventPackageName]
                        if (foregroundStart != null) {
                            // 포그라운드에 있었던 시간 계산
                            val foregroundDuration = event.timeStamp - foregroundStart
                            // 해당 날짜 범위 내의 시간만 계산
                            val dayStartTime = startTime.timeInMillis
                            val dayEndTime = endTime.timeInMillis
                            
                            // 포그라운드 기간이 해당 날짜 범위와 겹치는 부분 계산
                            val overlapStart = maxOf(foregroundStart, dayStartTime)
                            val overlapEnd = minOf(event.timeStamp, dayEndTime)
                            
                            if (overlapStart < overlapEnd) {
                                val overlapDuration = overlapEnd - overlapStart
                                dayUsage[eventPackageName] = (dayUsage[eventPackageName] ?: 0L) + overlapDuration
                            }
                            
                            // 포그라운드 시작 시간 초기화
                            appForegroundStart.remove(eventPackageName)
                        }
                    }
                }
            }
            
            // 아직 포그라운드에 있는 앱들 처리 (날짜가 끝날 때까지 포그라운드에 있었던 경우)
            appForegroundStart.forEach { (packageName, foregroundStart) ->
                val dayStartTime = startTime.timeInMillis
                val dayEndTime = endTime.timeInMillis
                
                // 해당 날짜 범위 내의 시간만 계산
                val overlapStart = maxOf(foregroundStart, dayStartTime)
                val overlapEnd = dayEndTime
                
                if (overlapStart < overlapEnd) {
                    val overlapDuration = overlapEnd - overlapStart
                    dayUsage[packageName] = (dayUsage[packageName] ?: 0L) + overlapDuration
                }
            }
            
            result[dateMillis] = dayUsage
        }
        
        return result
    }
    
    /**
     * 오늘의 앱별 사용 시간 가져오기
     */
    fun getTodayAppUsageTime(
        context: Context,
        packageNames: Set<String>
    ): Map<String, Long> {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        
        return getAppUsageTimeForDate(context, packageNames, today)
    }
}

