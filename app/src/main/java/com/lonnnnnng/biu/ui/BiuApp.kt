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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil3.compose.AsyncImage
import com.lonnnnnng.biu.core.model.AudioQualityPreference
import com.lonnnnnng.biu.core.model.Track
import com.lonnnnnng.biu.core.model.toMediaItem
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import com.lonnnnnng.biu.data.bilibili.RecommendFeed
import com.lonnnnnng.biu.playback.PlaybackService
import kotlinx.coroutines.delay

private data class PlaybackSnapshot(
    val title: String = "还没有播放",
    val artist: String = "选择内容开始播放",
    val quality: String = "",
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiuApp(viewModel: BiuViewModel = viewModel()) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val controller = rememberMediaController()
    val snackbarHostState = remember { SnackbarHostState() }
    var playback by remember { mutableStateOf(PlaybackSnapshot()) }
    var showLogin by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var playbackErrorEventId by remember { mutableLongStateOf(0L) }

    DisposableEffect(controller) {
        fun publishSnapshot() {
            playback = controller?.let { activeController ->
                PlaybackSnapshot(
                    title = activeController.mediaMetadata.title?.toString() ?: "还没有播放",
                    artist = activeController.mediaMetadata.artist?.toString() ?: "选择内容开始播放",
                    quality = activeController.mediaMetadata.description?.toString().orEmpty(),
                    artworkUrl = activeController.mediaMetadata.artworkUri?.toString(),
                    isPlaying = activeController.isPlaying,
                    hasPrevious = activeController.hasPreviousMediaItem(),
                    hasNext = activeController.hasNextMediaItem(),
                )
            } ?: PlaybackSnapshot()
        }

        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = publishSnapshot()

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackErrorEventId += 1
            }
        }
        controller?.addListener(listener)
        publishSnapshot()
        onDispose { controller?.removeListener(listener) }
    }

    LaunchedEffect(uiState.playbackRequest?.eventId, controller) {
        val request = uiState.playbackRequest ?: return@LaunchedEffect
        val activeController = controller ?: return@LaunchedEffect
        activeController.setMediaItem(request.track.toMediaItem())
        activeController.prepare()
        activeController.play()
        viewModel.consumePlaybackRequest(request.eventId)
    }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    LaunchedEffect(playbackErrorEventId) {
        if (playbackErrorEventId == 0L) return@LaunchedEffect
        // long: 首次 CDN 失败会由服务自动换址；延迟确认可避免备用地址已经恢复时仍向用户误报失败。
        delay(1_500)
        if (controller?.playerError != null) {
            snackbarHostState.showSnackbar("播放失败，请重新选择内容重试")
        }
    }

    if (showLogin) {
        LoginWebViewDialog(
            onDismiss = {
                showLogin = false
                viewModel.refreshAccount()
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Biu", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        IconButton(onClick = { showQualityMenu = true }) {
                            Icon(Icons.Rounded.HighQuality, contentDescription = "播放音质")
                        }
                        DropdownMenu(
                            expanded = showQualityMenu,
                            onDismissRequest = { showQualityMenu = false },
                        ) {
                            AudioQualityPreference.entries.forEach { preference ->
                                DropdownMenuItem(
                                    text = { Text(preference.label) },
                                    leadingIcon = {
                                        if (uiState.qualityPreference == preference) {
                                            Icon(Icons.Rounded.Check, contentDescription = null)
                                        } else {
                                            Spacer(Modifier.size(24.dp))
                                        }
                                    },
                                    onClick = {
                                        showQualityMenu = false
                                        viewModel.selectQualityPreference(preference)
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.selectSection(MainSection.ACCOUNT) }) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = "账号")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
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
                NavigationBar {
                    MainSection.entries.forEach { section ->
                        NavigationBarItem(
                            selected = uiState.section == section,
                            onClick = { viewModel.selectSection(section) },
                            icon = {
                                Icon(
                                    when (section) {
                                        MainSection.RECOMMEND -> Icons.Rounded.Album
                                        MainSection.SEARCH -> Icons.Rounded.Search
                                        MainSection.ACCOUNT -> Icons.Rounded.AccountCircle
                                    },
                                    contentDescription = null,
                                )
                            },
                            label = { Text(section.label) },
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        when (uiState.section) {
            MainSection.RECOMMEND -> RecommendationScreen(
                videos = uiState.recommendations,
                feed = uiState.feed,
                loading = uiState.isFeedLoading,
                resolvingBvid = uiState.resolvingBvid,
                onFeedChange = viewModel::loadRecommendations,
                onRefresh = { viewModel.loadRecommendations() },
                onPlay = viewModel::play,
                modifier = Modifier.padding(contentPadding),
            )
            MainSection.SEARCH -> SearchScreen(
                videos = uiState.searchResults,
                submittedKeyword = uiState.submittedKeyword,
                loading = uiState.isSearchLoading,
                resolvingBvid = uiState.resolvingBvid,
                onSearch = viewModel::search,
                onPlay = viewModel::play,
                modifier = Modifier.padding(contentPadding),
            )
            MainSection.ACCOUNT -> AccountScreen(
                state = uiState,
                onLogin = { showLogin = true },
                onRefresh = viewModel::refreshAccount,
                onLogout = viewModel::logout,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

@Composable
private fun RecommendationScreen(
    videos: List<BilibiliVideo>,
    feed: RecommendFeed,
    loading: Boolean,
    resolvingBvid: String?,
    onFeedChange: (RecommendFeed) -> Unit,
    onRefresh: () -> Unit,
    onPlay: (BilibiliVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecommendFeed.entries.forEach { option ->
                FilterChip(
                    selected = feed == option,
                    onClick = { onFeedChange(option) },
                    label = { Text(option.label) },
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRefresh, enabled = !loading) {
                Icon(Icons.Rounded.Refresh, contentDescription = "刷新推荐")
            }
        }
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        VideoList(videos, resolvingBvid, onPlay, Modifier.weight(1f))
    }
}

@Composable
private fun SearchScreen(
    videos: List<BilibiliVideo>,
    submittedKeyword: String,
    loading: Boolean,
    resolvingBvid: String?,
    onSearch: (String) -> Unit,
    onPlay: (BilibiliVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var keyword by remember { mutableStateOf(submittedKeyword) }
    val focusManager = LocalFocusManager.current
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            singleLine = true,
            label = { Text("搜索音乐视频") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (keyword.isNotEmpty()) {
                    IconButton(onClick = { keyword = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "清空")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    onSearch(keyword)
                },
            ),
        )
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (!loading && videos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (submittedKeyword.isBlank()) "输入关键词搜索" else "没有找到结果")
            }
        } else {
            VideoList(videos, resolvingBvid, onPlay, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AccountScreen(
    state: BiuUiState,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.isAccountLoading) {
            CircularProgressIndicator()
        } else if (state.account.isLoggedIn) {
            AsyncImage(
                model = state.account.faceUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Text(state.account.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("已登录 Bilibili", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onLogout) { Text("退出登录") }
        } else {
            Icon(
                Icons.Rounded.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(88.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text("未登录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("公开推荐和搜索可以直接使用，登录后可获得账号画质与后续收藏能力。")
            OutlinedButton(onClick = onLogin) { Text("登录 Bilibili") }
            TextButton(onClick = onRefresh) { Text("刷新状态") }
        }
    }
}

@Composable
private fun VideoList(
    videos: List<BilibiliVideo>,
    resolvingBvid: String?,
    onPlay: (BilibiliVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(videos, key = BilibiliVideo::bvid) { video ->
            VideoRow(
                video = video,
                resolving = resolvingBvid == video.bvid,
                enabled = resolvingBvid == null,
                onClick = { onPlay(video) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 132.dp))
        }
    }
}

@Composable
private fun VideoRow(
    video: BilibiliVideo,
    resolving: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = video.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(width = 104.dp, height = 64.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                video.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                buildList {
                    if (video.author.isNotBlank()) add(video.author)
                    video.playCount?.let { add(formatCount(it)) }
                    video.durationSeconds?.let { add(formatDuration(it)) }
                }.joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (resolving) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "播放 ${video.title}")
        }
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
                .height(72.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (snapshot.artworkUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            } else {
                AsyncImage(
                    model = snapshot.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            ) {
                Text(snapshot.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(
                    listOf(snapshot.artist, snapshot.quality).filter(String::isNotBlank).joinToString(" · "),
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

private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

private fun formatCount(value: Long): String {
    return when {
        value >= 100_000_000 -> "%.1f 亿".format(value / 100_000_000.0)
        value >= 10_000 -> "%.1f 万".format(value / 10_000.0)
        else -> value.toString()
    }
}
