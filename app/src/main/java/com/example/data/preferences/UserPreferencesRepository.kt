package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jevanmantra_prefs")

data class UserPreferences(
    val isDarkMode: Boolean = true,
    val selectedRashiId: String = "mesha",
    val dailyNotificationEnabled: Boolean = true,
    val notificationHour: Int = 7,
    val notificationMinute: Int = 30
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val SELECTED_RASHI_ID = stringPreferencesKey("selected_rashi_id")
        val DAILY_NOTIFICATION_ENABLED = booleanPreferencesKey("daily_notification_enabled")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: true,
            selectedRashiId = prefs[Keys.SELECTED_RASHI_ID] ?: "mesha",
            dailyNotificationEnabled = prefs[Keys.DAILY_NOTIFICATION_ENABLED] ?: true,
            notificationHour = prefs[Keys.NOTIFICATION_HOUR] ?: 7,
            notificationMinute = prefs[Keys.NOTIFICATION_MINUTE] ?: 30
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_DARK_MODE] = enabled
        }
    }

    suspend fun setSelectedRashi(rashiId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_RASHI_ID] = rashiId
        }
    }

    suspend fun setNotificationSchedule(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_NOTIFICATION_ENABLED] = enabled
            prefs[Keys.NOTIFICATION_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.NOTIFICATION_MINUTE] = minute.coerceIn(0, 59)
        }
    }
}
