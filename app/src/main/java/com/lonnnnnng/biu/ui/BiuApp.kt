package com.lonnnnnng.biu.ui

import android.content.ComponentName
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
    val mediaId: String = "",
    val title: String = "还没有播放",
    val artist: String = "选择内容开始播放",
    val quality: String = "",
    val pageTitle: String? = null,
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isSeekable: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiuApp(viewModel: BiuViewModel = viewModel()) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val controller = rememberMediaController()
    val snackbarHostState = remember { SnackbarHostState() }
    var playback by remember { mutableStateOf(PlaybackSnapshot()) }
    var showLogin by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var playbackErrorEventId by remember { mutableLongStateOf(0L) }

    DisposableEffect(controller) {
        fun publishSnapshot() {
            playback = controller?.let { activeController ->
                val progress = activeController.currentPlaybackProgress()
                PlaybackSnapshot(
                    mediaId = activeController.currentMediaItem?.mediaId.orEmpty(),
                    title = activeController.mediaMetadata.title?.toString() ?: "还没有播放",
                    artist = activeController.mediaMetadata.artist?.toString() ?: "选择内容开始播放",
                    quality = activeController.mediaMetadata.description?.toString().orEmpty(),
                    pageTitle = activeController.mediaMetadata.subtitle?.toString()?.takeIf(String::isNotBlank),
                    artworkUrl = activeController.mediaMetadata.artworkUri?.toString(),
                    isPlaying = activeController.isPlaying,
                    hasPrevious = activeController.hasPreviousMediaItem(),
                    hasNext = activeController.hasNextMediaItem(),
                    positionMs = progress.positionMs,
                    durationMs = progress.durationMs,
                    bufferedPositionMs = progress.bufferedPositionMs,
                    isSeekable = progress.isSeekable,
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

    LaunchedEffect(controller) {
        while (true) {
            controller?.let { activeController ->
                val progress = activeController.currentPlaybackProgress()
                viewModel.updatePlaybackQueuePosition(activeController.currentMediaItem?.mediaId.orEmpty(), progress.positionMs)
                playback = playback.copy(
                    positionMs = progress.positionMs,
                    durationMs = progress.durationMs,
                    bufferedPositionMs = progress.bufferedPositionMs,
                    isSeekable = progress.isSeekable,
                )
            }
            delay(PLAYBACK_PROGRESS_TICK_MS)
        }
    }

    LaunchedEffect(controller, viewModel) {
        val activeController = controller ?: return@LaunchedEffect
        var appliedQueueId: Long? = null
        var restoredQueueId: Long? = null
        viewModel.currentPlaybackQueue()?.let { snapshot ->
            if (activeController.matchesPlaybackQueue(snapshot)) {
                appliedQueueId = snapshot.queueId
            } else {
                activeController.restorePlaybackQueue(snapshot)
                appliedQueueId = snapshot.queueId
                restoredQueueId = snapshot.queueId
            }
        }
        viewModel.playbackCommands.collect { command ->
            when (command) {
                is PlaybackCommand.Replace -> {
                    if (restoredQueueId == command.queueId) {
                        restoredQueueId = null
                        return@collect
                    }
                    activeController.restorePlaybackQueue(
                        PlaybackQueueSnapshot(
                            queueId = command.queueId,
                            items = command.tracks,
                            startIndex = command.startIndex,
                            startPositionMs = command.startPositionMs,
                        ),
                    )
                    appliedQueueId = command.queueId
                }
                is PlaybackCommand.Expand -> {
                    if (!viewModel.isActivePlaybackQueue(command.queueId)) {
                        return@collect
                    }
                    if (appliedQueueId != command.queueId || activeController.mediaItemCount == 0) {
                        val snapshot = viewModel.currentPlaybackQueue(command.queueId) ?: return@collect
                        activeController.restorePlaybackQueue(snapshot)
                        appliedQueueId = command.queueId
                        return@collect
                    }
                    if (activeController.containsMediaId(command.track.id)) return@collect
                    // long: 前置 P 插入队首、后置 P 追加队尾，不替换当前媒体项，因此后台补齐不会打断已开始的播放。
                    when (command.placement) {
                        QueuePlacement.PREPEND -> activeController.addMediaItem(0, command.track.toMediaItem())
                        QueuePlacement.APPEND -> activeController.addMediaItem(command.track.toMediaItem())
                    }
                }
            }
        }
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

    uiState.pageSelection?.let { selection ->
        MultiPageSelectionSheet(
            selection = selection,
            loading = uiState.isPageQueueLoading,
            onDismiss = viewModel::dismissPageSelection,
            onPlayPage = viewModel::playPageQueue,
        )
    }

    Scaffold(
        topBar = {
            BiuTopBar(
                section = uiState.section,
                qualityPreference = uiState.qualityPreference,
                qualityMenuExpanded = showQualityMenu,
                onShowQualityMenu = { showQualityMenu = true },
                onDismissQualityMenu = { showQualityMenu = false },
                onQualitySelected = { preference ->
                    showQualityMenu = false
                    viewModel.selectQualityPreference(preference)
                },
                onAccount = { viewModel.selectSection(MainSection.ACCOUNT) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BiuBottomBar(
                playback = playback,
                controllerReady = controller != null,
                selectedSection = uiState.section,
                showNavigation = !isLandscape,
                onPrevious = { controller?.seekToPreviousMediaItem() },
                onToggle = {
                    controller?.let { activeController ->
                        if (activeController.isPlaying) activeController.pause() else activeController.play()
                    }
                },
                onNext = { controller?.seekToNextMediaItem() },
                onSeek = { positionMs -> controller?.seekTo(positionMs) },
                onSectionSelected = viewModel::selectSection,
            )
        },
    ) { contentPadding ->
        val pageContent: @Composable () -> Unit = {
            val pageModifier = Modifier.fillMaxSize().widthIn(max = 840.dp)
            when (uiState.section) {
                MainSection.RECOMMEND -> RecommendationScreen(
                    videos = uiState.recommendations,
                    feed = uiState.feed,
                    loading = uiState.isFeedLoading,
                    resolvingBvid = uiState.resolvingBvid,
                    onFeedChange = viewModel::loadRecommendations,
                    onRefresh = { viewModel.loadRecommendations() },
                    onPlay = viewModel::play,
                    modifier = pageModifier,
                )
                MainSection.SEARCH -> SearchScreen(
                    videos = uiState.searchResults,
                    submittedKeyword = uiState.submittedKeyword,
                    loading = uiState.isSearchLoading,
                    resolvingBvid = uiState.resolvingBvid,
                    onSearch = viewModel::search,
                    onPlay = viewModel::play,
                    modifier = pageModifier,
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
                    modifier = pageModifier,
                )
            }
        }
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                BiuNavigationRail(
                    selectedSection = uiState.section,
                    onSectionSelected = viewModel::selectSection,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    pageContent()
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                pageContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BiuTopBar(
    section: MainSection,
    qualityPreference: AudioQualityPreference,
    qualityMenuExpanded: Boolean,
    onShowQualityMenu: () -> Unit,
    onDismissQualityMenu: () -> Unit,
    onQualitySelected: (AudioQualityPreference) -> Unit,
    onAccount: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text("BIU", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(section.label, style = MaterialTheme.typography.titleLarge)
            }
        },
        actions = {
            Box {
                IconButton(onClick = onShowQualityMenu) {
                    Icon(
                        Icons.Rounded.HighQuality,
                        contentDescription = "播放音质：${qualityPreference.label}",
                    )
                }
                DropdownMenu(
                    expanded = qualityMenuExpanded,
                    onDismissRequest = onDismissQualityMenu,
                ) {
                    AudioQualityPreference.entries.forEach { preference ->
                        DropdownMenuItem(
                            text = { Text(preference.label) },
                            leadingIcon = {
                                if (qualityPreference == preference) {
                                    Icon(Icons.Rounded.Check, contentDescription = "已选择")
                                } else {
                                    Spacer(Modifier.size(24.dp))
                                }
                            },
                            onClick = { onQualitySelected(preference) },
                        )
                    }
                }
            }
            IconButton(onClick = onAccount) {
                Icon(Icons.Rounded.AccountCircle, contentDescription = "打开账号音乐库")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun BiuBottomBar(
    playback: PlaybackSnapshot,
    controllerReady: Boolean,
    selectedSection: MainSection,
    showNavigation: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSectionSelected: (MainSection) -> Unit,
) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        if (playback.mediaId.isNotBlank()) {
            MiniPlayer(
                snapshot = playback,
                controllerReady = controllerReady,
                onPrevious = onPrevious,
                onToggle = onToggle,
                onNext = onNext,
                onSeek = onSeek,
            )
        }
        if (showNavigation) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                MainSection.entries.forEach { section ->
                    val icon = section.icon()
                    NavigationBarItem(
                        selected = selectedSection == section,
                        onClick = { onSectionSelected(section) },
                        icon = { Icon(icon, contentDescription = section.label) },
                        label = { Text(section.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun BiuNavigationRail(
    selectedSection: MainSection,
    onSectionSelected: (MainSection) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.width(88.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Spacer(Modifier.height(8.dp))
        MainSection.entries.forEach { section ->
            NavigationRailItem(
                selected = selectedSection == section,
                onClick = { onSectionSelected(section) },
                icon = { Icon(section.icon(), contentDescription = section.label) },
                label = { Text(section.label) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

private fun MainSection.icon(): ImageVector = when (this) {
    MainSection.RECOMMEND -> Icons.Rounded.Album
    MainSection.SEARCH -> Icons.Rounded.Search
    MainSection.ACCOUNT -> Icons.Rounded.AccountCircle
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("发现音乐", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onRefresh, enabled = !loading) {
                Icon(Icons.Rounded.Refresh, contentDescription = "刷新推荐")
            }
        }
        FeedSelector(
            selected = feed,
            onSelected = onFeedChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (!loading && videos.isEmpty()) {
            BiuEmptyState(
                icon = Icons.Rounded.LibraryMusic,
                title = "暂时没有推荐",
                actionLabel = "重新加载",
                onAction = onRefresh,
                modifier = Modifier.weight(1f),
            )
        } else {
            VideoList(videos, resolvingBvid, onPlay, Modifier.weight(1f))
        }
    }
}

@Composable
private fun FeedSelector(
    selected: RecommendFeed,
    onSelected: (RecommendFeed) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RecommendFeed.entries.forEach { option ->
                val isSelected = selected == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        )
                        .semantics {
                            role = Role.Tab
                            this.selected = isSelected
                        }
                        .clickable { onSelected(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        option.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
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
    val submitSearch = {
        focusManager.clearFocus()
        onSearch(keyword)
    }
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "搜索 Bilibili 音乐",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("标题或 UP 主") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (keyword.isNotEmpty()) {
                        IconButton(onClick = { keyword = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "清空搜索词")
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
            )
            FilledIconButton(
                onClick = submitSearch,
                enabled = keyword.isNotBlank() && !loading,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "搜索")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (!loading && videos.isEmpty()) {
            BiuEmptyState(
                icon = Icons.Rounded.Search,
                title = if (submittedKeyword.isBlank()) "开始搜索" else "没有找到结果",
                message = if (submittedKeyword.isBlank()) null else "换一个关键词再试试",
                modifier = Modifier.weight(1f),
            )
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
    Column(modifier = modifier.fillMaxSize()) {
        if (state.isAccountLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        AccountHeader(
            state = state,
            onLogin = onLogin,
            onRefresh = onRefresh,
            onLogout = onLogout,
        )
        Text(
            "音乐库",
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
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
            BiuEmptyState(
                icon = Icons.Rounded.AccountCircle,
                title = "登录后查看${state.librarySection.label}",
                actionLabel = "登录 Bilibili",
                onAction = onLogin,
                modifier = Modifier.weight(1f),
            )
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
private fun AccountHeader(
    state: BiuUiState,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.account.isLoggedIn && state.account.faceUrl.isNotBlank()) {
                AsyncImage(
                    model = state.account.faceUrl,
                    contentDescription = "${state.account.name}的头像",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (state.account.isLoggedIn) state.account.name else "未登录",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (state.account.isLoggedIn) "Bilibili 账号已连接" else "连接账号以同步收藏与历史",
                    maxLines = 2,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "刷新账号状态")
            }
            if (state.account.isLoggedIn) {
                TextButton(onClick = onLogout) { Text("退出") }
            } else {
                FilledTonalButton(onClick = onLogin) { Text("登录") }
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
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCloseFolder) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "返回收藏夹列表")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        selectedFolder.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "${selectedFolder.mediaCount} 项内容",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LibraryVideoList(videos, resolvingBvid, loading, onPlay, Modifier.weight(1f))
        }
    } else if (!loading && folders.isEmpty()) {
        BiuEmptyState(
            icon = Icons.Rounded.Folder,
            title = "收藏夹为空",
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(folders, key = BilibiliFavoriteFolder::id) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenFolder(folder) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (folder.coverUrl.isNotBlank()) {
                            AsyncImage(
                                model = folder.coverUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            folder.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "${folder.mediaCount} 项内容",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "打开 ${folder.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 80.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
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
        BiuEmptyState(
            icon = Icons.Rounded.LibraryMusic,
            title = "暂无内容",
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
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
            MediaDivider()
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
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("最近播放", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { showClearConfirmation = true }, enabled = history.isNotEmpty()) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "清空本地历史")
            }
        }
        if (history.isEmpty()) {
            BiuEmptyState(
                icon = Icons.Rounded.History,
                title = "暂无本地历史",
                message = "播放过的内容会保存在这里",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(history, key = PlaybackHistoryEntity::mediaId) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = resolvingBvid == null) { onPlay(item) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AsyncImage(
                            model = item.artworkUrl,
                            contentDescription = item.title,
                            modifier = Modifier
                                .size(width = 112.dp, height = 72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop,
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                item.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                formatProgress(item.lastPositionMs, item.durationMs),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                listOf(item.artist, "播放 ${item.playCount} 次")
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
                            PlayAffordance(contentDescription = "播放 ${item.title}")
                        }
                    }
                    MediaDivider()
                }
            }
        }
    }
}

@Composable
private fun BiuEmptyState(
    icon: ImageVector,
    title: String,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        message?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
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
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(videos, key = BilibiliVideo::bvid) { video ->
            VideoRow(
                video = video,
                resolving = resolvingBvid == video.bvid,
                enabled = resolvingBvid == null,
                onClick = { onPlay(video) },
            )
            MediaDivider()
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 112.dp, height = 72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = video.coverUrl,
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            video.durationSeconds?.let { duration ->
                Text(
                    formatDuration(duration),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                video.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
            contextLabel?.takeIf(String::isNotBlank)?.let { label ->
                Text(
                    label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                buildList {
                    if (video.author.isNotBlank()) add(video.author)
                    video.playCount?.let { add("${formatCount(it)} 播放") }
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
            PlayAffordance(contentDescription = "播放 ${video.title}")
        }
    }
}

@Composable
private fun PlayAffordance(contentDescription: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun MediaDivider(start: androidx.compose.ui.unit.Dp = 140.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = start, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiPageSelectionSheet(
    selection: VideoPageSelection,
    loading: Boolean,
    onDismiss: () -> Unit,
    onPlayPage: (Int) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { if (!loading) onDismiss() },
        modifier = Modifier.widthIn(max = 840.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("分 P 列表", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${selection.detail.pages.size} P · ${selection.detail.title}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss, enabled = !loading) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭分 P 列表")
                }
            }
            FilledTonalButton(
                onClick = { onPlayPage(0) },
                enabled = !loading && selection.detail.pages.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "正在建立播放队列" else "全部播放")
            }
            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                itemsIndexed(
                    items = selection.detail.pages,
                    key = { _, page -> page.cid },
                ) { index, page ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !loading) { onPlayPage(index) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(width = 48.dp, height = 40.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("P${page.page}", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                page.title.ifBlank { "第 ${page.page} P" },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                formatDuration(page.durationSeconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        PlayAffordance(contentDescription = "从 P${page.page} 开始播放")
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
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
    onSeek: (Long) -> Unit,
) {
    var isDragging by remember(snapshot.mediaId) { mutableStateOf(false) }
    var dragFraction by remember(snapshot.mediaId) { mutableFloatStateOf(0f) }
    val progress = PlaybackProgressPolicy.normalize(
        positionMs = snapshot.positionMs,
        durationMs = snapshot.durationMs,
        bufferedPositionMs = snapshot.bufferedPositionMs,
        isSeekable = snapshot.isSeekable,
    )
    val sliderValue = if (isDragging) dragFraction else progress.fraction
    val displayedPositionMs = if (isDragging) {
        PlaybackProgressPolicy.seekPositionMs(dragFraction, progress.durationMs)
    } else {
        progress.positionMs
    }
    // long: 多 P 优先展示当前分集名称；单 P 没有 subtitle 时保留原来的作者和音质信息。
    val secondaryText = snapshot.pageTitle
        ?: listOf(snapshot.artist, snapshot.quality).filter(String::isNotBlank).joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (snapshot.artworkUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            } else {
                AsyncImage(
                    model = snapshot.artworkUrl,
                    contentDescription = snapshot.title,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            ) {
                Text(snapshot.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                if (secondaryText.isNotBlank()) {
                    Text(
                        secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (snapshot.pageTitle == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
            IconButton(onClick = onPrevious, enabled = controllerReady && snapshot.hasPrevious) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "上一首")
            }
            FilledIconButton(
                onClick = onToggle,
                enabled = controllerReady,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    if (snapshot.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (snapshot.isPlaying) "暂停" else "播放",
                )
            }
            IconButton(onClick = onNext, enabled = controllerReady && snapshot.hasNext) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "下一首")
            }
        }
        LinearProgressIndicator(
            progress = { progress.bufferedFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                formatDurationMs(displayedPositionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    isDragging = true
                    dragFraction = value
                },
                onValueChangeFinished = {
                    onSeek(PlaybackProgressPolicy.seekPositionMs(dragFraction, progress.durationMs))
                    isDragging = false
                },
                modifier = Modifier.weight(1f),
                enabled = controllerReady && progress.isSeekable,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
            Text(
                formatDurationMs(progress.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

private fun formatDurationMs(durationMs: Long): String = formatDuration((durationMs.coerceAtLeast(0L) / 1000L).toInt())

private fun MediaController.currentPlaybackProgress(): PlaybackProgress = PlaybackProgressPolicy.normalize(
    positionMs = currentPosition,
    durationMs = duration,
    bufferedPositionMs = bufferedPosition,
    isSeekable = isCurrentMediaItemSeekable,
)

private fun MediaController.restorePlaybackQueue(snapshot: PlaybackQueueSnapshot<Track>) {
    // long: 控制器重连且服务队列为空时按 ViewModel 快照恢复，保证大型分 P 后台补齐不会因连接切换而丢失。
    setMediaItems(snapshot.items.map(Track::toMediaItem), snapshot.startIndex, snapshot.startPositionMs)
    prepare()
    play()
}

private fun MediaController.containsMediaId(mediaId: String): Boolean {
    if (mediaId.isBlank()) return false
    return (0 until mediaItemCount).any { getMediaItemAt(it).mediaId == mediaId }
}

private fun MediaController.matchesPlaybackQueue(snapshot: PlaybackQueueSnapshot<Track>): Boolean {
    if (mediaItemCount != snapshot.items.size) return false
    return snapshot.items.indices.all { index -> getMediaItemAt(index).mediaId == snapshot.items[index].id }
}

private const val PLAYBACK_PROGRESS_TICK_MS = 500L
