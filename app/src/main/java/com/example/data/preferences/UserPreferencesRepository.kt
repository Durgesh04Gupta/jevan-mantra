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
    val notificationMinute: Int = 30,
    val isManualLoggedIn: Boolean = false,
    val manualUserId: String = "",
    val manualFullName: String = "",
    val manualEmailOrMobile: String = "",
    val manualPassword: String = "",
    val manualBirthDate: String = "1998-05-14",
    val manualBirthTime: String = "10:30 AM",
    val manualBirthPlace: String = "Varanasi, UP (25.32°N, 83.00°E)"
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val SELECTED_RASHI_ID = stringPreferencesKey("selected_rashi_id")
        val DAILY_NOTIFICATION_ENABLED = booleanPreferencesKey("daily_notification_enabled")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")

        val IS_MANUAL_LOGGED_IN = booleanPreferencesKey("is_manual_logged_in")
        val MANUAL_USER_ID = stringPreferencesKey("manual_user_id")
        val MANUAL_FULL_NAME = stringPreferencesKey("manual_full_name")
        val MANUAL_EMAIL_OR_MOBILE = stringPreferencesKey("manual_email_or_mobile")
        val MANUAL_PASSWORD = stringPreferencesKey("manual_password")
        val MANUAL_BIRTH_DATE = stringPreferencesKey("manual_birth_date")
        val MANUAL_BIRTH_TIME = stringPreferencesKey("manual_birth_time")
        val MANUAL_BIRTH_PLACE = stringPreferencesKey("manual_birth_place")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: true,
            selectedRashiId = prefs[Keys.SELECTED_RASHI_ID] ?: "mesha",
            dailyNotificationEnabled = prefs[Keys.DAILY_NOTIFICATION_ENABLED] ?: true,
            notificationHour = prefs[Keys.NOTIFICATION_HOUR] ?: 7,
            notificationMinute = prefs[Keys.NOTIFICATION_MINUTE] ?: 30,
            isManualLoggedIn = prefs[Keys.IS_MANUAL_LOGGED_IN] ?: false,
            manualUserId = prefs[Keys.MANUAL_USER_ID] ?: "",
            manualFullName = prefs[Keys.MANUAL_FULL_NAME] ?: "",
            manualEmailOrMobile = prefs[Keys.MANUAL_EMAIL_OR_MOBILE] ?: "",
            manualPassword = prefs[Keys.MANUAL_PASSWORD] ?: "",
            manualBirthDate = prefs[Keys.MANUAL_BIRTH_DATE] ?: "1998-05-14",
            manualBirthTime = prefs[Keys.MANUAL_BIRTH_TIME] ?: "10:30 AM",
            manualBirthPlace = prefs[Keys.MANUAL_BIRTH_PLACE] ?: "Varanasi, UP (25.32°N, 83.00°E)"
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

    suspend fun saveManualAccount(
        userId: String,
        fullName: String,
        emailOrMobile: String,
        password: String,
        birthDate: String,
        birthTime: String,
        birthPlace: String,
        selectedRashiId: String? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_MANUAL_LOGGED_IN] = true
            prefs[Keys.MANUAL_USER_ID] = userId
            prefs[Keys.MANUAL_FULL_NAME] = fullName
            prefs[Keys.MANUAL_EMAIL_OR_MOBILE] = emailOrMobile
            prefs[Keys.MANUAL_PASSWORD] = password
            prefs[Keys.MANUAL_BIRTH_DATE] = birthDate
            prefs[Keys.MANUAL_BIRTH_TIME] = birthTime
            prefs[Keys.MANUAL_BIRTH_PLACE] = birthPlace
            if (!selectedRashiId.isNullOrBlank()) {
                prefs[Keys.SELECTED_RASHI_ID] = selectedRashiId
            }
        }
    }

    suspend fun setManualLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_MANUAL_LOGGED_IN] = loggedIn
        }
    }
}
