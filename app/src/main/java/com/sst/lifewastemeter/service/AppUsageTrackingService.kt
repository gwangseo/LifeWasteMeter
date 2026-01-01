package com.sst.lifewastemeter.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.sst.lifewastemeter.LifeWasteMeterApplication
import com.sst.lifewastemeter.util.ConversionUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "AppUsageTracking"

class AppUsageTrackingService : AccessibilityService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var currentPackageName: String? = null
    private var lastScrollTime: Long = 0
    private var scrollCount = 0
    private var appStartTime: Long = 0
    private var lastScrollY: Int = 0
    private var lastScrollX: Int = 0
    private var targetPackages: Set<String> = emptySet()
    
    private fun getRepository() = (application as? LifeWasteMeterApplication)?.repository
    
    private suspend fun updateTargetPackages() {
        getRepository()?.let { repo ->
            targetPackages = repo.getSelectedApps()
        }
    }
    
    private fun isTargetPackage(packageName: String?): Boolean {
        return packageName != null && targetPackages.contains(packageName)
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        // 서비스 시작 시 선택된 앱 목록 로드
        serviceScope.launch {
            updateTargetPackages()
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 주기적으로 선택된 앱 목록 업데이트 (5초마다)
        if (System.currentTimeMillis() % 5000 < 100) {
            serviceScope.launch {
                updateTargetPackages()
            }
        }
        
        val packageName = event.packageName?.toString()
        val isTarget = isTargetPackage(packageName)
        val isYouTube = packageName == "com.google.android.youtube"
        
        // 유튜브의 경우 모든 이벤트를 로그로 출력 (디버깅용)
        if (isYouTube && isTarget) {
            val eventTypeName = when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "TYPE_WINDOW_STATE_CHANGED"
                AccessibilityEvent.TYPE_VIEW_SCROLLED -> "TYPE_VIEW_SCROLLED"
                AccessibilityEvent.TYPE_GESTURE_DETECTION_START -> "TYPE_GESTURE_DETECTION_START"
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "TYPE_WINDOW_CONTENT_CHANGED"
                AccessibilityEvent.TYPE_VIEW_CLICKED -> "TYPE_VIEW_CLICKED"
                AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> "TYPE_TOUCH_INTERACTION_START"
                AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> "TYPE_TOUCH_INTERACTION_END"
                else -> "OTHER(${event.eventType})"
            }
            Log.d(TAG, "유튜브 이벤트: $eventTypeName, packageName=$packageName, currentPackage=$currentPackageName")
        }
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                // 사용자가 직접 스크롤한 경우만 감지
                handleScrollEvent(event)
            }
            AccessibilityEvent.TYPE_GESTURE_DETECTION_START -> {
                // TYPE_VIEW_SCROLLED가 발생하지 않는 앱을 위한 보조 감지
                handleGestureStart(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // 유튜브의 경우 TYPE_WINDOW_CONTENT_CHANGED를 사용 (다른 이벤트가 발생하지 않을 수 있음)
                if (isYouTube) {
                    handleWindowContentChangedForYouTube(event)
                }
            }
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
                // 유튜브의 경우 터치 시작 이벤트도 활용
                if (isYouTube) {
                    handleTouchInteractionForYouTube(event)
                }
            }
        }
    }
    
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        
        Log.d(TAG, "TYPE_WINDOW_STATE_CHANGED: packageName=$packageName, isTarget=${isTargetPackage(packageName)}")
        
        if (isTargetPackage(packageName)) {
            if (currentPackageName != packageName) {
                // 앱이 변경되었을 때
                savePreviousAppUsage()
                currentPackageName = packageName
                appStartTime = System.currentTimeMillis()
                scrollCount = 0
                lastScrollY = 0
                lastScrollX = 0
                
                // 현재 추적 중인 앱 정보 저장
                serviceScope.launch {
                    getRepository()?.setCurrentTrackingApp(packageName)
                }
                
                Log.d(TAG, "앱 추적 시작: $packageName")
            }
        } else {
            // 추적 대상이 아닌 앱으로 변경
            if (currentPackageName != null) {
                savePreviousAppUsage()
                currentPackageName = null
                
                // 추적 중인 앱 정보 초기화
                serviceScope.launch {
                    getRepository()?.setCurrentTrackingApp(null)
                }
                
                Log.d(TAG, "앱 추적 종료")
            }
        }
    }
    
    private fun handleScrollEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        
        Log.d(TAG, "TYPE_VIEW_SCROLLED: packageName=$packageName, scrollX=${event.scrollX}, scrollY=${event.scrollY}, maxScrollY=${event.maxScrollY}, isTarget=${isTargetPackage(packageName)}, currentPackage=$currentPackageName")
        
        // 설정에서 선택한 앱이고, 현재 추적 중인 앱인지 확인
        if (!isTargetPackage(packageName) || packageName != currentPackageName) {
            Log.d(TAG, "스크롤 이벤트 무시: isTarget=${isTargetPackage(packageName)}, currentPackage=$currentPackageName")
            return
        }
        
        // TYPE_VIEW_SCROLLED는 사용자가 직접 스크롤한 것이므로 바로 카운트
        val currentTime = System.currentTimeMillis()
        
        // 앱별로 다른 감지 간격 설정 (너무 빈번한 이벤트 방지)
        val timeThreshold = when (packageName) {
            "com.google.android.youtube" -> 100  // 유튜브는 100ms 간격
            "com.instagram.android" -> 50  // 인스타그램은 50ms 간격
            else -> 50
        }
        
        // 스크롤 이벤트가 너무 빈번하게 발생하지 않도록 제한
        if (currentTime - lastScrollTime > timeThreshold) {
            // API 29+에서는 scrollDeltaX/Y를 직접 사용할 수 있음
            var scrollDeltaX = 0
            var scrollDeltaY = 0
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // API 29+에서는 스크롤 델타를 직접 가져올 수 있음
                scrollDeltaX = kotlin.math.abs(event.scrollDeltaX)
                scrollDeltaY = kotlin.math.abs(event.scrollDeltaY)
            }
            
            // scrollDelta가 0이거나 제공되지 않는 경우, scrollX/Y 위치 정보 사용
            if (scrollDeltaX == 0 && scrollDeltaY == 0) {
                val scrollX = event.scrollX
                val scrollY = event.scrollY
                
                scrollDeltaX = if (lastScrollX != 0 && scrollX >= 0) {
                    kotlin.math.abs(scrollX - lastScrollX)
                } else {
                    0
                }
                scrollDeltaY = if (lastScrollY != 0 && scrollY >= 0) {
                    kotlin.math.abs(scrollY - lastScrollY)
                } else {
                    0
                }
                
                // scrollX/Y도 0이면 이벤트 발생 자체를 스크롤로 간주
                // (일부 앱에서는 스크롤 위치 정보를 제공하지 않음)
                if (scrollDeltaX == 0 && scrollDeltaY == 0 && scrollX == 0 && scrollY == 0) {
                    // TYPE_VIEW_SCROLLED 이벤트가 발생했다는 것 자체가 스크롤을 의미
                    scrollDeltaX = 1  // 최소값으로 설정하여 카운트
                    scrollDeltaY = 1
                }
            }
            
            // 앱별 최소 스크롤 거리 설정 (너무 작은 움직임 무시)
            val minScrollDelta = when (packageName) {
                "com.google.android.youtube" -> 1  // 유튜브는 1픽셀 이상 (스크롤 정보가 없을 수 있음)
                "com.instagram.android" -> 1  // 인스타그램은 1픽셀 이상
                else -> 1
            }
            
            // 수직 또는 수평 스크롤이 최소 거리 이상이거나, 처음 스크롤 시작인 경우 카운트
            val isScroll = (scrollDeltaY >= minScrollDelta || scrollDeltaX >= minScrollDelta) || 
                          (lastScrollY == 0 && event.scrollY > 0) || 
                          (lastScrollX == 0 && event.scrollX > 0)
            
            if (isScroll) {
                scrollCount++
                if (event.scrollY >= 0) {
                    lastScrollY = event.scrollY
                }
                if (event.scrollX >= 0) {
                    lastScrollX = event.scrollX
                }
                lastScrollTime = currentTime
                
                // 스크롤 델타를 미터로 환산
                // 피타고라스 정리를 사용하여 실제 스크롤 거리 계산 (단순 합산 대신)
                val totalPixels = if (scrollDeltaX > 0 || scrollDeltaY > 0) {
                    kotlin.math.sqrt(
                        (scrollDeltaX * scrollDeltaX + scrollDeltaY * scrollDeltaY).toDouble()
                    ).toInt()
                } else {
                    0
                }
                val distanceMeters = if (totalPixels > 0) {
                    ConversionUtil.pixelsToMeters(totalPixels, this@AppUsageTrackingService)
                } else {
                    // 스크롤 정보가 없으면 기본값 사용 (스크롤 이벤트 발생 자체를 의미)
                    0.05  // 5cm로 간주
                }
                
                Log.d(TAG, "스크롤 거리 추가: packageName=$packageName, scrollDeltaY=$scrollDeltaY, scrollDeltaX=$scrollDeltaX, totalPixels=$totalPixels, distanceMeters=$distanceMeters")
                
                // 실시간으로 스크롤 거리 및 카운트 저장
                serviceScope.launch {
                    getRepository()?.apply {
                        addScrollDistance(distanceMeters)
                        addScrollCount(1)
                    }
                }
            } else {
                Log.d(TAG, "스크롤 거리 부족: scrollDeltaY=$scrollDeltaY, scrollDeltaX=$scrollDeltaX, minScrollDelta=$minScrollDelta")
            }
        } else {
            Log.d(TAG, "스크롤 이벤트 시간 제한: elapsed=${currentTime - lastScrollTime}, threshold=$timeThreshold")
        }
    }
    
    private fun handleGestureStart(event: AccessibilityEvent) {
        // TYPE_VIEW_SCROLLED가 발생하지 않는 앱을 위한 보조 감지
        // 유튜브의 경우 TYPE_VIEW_SCROLLED가 발생하지 않으므로 더 적극적으로 활용
        val packageName = event.packageName?.toString()
        val isYouTube = packageName == "com.google.android.youtube"
        
        Log.d(TAG, "TYPE_GESTURE_DETECTION_START: packageName=$packageName, isTarget=${isTargetPackage(packageName)}, currentPackage=$currentPackageName, isYouTube=$isYouTube")
        
        if (!isTargetPackage(packageName) || packageName != currentPackageName) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        
        // 앱별로 다른 시간 간격 설정
        val timeThreshold = when (packageName) {
            "com.google.android.youtube" -> 200  // 유튜브는 200ms 간격 (더 관대하게)
            "com.instagram.android" -> 150  // 인스타그램은 150ms 간격
            else -> 150
        }
        
        // 최근에 제스처가 발생하지 않았을 때만 처리
        if (currentTime - lastScrollTime > timeThreshold) {
            // 유튜브의 경우 스크롤 가능한 노드 체크를 완화하거나 생략
            var shouldCount = false
            
            if (isYouTube) {
                // 유튜브는 TYPE_VIEW_SCROLLED가 발생하지 않으므로 제스처 이벤트 자체를 스크롤로 간주
                // 단, 너무 빈번한 클릭을 방지하기 위해 최소 간격은 유지
                shouldCount = true
                Log.d(TAG, "유튜브: 제스처 이벤트를 스크롤로 간주")
            } else {
                // 다른 앱은 스크롤 가능한 노드가 있을 때만 카운트
                try {
                    val rootNode = rootInActiveWindow
                    if (rootNode != null) {
                        val hasScrollableNode = hasScrollableNode(rootNode)
                        rootNode.recycle()
                        
                        shouldCount = hasScrollableNode
                        Log.d(TAG, "제스처 감지: hasScrollableNode=$hasScrollableNode")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "제스처 처리 중 예외 발생", e)
                }
            }
            
            if (shouldCount) {
                scrollCount++
                lastScrollTime = currentTime
                
                // 제스처로 인한 스크롤은 정확한 거리를 알 수 없으므로 기본값 사용
                // 유튜브의 경우 평균 스와이프 거리를 사용 (화면 높이의 약 1/3 정도)
                val defaultDistanceMeters = if (isYouTube) {
                    // 유튜브는 일반적으로 더 긴 스와이프를 하므로 더 큰 값 사용
                    try {
                        val displayMetrics = resources.displayMetrics
                        val screenHeightPx = displayMetrics.heightPixels
                        val averageSwipePx = screenHeightPx / 3  // 화면 높이의 1/3
                        ConversionUtil.pixelsToMeters(averageSwipePx, this@AppUsageTrackingService)
                    } catch (e: Exception) {
                        0.2  // 기본값 20cm
                    }
                } else {
                    0.1  // 다른 앱은 10cm로 간주
                }
                
                Log.d(TAG, "제스처로 인한 스크롤 거리 추가: packageName=$packageName, distanceMeters=$defaultDistanceMeters")
                
                serviceScope.launch {
                    getRepository()?.apply {
                        addScrollDistance(defaultDistanceMeters)
                        addScrollCount(1)
                    }
                }
            } else {
                Log.d(TAG, "제스처 카운트 조건 불만족: shouldCount=$shouldCount, elapsed=${currentTime - lastScrollTime}")
            }
        } else {
            Log.d(TAG, "제스처 시간 제한: elapsed=${currentTime - lastScrollTime}, threshold=$timeThreshold")
        }
    }
    
    private fun hasScrollableNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        
        // 스크롤 가능한 뷰인지 확인
        if (node.isScrollable) {
            return true
        }
        
        // 자식 노드들을 재귀적으로 탐색
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (hasScrollableNode(child)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }
        
        return false
    }
    
    // 유튜브 전용: TYPE_WINDOW_CONTENT_CHANGED 처리
    private fun handleWindowContentChangedForYouTube(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        
        Log.d(TAG, "유튜브 TYPE_WINDOW_CONTENT_CHANGED: packageName=$packageName, currentPackage=$currentPackageName")
        
        if (!isTargetPackage(packageName) || packageName != currentPackageName) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        
        // 유튜브는 다른 이벤트가 발생하지 않을 수 있으므로
        // TYPE_WINDOW_CONTENT_CHANGED를 활용하되, 연속된 변경은 필터링
        val timeSinceLastScroll = currentTime - lastScrollTime
        
        // 최소 300ms 간격으로만 카운트 (너무 빈번한 자동 업데이트 방지)
        if (timeSinceLastScroll > 300) {
            scrollCount++
            lastScrollTime = currentTime
            
            // 콘텐츠 변경으로 인한 스크롤은 정확한 거리를 알 수 없으므로
            // 평균 스와이프 거리 사용
            val defaultDistanceMeters = try {
                val displayMetrics = resources.displayMetrics
                val screenHeightPx = displayMetrics.heightPixels
                val averageSwipePx = screenHeightPx / 3  // 화면 높이의 1/3
                ConversionUtil.pixelsToMeters(averageSwipePx, this@AppUsageTrackingService)
            } catch (e: Exception) {
                0.2  // 기본값 20cm
            }
            
            Log.d(TAG, "유튜브 콘텐츠 변경으로 인한 스크롤 거리 추가: distanceMeters=$defaultDistanceMeters")
            
            serviceScope.launch {
                getRepository()?.apply {
                    addScrollDistance(defaultDistanceMeters)
                    addScrollCount(1)
                }
            }
        } else {
            Log.d(TAG, "유튜브 콘텐츠 변경 시간 제한: elapsed=$timeSinceLastScroll")
        }
    }
    
    // 유튜브 전용: TYPE_TOUCH_INTERACTION_START 처리
    private fun handleTouchInteractionForYouTube(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        
        Log.d(TAG, "유튜브 TYPE_TOUCH_INTERACTION_START: packageName=$packageName, currentPackage=$currentPackageName")
        
        if (!isTargetPackage(packageName) || packageName != currentPackageName) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        
        // 터치 시작 이벤트는 스크롤일 수도 있고 클릭일 수도 있으므로
        // 최소 간격을 두고 처리
        val timeSinceLastScroll = currentTime - lastScrollTime
        
        // 최소 200ms 간격으로만 카운트
        if (timeSinceLastScroll > 200) {
            // 터치가 스크롤인지 클릭인지 구분하기 어려우므로
            // 스크롤 가능한 노드가 있는지 확인
            try {
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    val hasScrollableNode = hasScrollableNode(rootNode)
                    rootNode.recycle()
                    
                    if (hasScrollableNode) {
                        scrollCount++
                        lastScrollTime = currentTime
                        
                        val defaultDistanceMeters = try {
                            val displayMetrics = resources.displayMetrics
                            val screenHeightPx = displayMetrics.heightPixels
                            val averageSwipePx = screenHeightPx / 3
                            ConversionUtil.pixelsToMeters(averageSwipePx, this@AppUsageTrackingService)
                        } catch (e: Exception) {
                            0.2
                        }
                        
                        Log.d(TAG, "유튜브 터치 상호작용으로 인한 스크롤 거리 추가: distanceMeters=$defaultDistanceMeters")
                        
                        serviceScope.launch {
                            getRepository()?.apply {
                                addScrollDistance(defaultDistanceMeters)
                                addScrollCount(1)
                            }
                        }
                    } else {
                        Log.d(TAG, "유튜브 터치 상호작용: 스크롤 가능한 노드 없음 (클릭일 수 있음)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "유튜브 터치 상호작용 처리 중 예외 발생", e)
            }
        } else {
            Log.d(TAG, "유튜브 터치 상호작용 시간 제한: elapsed=$timeSinceLastScroll")
        }
    }
    
    private fun savePreviousAppUsage() {
        if (currentPackageName != null && appStartTime > 0) {
            val usageTime = System.currentTimeMillis() - appStartTime
            
            serviceScope.launch {
                getRepository()?.apply {
                    addUsageTime(usageTime)
                    addAppUsageTime(currentPackageName!!, usageTime)
                }
            }
        }
    }
    
    override fun onInterrupt() {
        savePreviousAppUsage()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        savePreviousAppUsage()
    }
    
    companion object {
        fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            
            return enabledServices.contains(context.packageName)
        }
    }
}


