package com.lonnnnnng.biu.ui

internal enum class LyricsEntryDestination {
    WAIT_FOR_CACHE,
    SHOW_LYRICS,
    SHOW_SEARCH,
}

internal object LyricsEntryPolicy {
    fun destination(
        expectedCacheKey: String,
        state: LyricsUiState,
    ): LyricsEntryDestination {
        if (state.cacheKey != expectedCacheKey || state.status == LyricsLoadStatus.LOADING) {
            return LyricsEntryDestination.WAIT_FOR_CACHE
        }
        // long: 只有当前曲目明确命中有效缓存才一步进入歌词，其余终态都回到用户可编辑关键词的手动搜索入口。
        return if (state.status == LyricsLoadStatus.LOADED && state.document != null) {
            LyricsEntryDestination.SHOW_LYRICS
        } else {
            LyricsEntryDestination.SHOW_SEARCH
        }
    }
}
