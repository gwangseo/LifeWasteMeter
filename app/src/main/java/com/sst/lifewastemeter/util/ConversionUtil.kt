package com.sst.lifewastemeter.util

import android.content.Context
import android.util.DisplayMetrics
import com.sst.lifewastemeter.data.model.DisplayMode

object ConversionUtil {
    // 픽셀을 미터로 환산하는 함수
    // 픽셀 / DPI * 0.0254 = 미터 (1인치 = 0.0254미터)
    fun pixelsToMeters(pixels: Int, context: Context): Double {
        val displayMetrics = context.resources.displayMetrics
        // xdpi와 ydpi의 평균을 사용 (스크롤은 주로 수직이지만 수평도 고려)
        val dpi = (displayMetrics.xdpi + displayMetrics.ydpi) / 2.0
        // DPI가 0이거나 이상한 값이면 기본값 사용
        val validDpi = if (dpi > 0) dpi else 160.0
        return (pixels / validDpi) * 0.0254
    }
    
    // 스크롤 1회 = 15cm = 0.15m (레거시 호환용, 이제는 사용하지 않음)
    private const val SCROLL_DISTANCE_METERS = 0.15
    
    // 휴지 1칸 = 11.4cm = 0.114m
    private const val TOILET_PAPER_LENGTH_METERS = 0.114
    
    // 에베레스트 높이: 8,848m
    private const val EVEREST_HEIGHT = 8848.0
    
    // 서울-부산 거리: 약 325km = 325,000m
    private const val SEOUL_BUSAN_DISTANCE = 325000.0
    
    // 63빌딩 높이: 264m
    private const val BUILDING_63_HEIGHT = 264.0
    
    // 미터 단위 거리를 받아서 계산
    fun calculateDistance(distanceMeters: Double): Double {
        return distanceMeters
    }
    
    // 레거시 호환용 (스크롤 횟수 기반)
    fun calculateDistanceFromCount(scrollCount: Int): Double {
        return scrollCount * SCROLL_DISTANCE_METERS
    }
    
    fun calculateToiletPaperSheets(distanceMeters: Double): Int {
        return (distanceMeters / TOILET_PAPER_LENGTH_METERS).toInt()
    }
    
    fun getClimbingMessage(distanceMeters: Double): String {
        val distance = calculateDistance(distanceMeters)
        
        return when {
            distance >= EVEREST_HEIGHT -> {
                val times = (distance / EVEREST_HEIGHT).toInt()
                "오늘 당신은 에베레스트 산(${times}회) 높이만큼 스크롤했습니다!"
            }
            distance >= BUILDING_63_HEIGHT -> {
                val times = (distance / BUILDING_63_HEIGHT).toInt()
                "오늘 당신은 63빌딩(${times}회) 높이만큼 스크롤했습니다!"
            }
            distance >= SEOUL_BUSAN_DISTANCE -> {
                "지금까지 엄지손가락으로 서울에서 부산까지 걸어갔습니다!"
            }
            distance >= 1000 -> {
                "오늘 ${String.format("%.1f", distance / 1000)}km를 스크롤했습니다!"
            }
            else -> {
                "오늘 ${String.format("%.1f", distance)}m를 스크롤했습니다!"
            }
        }
    }
    
    fun getToiletPaperMessage(distanceMeters: Double): String {
        val sheets = calculateToiletPaperSheets(distanceMeters)
        val trees = sheets / 6480.0 // 대략 나무 1그루당 6480칸
        
        return when {
            trees >= 1 -> {
                "오늘 낭비한 휴지: ${sheets}칸\n(이 정도면 나무 ${String.format("%.1f", trees)}그루가 사라진 셈입니다)"
            }
            else -> {
                "오늘 낭비한 휴지: ${sheets}칸"
            }
        }
    }
    
    fun getRandomFactMessage(distanceMeters: Double, mode: DisplayMode): String {
        val facts = when (mode) {
            DisplayMode.CLIMBING -> listOf(
                "이 정도면 하루 종일 계단을 오르내린 셈입니다.",
                "당신의 엄지손가락은 오늘 하루 종일 운동했습니다.",
                "이 스크롤로는 작은 산 하나는 오를 수 있습니다.",
                "스크롤한 거리만큼 걷는다면 하루 운동량은 충분합니다."
            )
            DisplayMode.TOILET_PAPER -> listOf(
                "이 휴지로는 작은 나무 한 그루가 필요합니다.",
                "환경을 생각한다면 오늘 하루는 충분합니다.",
                "이 정도면 한 달치 휴지를 하루에 쓴 셈입니다.",
                "지구를 위해 조금만 덜 스크롤해보세요."
            )
        }
        return facts.random()
    }
}





