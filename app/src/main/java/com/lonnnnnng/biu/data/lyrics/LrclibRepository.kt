package com.lonnnnnng.biu.data.lyrics

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class LyricsRateLimitedException(
    val retryAfterSeconds: Long?,
) : IOException("歌词服务请求过于频繁")

class LrclibRepository(
    private val client: OkHttpClient,
    private val apiBase: HttpUrl = LRCLIB_API_BASE.toHttpUrl(),
    private val userAgent: String = DEFAULT_USER_AGENT,
) {
    private val requestMutex = Mutex()

    suspend fun search(query: String): List<LyricsSearchResult> {
        val normalizedQuery = query.trim()
        require(normalizedQuery.isNotBlank()) { "歌词搜索关键词不能为空" }
        // long: LRCLIB 明确要求客户端串行请求；在数据源边界加锁可覆盖快速重复点击和切歌并发，避免触发服务端限流。
        return requestMutex.withLock {
            withContext(Dispatchers.IO) {
                val url = apiBase.newBuilder()
                    .addPathSegments("api/search")
                    .addQueryParameter("q", normalizedQuery)
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.code == 429) {
                        throw LyricsRateLimitedException(
                            response.header("Retry-After")?.trim()?.toLongOrNull(),
                        )
                    }
                    if (!response.isSuccessful) {
                        throw IOException("LRCLIB HTTP ${response.code}")
                    }
                    val payload = response.body?.string().orEmpty()
                    val root = runCatching { JSONArray(payload) }
                        .getOrElse { error -> throw IOException("LRCLIB response is not JSON", error) }
                    buildList {
                        for (index in 0 until root.length()) {
                            val item = root.optJSONObject(index) ?: continue
                            val syncedLyrics = item.optString("syncedLyrics")
                                .takeIf(String::isNotBlank)
                                ?: continue
                            add(
                                LyricsSearchResult(
                                    id = item.optLong("id"),
                                    trackName = item.optString("trackName"),
                                    artistName = item.optString("artistName"),
                                    albumName = item.optString("albumName").takeIf(String::isNotBlank),
                                    durationSeconds = item.optDouble("duration", 0.0).toInt().coerceAtLeast(0),
                                    syncedLyrics = syncedLyrics,
                                ),
                            )
                        }
                    }.take(MAX_RESULTS)
                }
            }
        }
    }

    private companion object {
        const val LRCLIB_API_BASE = "https://lrclib.net/"
        const val DEFAULT_USER_AGENT = "BiuAndroid/0.1.10 (https://github.com/lonnnnnng/biuapp)"
        const val MAX_RESULTS = 20
    }
}
