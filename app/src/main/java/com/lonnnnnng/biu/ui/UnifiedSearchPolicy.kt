package com.lonnnnnng.biu.ui

import com.lonnnnnng.biu.data.bilibili.BilibiliCreatorCollection
import com.lonnnnnng.biu.data.local.LocalPlaylistEntity

enum class UnifiedSearchType(val label: String) {
    VIDEOS("视频"),
    CREATORS("UP 主"),
    COLLECTIONS("合集"),
    PLAYLISTS("歌单"),
}

internal object UnifiedSearchPolicy {
    fun filterCollections(
        collections: List<BilibiliCreatorCollection>,
        keyword: String,
    ): List<BilibiliCreatorCollection> {
        val normalized = keyword.trim()
        return collections
            .distinctBy { collection -> collection.type to collection.id }
            .filter { collection ->
                normalized.isEmpty() ||
                    collection.title.contains(normalized, ignoreCase = true) ||
                    collection.ownerName.contains(normalized, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<BilibiliCreatorCollection> { it.publishedAtEpochSeconds ?: Long.MIN_VALUE }
                    .thenBy { it.title },
            )
    }

    fun filterPlaylists(
        playlists: List<LocalPlaylistEntity>,
        keyword: String,
    ): List<LocalPlaylistEntity> {
        val normalized = keyword.trim()
        return playlists
            .filter { playlist -> normalized.isEmpty() || playlist.name.contains(normalized, ignoreCase = true) }
            .sortedWith(compareByDescending<LocalPlaylistEntity>(LocalPlaylistEntity::updatedAtEpochMs).thenBy { it.name })
    }
}
