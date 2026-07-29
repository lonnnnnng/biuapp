package com.lonnnnnng.biu.ui

import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.AlertDialog
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
import com.lonnnnnng.biu.data.bilibili.AccountLibrarySection
import com.lonnnnnng.biu.data.bilibili.BilibiliFavoriteFolder
import com.lonnnnnng.biu.data.bilibili.BilibiliLibraryVideo
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import com.lonnnnnng.biu.data.bilibili.RecommendFeed
import com.lonnnnnng.biu.data.local.PlaybackHistoryEntity
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
        if (request.startPositionMs > 0L) {
            activeController.setMediaItem(request.track.toMediaItem(), request.startPositionMs)
        } else {
            activeController.setMediaItem(request.track.toMediaItem())
        }
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

    LaunchedEffect(showLogin, uiState.account.isLoggedIn) {
        // long: 账号接口确认登录成功后立即退出 WebView，避免 H5 的 XHR 登录停留在原表单页面。
        if (showLogin && uiState.account.isLoggedIn) showLogin = false
    }

    if (showLogin) {
        LoginWebViewDialog(
            onDismiss = {
                showLogin = false
                viewModel.refreshAccount()
            },
            onSessionAvailable = viewModel::refreshAccount,
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
                onLoadLibrary = viewModel::loadLibrary,
                onOpenFavoriteFolder = viewModel::openFavoriteFolder,
                onCloseFavoriteFolder = viewModel::closeFavoriteFolder,
                onPlay = viewModel::play,
                onPlayHistory = viewModel::play,
                onClearLocalHistory = viewModel::clearLocalHistory,
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
    onLoadLibrary: (AccountLibrarySection) -> Unit,
    onOpenFavoriteFolder: (BilibiliFavoriteFolder) -> Unit,
    onCloseFavoriteFolder: () -> Unit,
    onPlay: (BilibiliVideo) -> Unit,
    onPlayHistory: (PlaybackHistoryEntity) -> Unit,
    onClearLocalHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        if (state.isAccountLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else if (state.account.isLoggedIn) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(
                    model = state.account.faceUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.account.name, fontWeight = FontWeight.Bold)
                    Text("已登录 Bilibili", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "刷新账号音乐库") }
                TextButton(onClick = onLogout) { Text("退出") }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(52.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("未登录", fontWeight = FontWeight.Bold)
                    Text("登录后可浏览账号收藏和历史", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = onLogin) { Text("登录") }
                IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "刷新登录状态") }
            }
        }

        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccountLibrarySection.entries.forEach { section ->
                FilterChip(
                    selected = state.librarySection == section,
                    onClick = { onLoadLibrary(section) },
                    label = { Text(section.label) },
                    leadingIcon = {
                        Icon(
                            when (section) {
                                AccountLibrarySection.FAVORITES -> Icons.Rounded.Favorite
                                AccountLibrarySection.WATCH_LATER -> Icons.Rounded.WatchLater
                                AccountLibrarySection.ONLINE_HISTORY -> Icons.Rounded.History
                                AccountLibrarySection.LOCAL_HISTORY -> Icons.Rounded.Album
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }

        if (state.isLibraryLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        val onlineSection = state.librarySection != AccountLibrarySection.LOCAL_HISTORY
        if (onlineSection && !state.account.isLoggedIn) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("登录后可浏览${state.librarySection.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            when (state.librarySection) {
                AccountLibrarySection.FAVORITES -> FavoriteLibrary(
                    folders = state.favoriteFolders,
                    selectedFolder = state.selectedFavoriteFolder,
                    videos = state.libraryVideos,
                    resolvingBvid = state.resolvingBvid,
                    loading = state.isLibraryLoading,
                    onOpenFolder = onOpenFavoriteFolder,
                    onCloseFolder = onCloseFavoriteFolder,
                    onPlay = onPlay,
                    modifier = Modifier.weight(1f),
                )
                AccountLibrarySection.WATCH_LATER,
                AccountLibrarySection.ONLINE_HISTORY,
                -> LibraryVideoList(
                    videos = state.libraryVideos,
                    resolvingBvid = state.resolvingBvid,
                    loading = state.isLibraryLoading,
                    onPlay = onPlay,
                    modifier = Modifier.weight(1f),
                )
                AccountLibrarySection.LOCAL_HISTORY -> LocalHistoryList(
                    history = state.localHistory,
                    resolvingBvid = state.resolvingBvid,
                    onPlay = onPlayHistory,
                    onClear = onClearLocalHistory,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FavoriteLibrary(
    folders: List<BilibiliFavoriteFolder>,
    selectedFolder: BilibiliFavoriteFolder?,
    videos: List<BilibiliLibraryVideo>,
    resolvingBvid: String?,
    loading: Boolean,
    onOpenFolder: (BilibiliFavoriteFolder) -> Unit,
    onCloseFolder: () -> Unit,
    onPlay: (BilibiliVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectedFolder != null) {
        Column(modifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCloseFolder)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null)
                Text(selectedFolder.title, fontWeight = FontWeight.Bold)
            }
            LibraryVideoList(videos, resolvingBvid, loading, onPlay, Modifier.weight(1f))
        }
    } else if (!loading && folders.isEmpty()) {
        EmptyLibrary("收藏夹为空", modifier)
    } else {
        LazyColumn(modifier.fillMaxWidth()) {
            items(folders, key = BilibiliFavoriteFolder::id) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenFolder(folder) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(folder.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${folder.mediaCount} 首", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                }
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            }
        }
    }
}

@Composable
private fun LibraryVideoList(
    videos: List<BilibiliLibraryVideo>,
    resolvingBvid: String?,
    loading: Boolean,
    onPlay: (BilibiliVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!loading && videos.isEmpty()) {
        EmptyLibrary("暂无内容", modifier)
        return
    }
    LazyColumn(modifier.fillMaxWidth()) {
        items(videos, key = { item -> "${item.video.bvid}:${item.savedAtEpochSeconds ?: 0L}" }) { item ->
            VideoRow(
                video = item.video,
                resolving = resolvingBvid == item.video.bvid,
                enabled = resolvingBvid == null,
                contextLabel = item.progressSeconds
                    ?.takeIf { progress -> progress > 0 }
                    ?.let { progress -> "已看 ${formatDuration(progress)}" },
                onClick = { onPlay(item.video) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 132.dp))
        }
    }
}

@Composable
private fun LocalHistoryList(
    history: List<PlaybackHistoryEntity>,
    resolvingBvid: String?,
    onPlay: (PlaybackHistoryEntity) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearConfirmation by remember { mutableStateOf(false) }
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("清空本地历史？") },
            text = { Text("此操作只删除本机的播放记录，不影响 Bilibili 在线历史。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClear()
                    },
                ) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("取消") }
            },
        )
    }
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("最近播放", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            IconButton(onClick = { showClearConfirmation = true }, enabled = history.isNotEmpty()) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "清空本地历史")
            }
        }
        if (history.isEmpty()) {
            EmptyLibrary("播放内容后会出现在这里", Modifier.weight(1f))
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(history, key = PlaybackHistoryEntity::mediaId) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = resolvingBvid == null) { onPlay(item) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AsyncImage(
                            model = item.artworkUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOf(item.artist, formatProgress(item.lastPositionMs, item.durationMs), "播放 ${item.playCount} 次")
                                    .filter(String::isNotBlank)
                                    .joinToString(" · "),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (resolvingBvid == item.bvid) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "播放 ${item.title}")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 84.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    contextLabel: String? = null,
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
                    contextLabel?.takeIf(String::isNotBlank)?.let(::add)
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

private fun formatProgress(positionMs: Long, durationMs: Long): String {
    if (positionMs <= 0L) return "刚刚开始"
    val position = formatDuration((positionMs / 1000L).toInt())
    val duration = durationMs.takeIf { it > 0L }
        ?.let { formatDuration((it / 1000L).toInt()) }
    return if (duration == null) "已听 $position" else "$position / $duration"
}
