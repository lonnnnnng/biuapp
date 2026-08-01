package com.lonnnnnng.biu.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.displayPreferencesDataStore by preferencesDataStore(name = "display_preferences")

enum class AppListDensity(val label: String) {
    STANDARD("标准"),
    COMPACT("紧凑"),
    ;

    companion object {
        fun fromStoredValue(value: String?): AppListDensity {
            // long: 首次安装、旧版升级和损坏值都回退到标准密度，保证三行媒体信息仍完整可读。
            return entries.firstOrNull { density -> density.name == value } ?: STANDARD
        }
    }
}

enum class AppTextScale(val label: String, val multiplier: Float) {
    SMALL("小号", 0.9f),
    STANDARD("标准", 1f),
    LARGE("大号", 1.12f),
    ;

    companion object {
        fun fromStoredValue(value: String?): AppTextScale {
            // long: 未知倍率不能影响 MaterialTheme 的排版计算，统一回退为标准字号。
            return entries.firstOrNull { scale -> scale.name == value } ?: STANDARD
        }
    }
}

enum class AppVideoLayout(val label: String) {
    LIST("列表"),
    GRID("网格"),
    ;

    companion object {
        fun fromStoredValue(value: String?): AppVideoLayout {
            // long: 网格是可选浏览方式，旧版与异常持久化值保持原有列表体验，避免推荐页布局意外变化。
            return entries.firstOrNull { layout -> layout.name == value } ?: LIST
        }
    }
}

data class AppDisplayPreferences(
    val listDensity: AppListDensity,
    val textScale: AppTextScale,
    val videoLayout: AppVideoLayout,
)

class DisplayPreferenceRepository(context: Context) {
    private val dataStore = context.applicationContext.displayPreferencesDataStore

    val preferences: Flow<AppDisplayPreferences> = dataStore.data.map { values ->
        AppDisplayPreferences(
            listDensity = AppListDensity.fromStoredValue(values[KEY_LIST_DENSITY]),
            textScale = AppTextScale.fromStoredValue(values[KEY_TEXT_SCALE]),
            videoLayout = AppVideoLayout.fromStoredValue(values[KEY_VIDEO_LAYOUT]),
        )
    }

    suspend fun saveListDensity(density: AppListDensity) {
        dataStore.edit { values -> values[KEY_LIST_DENSITY] = density.name }
    }

    suspend fun saveTextScale(scale: AppTextScale) {
        dataStore.edit { values -> values[KEY_TEXT_SCALE] = scale.name }
    }

    suspend fun saveVideoLayout(layout: AppVideoLayout) {
        dataStore.edit { values -> values[KEY_VIDEO_LAYOUT] = layout.name }
    }

    private companion object {
        val KEY_LIST_DENSITY = stringPreferencesKey("list_density")
        val KEY_TEXT_SCALE = stringPreferencesKey("text_scale")
        val KEY_VIDEO_LAYOUT = stringPreferencesKey("video_layout")
    }
}
