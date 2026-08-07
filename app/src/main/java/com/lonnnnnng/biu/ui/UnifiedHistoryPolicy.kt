package com.lonnnnnng.biu.ui

import com.lonnnnnng.biu.data.bilibili.AccountLibrarySection
import com.lonnnnnng.biu.data.bilibili.BilibiliLibraryVideo
import com.lonnnnnng.biu.data.local.PlaybackHistoryEntity

internal fun AccountLibrarySection.normalizedLibrarySection(): AccountLibrarySection = when (this) {
    // long: 旧版页面可能仍持有拆分后的在线/本地历史状态；统一映射后既避免导航找不到分组，也保证升级后进入同一个历史页面。
    AccountLibrarySection.ONLINE_HISTORY,
    AccountLibrarySection.LOCAL_HISTORY,
    -> AccountLibrarySection.HISTORY
    else -> this
}

internal enum class HistorySourceFilter(val label: String) {
    ALL("全部"),
    ONLINE("在线"),
    LOCAL("本地"),
}

internal sealed interface UnifiedHistoryItem {
    val key: String
    val timestampEpochMs: Long

    data class Online(val value: BilibiliLibraryVideo) : UnifiedHistoryItem {
        override val key: String = value.historyKey
            ?: "online:${value.video.bvid}:${value.savedAtEpochSeconds ?: 0L}"
        override val timestampEpochMs: Long = (value.savedAtEpochSeconds ?: 0L) * 1_000L
    }

    data class Local(val value: PlaybackHistoryEntity) : UnifiedHistoryItem {
        override val key: String = "local:${value.mediaId}"
        override val timestampEpochMs: Long = value.playedAtEpochMs
    }
}

internal object UnifiedHistoryPolicy {
    fun merge(
        online: List<BilibiliLibraryVideo>,
        local: List<PlaybackHistoryEntity>,
        source: HistorySourceFilter,
        query: String,
    ): List<UnifiedHistoryItem> {
        val normalizedQuery = query.trim()
        val items = buildList {
            if (source != HistorySourceFilter.LOCAL) {
                online.distinctBy { item -> item.historyKey ?: item.video.bvid }
                    .mapTo(this, UnifiedHistoryItem::Online)
            }
            if (source != HistorySourceFilter.ONLINE) {
                local.mapTo(this, UnifiedHistoryItem::Local)
            }
        }
        return items
            .filter { item -> matches(item, normalizedQuery) }
            .sortedWith(
                compareByDescending<UnifiedHistoryItem> { it.timestampEpochMs }
                    .thenBy { it.key },
            )
    }

    private fun matches(item: UnifiedHistoryItem, query: String): Boolean {
        if (query.isBlank()) return true
        val fields = when (item) {
            is UnifiedHistoryItem.Online -> listOf(item.value.video.title, item.value.video.author)
            is UnifiedHistoryItem.Local -> listOf(item.value.title, item.value.artist)
        }
        return fields.any { field -> field.contains(query, ignoreCase = true) }
    }
}
