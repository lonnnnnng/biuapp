package com.lonnnnnng.biu.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themePreferencesDataStore by preferencesDataStore(name = "theme_preferences")

enum class AppThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("亮色"),
    DARK("暗色"),
    ;

    companion object {
        fun fromStoredValue(value: String?): AppThemeMode {
            // long: 旧版本没有主题字段，损坏或未来版本的未知值也必须回退到可用的系统主题。
            return entries.firstOrNull { mode -> mode.name == value } ?: SYSTEM
        }
    }
}

class ThemePreferenceRepository(context: Context) {
    private val dataStore = context.applicationContext.themePreferencesDataStore

    val mode: Flow<AppThemeMode> = dataStore.data.map { values ->
        AppThemeMode.fromStoredValue(values[KEY_MODE])
    }

    suspend fun save(mode: AppThemeMode) {
        dataStore.edit { values -> values[KEY_MODE] = mode.name }
    }

    private companion object {
        val KEY_MODE = stringPreferencesKey("theme_mode")
    }
}
