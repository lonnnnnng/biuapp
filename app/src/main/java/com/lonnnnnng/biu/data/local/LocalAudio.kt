package com.lonnnnnng.biu.data.local

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.lonnnnnng.biu.core.model.Track
import com.lonnnnnng.biu.download.AudioDownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LocalAudio(
    val mediaStoreId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val artworkUri: String? = null,
)

internal fun LocalAudio.toTrack(): Track {
    return Track(
        id = "local:$mediaStoreId",
        title = title,
        artist = artist,
        streamUrl = contentUri,
        artworkUrl = artworkUri,
        qualityLabel = "本地音频",
    )
}

internal object LocalAudioDownloadMetadataPolicy {
    fun apply(
        audio: List<LocalAudio>,
        downloads: List<AudioDownloadTaskEntity>,
    ): List<LocalAudio> {
        val metadataByMediaStoreId = downloads.mapNotNull { task ->
            if (task.downloadStatus != AudioDownloadStatus.COMPLETED) return@mapNotNull null
            val mediaStoreId = task.publishedUri
                ?.substringAfterLast('/', missingDelimiterValue = "")
                ?.toLongOrNull()
                ?: return@mapNotNull null
            mediaStoreId to task
        }.toMap()
        if (metadataByMediaStoreId.isEmpty()) return audio
        return audio.map { item ->
            val task = metadataByMediaStoreId[item.mediaStoreId] ?: return@map item
            // long: MediaProvider 会重新扫描无标签的 DASH 音频并覆盖 TITLE/ARTIST；已完成任务以发布后的 MediaStore ID 回填在线元数据。
            item.copy(
                title = task.title,
                artist = task.artist,
                artworkUri = item.artworkUri ?: task.artworkUrl,
            )
        }
    }
}

class LocalAudioRepository(context: Context) {
    private val contentResolver = context.applicationContext.contentResolver

    suspend fun audioTracks(directory: LocalAudioDirectory? = null): List<LocalAudio> = withContext(Dispatchers.IO) {
        val directoryFilter = directory?.let { LocalAudioMediaStorePolicy.filterFor(it) }
        val collection = directoryFilter?.let { filter ->
            MediaStore.Audio.Media.getContentUri(filter.volumeName)
        } ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        val selectionParts = mutableListOf(
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            "${MediaStore.Audio.Media.DURATION} > 0",
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selectionParts += "${MediaStore.Audio.Media.IS_PENDING} = 0"
        }
        directoryFilter?.selection?.let(selectionParts::add)
        val result = mutableListOf<LocalAudio>()
        contentResolver.query(
            collection,
            projection,
            selectionParts.joinToString(" AND "),
            directoryFilter?.selectionArgs?.toTypedArray()?.takeIf { it.isNotEmpty() },
            "${MediaStore.Audio.Media.DATE_ADDED} DESC, ${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val mediaStoreId = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)
                // long: 播放 URI 由 MediaStore ID 生成，不读取已被分区存储隐藏的裸文件路径。
                result += LocalAudio(
                    mediaStoreId = mediaStoreId,
                    title = LocalAudioMetadataPolicy.title(
                        title = cursor.getString(titleColumn).orEmpty(),
                        displayName = cursor.getString(displayNameColumn).orEmpty(),
                    ),
                    artist = LocalAudioMetadataPolicy.artist(cursor.getString(artistColumn).orEmpty()),
                    album = LocalAudioMetadataPolicy.album(cursor.getString(albumColumn).orEmpty()),
                    durationMs = cursor.getLong(durationColumn).coerceAtLeast(0L),
                    contentUri = ContentUris.withAppendedId(collection, mediaStoreId).toString(),
                    artworkUri = albumId.takeIf { it > 0L }?.let { id ->
                        ContentUris.withAppendedId(ALBUM_ART_URI, id).toString()
                    },
                )
            }
        }
        result
    }

    private companion object {
        val ALBUM_ART_URI = android.net.Uri.parse("content://media/external/audio/albumart")
    }
}

internal object LocalAudioMetadataPolicy {
    fun title(title: String, displayName: String): String {
        return title.trim().takeIf(String::isNotBlank)
            ?: displayName.substringBeforeLast('.', missingDelimiterValue = displayName).trim()
                .ifBlank { "未知音频" }
    }

    fun artist(artist: String): String {
        return artist.trim().takeUnless { it.isBlank() || it == MediaStore.UNKNOWN_STRING } ?: "未知艺术家"
    }

    fun album(album: String): String {
        return album.trim().takeUnless { it.isBlank() || it == MediaStore.UNKNOWN_STRING }.orEmpty()
    }
}

object LocalMediaPermissionPolicy {
    fun permissionFor(sdkInt: Int = Build.VERSION.SDK_INT): String {
        return if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}
