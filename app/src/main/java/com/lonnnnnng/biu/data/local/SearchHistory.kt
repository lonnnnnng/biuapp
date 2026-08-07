package com.lonnnnnng.biu.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.searchHistoryDataStore by preferencesDataStore(name = "search_history")

object SearchHistoryPolicy {
    fun record(current: List<String>, keyword: String, limit: Int = DEFAULT_LIMIT): List<String> {
        val normalized = keyword.trim()
        if (normalized.isEmpty()) return current.take(limit.coerceAtLeast(0))
        return buildList {
            add(normalized)
            current.filterNot { existing -> existing.equals(normalized, ignoreCase = true) }
                .forEach(::add)
        }.take(limit.coerceAtLeast(0))
    }

    internal fun encode(history: List<String>): String = JSONArray(history).toString()

    internal fun decode(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                repeat(array.length()) { index ->
                    array.optString(index).trim().takeIf(String::isNotEmpty)?.let(::add)
                }
            }.distinct().take(DEFAULT_LIMIT)
        }.getOrDefault(emptyList())
    }

    private const val DEFAULT_LIMIT = 10
}

class SearchHistoryRepository(context: Context) {
    private val dataStore = context.applicationContext.searchHistoryDataStore

    val history: Flow<List<String>> = dataStore.data.map { values ->
        SearchHistoryPolicy.decode(values[KEY_HISTORY])
    }

    suspend fun record(keyword: String) {
        dataStore.edit { values ->
            val updated = SearchHistoryPolicy.record(
                current = SearchHistoryPolicy.decode(values[KEY_HISTORY]),
                keyword = keyword,
            )
            values[KEY_HISTORY] = SearchHistoryPolicy.encode(updated)
        }
    }

    suspend fun clear() {
        dataStore.edit { values -> values.remove(KEY_HISTORY) }
    }

    private companion object {
        val KEY_HISTORY = stringPreferencesKey("keywords")
    }
}
