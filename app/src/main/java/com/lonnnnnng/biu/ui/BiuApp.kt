package com.lonnnnnng.biu.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
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
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.core.model.Track
import com.lonnnnnng.biu.core.model.bilibiliSource
import com.lonnnnnng.biu.core.model.toMediaItem
import com.lonnnnnng.biu.data.bilibili.AccountLibrarySection
import com.lonnnnnng.biu.data.bilibili.BilibiliAccount
import com.lonnnnnng.biu.data.bilibili.BilibiliFavoriteFolder
import com.lonnnnnng.biu.data.bilibili.BilibiliFavoriteFolderType
import com.lonnnnnng.biu.data.bilibili.BilibiliCreator
import com.lonnnnnng.biu.data.bilibili.BilibiliLibraryVideo
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import com.lonnnnnng.biu.data.bilibili.HomeFeedMode
import com.lonnnnnng.biu.data.bilibili.RecommendFeed
import com.lonnnnnng.biu.data.local.AudioDownloadTaskEntity
import com.lonnnnnng.biu.data.local.AppThemeMode
import com.lonnnnnng.biu.data.local.PlaybackHistoryEntity
import com.lonnnnnng.biu.data.local.LocalAudio
import com.lonnnnnng.biu.data.local.LocalAudioDirectory
import com.lonnnnnng.biu.data.local.LocalMediaPermissionPolicy
import com.lonnnnnng.biu.data.local.VideoDownloadTaskEntity
import com.lonnnnnng.biu.data.lyrics.LrcParser
import com.lonnnnnng.biu.data.lyrics.LyricsSearchResult
import com.lonnnnnng.biu.data.update.AppUpdate
import com.lonnnnnng.biu.download.AudioDownloadRequest
import com.lonnnnnng.biu.download.AudioDownloadStatus
import com.lonnnnnng.biu.download.DownloadNetworkPreference
import com.lonnnnnng.biu.download.FavoriteBatchDownloadKind
import com.lonnnnnng.biu.download.VideoDownloadRequest
import com.lonnnnnng.biu.download.VideoDownloadStatus
import com.lonnnnnng.biu.playback.PlaybackService
import com.lonnnnnng.biu.playback.PlaybackMode
import com.lonnnnnng.biu.playback.PlaybackSpeedPolicy
import com.lonnnnnng.biu.playback.applyPlaybackMode
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
    val playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL,
    val playbackSpeed: Float = PlaybackSpeedPolicy.DEFAULT,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isSeekable: Boolean = false,
    val currentIndex: Int = 0,
    val queueItems: List<PlaybackQueueItem> = emptyList(),
    val downloadRequest: AudioDownloadRequest? = null,
    val videoDownloadRequest: VideoDownloadRequest? = null,
    val bilibiliSource: BilibiliTrackSource? = null,
)

private enum class DownloadTaskKind(val label: String) {
    AUDIO("音频"),
    VIDEO("视频"),
}

private sealed interface PendingDownload {
    data class Audio(val request: AudioDownloadRequest) : PendingDownload
    data class Video(val request: VideoDownloadRequest) : PendingDownload
    data class FavoriteBatch(
        val kind: FavoriteBatchDownloadKind,
        val videos: List<BilibiliLibraryVideo>,
    ) : PendingDownload
}

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
    var showThemeMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showCreatorConfig by remember { mutableStateOf(false) }
    var favoritePickerVideo by remember { mutableStateOf<BilibiliVideo?>(null) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showDownloads by remember { mutableStateOf(false) }
    var downloadTaskKind by remember { mutableStateOf(DownloadTaskKind.AUDIO) }
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }
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
    val localAudioDirectoryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        treeUri?.let(viewModel::selectLocalAudioDirectory)
    }
    val downloadPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val request = pendingDownload
        pendingDownload = null
        if (request == null) return@rememberLauncherForActivityResult
        val legacyStorageGranted = Build.VERSION.SDK_INT > Build.VERSION_CODES.P ||
            grants[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        if (!legacyStorageGranted) {
            coroutineScope.launch { snackbarHostState.showSnackbar("需要存储权限才能保存下载内容") }
            return@rememberLauncherForActivityResult
        }
        when (request) {
            is PendingDownload.Audio -> viewModel.startAudioDownload(request.request)
            is PendingDownload.Video -> viewModel.startVideoDownload(request.request)
            is PendingDownload.FavoriteBatch -> viewModel.startFavoriteBatchDownload(request.kind, request.videos)
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            grants[Manifest.permission.POST_NOTIFICATIONS] == false
        ) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("下载会继续，但通知权限关闭时系统通知栏不显示进度")
            }
        }
    }
    val startOrRequestDownload: (PendingDownload) -> Unit = { request ->
        val missingPermissions = buildList {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        if (missingPermissions.isEmpty()) {
            when (request) {
                is PendingDownload.Audio -> viewModel.startAudioDownload(request.request)
                is PendingDownload.Video -> viewModel.startVideoDownload(request.request)
                is PendingDownload.FavoriteBatch -> viewModel.startFavoriteBatchDownload(request.kind, request.videos)
            }
        } else {
            pendingDownload = request
            downloadPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
    val requestAudioDownload: () -> Unit = {
        val request = controller?.currentMediaItem?.let(AudioDownloadRequest::fromMediaItem)
        if (request == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar("当前内容不是可下载的 Bilibili 在线音频") }
        } else {
            startOrRequestDownload(PendingDownload.Audio(request))
        }
    }
    val requestVideoDownload: () -> Unit = {
        val request = controller?.currentMediaItem?.let(VideoDownloadRequest::fromMediaItem)
        if (request == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar("当前内容不是可下载的 Bilibili 在线视频") }
        } else {
            startOrRequestDownload(PendingDownload.Video(request))
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
                    playbackMode = PlaybackMode.fromPlayer(
                        activeController.repeatMode,
                        activeController.shuffleModeEnabled,
                    ),
                    playbackSpeed = PlaybackSpeedPolicy.normalize(activeController.playbackParameters.speed),
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
                    downloadRequest = activeController.currentMediaItem?.let(AudioDownloadRequest::fromMediaItem),
                    videoDownloadRequest = activeController.currentMediaItem?.let(VideoDownloadRequest::fromMediaItem),
                    bilibiliSource = activeController.currentMediaItem?.bilibiliSource(),
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

    uiState.favoriteBatchFolder?.let { folder ->
        FavoriteBatchDownloadSheet(
            folder = folder,
            videos = uiState.favoriteBatchVideos,
            loading = uiState.isFavoriteBatchLoading,
            submitting = uiState.isFavoriteBatchSubmitting,
            onDismiss = viewModel::dismissFavoriteBatchDownload,
            onStart = { kind, videos ->
                startOrRequestDownload(PendingDownload.FavoriteBatch(kind, videos))
            },
        )
    }

    favoritePickerVideo?.let { video ->
        FavoritePickerDialog(
            video = video,
            folders = uiState.createdFavoriteFolders.filter(BilibiliFavoriteFolder::isUserManaged),
            mutating = uiState.isFavoriteMutating,
            onDismiss = { favoritePickerVideo = null },
            onSelect = { folder ->
                favoritePickerVideo = null
                viewModel.addVideoToFavorite(video, folder)
            },
            onOpenAccount = {
                favoritePickerVideo = null
                viewModel.selectSection(MainSection.ACCOUNT)
                viewModel.loadLibrary(AccountLibrarySection.FAVORITES)
            },
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

    if (showDownloads) {
        ModalBottomSheet(
            onDismissRequest = { showDownloads = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            DownloadTaskPanel(
                selectedKind = downloadTaskKind,
                onSelectedKindChange = { downloadTaskKind = it },
                audioTasks = uiState.audioDownloads,
                videoTasks = uiState.videoDownloads,
                networkPreference = uiState.downloadNetworkPreference,
                onNetworkPreferenceChange = viewModel::setDownloadUnmeteredOnly,
                onRetryFailedAudio = viewModel::retryFailedAudioDownloads,
                onRetryFailedVideo = viewModel::retryFailedVideoDownloads,
                onResumeAudio = viewModel::resumeAudioDownload,
                onPauseAudio = viewModel::pauseAudioDownload,
                onCancelAudio = viewModel::cancelAudioDownload,
                onResumeVideo = viewModel::resumeVideoDownload,
                onPauseVideo = viewModel::pauseVideoDownload,
                onCancelVideo = viewModel::cancelVideoDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp, max = 640.dp),
            )
        }
    }

    if (showNowPlaying && playback.mediaId.isNotBlank()) {
        BackHandler { showNowPlaying = false }
        NowPlayingScreen(
            snapshot = playback,
            lyrics = uiState.lyrics,
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
            onPlaybackModeChange = { mode -> controller?.applyPlaybackMode(mode) },
            onPlaybackSpeedChange = { speed -> controller?.setPlaybackSpeed(PlaybackSpeedPolicy.normalize(speed)) },
            onDownload = requestAudioDownload,
            onVideoDownload = requestVideoDownload,
            onShowDownloads = { showDownloads = true },
            onSelectQueueItem = { index ->
                controller?.seekToDefaultPosition(index)
                controller?.play()
            },
            onMoveQueueItemNext = { item ->
                controller?.let { activeController ->
                    val currentMediaId = activeController.currentMediaItem?.mediaId.orEmpty()
                    if (activeController.moveQueueItemNext(item.mediaId)) {
                        viewModel.movePlaybackQueueItemNext(item.mediaId, currentMediaId)
                    }
                }
            },
            onRemoveQueueItem = { item ->
                controller?.let { activeController ->
                    if (activeController.removeQueueItem(item.mediaId)) {
                        viewModel.removePlaybackQueueItem(item.mediaId)
                    }
                }
            },
            onClearQueue = {
                controller?.let { activeController ->
                    viewModel.clearPlaybackQueue()
                    activeController.stop()
                    activeController.clearMediaItems()
                }
            },
            onPrepareLyrics = {
                viewModel.prepareLyrics(
                    source = playback.bilibiliSource,
                    mediaId = playback.mediaId,
                )
            },
            onSearchLyrics = viewModel::searchLyrics,
            onSelectLyrics = viewModel::selectLyrics,
        )
        return
    }

    Scaffold(
        topBar = {
            BiuTopBar(
                section = uiState.section,
                themeMode = uiState.themeMode,
                themeMenuExpanded = showThemeMenu,
                qualityPreference = uiState.qualityPreference,
                qualityMenuExpanded = showQualityMenu,
                account = uiState.account,
                accountMenuExpanded = showAccountMenu,
                isAccountLoading = uiState.isAccountLoading,
                isUpdateChecking = uiState.isUpdateChecking,
                onShowThemeMenu = {
                    showQualityMenu = false
                    showAccountMenu = false
                    showThemeMenu = true
                },
                onDismissThemeMenu = { showThemeMenu = false },
                onThemeSelected = { mode ->
                    showThemeMenu = false
                    viewModel.selectThemeMode(mode)
                },
                onShowQualityMenu = {
                    showThemeMenu = false
                    showAccountMenu = false
                    showQualityMenu = true
                },
                onDismissQualityMenu = { showQualityMenu = false },
                onQualitySelected = { preference ->
                    showQualityMenu = false
                    viewModel.selectQualityPreference(preference)
                },
                onShowAccountMenu = {
                    showThemeMenu = false
                    showQualityMenu = false
                    showAccountMenu = true
                },
                onDismissAccountMenu = { showAccountMenu = false },
                onLogin = {
                    showAccountMenu = false
                    showLogin = true
                },
                onRefreshAccount = {
                    showAccountMenu = false
                    viewModel.refreshAccount()
                },
                onShowDownloads = {
                    showAccountMenu = false
                    showDownloads = true
                },
                onCheckUpdate = {
                    showAccountMenu = false
                    viewModel.checkForUpdate()
                },
                onLogout = {
                    showAccountMenu = false
                    viewModel.logout()
                },
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
                    onAddFavorite = { video ->
                        if (uiState.account.isLoggedIn) favoritePickerVideo = video else showLogin = true
                    },
                    modifier = pageModifier,
                )
                MainSection.ACCOUNT -> AccountScreen(
                    state = uiState,
                    localAudioPermissionGranted = localAudioPermissionGranted,
                    onLogin = { showLogin = true },
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
                    onSelectLocalAudioDirectory = {
                        val initialUri = uiState.localAudioDirectory?.treeUri?.let(Uri::parse)
                        localAudioDirectoryLauncher.launch(initialUri)
                    },
                    onClearLocalAudioDirectory = viewModel::clearLocalAudioDirectory,
                    onOpenFavoriteFolder = viewModel::openFavoriteFolder,
                    onCloseFavoriteFolder = viewModel::closeFavoriteFolder,
                    onOpenFavoriteBatchDownload = viewModel::openFavoriteBatchDownload,
                    onLoadMoreFavoriteFolder = viewModel::loadMoreFavoriteFolder,
                    onCreateFavoriteFolder = viewModel::createFavoriteFolder,
                    onRenameFavoriteFolder = viewModel::renameFavoriteFolder,
                    onDeleteFavoriteFolder = viewModel::deleteFavoriteFolder,
                    onRemoveFavoriteVideo = viewModel::removeVideoFromFavorite,
                    onPlay = viewModel::play,
                    onPlayHistory = viewModel::play,
                    onPlayLocalAudio = viewModel::play,
                    onClearLocalHistory = viewModel::clearLocalHistory,
                    onSearchOnlineHistory = viewModel::searchOnlineHistory,
                    onLoadMoreOnlineHistory = viewModel::loadMoreOnlineHistory,
                    onDeleteOnlineHistory = viewModel::deleteOnlineHistory,
                    onClearOnlineHistory = viewModel::clearOnlineHistory,
                    onReportPlayHistoryChange = viewModel::setReportPlayHistory,
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
    themeMode: AppThemeMode,
    themeMenuExpanded: Boolean,
    qualityPreference: AudioQualityPreference,
    qualityMenuExpanded: Boolean,
    account: BilibiliAccount,
    accountMenuExpanded: Boolean,
    isAccountLoading: Boolean,
    isUpdateChecking: Boolean,
    onShowThemeMenu: () -> Unit,
    onDismissThemeMenu: () -> Unit,
    onThemeSelected: (AppThemeMode) -> Unit,
    onShowQualityMenu: () -> Unit,
    onDismissQualityMenu: () -> Unit,
    onQualitySelected: (AudioQualityPreference) -> Unit,
    onShowAccountMenu: () -> Unit,
    onDismissAccountMenu: () -> Unit,
    onLogin: () -> Unit,
    onRefreshAccount: () -> Unit,
    onShowDownloads: () -> Unit,
    onCheckUpdate: () -> Unit,
    onLogout: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(section.label, style = MaterialTheme.typography.titleLarge)
        },
        actions = {
            Box {
                IconButton(onClick = onShowThemeMenu) {
                    Icon(
                        imageVector = when (themeMode) {
                            AppThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                            AppThemeMode.LIGHT -> Icons.Rounded.LightMode
                            AppThemeMode.DARK -> Icons.Rounded.DarkMode
                        },
                        contentDescription = "主题：${themeMode.label}",
                    )
                }
                DropdownMenu(
                    expanded = themeMenuExpanded,
                    onDismissRequest = onDismissThemeMenu,
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            leadingIcon = {
                                if (themeMode == mode) {
                                    Icon(Icons.Rounded.Check, contentDescription = "已选择")
                                } else {
                                    Spacer(Modifier.size(24.dp))
                                }
                            },
                            onClick = { onThemeSelected(mode) },
                        )
                    }
                }
            }
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
            Box {
                IconButton(onClick = onShowAccountMenu) {
                    Icon(
                        Icons.Rounded.AccountCircle,
                        contentDescription = if (accountMenuExpanded) "关闭账号菜单" else "打开账号菜单",
                    )
                }
                AccountDropdownMenu(
                    account = account,
                    expanded = accountMenuExpanded,
                    isAccountLoading = isAccountLoading,
                    isUpdateChecking = isUpdateChecking,
                    onDismiss = onDismissAccountMenu,
                    onLogin = onLogin,
                    onRefresh = onRefreshAccount,
                    onShowDownloads = onShowDownloads,
                    onCheckUpdate = onCheckUpdate,
                    onLogout = onLogout,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun AccountDropdownMenu(
    account: BilibiliAccount,
    expanded: Boolean,
    isAccountLoading: Boolean,
    isUpdateChecking: Boolean,
    onDismiss: () -> Unit,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
    onShowDownloads: () -> Unit,
    onCheckUpdate: () -> Unit,
    onLogout: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 264.dp, max = 320.dp),
    ) {
        // long: 账号摘要上移到顶栏菜单，账号页首屏只保留音乐库和收藏内容。
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (account.isLoggedIn && account.faceUrl.isNotBlank()) {
                AsyncImage(
                    model = account.faceUrl,
                    contentDescription = "${account.name}的头像",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (account.isLoggedIn) account.name.ifBlank { "已登录" } else "未登录",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = if (account.isLoggedIn) "Bilibili 账号已连接" else "登录后同步在线音乐库",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (!account.isLoggedIn) {
            DropdownMenuItem(
                text = { Text("登录") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null) },
                onClick = onLogin,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        DropdownMenuItem(
            text = { Text(if (isAccountLoading) "刷新中" else "刷新") },
            leadingIcon = {
                if (isAccountLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                }
            },
            enabled = !isAccountLoading,
            onClick = onRefresh,
        )
        DropdownMenuItem(
            text = { Text("下载") },
            leadingIcon = { Icon(Icons.Rounded.Downloading, contentDescription = null) },
            onClick = onShowDownloads,
        )
        DropdownMenuItem(
            text = { Text(if (isUpdateChecking) "检查中" else "更新") },
            leadingIcon = {
                if (isUpdateChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = null)
                }
            },
            enabled = !isUpdateChecking,
            onClick = onCheckUpdate,
        )
        if (account.isLoggedIn) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DropdownMenuItem(
                text = { Text("退出登录", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = onLogout,
            )
        }
    }
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
                // long: 底部 Tab 标签需要贴住物理屏幕底边，因此不消费系统手势区 Insets，允许手势条覆盖导航区域。
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                MainSection.entries.forEach { section ->
                    val icon = section.icon()
                    val isSelected = selectedSection == section
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onSectionSelected(section) },
                        // long: 选中指示器比普通图标更高，分别补偿后两种状态的图标与标签组合都能视觉居中。
                        modifier = Modifier.offset(y = if (isSelected) 3.dp else 0.dp),
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
    onAddFavorite: (BilibiliVideo) -> Unit,
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
            VideoList(activeVideos, resolvingBvid, onPlay, onAddFavorite, Modifier.weight(1f))
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
    onLoadLibrary: (AccountLibrarySection) -> Unit,
    onRequestLocalAudioPermission: () -> Unit,
    onSelectLocalAudioDirectory: () -> Unit,
    onClearLocalAudioDirectory: () -> Unit,
    onOpenFavoriteFolder: (BilibiliFavoriteFolder) -> Unit,
    onCloseFavoriteFolder: () -> Unit,
    onOpenFavoriteBatchDownload: (BilibiliFavoriteFolder) -> Unit,
    onLoadMoreFavoriteFolder: () -> Unit,
    onCreateFavoriteFolder: (String) -> Unit,
    onRenameFavoriteFolder: (BilibiliFavoriteFolder, String) -> Unit,
    onDeleteFavoriteFolder: (BilibiliFavoriteFolder) -> Unit,
    onRemoveFavoriteVideo: (BilibiliLibraryVideo) -> Unit,
    onPlay: (BilibiliVideo) -> Unit,
    onPlayHistory: (PlaybackHistoryEntity) -> Unit,
    onPlayLocalAudio: (LocalAudio) -> Unit,
    onClearLocalHistory: () -> Unit,
    onSearchOnlineHistory: (String) -> Unit,
    onLoadMoreOnlineHistory: () -> Unit,
    onDeleteOnlineHistory: (BilibiliLibraryVideo) -> Unit,
    onClearOnlineHistory: () -> Unit,
    onReportPlayHistoryChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.isAccountLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        AccountLibraryNavigation(
            selectedSection = state.librarySection,
            onSelect = onLoadLibrary,
        )

        if (state.isLibraryLoading && state.librarySection != AccountLibrarySection.ONLINE_HISTORY) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
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
                    createdFolders = state.createdFavoriteFolders,
                    collectedFolders = state.collectedFavoriteFolders,
                    selectedFolder = state.selectedFavoriteFolder,
                    videos = state.libraryVideos,
                    resolvingBvid = state.resolvingBvid,
                    loading = state.isLibraryLoading,
                    loadingMore = state.isFavoriteLoadingMore,
                    mutating = state.isFavoriteMutating,
                    onOpenFolder = onOpenFavoriteFolder,
                    onCloseFolder = onCloseFavoriteFolder,
                    onBatchDownload = onOpenFavoriteBatchDownload,
                    onLoadMore = onLoadMoreFavoriteFolder,
                    onCreateFolder = onCreateFavoriteFolder,
                    onRenameFolder = onRenameFavoriteFolder,
                    onDeleteFolder = onDeleteFavoriteFolder,
                    onRemoveVideo = onRemoveFavoriteVideo,
                    onPlay = onPlay,
                    modifier = Modifier.weight(1f),
                )
                AccountLibrarySection.ONLINE_HISTORY -> OnlineHistoryList(
                    videos = state.libraryVideos,
                    resolvingBvid = state.resolvingBvid,
                    loading = state.isLibraryLoading,
                    loadingMore = state.isOnlineHistoryLoadingMore,
                    mutating = state.isOnlineHistoryMutating,
                    query = state.onlineHistoryQuery,
                    reportPlayHistory = state.reportPlayHistory,
                    onPlay = onPlay,
                    onSearch = onSearchOnlineHistory,
                    onLoadMore = onLoadMoreOnlineHistory,
                    onDelete = onDeleteOnlineHistory,
                    onClear = onClearOnlineHistory,
                    onReportPlayHistoryChange = onReportPlayHistoryChange,
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
                    directory = state.localAudioDirectory,
                    permissionGranted = localAudioPermissionGranted,
                    loading = state.isLocalAudioLoading,
                    onRequestPermission = onRequestLocalAudioPermission,
                    onSelectDirectory = onSelectLocalAudioDirectory,
                    onClearDirectory = onClearLocalAudioDirectory,
                    onRefresh = { onLoadLibrary(AccountLibrarySection.LOCAL_MUSIC) },
                    onPlay = onPlayLocalAudio,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AccountLibraryNavigation(
    selectedSection: AccountLibrarySection,
    onSelect: (AccountLibrarySection) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AccountLibraryGroupTitle("在线音乐库")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccountLibraryDestination(
                section = AccountLibrarySection.FAVORITES,
                subtitle = "收藏夹与合集",
                icon = Icons.Rounded.Favorite,
                selected = selectedSection == AccountLibrarySection.FAVORITES,
                onClick = onSelect,
                modifier = Modifier.weight(1f),
            )
            AccountLibraryDestination(
                section = AccountLibrarySection.ONLINE_HISTORY,
                subtitle = "B站播放记录",
                icon = Icons.Rounded.History,
                selected = selectedSection == AccountLibrarySection.ONLINE_HISTORY,
                onClick = onSelect,
                modifier = Modifier.weight(1f),
            )
        }
        AccountLibraryGroupTitle("本地内容")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccountLibraryDestination(
                section = AccountLibrarySection.LOCAL_HISTORY,
                subtitle = "本机播放记录",
                icon = Icons.Rounded.Album,
                selected = selectedSection == AccountLibrarySection.LOCAL_HISTORY,
                onClick = onSelect,
                modifier = Modifier.weight(1f),
            )
            AccountLibraryDestination(
                section = AccountLibrarySection.LOCAL_MUSIC,
                subtitle = "设备音频文件",
                icon = Icons.Rounded.MusicNote,
                selected = selectedSection == AccountLibrarySection.LOCAL_MUSIC,
                onClick = onSelect,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AccountLibraryGroupTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AccountLibraryDestination(
    section: AccountLibrarySection,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: (AccountLibrarySection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable { onClick(section) },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(section.label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun LocalAudioList(
    audio: List<LocalAudio>,
    directory: LocalAudioDirectory?,
    permissionGranted: Boolean,
    loading: Boolean,
    onRequestPermission: () -> Unit,
    onSelectDirectory: () -> Unit,
    onClearDirectory: () -> Unit,
    onRefresh: () -> Unit,
    onPlay: (LocalAudio) -> Unit,
    modifier: Modifier = Modifier,
) {
    val directoryFilteringSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
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
        if (permissionGranted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = directory != null,
                    onClick = onSelectDirectory,
                    enabled = !loading && directoryFilteringSupported,
                    label = {
                        Text(
                            when {
                                directory != null -> "目录：${directory.displayName}"
                                directoryFilteringSupported -> "筛选音乐目录"
                                else -> "目录筛选需 Android 10+"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                if (directory != null) {
                    TextButton(onClick = onClearDirectory, enabled = !loading) {
                        Text("显示全部")
                    }
                }
            }
            if (directory != null) {
                Text(
                    "仅显示该目录及其子目录",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                message = directory?.let { "“${it.displayName}”及其子目录中没有可播放的音乐。" }
                    ?: "把音频保存到系统 Music 目录后点击右上角重新扫描。",
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
private fun FavoritePickerDialog(
    video: BilibiliVideo,
    folders: List<BilibiliFavoriteFolder>,
    mutating: Boolean,
    onDismiss: () -> Unit,
    onSelect: (BilibiliFavoriteFolder) -> Unit,
    onOpenAccount: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("收藏到") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    video.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (folders.isEmpty()) {
                    Text("还没有可用的自建收藏夹，请先在账号页新建。", style = MaterialTheme.typography.bodyMedium)
                } else {
                    folders.forEach { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(enabled = !mutating) { onSelect(folder) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Rounded.Folder, contentDescription = null)
                            Text(
                                folder.title,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "${folder.mediaCount} 项",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (folders.isEmpty()) TextButton(onClick = onOpenAccount) { Text("前往账号页") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun FavoriteFolderNameDialog(
    title: String,
    initialName: String,
    mutating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("收藏夹名称") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank() && !mutating,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun FavoriteLibrary(
    createdFolders: List<BilibiliFavoriteFolder>,
    collectedFolders: List<BilibiliFavoriteFolder>,
    selectedFolder: BilibiliFavoriteFolder?,
    videos: List<BilibiliLibraryVideo>,
    resolvingBvid: String?,
    loading: Boolean,
    loadingMore: Boolean,
    mutating: Boolean,
    onOpenFolder: (BilibiliFavoriteFolder) -> Unit,
    onCloseFolder: () -> Unit,
    onBatchDownload: (BilibiliFavoriteFolder) -> Unit,
    onLoadMore: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFolder: (BilibiliFavoriteFolder, String) -> Unit,
    onDeleteFolder: (BilibiliFavoriteFolder) -> Unit,
    onRemoveVideo: (BilibiliLibraryVideo) -> Unit,
    onPlay: (BilibiliVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var createdExpanded by rememberSaveable { mutableStateOf(true) }
    var collectedExpanded by rememberSaveable { mutableStateOf(true) }
    var namingFolder by remember { mutableStateOf<BilibiliFavoriteFolder?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deletingFolder by remember { mutableStateOf<BilibiliFavoriteFolder?>(null) }
    var selectedMenuExpanded by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        FavoriteFolderNameDialog(
            title = "新建收藏夹",
            initialName = "",
            mutating = mutating,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                showCreateDialog = false
                onCreateFolder(name)
            },
        )
    }
    namingFolder?.let { folder ->
        FavoriteFolderNameDialog(
            title = "重命名收藏夹",
            initialName = folder.title,
            mutating = mutating,
            onDismiss = { namingFolder = null },
            onConfirm = { name ->
                namingFolder = null
                onRenameFolder(folder, name)
            },
        )
    }
    deletingFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { deletingFolder = null },
            title = { Text("删除收藏夹") },
            text = { Text("删除“${folder.title}”？收藏夹中的内容不会从 Bilibili 删除，但该收藏关系无法恢复。") },
            confirmButton = {
                TextButton(
                    enabled = !mutating,
                    onClick = {
                        deletingFolder = null
                        onDeleteFolder(folder)
                    },
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deletingFolder = null }) { Text("取消") } },
        )
    }
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
                        "${selectedFolder.type.label} · ${selectedFolder.mediaCount} 项内容",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onBatchDownload(selectedFolder) }) {
                    Icon(Icons.Rounded.Download, contentDescription = "批量下载 ${selectedFolder.title}")
                }
                if (selectedFolder.isUserManaged) {
                    Box {
                        IconButton(
                            onClick = { selectedMenuExpanded = true },
                            enabled = !mutating,
                        ) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "管理 ${selectedFolder.title}")
                        }
                        DropdownMenu(
                            expanded = selectedMenuExpanded,
                            onDismissRequest = { selectedMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("重命名") },
                                onClick = {
                                    selectedMenuExpanded = false
                                    namingFolder = selectedFolder
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("删除") },
                                onClick = {
                                    selectedMenuExpanded = false
                                    deletingFolder = selectedFolder
                                },
                            )
                        }
                    }
                }
            }
            LibraryVideoList(
                videos = videos,
                resolvingBvid = resolvingBvid,
                loading = loading,
                loadingMore = loadingMore,
                mutating = mutating,
                onPlay = onPlay,
                onLoadMore = onLoadMore,
                onRemove = onRemoveVideo.takeIf { selectedFolder.isUserManaged },
                modifier = Modifier.weight(1f),
            )
        }
    } else if (!loading && createdFolders.isEmpty() && collectedFolders.isEmpty()) {
        BiuEmptyState(
            icon = Icons.Rounded.Folder,
            title = "还没有收藏内容",
            actionLabel = "新建收藏夹",
            onAction = { showCreateDialog = true },
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            item(key = "created-header") {
                FavoriteFolderGroupHeader(
                    title = "我创建的",
                    count = createdFolders.size,
                    expanded = createdExpanded,
                    onToggle = { createdExpanded = !createdExpanded },
                    onAdd = { showCreateDialog = true },
                )
            }
            if (createdExpanded) {
                items(createdFolders, key = { folder -> "created:${folder.id}" }) { folder ->
                    FavoriteFolderRow(
                        folder = folder,
                        mutating = mutating,
                        onClick = { onOpenFolder(folder) },
                        onRename = { namingFolder = folder },
                        onDelete = { deletingFolder = folder },
                    )
                }
            }
            item(key = "collected-header") {
                FavoriteFolderGroupHeader(
                    title = "我收藏的",
                    count = collectedFolders.size,
                    expanded = collectedExpanded,
                    onToggle = { collectedExpanded = !collectedExpanded },
                )
            }
            if (collectedExpanded) {
                items(collectedFolders, key = { folder -> "collected:${folder.type.apiValue}:${folder.id}" }) { folder ->
                    FavoriteFolderRow(folder = folder, mutating = mutating, onClick = { onOpenFolder(folder) })
                }
            }
        }
    }
}

@Composable
private fun FavoriteFolderGroupHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdd: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        Text(
            count.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onAdd != null) {
            IconButton(onClick = onAdd, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = "新建$title", modifier = Modifier.size(20.dp))
            }
        }
        Icon(
            if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) "收起$title" else "展开$title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FavoriteFolderRow(
    folder: BilibiliFavoriteFolder,
    mutating: Boolean,
    onClick: () -> Unit,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
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
                    if (folder.type == BilibiliFavoriteFolderType.VIDEO_COLLECTION) {
                        Icons.Rounded.Movie
                    } else {
                        Icons.Rounded.Folder
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                folder.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            val ownerLabel = folder.ownerName.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
            Text(
                "${folder.type.label} · ${folder.mediaCount} 项$ownerLabel",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (folder.isUserManaged && onRename != null && onDelete != null) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    enabled = !mutating,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "管理 ${folder.title}")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        } else {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "打开 ${folder.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    MediaDivider(start = 76.dp)
}

@Composable
private fun LibraryVideoList(
    videos: List<BilibiliLibraryVideo>,
    resolvingBvid: String?,
    loading: Boolean,
    loadingMore: Boolean,
    mutating: Boolean,
    onPlay: (BilibiliVideo) -> Unit,
    onLoadMore: () -> Unit,
    onRemove: ((BilibiliLibraryVideo) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var pendingRemove by remember { mutableStateOf<BilibiliLibraryVideo?>(null) }
    pendingRemove?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("移出收藏夹") },
            text = { Text("将“${item.video.title}”从当前收藏夹移出？") },
            confirmButton = {
                TextButton(
                    enabled = !mutating,
                    onClick = {
                        pendingRemove = null
                        onRemove?.invoke(item)
                    },
                ) { Text("移出") }
            },
            dismissButton = { TextButton(onClick = { pendingRemove = null }) { Text("取消") } },
        )
    }
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
        itemsIndexed(
            items = videos,
            key = { _, item -> "${item.video.bvid}:${item.savedAtEpochSeconds ?: 0L}" },
        ) { index, item ->
            if (index >= videos.lastIndex - 2) {
                LaunchedEffect(videos.size, index) { onLoadMore() }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    LibraryVideoRow(
                        item = item,
                        resolving = resolvingBvid == item.video.bvid,
                        enabled = resolvingBvid == null && !mutating,
                        onClick = { onPlay(item.video) },
                    )
                }
                if (onRemove != null) {
                    IconButton(
                        onClick = { pendingRemove = item },
                        enabled = !mutating,
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "移出 ${item.video.title}",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            MediaDivider(start = 124.dp)
        }
        if (loadingMore) {
            item(key = "favorite-loading") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun OnlineHistoryList(
    videos: List<BilibiliLibraryVideo>,
    resolvingBvid: String?,
    loading: Boolean,
    loadingMore: Boolean,
    mutating: Boolean,
    query: String,
    reportPlayHistory: Boolean,
    onPlay: (BilibiliVideo) -> Unit,
    onSearch: (String) -> Unit,
    onLoadMore: () -> Unit,
    onDelete: (BilibiliLibraryVideo) -> Unit,
    onClear: () -> Unit,
    onReportPlayHistoryChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchText by rememberSaveable(query) { mutableStateOf(query) }
    var pendingDelete by remember { mutableStateOf<BilibiliLibraryVideo?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除在线历史") },
            text = { Text("从 Bilibili 在线历史中删除“${item.video.title}”？本地历史不会受到影响。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(item)
                    },
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("清空在线历史") },
            text = { Text("清空 Bilibili 账号的全部在线历史？此操作不会删除本机 Room 播放记录。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClear()
                    },
                ) { Text("清空") }
            },
            dismissButton = { TextButton(onClick = { showClearConfirmation = false }) { Text("取消") } },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("标题或 UP 主") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = if (searchText.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = {
                                searchText = ""
                                onSearch("")
                            },
                        ) { Icon(Icons.Rounded.Close, contentDescription = "清除在线历史搜索") }
                    }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onSearch(searchText)
                    },
                ),
            )
            IconButton(
                onClick = { showClearConfirmation = true },
                enabled = videos.isNotEmpty() && !mutating,
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "清空在线历史")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "记录播放历史",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = reportPlayHistory,
                onCheckedChange = onReportPlayHistoryChange,
            )
        }
        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (!loading && videos.isEmpty()) {
            BiuEmptyState(
                icon = Icons.Rounded.History,
                title = if (query.isBlank()) "暂无在线历史" else "没有匹配的在线历史",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                itemsIndexed(
                    items = videos,
                    key = { _, item -> item.historyKey ?: "${item.video.bvid}:${item.savedAtEpochSeconds ?: 0L}" },
                ) { index, item ->
                    if (index >= videos.lastIndex - 2) {
                        LaunchedEffect(videos.size, index) { onLoadMore() }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            LibraryVideoRow(
                                item = item,
                                resolving = resolvingBvid == item.video.bvid,
                                enabled = resolvingBvid == null && !mutating,
                                onClick = { onPlay(item.video) },
                            )
                        }
                        IconButton(
                            onClick = { pendingDelete = item },
                            enabled = item.historyKey != null && !mutating,
                            modifier = Modifier.padding(end = 4.dp),
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "删除 ${item.video.title}",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    MediaDivider(start = 124.dp)
                }
                if (loadingMore) {
                    item(key = "online-history-loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
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
    onAddFavorite: (BilibiliVideo) -> Unit,
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
                onAddFavorite = { onAddFavorite(video) },
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
    onAddFavorite: () -> Unit,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAddFavorite, enabled = enabled) {
                Icon(
                    Icons.Rounded.FavoriteBorder,
                    contentDescription = "收藏 ${video.title}",
                    modifier = Modifier.size(20.dp),
                )
            }
            if (resolving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                PlayAffordance(contentDescription = "播放 ${video.title}")
            }
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
private fun FavoriteBatchDownloadSheet(
    folder: BilibiliFavoriteFolder,
    videos: List<BilibiliLibraryVideo>,
    loading: Boolean,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onStart: (FavoriteBatchDownloadKind, List<BilibiliLibraryVideo>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var kind by remember(folder.id) { mutableStateOf(FavoriteBatchDownloadKind.AUDIO) }
    var selectedBvids by remember(folder.id, videos) {
        mutableStateOf(videos.map { item -> item.video.bvid }.toSet())
    }
    val selectedVideos = remember(videos, selectedBvids) {
        videos.filter { item -> item.video.bvid in selectedBvids }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.widthIn(max = 840.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        // long: 收藏夹可能包含大量资源和超长多 P，选择列表独立滚动，底部创建按钮始终固定可见。
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
                    Text("批量下载", style = MaterialTheme.typography.titleLarge)
                    Text(
                        folder.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss, enabled = !submitting) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭批量下载")
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FavoriteBatchDownloadKind.entries.forEach { option ->
                    FilterChip(
                        selected = kind == option,
                        onClick = { kind = option },
                        enabled = !submitting,
                        label = { Text(option.label) },
                        leadingIcon = {
                            Icon(
                                if (option == FavoriteBatchDownloadKind.AUDIO) {
                                    Icons.Rounded.MusicNote
                                } else {
                                    Icons.Rounded.Movie
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
            Text(
                "多 P 资源会按每个分 P 拆成独立任务；同类型文件按队列逐个下载。",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "已选 ${selectedBvids.size} / ${videos.size}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        selectedBvids = if (selectedBvids.size == videos.size) {
                            emptySet()
                        } else {
                            videos.map { item -> item.video.bvid }.toSet()
                        }
                    },
                    enabled = videos.isNotEmpty() && !submitting,
                ) {
                    Text(if (selectedBvids.size == videos.size) "取消全选" else "全选")
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when {
                    loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    videos.isEmpty() -> BiuEmptyState(
                        icon = Icons.Rounded.Folder,
                        title = "没有可下载内容",
                        message = "失效稿件和非普通视频不会加入批量任务",
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 2.dp),
                    ) {
                        items(videos, key = { item -> item.video.bvid }) { item ->
                            val selected = item.video.bvid in selectedBvids
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        role = Role.Checkbox
                                        this.selected = selected
                                        stateDescription = if (selected) "已选择" else "未选择"
                                    }
                                    .clickable(enabled = !submitting) {
                                        selectedBvids = if (selected) {
                                            selectedBvids - item.video.bvid
                                        } else {
                                            selectedBvids + item.video.bvid
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                AsyncImage(
                                    model = item.video.coverUrl,
                                    contentDescription = item.video.title,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop,
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        item.video.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        item.video.author.ifBlank { "未知作者" },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
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
                                                contentDescription = "已选择 ${item.video.title}",
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                            MediaDivider(start = 80.dp)
                        }
                    }
                }
            }
            Button(
                onClick = { onStart(kind, selectedVideos) },
                enabled = selectedVideos.isNotEmpty() && !loading && !submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    if (submitting) {
                        "正在解析分 P 并创建任务"
                    } else {
                        "下载 ${selectedVideos.size} 个资源的${kind.label}"
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
    lyrics: LyricsUiState,
    controllerReady: Boolean,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlaybackModeChange: (PlaybackMode) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onDownload: () -> Unit,
    onVideoDownload: () -> Unit,
    onShowDownloads: () -> Unit,
    onSelectQueueItem: (Int) -> Unit,
    onMoveQueueItemNext: (PlaybackQueueItem) -> Unit,
    onRemoveQueueItem: (PlaybackQueueItem) -> Unit,
    onClearQueue: () -> Unit,
    onPrepareLyrics: () -> Unit,
    onSearchLyrics: (String) -> Unit,
    onSelectLyrics: (LyricsSearchResult) -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var showLyricsSearch by remember { mutableStateOf(false) }
    val lyricsDefaults = lyricsSearchDefaults(snapshot.title, snapshot.pageTitle, snapshot.artist)
    val defaultLyricsQuery = listOf(lyricsDefaults.trackName, lyricsDefaults.artistName)
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .joinToString(" ")
    // long: 默认词直接来自当前播放快照，打开弹框无需等待 ViewModel 回传；mediaId 变化时同步切换到新 P 的名称。
    var lyricsQuery by remember(snapshot.mediaId, defaultLyricsQuery) { mutableStateOf(defaultLyricsQuery) }
    var confirmClearQueue by remember { mutableStateOf(false) }
    LaunchedEffect(snapshot.mediaId) {
        // long: 切换曲目后必须回到封面并关闭旧搜索框，防止上一首歌词在用户尚未手动确认时显示到新曲目。
        showLyrics = false
        showLyricsSearch = false
    }
    if (confirmClearQueue) {
        AlertDialog(
            onDismissRequest = { confirmClearQueue = false },
            title = { Text("清空播放列表？") },
            text = { Text("当前播放会停止，列表中的所有内容都会移除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearQueue = false
                        showQueue = false
                        onClearQueue()
                    },
                ) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearQueue = false }) { Text("取消") }
            },
        )
    }
    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            PlaybackQueue(
                snapshot = snapshot,
                onSelectQueueItem = onSelectQueueItem,
                onMoveQueueItemNext = onMoveQueueItemNext,
                onRemoveQueueItem = onRemoveQueueItem,
                onClearQueue = { confirmClearQueue = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp),
            )
        }
    }
    if (showLyricsSearch) {
        ModalBottomSheet(
            onDismissRequest = { showLyricsSearch = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            LyricsSearchPanel(
                query = lyricsQuery,
                onQueryChange = { lyricsQuery = it },
                state = lyrics,
                onSearch = { onSearchLyrics(lyricsQuery) },
                onSelect = { result ->
                    onSelectLyrics(result)
                    showLyricsSearch = false
                    showLyrics = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 360.dp, max = 640.dp),
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
                actions = {
                    IconButton(
                        onClick = {
                            if (showLyrics) {
                                showLyrics = false
                            } else {
                                // long: 第三方歌词请求只能由用户从这里主动发起，打开播放页和切歌都不会自动访问 LRCLIB。
                                onPrepareLyrics()
                                showLyricsSearch = true
                            }
                        },
                    ) {
                        Icon(
                            if (showLyrics) Icons.Rounded.Album else Icons.Rounded.Lyrics,
                            contentDescription = if (showLyrics) "显示封面" else "搜索歌词",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (showLyrics) {
                        IconButton(
                            onClick = {
                                onPrepareLyrics()
                                showLyricsSearch = true
                            },
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = if (lyrics.document == null) "搜索歌词" else "重新搜索歌词",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        NowPlayingDetails(
            snapshot = snapshot,
            lyrics = lyrics,
            showLyrics = showLyrics,
            controllerReady = controllerReady,
            onPrevious = onPrevious,
            onToggle = onToggle,
            onNext = onNext,
            onSeek = onSeek,
            onPlaybackModeChange = onPlaybackModeChange,
            onPlaybackSpeedChange = onPlaybackSpeedChange,
            onDownload = onDownload,
            onVideoDownload = onVideoDownload,
            onShowDownloads = onShowDownloads,
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
    lyrics: LyricsUiState,
    showLyrics: Boolean,
    controllerReady: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlaybackModeChange: (PlaybackMode) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onDownload: () -> Unit,
    onVideoDownload: () -> Unit,
    onShowDownloads: () -> Unit,
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
            onPlaybackModeChange = onPlaybackModeChange,
            onPlaybackSpeedChange = onPlaybackSpeedChange,
            downloadEnabled = snapshot.downloadRequest != null,
            videoDownloadEnabled = snapshot.videoDownloadRequest != null,
            onDownload = onDownload,
            onVideoDownload = onVideoDownload,
            onShowDownloads = onShowDownloads,
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
            if (showLyrics) {
                NowPlayingLyrics(
                    state = lyrics,
                    positionMs = snapshot.positionMs,
                    modifier = Modifier.size(300.dp, 240.dp),
                )
            } else {
                NowPlayingArtwork(snapshot, 240.dp)
            }
            controls(Modifier.weight(1f))
        }
    } else {
        Column(
            modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (showLyrics) {
                NowPlayingLyrics(
                    state = lyrics,
                    positionMs = snapshot.positionMs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                )
            } else {
                NowPlayingArtwork(snapshot, 300.dp)
            }
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
private fun NowPlayingLyrics(
    state: LyricsUiState,
    positionMs: Long,
    modifier: Modifier = Modifier,
) {
    val document = state.document
    val lines = document?.lines.orEmpty()
    val currentIndex = LrcParser.currentLineIndex(lines, positionMs)
    val listState = rememberLazyListState()
    // long: 只在当前歌词行发生变化时滚动，不跟随每次进度 tick 重启动画，保证用户阅读和手动滚动不会持续抖动。
    LaunchedEffect(document?.cacheKey, currentIndex) {
        if (currentIndex >= 0) {
            listState.animateScrollToItem(currentIndex)
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        // long: 歌词数据失败只替换中央展示区，底部 Media3 控件始终保留，因此网络和解析错误不会中断当前播放。
        when (state.status) {
            LyricsLoadStatus.LOADING -> CircularProgressIndicator(modifier = Modifier.size(28.dp))
            LyricsLoadStatus.LOADED -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 112.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(
                    items = lines,
                    key = { index, line -> "${line.startTimeMs}:$index" },
                ) { index, line ->
                    val isCurrent = index == currentIndex
                    Text(
                        text = line.text,
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = if (isCurrent) {
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                    )
                }
            }
            LyricsLoadStatus.EMPTY -> LyricsStatusMessage("暂未找到同步歌词")
            LyricsLoadStatus.ERROR -> LyricsStatusMessage(state.errorMessage ?: "歌词加载失败")
            LyricsLoadStatus.IDLE -> LyricsStatusMessage("可从右上角搜索歌词")
        }
    }
}

@Composable
private fun LyricsStatusMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(24.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LyricsSearchPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    state: LyricsUiState,
    onSearch: () -> Unit,
    onSelect: (LyricsSearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = "搜索歌词",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("歌曲名 + 歌手名") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            )
            FilledIconButton(
                onClick = onSearch,
                enabled = query.isNotBlank() && !state.isSearchLoading,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "开始搜索")
            }
        }
        when {
            state.isSearchLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
            state.searchErrorMessage != null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                LyricsStatusMessage(state.searchErrorMessage)
            }
            state.hasSearched && state.searchResults.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                LyricsStatusMessage("没有找到同步歌词")
            }
            else -> LazyColumn(modifier = Modifier.weight(1f)) {
                // long: 搜索结果保持可滚动的多候选列表；点击后由 ViewModel 写入当前媒体缓存，后续重启继续使用同一选择。
                items(state.searchResults, key = LyricsSearchResult::id) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(result) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Lyrics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                result.trackName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                listOfNotNull(
                                    result.artistName.takeIf(String::isNotBlank),
                                    result.albumName?.takeIf(String::isNotBlank),
                                ).joinToString(" · "),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            formatDurationMs(result.durationSeconds * 1_000L),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
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
    onPlaybackModeChange: (PlaybackMode) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    downloadEnabled: Boolean,
    videoDownloadEnabled: Boolean,
    onDownload: () -> Unit,
    onVideoDownload: () -> Unit,
    onShowDownloads: () -> Unit,
    onShowQueue: () -> Unit,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSpeedMenu by remember { mutableStateOf(false) }
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
            IconButton(onClick = onVideoDownload, enabled = videoDownloadEnabled) {
                Icon(
                    Icons.Rounded.Movie,
                    contentDescription = "下载当前视频",
                    tint = if (videoDownloadEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = onDownload, enabled = downloadEnabled) {
                Icon(
                    Icons.Rounded.Download,
                    contentDescription = "下载当前音频",
                    tint = if (downloadEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = onShowDownloads) {
                Icon(
                    Icons.Rounded.Downloading,
                    contentDescription = "打开下载任务",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
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
            IconButton(
                onClick = { onPlaybackModeChange(snapshot.playbackMode.next()) },
                enabled = controllerReady,
            ) {
                Icon(
                    playbackModeIcon(snapshot.playbackMode),
                    contentDescription = "播放模式：${snapshot.playbackMode.label}",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
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
            Box {
                TextButton(
                    onClick = { showSpeedMenu = true },
                    enabled = controllerReady,
                ) {
                    Text(formatPlaybackSpeed(snapshot.playbackSpeed))
                }
                DropdownMenu(
                    expanded = showSpeedMenu,
                    onDismissRequest = { showSpeedMenu = false },
                ) {
                    PlaybackSpeedPolicy.options.forEach { speed ->
                        DropdownMenuItem(
                            text = { Text(formatPlaybackSpeed(speed)) },
                            onClick = {
                                showSpeedMenu = false
                                onPlaybackSpeedChange(speed)
                            },
                            leadingIcon = if (speed == snapshot.playbackSpeed) {
                                { Icon(Icons.Rounded.Check, contentDescription = null) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackQueue(
    snapshot: PlaybackSnapshot,
    onSelectQueueItem: (Int) -> Unit,
    onMoveQueueItemNext: (PlaybackQueueItem) -> Unit,
    onRemoveQueueItem: (PlaybackQueueItem) -> Unit,
    onClearQueue: () -> Unit,
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
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClearQueue, enabled = snapshot.queueItems.isNotEmpty()) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "清空播放列表")
            }
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
                    } else {
                        IconButton(onClick = { onMoveQueueItemNext(item) }) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = "设为下一首")
                        }
                    }
                    IconButton(onClick = { onRemoveQueueItem(item) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "从播放列表移除")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun DownloadTaskPanel(
    selectedKind: DownloadTaskKind,
    onSelectedKindChange: (DownloadTaskKind) -> Unit,
    audioTasks: List<AudioDownloadTaskEntity>,
    videoTasks: List<VideoDownloadTaskEntity>,
    networkPreference: DownloadNetworkPreference,
    onNetworkPreferenceChange: (Boolean) -> Unit,
    onRetryFailedAudio: () -> Unit,
    onRetryFailedVideo: () -> Unit,
    onResumeAudio: (String) -> Unit,
    onPauseAudio: (String) -> Unit,
    onCancelAudio: (String) -> Unit,
    onResumeVideo: (String) -> Unit,
    onPauseVideo: (String) -> Unit,
    onCancelVideo: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.Downloading, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("下载任务", style = MaterialTheme.typography.titleMedium)
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DownloadTaskKind.entries.forEach { kind ->
                val count = if (kind == DownloadTaskKind.AUDIO) audioTasks.size else videoTasks.size
                FilterChip(
                    selected = kind == selectedKind,
                    onClick = { onSelectedKindChange(kind) },
                    label = { Text("${kind.label} · $count") },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("仅 Wi-Fi 下载", style = MaterialTheme.typography.bodySmall)
                Text(
                    "实际按系统非计费网络判断，切网时保留断点等待恢复",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = networkPreference == DownloadNetworkPreference.UNMETERED_ONLY,
                onCheckedChange = onNetworkPreferenceChange,
            )
        }
        when (selectedKind) {
            DownloadTaskKind.AUDIO -> AudioDownloadTaskList(
                tasks = audioTasks,
                onRetryFailed = onRetryFailedAudio,
                onResume = onResumeAudio,
                onPause = onPauseAudio,
                onCancel = onCancelAudio,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            DownloadTaskKind.VIDEO -> VideoDownloadTaskList(
                tasks = videoTasks,
                onRetryFailed = onRetryFailedVideo,
                onResume = onResumeVideo,
                onPause = onPauseVideo,
                onCancel = onCancelVideo,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun AudioDownloadTaskList(
    tasks: List<AudioDownloadTaskEntity>,
    onRetryFailed: () -> Unit,
    onResume: (String) -> Unit,
    onPause: (String) -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("音频任务 · ${tasks.size}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            val failedCount = tasks.count { task -> task.downloadStatus == AudioDownloadStatus.FAILED }
            if (failedCount > 0) {
                TextButton(onClick = onRetryFailed) { Text("重试失败 · $failedCount") }
            }
        }
        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "还没有音频下载任务",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tasks, key = AudioDownloadTaskEntity::taskId) { task ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                task.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                task.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                buildString {
                                    append(downloadStatusLabel(task.downloadStatus))
                                    if (task.qualityLabel.isNotBlank()) append(" · ${task.qualityLabel}")
                                    if (task.downloadedBytes > 0L) {
                                        append(" · ${formatDownloadBytes(task.downloadedBytes)}")
                                        if (task.totalBytes > 0L) append(" / ${formatDownloadBytes(task.totalBytes)}")
                                    }
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (task.downloadStatus == AudioDownloadStatus.FAILED) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        when (task.downloadStatus) {
                            AudioDownloadStatus.QUEUED,
                            AudioDownloadStatus.RESOLVING,
                            AudioDownloadStatus.DOWNLOADING,
                            -> {
                                IconButton(onClick = { onPause(task.taskId) }) {
                                    Icon(Icons.Rounded.Pause, contentDescription = "暂停下载")
                                }
                                IconButton(onClick = { onCancel(task.taskId) }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "取消下载")
                                }
                            }
                            AudioDownloadStatus.PAUSED,
                            AudioDownloadStatus.FAILED,
                            -> {
                                IconButton(onClick = { onResume(task.taskId) }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "继续下载")
                                }
                                IconButton(onClick = { onCancel(task.taskId) }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "取消下载")
                                }
                            }
                            AudioDownloadStatus.CANCELLED -> IconButton(onClick = { onResume(task.taskId) }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "重新下载")
                            }
                            AudioDownloadStatus.PUBLISHING,
                            AudioDownloadStatus.COMPLETED,
                            -> Unit
                        }
                    }
                    if (task.totalBytes > 0L && task.downloadStatus != AudioDownloadStatus.COMPLETED) {
                        LinearProgressIndicator(
                            progress = { task.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                        )
                    }
                    task.errorMessage?.takeIf(String::isNotBlank)?.let { errorMessage ->
                        Text(
                            errorMessage,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun VideoDownloadTaskList(
    tasks: List<VideoDownloadTaskEntity>,
    onRetryFailed: () -> Unit,
    onResume: (String) -> Unit,
    onPause: (String) -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("视频任务 · ${tasks.size}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            val failedCount = tasks.count { task -> task.downloadStatus == VideoDownloadStatus.FAILED }
            if (failedCount > 0) {
                TextButton(onClick = onRetryFailed) { Text("重试失败 · $failedCount") }
            }
        }
        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "还没有视频下载任务",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tasks, key = VideoDownloadTaskEntity::taskId) { task ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                task.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                task.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                buildString {
                                    append(videoDownloadStatusLabel(task.downloadStatus))
                                    if (task.videoQualityLabel.isNotBlank()) append(" · ${task.videoQualityLabel}")
                                    val downloadedBytes = task.videoDownloadedBytes + task.audioDownloadedBytes
                                    val totalBytes = task.videoTotalBytes + task.audioTotalBytes
                                    if (downloadedBytes > 0L) {
                                        append(" · ${formatDownloadBytes(downloadedBytes)}")
                                        if (totalBytes > 0L) append(" / ${formatDownloadBytes(totalBytes)}")
                                    }
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (task.downloadStatus == VideoDownloadStatus.FAILED) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        when (task.downloadStatus) {
                            VideoDownloadStatus.QUEUED,
                            VideoDownloadStatus.RESOLVING,
                            VideoDownloadStatus.DOWNLOADING_VIDEO,
                            VideoDownloadStatus.DOWNLOADING_AUDIO,
                            VideoDownloadStatus.MUXING,
                            VideoDownloadStatus.PUBLISHING,
                            -> {
                                IconButton(onClick = { onPause(task.taskId) }) {
                                    Icon(Icons.Rounded.Pause, contentDescription = "暂停视频下载")
                                }
                                IconButton(onClick = { onCancel(task.taskId) }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "取消视频下载")
                                }
                            }
                            VideoDownloadStatus.PAUSED,
                            VideoDownloadStatus.FAILED,
                            -> {
                                IconButton(onClick = { onResume(task.taskId) }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "继续视频下载")
                                }
                                IconButton(onClick = { onCancel(task.taskId) }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "取消视频下载")
                                }
                            }
                            VideoDownloadStatus.CANCELLED -> IconButton(onClick = { onResume(task.taskId) }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "重新下载视频")
                            }
                            VideoDownloadStatus.COMPLETED -> Unit
                        }
                    }
                    val totalBytes = task.videoTotalBytes + task.audioTotalBytes
                    if (totalBytes > 0L && task.downloadStatus != VideoDownloadStatus.COMPLETED) {
                        LinearProgressIndicator(
                            progress = { task.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                        )
                    }
                    task.errorMessage?.takeIf(String::isNotBlank)?.let { errorMessage ->
                        Text(
                            errorMessage,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
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

private fun downloadStatusLabel(status: AudioDownloadStatus): String {
    return when (status) {
        AudioDownloadStatus.QUEUED -> "等待下载"
        AudioDownloadStatus.RESOLVING -> "正在解析"
        AudioDownloadStatus.DOWNLOADING -> "正在下载"
        AudioDownloadStatus.PAUSED -> "已暂停"
        AudioDownloadStatus.PUBLISHING -> "正在保存"
        AudioDownloadStatus.COMPLETED -> "已保存到 Music/Biu"
        AudioDownloadStatus.FAILED -> "下载失败"
        AudioDownloadStatus.CANCELLED -> "已取消"
    }
}

private fun videoDownloadStatusLabel(status: VideoDownloadStatus): String {
    return when (status) {
        VideoDownloadStatus.QUEUED -> "等待下载"
        VideoDownloadStatus.RESOLVING -> "正在解析"
        VideoDownloadStatus.DOWNLOADING_VIDEO -> "正在下载视频轨"
        VideoDownloadStatus.DOWNLOADING_AUDIO -> "正在下载音频轨"
        VideoDownloadStatus.MUXING -> "正在合并"
        VideoDownloadStatus.PUBLISHING -> "正在保存"
        VideoDownloadStatus.PAUSED -> "已暂停"
        VideoDownloadStatus.COMPLETED -> "已保存到 Movies/Biu"
        VideoDownloadStatus.FAILED -> "视频下载失败"
        VideoDownloadStatus.CANCELLED -> "已取消"
    }
}

private fun formatDownloadBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kib = bytes / 1024.0
    if (kib < 1024.0) return "%.1f KB".format(kib)
    return "%.1f MB".format(kib / 1024.0)
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

internal data class LyricsSearchDefaults(
    val trackName: String,
    val artistName: String,
)

internal fun lyricsSearchDefaults(title: String, pageTitle: String?, fallbackArtist: String): LyricsSearchDefaults {
    val pageName = lyricsPageName(pageTitle)
    val sourceName = pageName ?: displayResourceTitle(title, pageTitle)
    val normalizedName = sourceName.replaceFirst(LEADING_TRACK_NUMBER_PATTERN, "").trim()
    val separator = TRACK_ARTIST_SEPARATOR_PATTERN.findAll(normalizedName).lastOrNull()
    val embeddedTrack = separator?.let { normalizedName.substring(0, it.range.first).trim() }
    val embeddedArtist = separator?.let { normalizedName.substring(it.range.last + 1).trim() }
    val hasEmbeddedMetadata = !embeddedTrack.isNullOrBlank() && !embeddedArtist.isNullOrBlank()
    // long: 多 P 只信任当前 P 名称中的曲目信息，不能把视频 UP 主误当歌手；单 P 无内嵌歌手时才回退作者字段。
    return LyricsSearchDefaults(
        trackName = embeddedTrack.takeIf { hasEmbeddedMetadata } ?: normalizedName.ifBlank { sourceName },
        artistName = when {
            hasEmbeddedMetadata -> embeddedArtist.orEmpty()
            pageName != null -> ""
            else -> fallbackArtist.trim()
        },
    )
}

private fun lyricsPageName(pageTitle: String?): String? = pageTitle
    ?.substringAfter(" · ", missingDelimiterValue = "")
    ?.trim()
    ?.takeIf(String::isNotBlank)

private val LEADING_TRACK_NUMBER_PATTERN = Regex("""^\s*\d{1,3}\s*[.．、:：_-]\s*""")
private val TRACK_ARTIST_SEPARATOR_PATTERN = Regex("""\s*[-－–—]\s*""")

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

private fun MediaController.removeQueueItem(mediaId: String): Boolean {
    val index = (0 until mediaItemCount).firstOrNull { getMediaItemAt(it).mediaId == mediaId } ?: return false
    removeMediaItem(index)
    return true
}

private fun MediaController.moveQueueItemNext(mediaId: String): Boolean {
    val activeMediaId = currentMediaItem?.mediaId.orEmpty()
    if (mediaId.isBlank() || mediaId == activeMediaId) return false
    val targetIndex = (0 until mediaItemCount).firstOrNull { getMediaItemAt(it).mediaId == mediaId } ?: return false
    val activeIndex = (0 until mediaItemCount).firstOrNull { getMediaItemAt(it).mediaId == activeMediaId } ?: return false
    val target = getMediaItemAt(targetIndex)
    // long: 移除前同时锁定当前项和目标项位置，既能修正目标位于当前项之前时的索引偏移，也避免移除后读取瞬时空索引造成半完成队列。
    val insertionIndex = if (targetIndex < activeIndex) activeIndex else activeIndex + 1
    removeMediaItem(targetIndex)
    addMediaItem(insertionIndex.coerceAtMost(mediaItemCount), target)
    return true
}

private fun playbackModeIcon(mode: PlaybackMode): ImageVector = when (mode) {
    PlaybackMode.SEQUENTIAL -> Icons.AutoMirrored.Rounded.PlaylistPlay
    PlaybackMode.REPEAT_ALL -> Icons.Rounded.Repeat
    PlaybackMode.SHUFFLE -> Icons.Rounded.Shuffle
    PlaybackMode.REPEAT_ONE -> Icons.Rounded.RepeatOne
}

private fun formatPlaybackSpeed(speed: Float): String = "${PlaybackSpeedPolicy.normalize(speed).toString().removeSuffix(".0")}x"

private fun MediaController.matchesPlaybackQueue(snapshot: PlaybackQueueSnapshot<Track>): Boolean {
    if (mediaItemCount != snapshot.items.size) return false
    return snapshot.items.indices.all { index -> getMediaItemAt(index).mediaId == snapshot.items[index].id }
}

private const val PLAYBACK_PROGRESS_TICK_MS = 500L

private val PUBLISHED_AT_FORMATTER: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())
