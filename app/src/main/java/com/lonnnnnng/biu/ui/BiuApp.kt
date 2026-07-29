package com.lonnnnnng.biu.ui

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
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
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import com.lonnnnnng.biu.data.bilibili.BilibiliCreator
import com.lonnnnnng.biu.data.bilibili.BilibiliLibraryVideo
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import com.lonnnnnng.biu.data.bilibili.HomeFeedMode
import com.lonnnnnng.biu.data.bilibili.RecommendFeed
import com.lonnnnnng.biu.data.local.PlaybackHistoryEntity
import com.lonnnnnng.biu.data.local.LocalAudio
import com.lonnnnnng.biu.data.local.LocalMediaPermissionPolicy
import com.lonnnnnng.biu.data.update.AppUpdate
import com.lonnnnnng.biu.playback.PlaybackService
import com.lonnnnnng.biu.update.AppUpdateInstaller
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class PlaybackQueueItem(
    val index: Int,
    val mediaId: String,
    val title: String,
    val artist: String,
    val pageTitle: String?,
    val artworkUrl: String?,
)

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
    val currentIndex: Int = 0,
    val queueItems: List<PlaybackQueueItem> = emptyList(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiuApp(viewModel: BiuViewModel = viewModel()) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val updateInstaller = remember(context.applicationContext) { AppUpdateInstaller(context.applicationContext) }
    val localAudioPermission = LocalMediaPermissionPolicy.permissionFor()
    var localAudioPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, localAudioPermission) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val controller = rememberMediaController()
    val snackbarHostState = remember { SnackbarHostState() }
    var playback by remember { mutableStateOf(PlaybackSnapshot()) }
    var showLogin by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showCreatorConfig by remember { mutableStateOf(false) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var playbackErrorEventId by remember { mutableLongStateOf(0L) }
    var activeUpdateDownloadId by rememberSaveable {
        mutableLongStateOf(updateInstaller.pendingDownloadId())
    }
    var pendingInstallDownloadId by rememberSaveable { mutableLongStateOf(-1L) }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val downloadId = pendingInstallDownloadId
        if (downloadId < 0L) return@rememberLauncherForActivityResult
        when (val result = updateInstaller.installDownloaded(downloadId)) {
            AppUpdateInstaller.InstallResult.Started -> pendingInstallDownloadId = -1L
            AppUpdateInstaller.InstallResult.PermissionRequired -> coroutineScope.launch {
                snackbarHostState.showSnackbar("请允许 BiuApp 安装未知来源应用后重试")
            }
            is AppUpdateInstaller.InstallResult.Failed -> coroutineScope.launch {
                snackbarHostState.showSnackbar(result.message)
            }
        }
    }
    val localAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        localAudioPermissionGranted = granted
        if (granted) {
            viewModel.loadLocalAudio()
        } else {
            viewModel.localAudioPermissionDenied()
        }
    }
    LifecycleResumeEffect(localAudioPermission) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            localAudioPermission,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted != localAudioPermissionGranted) {
            localAudioPermissionGranted = granted
            if (granted && viewModel.state.value.librarySection == AccountLibrarySection.LOCAL_MUSIC) {
                // long: 用户可能在系统设置中修改权限；返回前台后立即重新扫描，避免页面继续停留在过期的未授权状态。
                viewModel.loadLocalAudio()
            }
        }
        onPauseOrDispose { }
    }
    val installDownloadedUpdate: (Long) -> Unit = { downloadId ->
        when (val result = updateInstaller.installDownloaded(downloadId)) {
            AppUpdateInstaller.InstallResult.Started -> pendingInstallDownloadId = -1L
            AppUpdateInstaller.InstallResult.PermissionRequired -> {
                pendingInstallDownloadId = downloadId
                // long: 未获安装权限时只打开当前 App 的系统授权页，返回后继续同一个下载任务的安装。
                installPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            }
            is AppUpdateInstaller.InstallResult.Failed -> coroutineScope.launch {
                snackbarHostState.showSnackbar(result.message)
            }
        }
    }

    LaunchedEffect(activeUpdateDownloadId) {
        val downloadId = activeUpdateDownloadId
        if (downloadId < 0L) return@LaunchedEffect
        // long: 轮询系统下载状态可避免开放广播被伪造；任务 ID 已持久化，进程重启后也能继续进入安装流程。
        while (true) {
            when (val downloadState = updateInstaller.downloadState(downloadId)) {
                AppUpdateInstaller.DownloadState.Pending -> delay(1_000)
                AppUpdateInstaller.DownloadState.Successful -> {
                    activeUpdateDownloadId = -1L
                    installDownloadedUpdate(downloadId)
                    break
                }
                AppUpdateInstaller.DownloadState.Missing -> {
                    updateInstaller.clearPendingDownload(downloadId)
                    activeUpdateDownloadId = -1L
                    snackbarHostState.showSnackbar("找不到更新包下载任务，请重新下载")
                    break
                }
                is AppUpdateInstaller.DownloadState.Failed -> {
                    updateInstaller.clearPendingDownload(downloadId)
                    activeUpdateDownloadId = -1L
                    snackbarHostState.showSnackbar(downloadState.message)
                    break
                }
            }
        }
    }

    DisposableEffect(controller) {
        fun publishSnapshot() {
            playback = controller?.let { activeController ->
                val progress = activeController.currentPlaybackProgress()
                PlaybackSnapshot(
                    mediaId = activeController.currentMediaItem?.mediaId.orEmpty(),
                    // long: 多 P 的媒体 title 专供系统锁屏显示当前分 P，App 内仍以 albumTitle 展示视频总标题。
                    title = activeController.mediaMetadata.albumTitle?.toString()
                        ?: activeController.mediaMetadata.title?.toString()
                        ?: "还没有播放",
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
                    currentIndex = activeController.currentMediaItemIndex.coerceAtLeast(0),
                    queueItems = (0 until activeController.mediaItemCount).map { index ->
                        val item = activeController.getMediaItemAt(index)
                        PlaybackQueueItem(
                            index = index,
                            mediaId = item.mediaId,
                            title = item.mediaMetadata.albumTitle?.toString().orEmpty()
                                .ifBlank { item.mediaMetadata.title?.toString().orEmpty() }
                                .ifBlank { "未知内容" },
                            artist = item.mediaMetadata.artist?.toString().orEmpty(),
                            pageTitle = item.mediaMetadata.subtitle?.toString()?.takeIf(String::isNotBlank),
                            artworkUrl = item.mediaMetadata.artworkUri?.toString(),
                        )
                    },
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

    uiState.availableUpdate?.let { update ->
        AppUpdateDialog(
            update = update,
            onDismiss = viewModel::dismissUpdate,
            onDownload = {
                runCatching { updateInstaller.enqueue(update) }
                    .onSuccess { downloadId ->
                        activeUpdateDownloadId = downloadId
                        viewModel.dismissUpdate()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("更新包开始下载，完成后将打开系统安装界面")
                        }
                    }
                    .onFailure { error ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(error.message ?: "更新包下载启动失败")
                        }
                    }
            },
        )
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

    if (showCreatorConfig) {
        CreatorSelectionSheet(
            accountLoggedIn = uiState.account.isLoggedIn,
            creators = uiState.followedCreators,
            selectedCreators = uiState.selectedCreators,
            loading = uiState.isCreatorConfigLoading,
            saving = uiState.isCreatorConfigSaving,
            onDismiss = { showCreatorConfig = false },
            onRetry = viewModel::loadFollowingCreators,
            onOpenAccount = {
                showCreatorConfig = false
                viewModel.selectSection(MainSection.ACCOUNT)
            },
            onSave = { creators ->
                showCreatorConfig = false
                viewModel.saveCreatorSelection(creators)
            },
        )
    }

    if (showNowPlaying && playback.mediaId.isNotBlank()) {
        BackHandler { showNowPlaying = false }
        NowPlayingScreen(
            snapshot = playback,
            controllerReady = controller != null,
            onBack = { showNowPlaying = false },
            onPrevious = { controller?.seekToPreviousMediaItem() },
            onToggle = {
                controller?.let { activeController ->
                    if (activeController.isPlaying) activeController.pause() else activeController.play()
                }
            },
            onNext = { controller?.seekToNextMediaItem() },
            onSeek = { positionMs -> controller?.seekTo(positionMs) },
            onSelectQueueItem = { index ->
                controller?.seekToDefaultPosition(index)
                controller?.play()
            },
        )
        return
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
                onOpenNowPlaying = { showNowPlaying = true },
                onSectionSelected = viewModel::selectSection,
            )
        },
    ) { contentPadding ->
        val pageContent: @Composable () -> Unit = {
            val pageModifier = Modifier.fillMaxSize().widthIn(max = 840.dp)
            when (uiState.section) {
                MainSection.RECOMMEND -> RecommendationScreen(
                    videos = uiState.recommendations,
                    searchResults = uiState.searchResults,
                    submittedKeyword = uiState.submittedKeyword,
                    feed = uiState.feed,
                    homeFeedMode = uiState.homeFeedMode,
                    selectedCreatorCount = uiState.selectedCreators.size,
                    loading = uiState.isFeedLoading,
                    searchLoading = uiState.isSearchLoading,
                    resolvingBvid = uiState.resolvingBvid,
                    onFeedChange = viewModel::loadRecommendations,
                    onRefresh = { viewModel.loadRecommendations() },
                    onSearch = viewModel::search,
                    onClearSearch = viewModel::clearSearch,
                    onOpenCreatorConfig = {
                        showCreatorConfig = true
                        viewModel.loadFollowingCreators()
                    },
                    onPlay = viewModel::play,
                    modifier = pageModifier,
                )
                MainSection.ACCOUNT -> AccountScreen(
                    state = uiState,
                    localAudioPermissionGranted = localAudioPermissionGranted,
                    onLogin = { showLogin = true },
                    onRefresh = viewModel::refreshAccount,
                    onCheckUpdate = viewModel::checkForUpdate,
                    onLogout = viewModel::logout,
                    onLoadLibrary = { section ->
                        if (section == AccountLibrarySection.LOCAL_MUSIC && !localAudioPermissionGranted) {
                            viewModel.showLocalAudioPermission()
                            localAudioPermissionLauncher.launch(localAudioPermission)
                        } else {
                            viewModel.loadLibrary(section)
                        }
                    },
                    onRequestLocalAudioPermission = {
                        viewModel.showLocalAudioPermission()
                        localAudioPermissionLauncher.launch(localAudioPermission)
                    },
                    onOpenFavoriteFolder = viewModel::openFavoriteFolder,
                    onCloseFavoriteFolder = viewModel::closeFavoriteFolder,
                    onPlay = viewModel::play,
                    onPlayHistory = viewModel::play,
                    onPlayLocalAudio = viewModel::play,
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

@Composable
private fun AppUpdateDialog(
    update: AppUpdate,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null) },
        title = { Text("发现新版本 ${update.version}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("更新内容", style = MaterialTheme.typography.labelLarge)
                Text(
                    update.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { Button(onClick = onDownload) { Text("立即更新") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("稍后") } },
    )
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
            Text(section.label, style = MaterialTheme.typography.titleLarge)
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
    onOpenNowPlaying: () -> Unit,
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
                onOpenNowPlaying = onOpenNowPlaying,
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
    MainSection.ACCOUNT -> Icons.Rounded.AccountCircle
}

@Composable
private fun RecommendationScreen(
    videos: List<BilibiliVideo>,
    searchResults: List<BilibiliVideo>,
    submittedKeyword: String,
    feed: RecommendFeed,
    homeFeedMode: HomeFeedMode,
    selectedCreatorCount: Int,
    loading: Boolean,
    searchLoading: Boolean,
    resolvingBvid: String?,
    onFeedChange: (RecommendFeed) -> Unit,
    onRefresh: () -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onOpenCreatorConfig: () -> Unit,
    onPlay: (BilibiliVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var keyword by remember(submittedKeyword) { mutableStateOf(submittedKeyword) }
    val focusManager = LocalFocusManager.current
    val showingSearchResults = submittedKeyword.isNotBlank()
    val submitSearch = {
        focusManager.clearFocus()
        onSearch(keyword)
    }
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { value ->
                    keyword = value
                    if (value.isBlank() && showingSearchResults) onClearSearch()
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("搜索标题或 UP 主") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (keyword.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                keyword = ""
                                onClearSearch()
                            },
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "清空搜索")
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
            )
            FilledIconButton(
                onClick = submitSearch,
                enabled = keyword.isNotBlank() && !searchLoading,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "搜索")
            }
        }
        if (showingSearchResults) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "“$submittedKeyword”的结果",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        keyword = ""
                        onClearSearch()
                    },
                ) { Text("返回推荐") }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (homeFeedMode == HomeFeedMode.FALLBACK) {
                    FeedSelector(
                        selected = feed,
                        onSelected = onFeedChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp, top = 2.dp, bottom = 2.dp),
                    )
                } else {
                    SingleFeedLabel(
                        label = "我的关注",
                        supportingText = "已选 $selectedCreatorCount 位 UP",
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp, top = 2.dp, bottom = 2.dp),
                    )
                }
                IconButton(onClick = onOpenCreatorConfig) {
                    Icon(Icons.Rounded.Tune, contentDescription = "设置首页内容范围")
                }
                IconButton(onClick = onRefresh, enabled = !loading) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "刷新推荐")
                }
                Spacer(Modifier.width(8.dp))
            }
        }
        val activeLoading = if (showingSearchResults) searchLoading else loading
        val activeVideos = if (showingSearchResults) searchResults else videos
        if (activeLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (!activeLoading && activeVideos.isEmpty()) {
            BiuEmptyState(
                icon = if (showingSearchResults) Icons.Rounded.Search else Icons.Rounded.LibraryMusic,
                title = if (showingSearchResults) "没有找到结果" else "暂时没有推荐",
                message = if (showingSearchResults) "换一个关键词再试试" else null,
                actionLabel = if (showingSearchResults) null else "重新加载",
                onAction = if (showingSearchResults) null else onRefresh,
                modifier = Modifier.weight(1f),
            )
        } else {
            VideoList(activeVideos, resolvingBvid, onPlay, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SingleFeedLabel(
    label: String,
    supportingText: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Text(supportingText, style = MaterialTheme.typography.bodySmall)
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
private fun AccountScreen(
    state: BiuUiState,
    localAudioPermissionGranted: Boolean,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
    onCheckUpdate: () -> Unit,
    onLogout: () -> Unit,
    onLoadLibrary: (AccountLibrarySection) -> Unit,
    onRequestLocalAudioPermission: () -> Unit,
    onOpenFavoriteFolder: (BilibiliFavoriteFolder) -> Unit,
    onCloseFavoriteFolder: () -> Unit,
    onPlay: (BilibiliVideo) -> Unit,
    onPlayHistory: (PlaybackHistoryEntity) -> Unit,
    onPlayLocalAudio: (LocalAudio) -> Unit,
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
            onCheckUpdate = onCheckUpdate,
            onLogout = onLogout,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
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
                                AccountLibrarySection.ONLINE_HISTORY -> Icons.Rounded.History
                                AccountLibrarySection.LOCAL_HISTORY -> Icons.Rounded.Album
                                AccountLibrarySection.LOCAL_MUSIC -> Icons.Rounded.MusicNote
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }

        if (state.isLibraryLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        val onlineSection = state.librarySection in setOf(
            AccountLibrarySection.FAVORITES,
            AccountLibrarySection.ONLINE_HISTORY,
        )
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
                AccountLibrarySection.ONLINE_HISTORY -> LibraryVideoList(
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
                AccountLibrarySection.LOCAL_MUSIC -> LocalAudioList(
                    audio = state.localAudio,
                    permissionGranted = localAudioPermissionGranted,
                    loading = state.isLocalAudioLoading,
                    onRequestPermission = onRequestLocalAudioPermission,
                    onRefresh = { onLoadLibrary(AccountLibrarySection.LOCAL_MUSIC) },
                    onPlay = onPlayLocalAudio,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LocalAudioList(
    audio: List<LocalAudio>,
    permissionGranted: Boolean,
    loading: Boolean,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onPlay: (LocalAudio) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (permissionGranted) "本机音频 · ${audio.size}" else "本机音频",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            IconButton(onClick = onRefresh, enabled = permissionGranted && !loading) {
                Icon(Icons.Rounded.Refresh, contentDescription = "重新扫描本地音乐")
            }
        }
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        when {
            !permissionGranted -> BiuEmptyState(
                icon = Icons.Rounded.MusicNote,
                title = "允许读取本机音乐",
                message = "Biu 只读取系统媒体库中的音频，不会修改或上传本地文件。",
                actionLabel = "授权读取音频",
                onAction = onRequestPermission,
                modifier = Modifier.weight(1f),
            )
            !loading && audio.isEmpty() -> BiuEmptyState(
                icon = Icons.Rounded.MusicNote,
                title = "没有发现本地音乐",
                message = "把音频保存到系统 Music 目录后点击右上角重新扫描。",
                modifier = Modifier.weight(1f),
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(audio, key = LocalAudio::mediaStoreId) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(item) }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            item.artworkUri?.let { artworkUri ->
                                AsyncImage(
                                    model = artworkUri,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            Text(
                                item.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                listOf(formatDurationMs(item.durationMs), item.album)
                                    .filter(String::isNotBlank)
                                    .joinToString(" · "),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                item.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    MediaDivider(start = 84.dp)
                }
            }
        }
    }
}

@Composable
private fun AccountHeader(
    state: BiuUiState,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
    onCheckUpdate: () -> Unit,
    onLogout: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.account.isLoggedIn && state.account.faceUrl.isNotBlank()) {
                AsyncImage(
                    model = state.account.faceUrl,
                    contentDescription = "${state.account.name}的头像",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (state.account.isLoggedIn) state.account.name else "未登录",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "刷新账号状态")
            }
            IconButton(onClick = onCheckUpdate, enabled = !state.isUpdateChecking) {
                if (state.isUpdateChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = "检查更新")
                }
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
            LibraryVideoRow(
                item = item,
                resolving = resolvingBvid == item.video.bvid,
                enabled = resolvingBvid == null,
                onClick = { onPlay(item.video) },
            )
            MediaDivider(start = 124.dp)
        }
    }
}

@Composable
private fun LibraryVideoRow(
    item: BilibiliLibraryVideo,
    resolving: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val video = item.video
    val progressLabel = formatProgressSeconds(item.progressSeconds ?: 0, video.durationSeconds)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = video.coverUrl,
            contentDescription = video.title,
            modifier = Modifier
                .size(width = 96.dp, height = 60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                video.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                progressLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                video.author.ifBlank { "未知作者" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (resolving) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
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
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("最近播放", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
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
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AsyncImage(
                            model = item.artworkUrl,
                            contentDescription = item.title,
                            modifier = Modifier
                                .size(width = 96.dp, height = 60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop,
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                item.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                formatProgress(item.lastPositionMs, item.durationMs),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                item.artist.ifBlank { "未知作者" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (resolvingBvid == item.bvid) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                    MediaDivider(start = 124.dp)
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
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 104.dp, height = 64.dp)
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
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                video.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                formatPublishedAt(video.publishedAtEpochSeconds),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                video.author.ifBlank { "未知 UP 主" },
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
private fun CreatorSelectionSheet(
    accountLoggedIn: Boolean,
    creators: List<BilibiliCreator>,
    selectedCreators: List<BilibiliCreator>,
    loading: Boolean,
    saving: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onOpenAccount: () -> Unit,
    onSave: (List<BilibiliCreator>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var keyword by remember { mutableStateOf("") }
    var selectedMids by remember(creators, selectedCreators) {
        mutableStateOf(selectedCreators.map(BilibiliCreator::mid).toSet())
    }
    val visibleCreators = remember(creators, keyword) {
        val normalized = keyword.trim()
        if (normalized.isEmpty()) creators else creators.filter { creator ->
            creator.name.contains(normalized, ignoreCase = true)
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.widthIn(max = 840.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        // long: 内容范围包含可滚动的长关注列表，弹层直接占满可用高度，并把保存动作留在滚动区之外持续可见。
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("首页内容范围", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "从关注列表选择 UP，作品按发布时间倒排",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭首页内容范围设置")
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (!accountLoggedIn) {
                    BiuEmptyState(
                        icon = Icons.Rounded.AccountCircle,
                        title = "需要登录 Bilibili",
                        message = "登录后才能读取你的关注列表并选择 UP",
                        actionLabel = "前往账号页",
                        onAction = onOpenAccount,
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            singleLine = true,
                            placeholder = { Text("按 UP 名称搜索") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            trailingIcon = {
                                if (keyword.isNotEmpty()) {
                                    IconButton(onClick = { keyword = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "清空 UP 搜索")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                        )
                        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        if (!loading && creators.isEmpty()) {
                            BiuEmptyState(
                                icon = Icons.Rounded.AccountCircle,
                                title = "暂时没有关注列表",
                                message = "可以重试从 Bilibili 获取",
                                actionLabel = "重新加载",
                                onAction = onRetry,
                                modifier = Modifier.weight(1f),
                            )
                        } else if (!loading && visibleCreators.isEmpty()) {
                            BiuEmptyState(
                                icon = Icons.Rounded.Search,
                                title = "没有匹配的 UP",
                                message = "换一个名称关键词再试试",
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(vertical = 2.dp),
                            ) {
                                items(visibleCreators, key = BilibiliCreator::mid) { creator ->
                                    val selected = creator.mid in selectedMids
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .semantics {
                                                role = Role.Checkbox
                                                this.selected = selected
                                                stateDescription = if (selected) "已选择" else "未选择"
                                            }
                                            .clickable {
                                                selectedMids = if (selected) {
                                                    selectedMids - creator.mid
                                                } else {
                                                    selectedMids + creator.mid
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        AsyncImage(
                                            model = creator.faceUrl,
                                            contentDescription = creator.name,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(21.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentScale = ContentScale.Crop,
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(1.dp),
                                        ) {
                                            Text(
                                                creator.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                            Text(
                                                "UID ${creator.mid}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Surface(
                                            modifier = Modifier.size(28.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHighest
                                            },
                                            contentColor = if (selected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (selected) {
                                                    Icon(
                                                        Icons.Rounded.Check,
                                                        contentDescription = "已选择 ${creator.name}",
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    MediaDivider(start = 70.dp)
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { onSave(creators.filter { creator -> creator.mid in selectedMids }) },
                enabled = accountLoggedIn && !loading && !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    when {
                        !accountLoggedIn -> "登录后保存"
                        saving -> "正在保存"
                        selectedMids.isEmpty() -> "恢复音乐区和音乐榜"
                        else -> "保存 ${selectedMids.size} 位 UP"
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiPageSelectionSheet(
    selection: VideoPageSelection,
    loading: Boolean,
    onDismiss: () -> Unit,
    onPlayPage: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (!loading) onDismiss() },
        sheetState = sheetState,
        modifier = Modifier.widthIn(max = 840.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        // long: 分 P 可能很多，弹层打开即使用最大高度，把剩余空间全部交给列表，避免用户先拖动弹层才能浏览。
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
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
                    .weight(1f),
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
                                style = MaterialTheme.typography.bodySmall,
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
    onOpenNowPlaying: () -> Unit,
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
            .height(108.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenNowPlaying)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (snapshot.artworkUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
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
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        displayResourceTitle(snapshot.title, snapshot.pageTitle),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
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
            }
            IconButton(onClick = onPrevious, enabled = controllerReady && snapshot.hasPrevious) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "上一首", modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = onToggle, enabled = controllerReady) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(17.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (snapshot.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (snapshot.isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
            }
            IconButton(onClick = onNext, enabled = controllerReady && snapshot.hasNext) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "下一首", modifier = Modifier.size(30.dp))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                formatDurationMs(displayedPositionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BiuPlaybackSlider(
                value = sliderValue,
                bufferedValue = progress.bufferedFraction,
                onValueChange = { value ->
                    isDragging = true
                    dragFraction = value
                },
                onValueChangeFinished = { value ->
                    onSeek(PlaybackProgressPolicy.seekPositionMs(value, progress.durationMs))
                    isDragging = false
                },
                modifier = Modifier.weight(1f),
                enabled = controllerReady && progress.isSeekable,
            )
            Text(
                formatDurationMs(progress.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NowPlayingScreen(
    snapshot: PlaybackSnapshot,
    controllerReady: Boolean,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSelectQueueItem: (Int) -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var showQueue by remember { mutableStateOf(false) }
    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            PlaybackQueue(
                snapshot = snapshot,
                onSelectQueueItem = onSelectQueueItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp),
            )
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("正在播放") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        NowPlayingDetails(
            snapshot = snapshot,
            controllerReady = controllerReady,
            onPrevious = onPrevious,
            onToggle = onToggle,
            onNext = onNext,
            onSeek = onSeek,
            onShowQueue = { showQueue = true },
            isLandscape = isLandscape,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@Composable
private fun NowPlayingDetails(
    snapshot: PlaybackSnapshot,
    controllerReady: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShowQueue: () -> Unit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
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
    val controls: @Composable (Modifier) -> Unit = { controlsModifier ->
        NowPlayingControls(
            snapshot = snapshot,
            progress = progress,
            sliderValue = sliderValue,
            displayedPositionMs = displayedPositionMs,
            controllerReady = controllerReady,
            onPrevious = onPrevious,
            onToggle = onToggle,
            onNext = onNext,
            onShowQueue = onShowQueue,
            onValueChange = { value ->
                isDragging = true
                dragFraction = value
            },
            onValueChangeFinished = { value ->
                onSeek(PlaybackProgressPolicy.seekPositionMs(value, progress.durationMs))
                isDragging = false
            },
            modifier = controlsModifier,
        )
    }
    if (isLandscape) {
        Row(
            modifier = modifier.padding(horizontal = 32.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            NowPlayingArtwork(snapshot, 240.dp)
            controls(Modifier.weight(1f))
        }
    } else {
        Column(
            modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            NowPlayingArtwork(snapshot, 300.dp)
            Spacer(Modifier.height(24.dp))
            controls(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun NowPlayingArtwork(snapshot: PlaybackSnapshot, size: androidx.compose.ui.unit.Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        if (snapshot.artworkUrl.isNullOrBlank()) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Album,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            AsyncImage(
                model = snapshot.artworkUrl,
                contentDescription = snapshot.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun NowPlayingControls(
    snapshot: PlaybackSnapshot,
    progress: PlaybackProgress,
    sliderValue: Float,
    displayedPositionMs: Long,
    controllerReady: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onShowQueue: () -> Unit,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            displayResourceTitle(snapshot.title, snapshot.pageTitle),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            snapshot.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                snapshot.pageTitle.orEmpty(),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = onShowQueue) {
                Icon(
                    Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "打开播放列表",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        BiuPlaybackSlider(
            value = sliderValue,
            bufferedValue = progress.bufferedFraction,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = controllerReady && progress.isSeekable,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                formatDurationMs(displayedPositionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                formatDurationMs(progress.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious, enabled = controllerReady && snapshot.hasPrevious) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "上一首", modifier = Modifier.size(34.dp))
            }
            FilledIconButton(
                onClick = onToggle,
                enabled = controllerReady,
                modifier = Modifier.size(52.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    if (snapshot.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (snapshot.isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(onClick = onNext, enabled = controllerReady && snapshot.hasNext) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "下一首", modifier = Modifier.size(34.dp))
            }
        }
    }
}

@Composable
private fun PlaybackQueue(
    snapshot: PlaybackSnapshot,
    onSelectQueueItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("播放列表 · ${snapshot.queueItems.size}", style = MaterialTheme.typography.titleMedium)
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(snapshot.queueItems, key = { item -> "${item.index}:${item.mediaId}" }) { item ->
                val isCurrent = item.index == snapshot.currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        )
                        .clickable { onSelectQueueItem(item.index) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (item.artworkUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Album, contentDescription = null, modifier = Modifier.size(22.dp))
                        }
                    } else {
                        AsyncImage(
                            model = item.artworkUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            item.pageTitle ?: item.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Text(
                            item.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isCurrent) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "当前播放", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun BiuPlaybackSlider(
    value: Float,
    bufferedValue: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val normalizedValue = value.coerceIn(0f, 1f)
    val normalizedBuffered = bufferedValue.coerceIn(normalizedValue, 1f)
    val activeColor = MaterialTheme.colorScheme.primary
    val bufferedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val inactiveColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    Canvas(
        modifier = modifier
            .height(48.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(normalizedValue, 0f..1f)
                contentDescription = "播放进度"
                if (enabled) {
                    setProgress { targetValue ->
                        val adjusted = targetValue.coerceIn(0f, 1f)
                        onValueChange(adjusted)
                        onValueChangeFinished(adjusted)
                        true
                    }
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var finalValue = (down.position.x / size.width).coerceIn(0f, 1f)
                    onValueChange(finalValue)
                    down.consume()
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change != null) {
                            finalValue = (change.position.x / size.width).coerceIn(0f, 1f)
                            onValueChange(finalValue)
                            change.consume()
                        }
                    } while (change?.pressed == true)
                    onValueChangeFinished(finalValue)
                }
            },
    ) {
        val startX = 5.dp.toPx()
        val endX = size.width - startX
        val trackWidth = (endX - startX).coerceAtLeast(0f)
        val centerY = size.height / 2f
        val trackColor = if (enabled) inactiveColor else disabledColor
        drawLine(trackColor, Offset(startX, centerY), Offset(endX, centerY), 2.dp.toPx(), StrokeCap.Round)
        if (enabled) {
            drawLine(
                bufferedColor,
                Offset(startX, centerY),
                Offset(startX + trackWidth * normalizedBuffered, centerY),
                2.dp.toPx(),
                StrokeCap.Round,
            )
            drawLine(
                activeColor,
                Offset(startX, centerY),
                Offset(startX + trackWidth * normalizedValue, centerY),
                2.dp.toPx(),
                StrokeCap.Round,
            )
            drawCircle(activeColor, radius = 4.dp.toPx(), center = Offset(startX + trackWidth * normalizedValue, centerY))
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

private fun formatPublishedAt(epochSeconds: Long?): String {
    val instant = epochSeconds?.takeIf { it > 0L }?.let(Instant::ofEpochSecond)
    return instant?.let { "发布时间 · ${PUBLISHED_AT_FORMATTER.format(it)}" } ?: "发布时间 · 未知"
}

private fun displayResourceTitle(title: String, pageTitle: String?): String {
    val pageName = pageTitle?.substringAfter(" · ", missingDelimiterValue = "")?.takeIf(String::isNotBlank)
    return pageName?.let { title.removeSuffix(" · $it") } ?: title
}

private fun formatProgress(positionMs: Long, durationMs: Long): String {
    val position = formatDuration((positionMs.coerceAtLeast(0L) / 1000L).toInt())
    val duration = durationMs.takeIf { it > 0L }
        ?.let { formatDuration((it / 1000L).toInt()) }
    return if (duration == null) "已播放 $position" else "已播放 $position / $duration"
}

private fun formatProgressSeconds(positionSeconds: Int, durationSeconds: Int?): String {
    val position = formatDuration(positionSeconds.coerceAtLeast(0))
    val duration = durationSeconds?.takeIf { it > 0 }?.let(::formatDuration)
    return if (duration == null) "已播放 $position" else "已播放 $position / $duration"
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

private val PUBLISHED_AT_FORMATTER: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())
