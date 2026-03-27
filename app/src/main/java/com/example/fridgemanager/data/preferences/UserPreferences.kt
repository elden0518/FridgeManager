package com.example.fridgemanager.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

data class ReminderSettings(
    val enabled: Boolean = true,
    val daysBefore: Int = 7
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val REMINDER_DAYS = intPreferencesKey("reminder_days")
        private val AI_API_KEY = stringPreferencesKey("ai_api_key")
    }

    val reminderSettings: Flow<ReminderSettings> = context.dataStore.data.map { prefs ->
        ReminderSettings(
            enabled = prefs[REMINDER_ENABLED] ?: true,
            daysBefore = prefs[REMINDER_DAYS] ?: 7
        )
    }

    val aiApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[AI_API_KEY]?.takeIf { it.isNotBlank() }
            ?: com.example.fridgemanager.BuildConfig.DASHSCOPE_API_KEY
    }

    suspend fun updateReminderSettings(settings: ReminderSettings) {
        context.dataStore.edit { prefs ->
            prefs[REMINDER_ENABLED] = settings.enabled
            prefs[REMINDER_DAYS] = settings.daysBefore
        }
    }

    suspend fun updateAiApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[AI_API_KEY] = key
        }
    }
}
