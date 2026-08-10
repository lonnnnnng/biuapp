@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.lonnnnnng.biu.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.PlaybackPendingIntentBuilder
import com.lonnnnnng.biu.MainActivity
import com.lonnnnnng.biu.R
import com.lonnnnnng.biu.playback.PlaybackService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class PlaybackWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId -> render(context, manager, widgetId, latestSnapshot) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        render(context, manager, appWidgetId, latestSnapshot)
    }

    companion object {
        private const val EXPANDED_MIN_WIDTH_DP = 300
        private const val REQUEST_OPEN_APP = 9400
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val artworkCache = ConcurrentHashMap<String, Bitmap>()
        private val artworkLoads = ConcurrentHashMap.newKeySet<String>()
        // long: 封面域名不需要账号态，使用无 Cookie 的独立客户端，避免把 Bilibili 登录凭据带到图片请求。
        private val artworkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        @Volatile
        private var latestSnapshot = PlaybackWidgetSnapshot.empty()

        fun updateAll(context: Context, player: Player?) {
            val snapshot = PlaybackWidgetSnapshot.from(player)
            latestSnapshot = snapshot
            val applicationContext = context.applicationContext
            renderAll(applicationContext, snapshot)
            loadArtworkIfNeeded(applicationContext, snapshot)
        }

        private fun renderAll(context: Context, snapshot: PlaybackWidgetSnapshot) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, PlaybackWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            ids.forEach { widgetId -> render(context, manager, widgetId, snapshot) }
        }

        private fun render(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            snapshot: PlaybackWidgetSnapshot,
        ) {
            val expanded = manager.getAppWidgetOptions(widgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) >= EXPANDED_MIN_WIDTH_DP
            val layoutId = if (expanded) R.layout.widget_playback_expanded else R.layout.widget_playback_compact
            val views = RemoteViews(context.packageName, layoutId).apply {
                setTextViewText(R.id.widget_title, snapshot.title)
                setTextViewText(R.id.widget_artist, snapshot.artist)
                setTextViewText(R.id.widget_context, snapshot.contextLabel)
                setViewVisibility(R.id.widget_context, if (expanded && snapshot.contextLabel.isNotBlank()) View.VISIBLE else View.GONE)
                setProgressBar(R.id.widget_progress, 1_000, snapshot.progressPermille, false)
                setImageViewResource(
                    R.id.widget_play_pause,
                    if (snapshot.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                )
                val artwork = snapshot.artworkUrl?.let(artworkCache::get)
                if (artwork != null) {
                    setImageViewBitmap(R.id.widget_artwork, artwork)
                } else {
                    setImageViewResource(R.id.widget_artwork, R.drawable.ic_widget_music)
                }
                setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
                setOnClickPendingIntent(R.id.widget_artwork, openAppPendingIntent(context))
                setOnClickPendingIntent(R.id.widget_previous, playbackPendingIntent(context, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
                setOnClickPendingIntent(R.id.widget_play_pause, playbackPendingIntent(context, Player.COMMAND_PLAY_PAUSE))
                setOnClickPendingIntent(R.id.widget_next, playbackPendingIntent(context, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
            }
            manager.updateAppWidget(widgetId, views)
        }

        private fun openAppPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            return PendingIntent.getActivity(
                context,
                REQUEST_OPEN_APP,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun playbackPendingIntent(context: Context, command: Int): PendingIntent {
            return PlaybackPendingIntentBuilder(context, command, PlaybackService::class.java)
                // long: 桌面组件可能在应用进程退出后唤起播放服务，Android 8+ 必须以前台服务方式承接媒体命令。
                .setStartAsForegroundService(true)
                .build()
        }

        private fun loadArtworkIfNeeded(context: Context, snapshot: PlaybackWidgetSnapshot) {
            val artworkUrl = snapshot.artworkUrl ?: return
            if (artworkCache.containsKey(artworkUrl) || !artworkLoads.add(artworkUrl)) return
            widgetScope.launch {
                try {
                    decodeArtwork(context, artworkUrl)?.let { bitmap ->
                        if (artworkCache.size >= MAX_ARTWORK_CACHE_ENTRIES) {
                            artworkCache.keys.firstOrNull()?.let(artworkCache::remove)
                        }
                        artworkCache[artworkUrl] = bitmap
                        if (latestSnapshot.artworkUrl == artworkUrl) {
                            withContext(Dispatchers.Main) { renderAll(context, latestSnapshot) }
                        }
                    }
                } finally {
                    artworkLoads.remove(artworkUrl)
                }
            }
        }

        private fun decodeArtwork(context: Context, artworkUrl: String): Bitmap? {
            val uri = Uri.parse(artworkUrl)
            val bytes = when (uri.scheme) {
                "content", "file" -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                "http", "https" -> {
                    val request = Request.Builder().url(artworkUrl).build()
                    artworkHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) null else response.body?.bytes()
                    }
                }
                else -> null
            } ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > MAX_ARTWORK_SIZE_PX * 2 || bounds.outHeight / sampleSize > MAX_ARTWORK_SIZE_PX * 2) {
                sampleSize *= 2
            }
            val decoded = BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sampleSize },
            ) ?: return null
            val scale = minOf(
                1f,
                MAX_ARTWORK_SIZE_PX.toFloat() / decoded.width.coerceAtLeast(1),
                MAX_ARTWORK_SIZE_PX.toFloat() / decoded.height.coerceAtLeast(1),
            )
            if (scale >= 1f) return decoded
            return Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true,
            ).also { scaled -> if (scaled !== decoded) decoded.recycle() }
        }

        private const val MAX_ARTWORK_CACHE_ENTRIES = 8
        private const val MAX_ARTWORK_SIZE_PX = 256
    }
}

private data class PlaybackWidgetSnapshot(
    val mediaId: String,
    val title: String,
    val artist: String,
    val contextLabel: String,
    val artworkUrl: String?,
    val progressPermille: Int,
    val isPlaying: Boolean,
) {
    companion object {
        fun empty() = PlaybackWidgetSnapshot(
            mediaId = "",
            title = "打开 Biu 开始听歌",
            artist = "便携的 Bilibili 听歌播放器",
            contextLabel = "",
            artworkUrl = null,
            progressPermille = 0,
            isPlaying = false,
        )

        fun from(player: Player?): PlaybackWidgetSnapshot {
            val item = player?.currentMediaItem ?: return empty()
            val metadata = item.mediaMetadata
            val title = metadata.title?.toString().orEmpty().ifBlank { "未知曲目" }
            val albumTitle = metadata.albumTitle?.toString().orEmpty()
            val artist = metadata.artist?.toString().orEmpty().ifBlank { "未知 UP 主" }
            val duration = player.duration
            val progress = if (duration == C.TIME_UNSET || duration <= 0L) {
                0
            } else {
                ((player.currentPosition.coerceIn(0L, duration) * 1_000L) / duration).toInt()
            }
            return PlaybackWidgetSnapshot(
                mediaId = item.mediaId,
                title = title,
                artist = artist,
                contextLabel = albumTitle.takeIf { it.isNotBlank() && it != title }.orEmpty(),
                artworkUrl = metadata.artworkUri?.toString()?.takeIf(String::isNotBlank),
                progressPermille = progress,
                isPlaying = player.isPlaying,
            )
        }
    }
}
