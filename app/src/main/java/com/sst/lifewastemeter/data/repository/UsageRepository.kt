package com.sst.lifewastemeter.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sst.lifewastemeter.data.model.AppUsageData
import com.sst.lifewastemeter.data.model.DailyUsageData
import com.sst.lifewastemeter.data.model.DisplayMode
import com.sst.lifewastemeter.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "usage_data")

class UsageRepository(private val context: Context) {
    
    private val nicknameKey = stringPreferencesKey("nickname")
    private val selectedAppsKey = stringSetPreferencesKey("selected_apps")
    private val currentModeKey = stringPreferencesKey("current_mode")
    private val isFirstLaunchKey = booleanPreferencesKey("is_first_launch")
    private val currentTrackingAppKey = stringPreferencesKey("current_tracking_app")
    
    // Daily usage data keys
    private fun getDailyScrollKey(date: Long) = longPreferencesKey("daily_scroll_$date")
    private fun getDailyScrollDistanceKey(date: Long) = stringPreferencesKey("daily_scroll_distance_$date")  // Double을 String으로 저장
    private fun getDailyTimeKey(date: Long) = longPreferencesKey("daily_time_$date")
    
    val userSettings: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            nickname = preferences[nicknameKey] ?: "",
            selectedApps = preferences[selectedAppsKey] ?: setOf(
                "com.google.android.youtube",
                "com.instagram.android",
                "com.zhiliaoapp.musically"
            ),
            currentMode = DisplayMode.valueOf(
                preferences[currentModeKey] ?: DisplayMode.CLIMBING.name
            ),
            isFirstLaunch = preferences[isFirstLaunchKey] ?: true
        )
    }
    
    suspend fun updateNickname(nickname: String) {
        context.dataStore.edit { preferences ->
            preferences[nicknameKey] = nickname
        }
    }
    
    suspend fun updateSelectedApps(apps: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[selectedAppsKey] = apps
        }
    }
    
    suspend fun updateDisplayMode(mode: DisplayMode) {
        context.dataStore.edit { preferences ->
            preferences[currentModeKey] = mode.name
        }
    }
    
    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { preferences ->
            preferences[isFirstLaunchKey] = false
        }
    }
    
    suspend fun setCurrentTrackingApp(packageName: String?) {
        context.dataStore.edit { preferences ->
            preferences[currentTrackingAppKey] = packageName ?: ""
        }
    }
    
    fun getCurrentTrackingApp(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[currentTrackingAppKey] ?: ""
    }
    
    suspend fun addScrollCount(count: Int) {
        val today = getTodayTimestamp()
        context.dataStore.edit { preferences ->
            val currentCount = preferences[getDailyScrollKey(today)] ?: 0L
            preferences[getDailyScrollKey(today)] = currentCount + count
        }
    }
    
    suspend fun addScrollDistance(distanceMeters: Double) {
        val today = getTodayTimestamp()
        context.dataStore.edit { preferences ->
            val currentDistance = preferences[getDailyScrollDistanceKey(today)]?.toDoubleOrNull() ?: 0.0
            preferences[getDailyScrollDistanceKey(today)] = (currentDistance + distanceMeters).toString()
        }
    }
    
    suspend fun addUsageTime(timeMillis: Long) {
        val today = getTodayTimestamp()
        context.dataStore.edit { preferences ->
            val currentTime = preferences[getDailyTimeKey(today)] ?: 0L
            preferences[getDailyTimeKey(today)] = currentTime + timeMillis
        }
    }
    
    fun getTodayUsage(): Flow<DailyUsageData> = context.dataStore.data.map { preferences ->
        val today = getTodayTimestamp()
        DailyUsageData(
            date = today,
            totalScrollCount = (preferences[getDailyScrollKey(today)] ?: 0L).toInt(),
            totalScrollDistanceMeters = preferences[getDailyScrollDistanceKey(today)]?.toDoubleOrNull() ?: 0.0,
            totalUsageTimeMillis = preferences[getDailyTimeKey(today)] ?: 0L
        )
    }
    
    suspend fun getSelectedApps(): Set<String> {
        return context.dataStore.data.first().let { preferences ->
            preferences[selectedAppsKey] ?: setOf(
                "com.google.android.youtube",
                "com.instagram.android",
                "com.zhiliaoapp.musically"
            )
        }
    }
    
    private fun getTodayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}


