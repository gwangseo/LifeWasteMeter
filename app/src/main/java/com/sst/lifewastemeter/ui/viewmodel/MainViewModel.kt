package com.sst.lifewastemeter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sst.lifewastemeter.data.model.DisplayMode
import com.sst.lifewastemeter.data.model.DailyUsageData
import com.sst.lifewastemeter.data.model.UserSettings
import com.sst.lifewastemeter.data.repository.UsageRepository
import com.sst.lifewastemeter.util.ConversionUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: UsageRepository) : ViewModel() {
    
    val userSettings: StateFlow<UserSettings> = repository.userSettings.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
    )
    
    val todayUsage: StateFlow<DailyUsageData> = repository.getTodayUsage().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = DailyUsageData(date = System.currentTimeMillis())
    )
    
    val currentTrackingApp: StateFlow<String> = repository.getCurrentTrackingApp().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            combine(userSettings, todayUsage, currentTrackingApp) { settings, usage, trackingApp ->
                MainUiState(
                    scrollCount = usage.totalScrollCount,  // 레거시 호환용
                    scrollDistanceMeters = usage.totalScrollDistanceMeters,  // 실제 스크롤 거리
                    usageTimeMillis = usage.totalUsageTimeMillis,
                    displayMode = settings.currentMode,
                    message = when (settings.currentMode) {
                        DisplayMode.CLIMBING -> ConversionUtil.getClimbingMessage(usage.totalScrollDistanceMeters)
                        DisplayMode.TOILET_PAPER -> ConversionUtil.getToiletPaperMessage(usage.totalScrollDistanceMeters)
                    },
                    factMessage = ConversionUtil.getRandomFactMessage(usage.totalScrollDistanceMeters, settings.currentMode),
                    currentTrackingApp = trackingApp
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    fun updateDisplayMode(mode: DisplayMode) {
        viewModelScope.launch {
            repository.updateDisplayMode(mode)
        }
    }
    
    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            repository.updateNickname(nickname)
        }
    }
    
    fun updateSelectedApps(apps: Set<String>) {
        viewModelScope.launch {
            repository.updateSelectedApps(apps)
        }
    }
    
    fun setFirstLaunchComplete() {
        viewModelScope.launch {
            repository.setFirstLaunchComplete()
        }
    }
}

data class MainUiState(
    val scrollCount: Int = 0,  // 레거시 호환용
    val scrollDistanceMeters: Double = 0.0,  // 실제 스크롤 거리 (미터)
    val usageTimeMillis: Long = 0,
    val displayMode: DisplayMode = DisplayMode.CLIMBING,
    val message: String = "",
    val factMessage: String = "",
    val currentTrackingApp: String = ""
)

