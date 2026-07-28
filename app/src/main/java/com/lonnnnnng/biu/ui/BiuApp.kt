package com.lonnnnnng.biu.ui

import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.lonnnnnng.biu.core.model.Track
import com.lonnnnnng.biu.core.model.toMediaItem
import com.lonnnnnng.biu.playback.PlaybackService

private data class PlaybackSnapshot(
    val title: String = "还没有播放",
    val artist: String = "选择一首音乐开始",
    val isPlaying: Boolean = false,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
)

private val sampleTracks = listOf(
    Track("demo-1", "Biu 原生播放测试", "Media3 示例音轨", "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"),
    Track("demo-2", "队列切换测试", "Biu Android", "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"),
    Track("demo-3", "后台播放测试", "通知栏可继续控制", "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiuApp() {
    val controller = rememberMediaController()
    var playback by remember { mutableStateOf(PlaybackSnapshot()) }

    DisposableEffect(controller) {
        fun publishSnapshot() {
            playback = controller?.let { activeController ->
                PlaybackSnapshot(
                    title = activeController.mediaMetadata.title?.toString() ?: "还没有播放",
                    artist = activeController.mediaMetadata.artist?.toString() ?: "选择一首音乐开始",
                    isPlaying = activeController.isPlaying,
                    hasPrevious = activeController.hasPreviousMediaItem(),
                    hasNext = activeController.hasNextMediaItem(),
                )
            } ?: PlaybackSnapshot()
        }

        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = publishSnapshot()
        }
        controller?.addListener(listener)
        publishSnapshot()
        onDispose { controller?.removeListener(listener) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Biu", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "更多")
                    }
                },
            )
        },
        bottomBar = {
            MiniPlayer(
                snapshot = playback,
                controllerReady = controller != null,
                onPrevious = { controller?.seekToPreviousMediaItem() },
                onToggle = {
                    controller?.let { activeController ->
                        if (activeController.isPlaying) activeController.pause() else activeController.play()
                    }
                },
                onNext = { controller?.seekToNextMediaItem() },
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Text("推荐试听", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "当前是原生播放骨架，下一阶段接入 Bilibili 推荐、搜索和 DASH 音频解析。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(sampleTracks, key = { _, track -> track.id }) { index, track ->
                TrackRow(
                    track = track,
                    onClick = {
                        controller?.apply {
                            setMediaItems(sampleTracks.map(Track::toMediaItem), index, 0L)
                            prepare()
                            play()
                        }
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
            }
        }
    }
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(
                track.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Rounded.PlayArrow, contentDescription = "播放 ${track.title}")
    }
}

@Composable
private fun MiniPlayer(
    snapshot: PlaybackSnapshot,
    controllerReady: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(snapshot.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(
                    snapshot.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPrevious, enabled = controllerReady && snapshot.hasPrevious) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "上一首")
            }
            IconButton(onClick = onToggle, enabled = controllerReady) {
                Icon(
                    if (snapshot.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (snapshot.isPlaying) "暂停" else "播放",
                )
            }
            IconButton(onClick = onNext, enabled = controllerReady && snapshot.hasNext) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "下一首")
            }
        }
    }
}

@Composable
private fun rememberMediaController(): MediaController? {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(context) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            { controller = runCatching { controllerFuture.get() }.getOrNull() },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            controller = null
            MediaController.releaseFuture(controllerFuture)
        }
    }

    return controller
}
