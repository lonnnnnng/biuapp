package com.lonnnnnng.biu.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
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
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.lonnnnnng.biu.core.model.BilibiliTrackSource
import com.lonnnnnng.biu.core.model.PlaybackMediaMode
import com.lonnnnnng.biu.core.model.PlaybackVideoQuality
import com.lonnnnnng.biu.core.model.Track
import com.lonnnnnng.biu.core.model.bilibiliSource
import com.lonnnnnng.biu.core.model.playbackMediaMode
import com.lonnnnnng.biu.core.model.playbackStreamMetadata
import com.lonnnnnng.biu.core.model.toMediaItem
import com.lonnnnnng.biu.core.model.toTrackOrNull
import com.lonnnnnng.biu.data.bilibili.AccountLibrarySection
import com.lonnnnnng.biu.data.bilibili.BilibiliAccount
import com.lonnnnnng.biu.data.bilibili.BilibiliFavoriteFolder
import com.lonnnnnng.biu.data.bilibili.BilibiliFavoriteFolderType
import com.lonnnnnng.biu.data.bilibili.BilibiliCreator
import com.lonnnnnng.biu.data.bilibili.BilibiliDynamicItem
import com.lonnnnnng.biu.data.bilibili.BilibiliLibraryVideo
import com.lonnnnnng.biu.data.bilibili.BilibiliVideo
import com.lonnnnnng.biu.data.bilibili.CreatorFeedTabState
import com.lonnnnnng.biu.data.bilibili.RecommendFeed
import com.lonnnnnng.biu.data.local.AudioDownloadTaskEntity
import com.lonnnnnng.biu.data.local.AppListDensity
import com.lonnnnnng.biu.data.local.AppTextScale
import com.lonnnnnng.biu.data.local.AppThemeMode
import com.lonnnnnng.biu.data.local.AppVideoLayout
import com.lonnnnnng.biu.data.local.CreatorGroupEntity
import com.lonnnnnng.biu.data.local.PlaybackHistoryEntity
import com.lonnnnnng.biu.data.local.LocalAudio
import com.lonnnnnng.biu.data.local.LocalAudioDirectory
import com.lonnnnnng.biu.data.local.LocalAudioPlaybackMode
import com.lonnnnnng.biu.data.local.LocalMediaPermissionPolicy
import com.lonnnnnng.biu.data.local.LocalPlaylistEntity
import com.lonnnnnng.biu.data.local.LocalPlaylistItemEntity
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
import com.lonnnnnng.biu.ui.theme.LocalBiuListDensity
import com.lonnnnnng.biu.playback.PlaybackService
import com.lonnnnnng.biu.playback.PlaybackMode
import com.lonnnnnng.biu.playback.PlaybackSessionCommands
import com.lonnnnnng.biu.playback.PlaybackSpeedPolicy
import com.lonnnnnng.biu.playback.SleepTimerMode
import com.lonnnnnng.biu.playback.SleepTimerPolicy
import com.lonnnnnng.biu.playback.applyPlaybackMode
import com.lonnnnnng.biu.update.AppUpdateInstaller
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
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
    val publishedAtEpochSeconds: Long? = null,
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
    val mediaMode: PlaybackMediaMode = PlaybackMediaMode.AUDIO,
    val selectedVideoQualityId: Int? = null,
    val videoQualities: List<PlaybackVideoQuality> = emptyList(),
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

private sealed interface PendingPlaylistAddition {
    data class VideoPage(val selection: VideoPageSelection, val pageIndex: Int) : PendingPlaylistAddition
    data class LocalTrack(val audio: LocalAudio) : PendingPlaylistAddition
    data object CurrentQueue : PendingPlaylistAddition
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
    var showAccountMenu by remember { mutableStateOf(false) }
    var showDisplaySettings by rememberSaveable { mutableStateOf(false) }
    var showCreatorConfig by remember { mutableStateOf(false) }
    var showCreatorCenter by rememberSaveable { mutableStateOf(false) }
    var showQuickQueue by rememberSaveable { mutableStateOf(false) }
    var confirmClearQuickQueue by remember { mutableStateOf(false) }
    var favoritePickerVideo by remember { mutableStateOf<BilibiliVideo?>(null) }
    var tripleConfirmation by remember { mutableStateOf<BilibiliDynamicItem?>(null) }
    // long: 视频全屏期间横竖屏切换会重建 Activity；保存页面开关，避免重建后意外退回首页而中断控制链路。
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var downloadTaskKind by remember { mutableStateOf(DownloadTaskKind.AUDIO) }
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }
    var pendingPlaylistAddition by remember { mutableStateOf<PendingPlaylistAddition?>(null) }
    var mediaModeSwitching by remember { mutableStateOf(false) }
    var playbackErrorEventId by remember { mutableLongStateOf(0L) }
    var playbackErrorCode by remember { mutableStateOf<Int?>(null) }
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
    fun sendPlaybackStreamCommand(
        command: SessionCommand,
        arguments: Bundle,
        unsupportedMessage: String,
        failureMessage: String,
    ) {
        val activeController = controller
        if (activeController == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar("播放器尚未连接") }
        } else if (!activeController.isSessionCommandAvailable(command)) {
            coroutineScope.launch { snackbarHostState.showSnackbar(unsupportedMessage) }
        } else {
            mediaModeSwitching = true
            val future = activeController.sendCustomCommand(
                command,
                arguments,
            )
            future.addListener(
                {
                    val result = runCatching { future.get() }.getOrNull()
                    mediaModeSwitching = false
                    if (
                        result == null ||
                        result.resultCode != SessionResult.RESULT_SUCCESS
                    ) {
                        val message = result?.let { PlaybackSessionCommands.resultMessage(it.extras) }
                            ?: failureMessage
                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
    }
    val requestPlaybackMediaMode: (PlaybackMediaMode) -> Unit = { mode ->
        sendPlaybackStreamCommand(
            command = PlaybackSessionCommands.setMediaMode,
            arguments = PlaybackSessionCommands.mediaModeArguments(mode),
            unsupportedMessage = "当前播放服务不支持视频切换",
            failureMessage = "切换播放类型失败",
        )
    }
    val requestVideoQuality: (Int) -> Unit = { qualityId ->
        sendPlaybackStreamCommand(
            command = PlaybackSessionCommands.setVideoQuality,
            arguments = PlaybackSessionCommands.videoQualityArguments(qualityId),
            unsupportedMessage = "当前播放服务不支持画质切换",
            failureMessage = "切换视频画质失败",
        )
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
                // long: 切换音视频或画质重建媒体源时，Media3 会短暂清空 currentMediaItem；队列仍有内容时保留旧快照，避免全屏页被误判为“没有播放内容”而卸载。
                if (
                    activeController.currentMediaItem == null &&
                    activeController.mediaItemCount > 0 &&
                    playback.mediaId.isNotBlank()
                ) {
                    return@let playback
                }
                val progress = activeController.currentPlaybackProgress()
                val streamMetadata = activeController.currentMediaItem?.playbackStreamMetadata()
                PlaybackSnapshot(
                    mediaId = activeController.currentMediaItem?.mediaId.orEmpty(),
                    // long: 多 P 的媒体 title 专供系统锁屏显示当前分 P，App 内仍以 albumTitle 展示视频总标题。
                    title = activeController.mediaMetadata.albumTitle?.toString()
                        ?: activeController.mediaMetadata.title?.toString()
                        ?: "还没有播放",
                    artist = activeController.mediaMetadata.artist?.toString() ?: "选择内容开始播放",
                    quality = activeController.mediaMetadata.description?.toString().orEmpty(),
                    pageTitle = activeController.mediaMetadata.subtitle?.toString()?.takeIf(String::isNotBlank),
                    publishedAtEpochSeconds = activeController.currentMediaItem
                        ?.toTrackOrNull()
                        ?.publishedAtEpochSeconds,
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
                    mediaMode = streamMetadata?.mode ?: PlaybackMediaMode.AUDIO,
                    selectedVideoQualityId = streamMetadata?.selectedVideoQualityId,
                    videoQualities = streamMetadata?.availableVideoQualities.orEmpty(),
                )
            } ?: PlaybackSnapshot()
        }

        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = publishSnapshot()

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackErrorCode = error.errorCode
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
        delay(5_000)
        if (controller?.playerError != null) {
            snackbarHostState.showSnackbar(
                if ((playbackErrorCode ?: -1) in 2000..2999) {
                    "网络不可用且没有可用本地副本，请稍后重试"
                } else {
                    "媒体解码失败，请切换曲目后重试"
                },
            )
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
        val lastPlayed = uiState.localHistory.firstOrNull { history -> history.bvid == selection.video.bvid }
        MultiPageSelectionSheet(
            selection = selection,
            lastPlayed = lastPlayed,
            loading = uiState.isPageQueueLoading,
            onDismiss = viewModel::dismissPageSelection,
            onPlayPage = viewModel::playPageQueue,
            onAddPage = { pageIndex ->
                pendingPlaylistAddition = PendingPlaylistAddition.VideoPage(selection, pageIndex)
            },
            onResumePage = { pageIndex, history ->
                viewModel.resumePageQueue(pageIndex, history.lastPositionMs, history.durationMs)
            },
        )
    }

    pendingPlaylistAddition?.let { pending ->
        LocalPlaylistPickerDialog(
            playlists = uiState.localPlaylists,
            onDismiss = { pendingPlaylistAddition = null },
            onSelect = { playlist ->
                pendingPlaylistAddition = null
                when (pending) {
                    is PendingPlaylistAddition.VideoPage -> viewModel.addPageToLocalPlaylist(
                        playlist.playlistId,
                        pending.selection,
                        pending.pageIndex,
                    )
                    is PendingPlaylistAddition.LocalTrack -> viewModel.addLocalAudioToPlaylist(
                        playlist.playlistId,
                        pending.audio,
                    )
                    PendingPlaylistAddition.CurrentQueue -> viewModel.savePlaybackQueueToPlaylist(playlist.playlistId)
                }
            },
            onOpenLibrary = {
                pendingPlaylistAddition = null
                viewModel.dismissPageSelection()
                viewModel.selectSection(MainSection.ACCOUNT)
                viewModel.loadLibrary(AccountLibrarySection.PLAYLISTS)
            },
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

    tripleConfirmation?.let { dynamic ->
        AlertDialog(
            onDismissRequest = { tripleConfirmation = null },
            icon = { Icon(Icons.Rounded.Bolt, contentDescription = null) },
            title = { Text("确认一键三连？") },
            text = {
                Text("将为《${dynamic.video.title}》点赞、投币并收藏到默认收藏夹。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        tripleConfirmation = null
                        viewModel.tripleDynamic(dynamic)
                    },
                ) { Text("确认三连") }
            },
            dismissButton = {
                TextButton(onClick = { tripleConfirmation = null }) { Text("取消") }
            },
        )
    }

    if (showQuickQueue && playback.mediaId.isNotBlank()) {
        ModalBottomSheet(
            onDismissRequest = { showQuickQueue = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            PlaybackQueue(
                snapshot = playback,
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
                onMoveQueueItem = { item, targetIndex ->
                    controller?.let { activeController ->
                        if (activeController.moveQueueItem(item.mediaId, targetIndex)) {
                            viewModel.movePlaybackQueueItem(item.mediaId, targetIndex)
                        }
                    }
                },
                onReorderQueue = { mediaIds ->
                    controller?.let { activeController ->
                        if (activeController.reorderQueue(mediaIds)) {
                            viewModel.reorderPlaybackQueue(mediaIds)
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
                onRemoveQueueItems = { mediaIds ->
                    controller?.let { activeController ->
                        if (activeController.removeQueueItems(mediaIds)) {
                            viewModel.removePlaybackQueueItems(mediaIds)
                        }
                    }
                },
                onSaveQueue = {
                    showQuickQueue = false
                    pendingPlaylistAddition = PendingPlaylistAddition.CurrentQueue
                },
                onClearQueue = { confirmClearQuickQueue = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp),
            )
        }
    }
    if (confirmClearQuickQueue) {
        AlertDialog(
            onDismissRequest = { confirmClearQuickQueue = false },
            title = { Text("清空播放列表？") },
            text = { Text("当前播放会停止，列表中的所有内容都会移除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearQuickQueue = false
                        showQuickQueue = false
                        controller?.let { activeController ->
                            viewModel.clearPlaybackQueue()
                            activeController.stop()
                            activeController.clearMediaItems()
                        }
                    },
                ) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearQuickQueue = false }) { Text("取消") }
            },
        )
    }

    if (showNowPlaying && playback.mediaId.isNotBlank()) {
        BackHandler { showNowPlaying = false }
        NowPlayingScreen(
            snapshot = playback,
            lyrics = uiState.lyrics,
            player = controller,
            controllerReady = controller != null,
            mediaModeSwitching = mediaModeSwitching,
            sleepTimerMode = uiState.sleepTimerMode,
            sleepTimerDeadlineEpochMs = uiState.sleepTimerDeadlineEpochMs,
            snackbarHostState = snackbarHostState,
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
            onPlaybackMediaModeChange = requestPlaybackMediaMode,
            onVideoQualityChange = requestVideoQuality,
            onDownload = requestAudioDownload,
            onVideoDownload = requestVideoDownload,
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
            onMoveQueueItem = { item, targetIndex ->
                controller?.let { activeController ->
                    if (activeController.moveQueueItem(item.mediaId, targetIndex)) {
                        viewModel.movePlaybackQueueItem(item.mediaId, targetIndex)
                    }
                }
            },
            onReorderQueue = { mediaIds ->
                controller?.let { activeController ->
                    if (activeController.reorderQueue(mediaIds)) {
                        viewModel.reorderPlaybackQueue(mediaIds)
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
            onRemoveQueueItems = { mediaIds ->
                controller?.let { activeController ->
                    if (activeController.removeQueueItems(mediaIds)) {
                        viewModel.removePlaybackQueueItems(mediaIds)
                    }
                }
            },
            onSaveQueue = {
                pendingPlaylistAddition = PendingPlaylistAddition.CurrentQueue
            },
            onClearQueue = {
                controller?.let { activeController ->
                    viewModel.clearPlaybackQueue()
                    activeController.stop()
                    activeController.clearMediaItems()
                }
            },
            onSetSleepTimerMinutes = viewModel::setSleepTimerMinutes,
            onSetSleepTimerAtTrackEnd = viewModel::setSleepTimerAtTrackEnd,
            onSetSleepTimerAtQueueEnd = viewModel::setSleepTimerAtQueueEnd,
            onCancelSleepTimer = viewModel::cancelSleepTimer,
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

    val openCreatorCenter: () -> Unit = {
        showCreatorCenter = true
        viewModel.selectCreatorCenterTab(
            if (uiState.account.isLoggedIn) CreatorCenterTab.FOLLOWING else CreatorCenterTab.SEARCH,
        )
    }

    Scaffold(
        topBar = {
            BiuTopBar(
                section = uiState.section,
                themeMode = uiState.themeMode,
                themeMenuExpanded = showThemeMenu,
                account = uiState.account,
                accountMenuExpanded = showAccountMenu,
                isAccountLoading = uiState.isAccountLoading,
                isUpdateChecking = uiState.isUpdateChecking,
                isDynamicLoading = uiState.dynamicFeed.isLoading,
                onOpenCreatorCenter = openCreatorCenter,
                onRefreshDynamic = { viewModel.loadDynamicFeed(reset = true) },
                onShowThemeMenu = {
                    showAccountMenu = false
                    showThemeMenu = true
                },
                onDismissThemeMenu = { showThemeMenu = false },
                onThemeSelected = { mode ->
                    showThemeMenu = false
                    viewModel.selectThemeMode(mode)
                },
                onShowAccountMenu = {
                    showThemeMenu = false
                    showAccountMenu = true
                },
                onDismissAccountMenu = { showAccountMenu = false },
                onOpenDisplaySettings = {
                    showAccountMenu = false
                    showDisplaySettings = true
                },
                onLogin = {
                    showAccountMenu = false
                    showLogin = true
                },
                onRefreshAccount = {
                    showAccountMenu = false
                    viewModel.refreshAccount()
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
                onOpenQueue = { showQuickQueue = true },
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
                    creatorFeedTabs = uiState.creatorFeedTabs,
                    selectedCreators = uiState.selectedCreators,
                    creatorGroups = uiState.creatorGroups,
                    creatorGroupMembers = uiState.creatorGroupMembers,
                    localHistory = uiState.localHistory,
                    homeDiscoveryScope = uiState.homeDiscoveryScope,
                    homeDiscoveryMode = uiState.homeDiscoveryMode,
                    loading = uiState.isFeedLoading,
                    loadingMore = uiState.isFeedLoadingMore,
                    hasMore = uiState.recommendationHasMore,
                    searchLoading = uiState.isSearchLoading,
                    searchLoadingMore = uiState.isSearchLoadingMore,
                    searchHasMore = uiState.searchHasMore,
                    resolvingBvid = uiState.resolvingBvid,
                    videoLayout = uiState.videoLayout,
                    onFeedChange = viewModel::loadRecommendations,
                    onHomeDiscoveryScopeChange = viewModel::selectHomeDiscoveryScope,
                    onHomeDiscoveryModeChange = viewModel::selectHomeDiscoveryMode,
                    onRefresh = { viewModel.loadRecommendations() },
                    onLoadMore = viewModel::loadMoreRecommendations,
                    onSearch = viewModel::search,
                    onLoadMoreSearch = viewModel::loadMoreSearchResults,
                    onClearSearch = viewModel::clearSearch,
                    onPlay = viewModel::play,
                    onAddFavorite = { video ->
                        if (uiState.account.isLoggedIn) favoritePickerVideo = video else showLogin = true
                    },
                    modifier = pageModifier,
                )
                MainSection.DYNAMIC -> DynamicFeedScreen(
                    state = uiState.dynamicFeed,
                    accountLoggedIn = uiState.account.isLoggedIn,
                    resolvingBvid = uiState.resolvingBvid,
                    onLogin = { showLogin = true },
                    onRefresh = { viewModel.loadDynamicFeed(reset = true) },
                    onLoadMore = viewModel::loadMoreDynamicFeed,
                    onPlay = viewModel::play,
                    onLike = viewModel::toggleDynamicLike,
                    onTriple = { dynamic -> tripleConfirmation = dynamic },
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
                    onPlayLocalAudio = viewModel::playLocalAudio,
                    onAddLocalAudioToPlaylist = { audio ->
                        pendingPlaylistAddition = PendingPlaylistAddition.LocalTrack(audio)
                    },
                    onCreateLocalPlaylist = viewModel::createLocalPlaylist,
                    onRenameLocalPlaylist = viewModel::renameLocalPlaylist,
                    onDeleteLocalPlaylist = viewModel::deleteLocalPlaylist,
                    onOpenLocalPlaylist = viewModel::openLocalPlaylist,
                    onCloseLocalPlaylist = viewModel::closeLocalPlaylist,
                    onPlayLocalPlaylist = viewModel::playLocalPlaylist,
                    onRemoveLocalPlaylistItem = viewModel::removeLocalPlaylistItem,
                    onMoveLocalPlaylistItem = viewModel::moveLocalPlaylistItem,
                    onClearLocalHistory = viewModel::clearLocalHistory,
                    onClearAllHistory = viewModel::clearAllHistory,
                    onSearchOnlineHistory = viewModel::searchOnlineHistory,
                    onLoadMoreOnlineHistory = viewModel::loadMoreOnlineHistory,
                    onDeleteOnlineHistory = viewModel::deleteOnlineHistory,
                    onClearOnlineHistory = viewModel::clearOnlineHistory,
                    onReportPlayHistoryChange = viewModel::setReportPlayHistory,
                    downloadContent = { downloadModifier ->
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
                            onPlayAudio = viewModel::playDownloadedAudio,
                            onPlayVideo = viewModel::playDownloadedVideo,
                            modifier = downloadModifier,
                        )
                    },
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

    if (showCreatorCenter) {
        // long: UP 主搜索与首页范围都覆盖在推荐页之上，关闭后保留列表位置和筛选状态，不打断首页浏览上下文。
        CreatorCenterScreen(
            state = uiState.creatorCenter,
            selectedCreators = uiState.selectedCreators,
            groups = uiState.creatorGroups,
            groupMembers = uiState.creatorGroupMembers,
            accountLoggedIn = uiState.account.isLoggedIn,
            resolvingBvid = uiState.resolvingBvid,
            onBack = {
                viewModel.closeCreatorProfile()
                showCreatorCenter = false
            },
            onTabSelected = viewModel::selectCreatorCenterTab,
            onGroupSelected = viewModel::selectCreatorGroup,
            onSearch = viewModel::searchCreators,
            onClearSearch = viewModel::clearCreatorSearch,
            onLoadMoreSearch = { viewModel.searchCreators("", loadMore = true) },
            onLoadFollowing = viewModel::loadCreatorCenterFollowing,
            onOpenCreator = viewModel::openCreatorProfile,
            onCloseCreator = viewModel::closeCreatorProfile,
            onProfileTabSelected = viewModel::selectCreatorProfileTab,
            onLoadMoreCollections = viewModel::loadMoreCreatorCollections,
            onOpenCollection = viewModel::openCreatorCollection,
            onCloseCollection = viewModel::closeCreatorCollection,
            onLoadMoreCollectionVideos = viewModel::loadMoreCreatorCollectionVideos,
            onPlayCollection = viewModel::playCreatorCollection,
            onToggleRelation = viewModel::toggleCreatorRelation,
            onLoadMoreVideos = viewModel::loadMoreCreatorVideos,
            onOpenHomeScope = {
                showCreatorCenter = false
                showCreatorConfig = true
                viewModel.loadFollowingCreators()
            },
            onCreateGroup = viewModel::createCreatorGroup,
            onRenameGroup = viewModel::renameCreatorGroup,
            onDeleteGroup = viewModel::deleteCreatorGroup,
            onToggleCreatorGroup = viewModel::toggleCreatorGroup,
            onPlay = viewModel::play,
            onAddFavorite = { video ->
                if (uiState.account.isLoggedIn) favoritePickerVideo = video else showLogin = true
            },
            onLogin = { showLogin = true },
        )
    }

    if (showDisplaySettings) {
        DisplaySettingsSheet(
            listDensity = uiState.listDensity,
            textScale = uiState.textScale,
            videoLayout = uiState.videoLayout,
            onDismiss = { showDisplaySettings = false },
            onListDensitySelected = viewModel::selectListDensity,
            onTextScaleSelected = viewModel::selectTextScale,
            onVideoLayoutSelected = viewModel::selectVideoLayout,
        )
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
    account: BilibiliAccount,
    accountMenuExpanded: Boolean,
    isAccountLoading: Boolean,
    isUpdateChecking: Boolean,
    isDynamicLoading: Boolean,
    onOpenCreatorCenter: () -> Unit,
    onRefreshDynamic: () -> Unit,
    onShowThemeMenu: () -> Unit,
    onDismissThemeMenu: () -> Unit,
    onThemeSelected: (AppThemeMode) -> Unit,
    onShowAccountMenu: () -> Unit,
    onDismissAccountMenu: () -> Unit,
    onOpenDisplaySettings: () -> Unit,
    onLogin: () -> Unit,
    onRefreshAccount: () -> Unit,
    onCheckUpdate: () -> Unit,
    onLogout: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(section.label, style = MaterialTheme.typography.titleLarge)
        },
        actions = {
            if (section == MainSection.RECOMMEND) {
                IconButton(onClick = onOpenCreatorCenter) {
                    Icon(Icons.Rounded.PersonSearch, contentDescription = "管理音乐来源")
                }
            }
            if (section == MainSection.DYNAMIC) {
                IconButton(
                    onClick = onRefreshDynamic,
                    enabled = account.isLoggedIn && !isDynamicLoading,
                ) {
                    if (isDynamicLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .semantics { contentDescription = "正在刷新动态" },
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新动态")
                    }
                }
            }
            if (section == MainSection.ACCOUNT) {
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
                        onOpenDisplaySettings = onOpenDisplaySettings,
                        onLogin = onLogin,
                        onRefresh = onRefreshAccount,
                        onCheckUpdate = onCheckUpdate,
                        onLogout = onLogout,
                    )
                }
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
    onOpenDisplaySettings: () -> Unit,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
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
            text = { Text("界面显示") },
            leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
            onClick = onOpenDisplaySettings,
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplaySettingsSheet(
    listDensity: AppListDensity,
    textScale: AppTextScale,
    videoLayout: AppVideoLayout,
    onDismiss: () -> Unit,
    onListDensitySelected: (AppListDensity) -> Unit,
    onTextScaleSelected: (AppTextScale) -> Unit,
    onVideoLayoutSelected: (AppVideoLayout) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.widthIn(max = 840.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        // long: 显示偏好即时写入且可随时关闭，避免额外保存步骤让已经生效的设置与当前界面脱节。
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            BiuSheetHeader(title = "界面显示", onClose = onDismiss)
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("字体大小", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTextScale.entries.forEach { scale ->
                            FilterChip(
                                selected = textScale == scale,
                                onClick = { onTextScaleSelected(scale) },
                                label = { Text(scale.label) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("媒体列表", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppListDensity.entries.forEach { density ->
                            FilterChip(
                                selected = listDensity == density,
                                onClick = { onListDensitySelected(density) },
                                label = { Text(density.label) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Text(
                        "紧凑模式会缩小媒体封面与列表间距，保留标题、时长和作者信息。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("推荐视频布局", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppVideoLayout.entries.forEach { layout ->
                            FilterChip(
                                selected = videoLayout == layout,
                                onClick = { onVideoLayoutSelected(layout) },
                                label = { Text(layout.label) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Text(
                        "网格仅用于推荐和搜索结果，保留播放、收藏与继续加载。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
    onOpenQueue: () -> Unit,
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
                onOpenQueue = onOpenQueue,
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
    MainSection.DYNAMIC -> Icons.Rounded.DynamicFeed
    MainSection.ACCOUNT -> Icons.Rounded.LibraryMusic
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RecommendationScreen(
    videos: List<BilibiliVideo>,
    searchResults: List<BilibiliVideo>,
    submittedKeyword: String,
    feed: RecommendFeed,
    creatorFeedTabs: List<CreatorFeedTabState>,
    selectedCreators: List<BilibiliCreator>,
    creatorGroups: List<CreatorGroupEntity>,
    creatorGroupMembers: Map<Long, Set<Long>>,
    localHistory: List<PlaybackHistoryEntity>,
    homeDiscoveryScope: HomeDiscoveryScope,
    homeDiscoveryMode: HomeDiscoveryMode,
    loading: Boolean,
    loadingMore: Boolean,
    hasMore: Boolean,
    searchLoading: Boolean,
    searchLoadingMore: Boolean,
    searchHasMore: Boolean,
    resolvingBvid: String?,
    videoLayout: AppVideoLayout,
    onFeedChange: (RecommendFeed) -> Unit,
    onHomeDiscoveryScopeChange: (HomeDiscoveryScope) -> Unit,
    onHomeDiscoveryModeChange: (HomeDiscoveryMode) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onSearch: (String) -> Unit,
    onLoadMoreSearch: () -> Unit,
    onClearSearch: () -> Unit,
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
    val scopeOptions = HomeDiscoveryPolicy.scopeOptions(
        creators = selectedCreators,
        groups = creatorGroups,
        memberships = creatorGroupMembers,
    )
    val activeScope = HomeDiscoveryPolicy.normalizeScope(
        scope = homeDiscoveryScope,
        creators = selectedCreators,
        groups = creatorGroups,
        memberships = creatorGroupMembers,
    )
    val discovery = HomeDiscoveryPolicy.snapshot(
        tabs = creatorFeedTabs,
        scope = activeScope,
        mode = homeDiscoveryMode,
        creators = selectedCreators,
        memberships = creatorGroupMembers,
        history = localHistory,
    )
    val usingCreatorSources = selectedCreators.isNotEmpty()
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactSearchField(
                value = keyword,
                onValueChange = { value ->
                    keyword = value
                    if (value.isBlank() && showingSearchResults) onClearSearch()
                },
                onSearch = submitSearch,
                onClear = {
                    keyword = ""
                    onClearSearch()
                },
                placeholder = "搜索标题或 UP 主",
                loading = searchLoading,
                modifier = Modifier.fillMaxWidth(),
            )
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
            RecommendationTabs(
                selectedFeed = feed,
                creatorScopes = scopeOptions,
                selectedCreatorScope = activeScope,
                discoveryMode = homeDiscoveryMode,
                onFeedSelected = onFeedChange,
                onCreatorScopeSelected = onHomeDiscoveryScopeChange,
                onDiscoveryModeSelected = onHomeDiscoveryModeChange,
            )
        }
        val recommendationVideos = if (usingCreatorSources) discovery.videos else videos
        val recommendationLoading = if (usingCreatorSources) discovery.isLoading else loading
        val recommendationLoadingMore = if (usingCreatorSources) discovery.isLoadingMore else loadingMore
        val recommendationHasMore = if (usingCreatorSources) discovery.hasMore else hasMore
        val recommendationPaginationKey = if (usingCreatorSources) discovery.paginationKey else recommendationVideos.size
        val activeLoading = if (showingSearchResults) searchLoading else recommendationLoading
        val activeVideos = if (showingSearchResults) searchResults else recommendationVideos
        PullToRefreshBox(
            isRefreshing = activeLoading,
            onRefresh = {
                if (showingSearchResults) onSearch(submittedKeyword) else onRefresh()
            },
            modifier = Modifier.weight(1f),
        ) {
            if (!activeLoading && activeVideos.isEmpty()) {
                val canContinueDiscovery = !showingSearchResults && usingCreatorSources && recommendationHasMore
                BiuEmptyState(
                    icon = if (showingSearchResults) Icons.Rounded.Search else Icons.Rounded.LibraryMusic,
                    title = when {
                        showingSearchResults -> "没有找到结果"
                        usingCreatorSources && homeDiscoveryMode == HomeDiscoveryMode.UNPLAYED -> "当前范围没有未播放投稿"
                        usingCreatorSources && homeDiscoveryMode == HomeDiscoveryMode.RECENT -> "当前范围没有最近播放投稿"
                        else -> "暂时没有推荐"
                    },
                    message = when {
                        showingSearchResults -> "换一个关键词再试试"
                        canContinueDiscovery -> "可以继续读取更早的 UP 主投稿"
                        else -> null
                    },
                    actionLabel = when {
                        showingSearchResults -> "返回推荐"
                        canContinueDiscovery -> "继续查找"
                        else -> "重新加载"
                    },
                    onAction = if (showingSearchResults) {
                        {
                            keyword = ""
                            onClearSearch()
                        }
                    } else if (canContinueDiscovery) {
                        onLoadMore
                    } else {
                        onRefresh
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                VideoList(
                    videos = activeVideos,
                    resolvingBvid = resolvingBvid,
                    videoLayout = videoLayout,
                    onPlay = onPlay,
                    onAddFavorite = onAddFavorite,
                    onLoadMore = if (showingSearchResults) onLoadMoreSearch else onLoadMore,
                    hasMore = if (showingSearchResults) searchHasMore else recommendationHasMore,
                    loadingMore = if (showingSearchResults) searchLoadingMore else recommendationLoadingMore,
                    paginationKey = if (showingSearchResults) searchResults.size else recommendationPaginationKey,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DynamicFeedScreen(
    state: DynamicFeedUiState,
    accountLoggedIn: Boolean,
    resolvingBvid: String?,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPlay: (BilibiliVideo) -> Unit,
    onLike: (BilibiliDynamicItem) -> Unit,
    onTriple: (BilibiliDynamicItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!accountLoggedIn) {
        BiuEmptyState(
            icon = Icons.Rounded.DynamicFeed,
            title = "登录后查看关注动态",
            actionLabel = "登录",
            onAction = onLogin,
            modifier = modifier,
        )
        return
    }
    val listState = rememberLazyListState()
    val listMetrics = LocalBiuListDensity.current
    val shouldLoadMore by remember(listState, state.items.size, state.hasMore) {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            state.hasMore && state.items.isNotEmpty() && lastVisibleIndex >= state.items.lastIndex - 2
        }
    }
    LaunchedEffect(shouldLoadMore, state.items.size, state.hasMore) {
        if (shouldLoadMore && !state.isLoadingMore) onLoadMore()
    }
    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        if (!state.isLoading && state.items.isEmpty()) {
            BiuEmptyState(
                icon = Icons.Rounded.DynamicFeed,
                title = "暂时没有视频动态",
                actionLabel = "重新加载",
                onAction = onRefresh,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                items(state.items, key = BilibiliDynamicItem::id) { item ->
                    DynamicFeedItem(
                        item = item,
                        resolving = resolvingBvid == item.video.bvid,
                        mutating = item.id in state.mutatingIds,
                        onPlay = { onPlay(item.video) },
                        onLike = { onLike(item) },
                        onTriple = { onTriple(item) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            start = 16.dp + listMetrics.dynamicThumbnailWidth + listMetrics.dynamicRowSpacing,
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                if (state.isLoadingMore) {
                    item(key = "dynamic-loading-more") {
                        ListLoadingFooter()
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicFeedItem(
    item: BilibiliDynamicItem,
    resolving: Boolean,
    mutating: Boolean,
    onPlay: () -> Unit,
    onLike: () -> Unit,
    onTriple: () -> Unit,
) {
    val density = LocalDensity.current
    val listMetrics = LocalBiuListDensity.current
    val authorRowHeight = maxOf(
        18.dp,
        with(density) { MaterialTheme.typography.labelSmall.lineHeight.toDp() } + 1.dp,
    )
    val titleHeight = with(density) { MaterialTheme.typography.bodyMedium.lineHeight.toDp() } * 2 + 1.dp
    val actionRowHeight = maxOf(
        40.dp,
        with(density) { MaterialTheme.typography.labelSmall.lineHeight.toDp() } + 1.dp,
    )
    val itemHeight = authorRowHeight + titleHeight + actionRowHeight
    // long: 四行高度按当前字体行高计算并预留像素舍入余量，确保标题真正获得两行，而不是在高密度屏幕上退化成单行省略。
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = listMetrics.rowVerticalPadding)
            .height(itemHeight),
        horizontalArrangement = Arrangement.spacedBy(listMetrics.dynamicRowSpacing),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(width = listMetrics.dynamicThumbnailWidth, height = itemHeight)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics {
                    role = Role.Button
                    contentDescription = "播放 ${item.video.title}"
                }
                .clickable(enabled = !resolving, onClick = onPlay),
        ) {
            AsyncImage(
                model = item.video.coverUrl,
                contentDescription = item.video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            item.video.durationSeconds?.let { duration ->
                Text(
                    formatDuration(duration),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (resolving) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .semantics {
                        role = Role.Button
                        contentDescription = "播放 ${item.video.title}"
                    }
                    .clickable(enabled = !resolving, onClick = onPlay),
            ) {
                Row(
                    modifier = Modifier.height(authorRowHeight),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AsyncImage(
                        model = item.authorFaceUrl,
                        contentDescription = item.video.author,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop,
                    )
                    Text(
                        item.video.author.ifBlank { "未知 UP 主" },
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        formatPublishedDateTime(item.publishedAtEpochSeconds),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    item.video.title,
                    modifier = Modifier.height(titleHeight),
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(actionRowHeight),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DynamicAction(
                    icon = Icons.Rounded.ThumbUp,
                    label = item.likeCount.toString(),
                    contentDescription = if (item.isLiked) "取消点赞" else "点赞",
                    enabled = !mutating && !item.isLikeForbidden,
                    loading = mutating,
                    selected = item.isLiked,
                    onClick = onLike,
                )
                DynamicAction(
                    icon = Icons.Rounded.Bolt,
                    label = "三连",
                    contentDescription = "一键三连",
                    enabled = !mutating,
                    onClick = onTriple,
                )
            }
        }
    }
}

@Composable
private fun DynamicAction(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    enabled: Boolean,
    loading: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f)
                } else {
                    Color.Transparent
                },
            )
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
                if (selected) stateDescription = "已选中"
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = contentColor)
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}

@Composable
private fun RecommendationTabs(
    selectedFeed: RecommendFeed,
    creatorScopes: List<HomeDiscoveryScopeOption>,
    selectedCreatorScope: HomeDiscoveryScope,
    discoveryMode: HomeDiscoveryMode,
    onFeedSelected: (RecommendFeed) -> Unit,
    onCreatorScopeSelected: (HomeDiscoveryScope) -> Unit,
    onDiscoveryModeSelected: (HomeDiscoveryMode) -> Unit,
) {
    // long: 未配置来源时继续展示 B 站热门分类；配置后先选择发现语义，再按全部、分组或单个 UP 缩小范围。
    if (creatorScopes.size <= 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            RecommendFeed.entries.forEach { option ->
                RecommendationTab(
                    label = option.label,
                    selected = selectedFeed == option,
                    onClick = { onFeedSelected(option) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            HomeDiscoveryMode.entries.forEach { mode ->
                RecommendationTab(
                    label = mode.label,
                    selected = discoveryMode == mode,
                    onClick = { onDiscoveryModeSelected(mode) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .horizontalScroll(rememberScrollState()),
        ) {
            creatorScopes.forEach { option ->
                RecommendationTab(
                    label = option.label,
                    selected = selectedCreatorScope == option.scope,
                    onClick = { onCreatorScopeSelected(option.scope) },
                    modifier = Modifier
                        .widthIn(min = 88.dp, max = 160.dp)
                        .fillMaxHeight(),
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun RecommendationTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
internal fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: (() -> Unit)?,
    onClear: () -> Unit,
    placeholder: String,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onSearch == null) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null)
                        }
                    } else {
                        IconButton(
                            onClick = onSearch,
                            enabled = value.isNotBlank() && !loading,
                            modifier = Modifier.size(40.dp),
                        ) {
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.Search, contentDescription = "搜索")
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isBlank()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                    if (value.isNotBlank()) {
                        IconButton(
                            onClick = onClear,
                            enabled = !loading,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "清空搜索")
                        }
                    }
                }
            },
        )
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
    onPlayLocalAudio: (LocalAudio, LocalAudioPlaybackMode) -> Unit,
    onAddLocalAudioToPlaylist: (LocalAudio) -> Unit,
    onCreateLocalPlaylist: (String) -> Unit,
    onRenameLocalPlaylist: (LocalPlaylistEntity, String) -> Unit,
    onDeleteLocalPlaylist: (Long) -> Unit,
    onOpenLocalPlaylist: (LocalPlaylistEntity) -> Unit,
    onCloseLocalPlaylist: () -> Unit,
    onPlayLocalPlaylist: (Int) -> Unit,
    onRemoveLocalPlaylistItem: (String) -> Unit,
    onMoveLocalPlaylistItem: (String, Int) -> Unit,
    onClearLocalHistory: () -> Unit,
    onClearAllHistory: () -> Unit,
    onSearchOnlineHistory: (String) -> Unit,
    onLoadMoreOnlineHistory: () -> Unit,
    onDeleteOnlineHistory: (BilibiliLibraryVideo) -> Unit,
    onClearOnlineHistory: () -> Unit,
    onReportPlayHistoryChange: (Boolean) -> Unit,
    downloadContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeLibrarySection = state.librarySection.normalizedLibrarySection()
    Column(modifier = modifier.fillMaxSize()) {
        if (state.isAccountLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        AccountLibraryNavigation(
            selectedSection = activeLibrarySection,
            onSelect = onLoadLibrary,
        )

        if (state.isLibraryLoading && activeLibrarySection != AccountLibrarySection.HISTORY) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        val onlineSection = activeLibrarySection == AccountLibrarySection.FAVORITES
        if (onlineSection && !state.account.isLoggedIn) {
            BiuEmptyState(
                icon = Icons.Rounded.AccountCircle,
                title = "登录后查看${activeLibrarySection.label}",
                actionLabel = "登录 Bilibili",
                onAction = onLogin,
                modifier = Modifier.weight(1f),
            )
        } else {
            when (activeLibrarySection) {
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
                AccountLibrarySection.HISTORY,
                AccountLibrarySection.ONLINE_HISTORY,
                AccountLibrarySection.LOCAL_HISTORY,
                -> HistoryList(
                    onlineVideos = state.libraryVideos,
                    localHistory = state.localHistory,
                    accountLoggedIn = state.account.isLoggedIn,
                    resolvingBvid = state.resolvingBvid,
                    loading = state.isLibraryLoading,
                    loadingMore = state.isOnlineHistoryLoadingMore,
                    mutating = state.isOnlineHistoryMutating,
                    hasMore = state.onlineHistoryHasMore,
                    query = state.onlineHistoryQuery,
                    reportPlayHistory = state.reportPlayHistory,
                    onLogin = onLogin,
                    onPlay = onPlay,
                    onPlayLocal = onPlayHistory,
                    onSearch = onSearchOnlineHistory,
                    onLoadMore = onLoadMoreOnlineHistory,
                    onDeleteOnline = onDeleteOnlineHistory,
                    onClearLocal = onClearLocalHistory,
                    onClearOnline = onClearOnlineHistory,
                    onClearAll = onClearAllHistory,
                    onReportPlayHistoryChange = onReportPlayHistoryChange,
                    modifier = Modifier.weight(1f),
                )
                AccountLibrarySection.PLAYLISTS -> LocalPlaylistLibrary(
                    playlists = state.localPlaylists,
                    selectedPlaylist = state.selectedLocalPlaylist,
                    items = state.localPlaylistItems,
                    loading = state.isLocalPlaylistLoading,
                    onCreate = onCreateLocalPlaylist,
                    onRename = onRenameLocalPlaylist,
                    onDelete = onDeleteLocalPlaylist,
                    onOpen = onOpenLocalPlaylist,
                    onClose = onCloseLocalPlaylist,
                    onPlay = onPlayLocalPlaylist,
                    onRemoveItem = onRemoveLocalPlaylistItem,
                    onMoveItem = onMoveLocalPlaylistItem,
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
                    onAddToPlaylist = onAddLocalAudioToPlaylist,
                    modifier = Modifier.weight(1f),
                )
                AccountLibrarySection.DOWNLOADS -> downloadContent(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
    val activeSection = selectedSection.normalizedLibrarySection()
    val selectedGroup = AccountLibraryGroup.entries.first { group -> activeSection in group.sections }
    // long: 在线、本地和下载先按数据来源分层，收藏与历史作为二级切换，窄屏不再挤压五个并列标签。
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        AccountLibraryGroup.entries.forEach { group ->
            val selected = selectedGroup == group
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics {
                        role = Role.Tab
                        this.selected = selected
                    }
                    .clickable {
                        if (!selected) onSelect(group.sections.first())
                    },
            ) {
                Text(
                    text = group.label,
                    modifier = Modifier.align(Alignment.Center),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    if (selectedGroup.sections.size > 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            selectedGroup.sections.forEach { section ->
                val selected = activeSection == section
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            role = Role.Tab
                            this.selected = selected
                        }
                        .clickable { onSelect(section) },
                ) {
                    Text(
                        text = section.label,
                        modifier = Modifier.align(Alignment.Center),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .width(56.dp)
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun LocalPlaylistLibrary(
    playlists: List<LocalPlaylistEntity>,
    selectedPlaylist: LocalPlaylistEntity?,
    items: List<LocalPlaylistItemEntity>,
    loading: Boolean,
    onCreate: (String) -> Unit,
    onRename: (LocalPlaylistEntity, String) -> Unit,
    onDelete: (Long) -> Unit,
    onOpen: (LocalPlaylistEntity) -> Unit,
    onClose: () -> Unit,
    onPlay: (Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onMoveItem: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditor by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<LocalPlaylistEntity?>(null) }
    var playlistName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<LocalPlaylistEntity?>(null) }
    var itemMenuId by remember { mutableStateOf<String?>(null) }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingPlaylist == null) "新建歌单" else "重命名歌单") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("歌单名称") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val normalized = playlistName.trim()
                        val editing = editingPlaylist
                        if (editing == null) onCreate(normalized) else onRename(editing, normalized)
                        showEditor = false
                    },
                    enabled = playlistName.isNotBlank(),
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("取消") } },
        )
    }
    pendingDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除歌单？") },
            text = { Text("将删除“${playlist.name}”和本地曲目清单，不会删除 Bilibili 收藏或本机音频文件。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(playlist.playlistId)
                    },
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }

    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectedPlaylist != null) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回歌单列表")
                }
            }
            Text(
                selectedPlaylist?.name ?: "本地歌单",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            if (selectedPlaylist != null && items.isNotEmpty()) {
                IconButton(onClick = { onPlay(0) }, enabled = !loading) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = "播放整个歌单")
                }
            }
            IconButton(
                onClick = {
                    editingPlaylist = selectedPlaylist
                    playlistName = selectedPlaylist?.name.orEmpty()
                    showEditor = true
                },
            ) {
                Icon(
                    if (selectedPlaylist == null) Icons.Rounded.Add else Icons.Rounded.Edit,
                    contentDescription = if (selectedPlaylist == null) "新建歌单" else "重命名歌单",
                )
            }
            selectedPlaylist?.let { playlist ->
                IconButton(onClick = { pendingDelete = playlist }) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除歌单")
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (selectedPlaylist == null) {
            if (playlists.isEmpty()) {
                BiuEmptyState(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    title = "还没有本地歌单",
                    message = "可以混合保存单 P、某个分 P 和本机音乐。",
                    actionLabel = "新建歌单",
                    onAction = {
                        editingPlaylist = null
                        playlistName = ""
                        showEditor = true
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(playlists, key = LocalPlaylistEntity::playlistId) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(playlist) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null)
                            Text(
                                playlist.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        MediaDivider(start = 52.dp)
                    }
                }
            }
        } else if (!loading && items.isEmpty()) {
            BiuEmptyState(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                title = "歌单还是空的",
                message = "在多 P 列表或本地音乐中选择“加入歌单”。",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 2.dp)) {
                itemsIndexed(items, key = { _, item -> item.mediaId }) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !loading) { onPlay(index) }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null)
                            item.artworkUrl?.let { artwork ->
                                AsyncImage(
                                    model = artwork,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                item.pageTitle?.substringAfter(" · ")?.ifBlank { item.title } ?: item.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                item.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box {
                            IconButton(onClick = { itemMenuId = item.mediaId }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "编辑${item.title}")
                            }
                            DropdownMenu(
                                expanded = itemMenuId == item.mediaId,
                                onDismissRequest = { itemMenuId = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("上移") },
                                    enabled = index > 0,
                                    onClick = {
                                        itemMenuId = null
                                        onMoveItem(item.mediaId, -1)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("下移") },
                                    enabled = index < items.lastIndex,
                                    onClick = {
                                        itemMenuId = null
                                        onMoveItem(item.mediaId, 1)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("移出歌单") },
                                    onClick = {
                                        itemMenuId = null
                                        onRemoveItem(item.mediaId)
                                    },
                                )
                            }
                        }
                    }
                    MediaDivider(start = 72.dp)
                }
            }
        }
    }
}

@Composable
private fun LocalPlaylistPickerDialog(
    playlists: List<LocalPlaylistEntity>,
    onDismiss: () -> Unit,
    onSelect: (LocalPlaylistEntity) -> Unit,
    onOpenLibrary: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
        title = { Text("加入本地歌单") },
        text = {
            if (playlists.isEmpty()) {
                Text("还没有本地歌单，请先在音乐库中创建。")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    playlists.forEach { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { onSelect(playlist) }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null)
                            Text(
                                playlist.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (playlists.isEmpty()) {
                TextButton(onClick = onOpenLibrary) { Text("去新建歌单") }
            } else {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

private enum class AccountLibraryGroup(
    val label: String,
    val sections: List<AccountLibrarySection>,
) {
    ONLINE("在线", listOf(AccountLibrarySection.FAVORITES, AccountLibrarySection.HISTORY)),
    LOCAL(
        "本地",
        listOf(AccountLibrarySection.PLAYLISTS, AccountLibrarySection.LOCAL_MUSIC),
    ),
    DOWNLOADS("下载", listOf(AccountLibrarySection.DOWNLOADS)),
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onPlay: (LocalAudio, LocalAudioPlaybackMode) -> Unit,
    onAddToPlaylist: (LocalAudio) -> Unit,
    modifier: Modifier = Modifier,
) {
    val directoryFilteringSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val listMetrics = LocalBiuListDensity.current
    var pendingPlaybackId by rememberSaveable { mutableStateOf<Long?>(null) }
    val pendingPlayback = audio.firstOrNull { item -> item.mediaStoreId == pendingPlaybackId }
    if (pendingPlayback != null) {
        ModalBottomSheet(
            onDismissRequest = { pendingPlaybackId = null },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
            ) {
                Text(
                    "选择播放范围",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    pendingPlayback.title,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LocalAudioPlaybackChoice(
                    icon = Icons.Rounded.PlayArrow,
                    title = "仅播放此曲",
                    description = "加入播放队列 · 1 首",
                    onClick = {
                        pendingPlaybackId = null
                        onPlay(pendingPlayback, LocalAudioPlaybackMode.SINGLE)
                    },
                )
                LocalAudioPlaybackChoice(
                    icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    title = "播放当前目录",
                    description = "加入播放队列 · ${audio.size} 首，从当前歌曲开始",
                    onClick = {
                        pendingPlaybackId = null
                        onPlay(pendingPlayback, LocalAudioPlaybackMode.CURRENT_DIRECTORY)
                    },
                )
            }
        }
    }
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
                contentPadding = PaddingValues(vertical = listMetrics.contentVerticalPadding),
            ) {
                items(audio, key = LocalAudio::mediaStoreId) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pendingPlaybackId = item.mediaStoreId }
                            .padding(horizontal = 16.dp, vertical = listMetrics.rowVerticalPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(listMetrics.mediaRowSpacing),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(listMetrics.localAudioArtworkSize)
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
                        IconButton(onClick = { onAddToPlaylist(item) }) {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "将${item.title}加入歌单")
                        }
                    }
                    MediaDivider(
                        start = 16.dp + listMetrics.localAudioArtworkSize + listMetrics.mediaRowSpacing,
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalAudioPlaybackChoice(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    // long: 收藏夹首页先展示两个分组摘要，用户主动展开后才渲染明细，避免长列表一进入页面就占满视野。
    var createdExpanded by rememberSaveable { mutableStateOf(false) }
    var collectedExpanded by rememberSaveable { mutableStateOf(false) }
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
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp),
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
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
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
    MediaDivider(start = 66.dp)
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
    val listMetrics = LocalBiuListDensity.current
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
        contentPadding = PaddingValues(vertical = listMetrics.contentVerticalPadding),
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
            MediaDivider(
                start = 16.dp + listMetrics.mediaThumbnailWidth + listMetrics.mediaRowSpacing,
            )
        }
        if (loadingMore) {
            item(key = "favorite-loading") {
                ListLoadingFooter(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun HistoryList(
    onlineVideos: List<BilibiliLibraryVideo>,
    localHistory: List<PlaybackHistoryEntity>,
    accountLoggedIn: Boolean,
    resolvingBvid: String?,
    loading: Boolean,
    loadingMore: Boolean,
    mutating: Boolean,
    hasMore: Boolean,
    query: String,
    reportPlayHistory: Boolean,
    onLogin: () -> Unit,
    onPlay: (BilibiliVideo) -> Unit,
    onPlayLocal: (PlaybackHistoryEntity) -> Unit,
    onSearch: (String) -> Unit,
    onLoadMore: () -> Unit,
    onDeleteOnline: (BilibiliLibraryVideo) -> Unit,
    onClearLocal: () -> Unit,
    onClearOnline: () -> Unit,
    onClearAll: () -> Unit,
    onReportPlayHistoryChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var source by rememberSaveable { mutableStateOf(HistorySourceFilter.ALL) }
    var searchText by rememberSaveable(query) { mutableStateOf(query) }
    var showClearChooser by remember { mutableStateOf(false) }
    var pendingClearScope by remember { mutableStateOf<HistoryClearScope?>(null) }
    val focusManager = LocalFocusManager.current
    val listMetrics = LocalBiuListDensity.current
    val entries = UnifiedHistoryPolicy.merge(
        online = onlineVideos,
        local = localHistory,
        source = source,
        query = searchText,
    )
    val hasVisibleSource = when (source) {
        HistorySourceFilter.ALL -> onlineVideos.isNotEmpty() || localHistory.isNotEmpty()
        HistorySourceFilter.ONLINE -> onlineVideos.isNotEmpty()
        HistorySourceFilter.LOCAL -> localHistory.isNotEmpty()
    }

    if (showClearChooser && pendingClearScope == null) {
        AlertDialog(
            onDismissRequest = { showClearChooser = false },
            title = { Text("选择清空范围") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "在线历史和本地历史相互独立，请先选择要影响的来源。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (accountLoggedIn) {
                        TextButton(
                            onClick = {
                                showClearChooser = false
                                pendingClearScope = HistoryClearScope.ONLINE
                            },
                            enabled = onlineVideos.isNotEmpty() && !mutating,
                        ) { Text("仅清空在线历史") }
                    }
                    TextButton(
                        onClick = {
                            showClearChooser = false
                            pendingClearScope = HistoryClearScope.LOCAL
                        },
                        enabled = localHistory.isNotEmpty(),
                    ) { Text("仅清空本地历史") }
                    TextButton(
                        onClick = {
                            showClearChooser = false
                            pendingClearScope = HistoryClearScope.ALL
                        },
                        enabled = hasVisibleSource && (!mutating || !accountLoggedIn),
                    ) { Text("清空全部历史") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showClearChooser = false }) { Text("取消") }
            },
        )
    }
    pendingClearScope?.let { scope ->
        AlertDialog(
            onDismissRequest = { pendingClearScope = null },
            title = { Text("确认${scope.label}") },
            text = {
                Text(
                    when (scope) {
                        HistoryClearScope.ONLINE -> "将删除 Bilibili 账号中的全部在线历史，本机播放记录不会受到影响。"
                        HistoryClearScope.LOCAL -> "将删除本机 Room 中的全部播放记录，Bilibili 在线历史不会受到影响。"
                        HistoryClearScope.ALL -> if (accountLoggedIn) {
                            "将同时删除 Bilibili 在线历史和本机播放记录。"
                        } else {
                            "当前未登录，只会删除本机播放记录；在线历史不会受到影响。"
                        }
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (scope) {
                            HistoryClearScope.ONLINE -> onClearOnline()
                            HistoryClearScope.LOCAL -> onClearLocal()
                            HistoryClearScope.ALL -> onClearAll()
                        }
                        pendingClearScope = null
                    },
                ) { Text("确认清空") }
            },
            dismissButton = {
                TextButton(onClick = { pendingClearScope = null }) { Text("取消") }
            },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            HistorySourceFilter.entries.forEach { candidate ->
                val selected = source == candidate
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            role = Role.Tab
                            this.selected = selected
                        }
                        .clickable { source = candidate },
                ) {
                    Text(
                        candidate.label,
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompactSearchField(
                value = searchText,
                onValueChange = { searchText = it },
                onSearch = {
                    focusManager.clearFocus()
                    if (source != HistorySourceFilter.LOCAL) onSearch(searchText)
                },
                onClear = {
                    searchText = ""
                    if (source != HistorySourceFilter.LOCAL) onSearch("")
                },
                placeholder = "筛选标题或作者",
                loading = loading,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { showClearChooser = true },
                enabled = hasVisibleSource && !mutating,
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "选择清空历史范围")
            }
        }
        if (source != HistorySourceFilter.LOCAL && accountLoggedIn) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "记录在线播放历史",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(
                    checked = reportPlayHistory,
                    onCheckedChange = onReportPlayHistoryChange,
                )
            }
        }
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (source == HistorySourceFilter.ONLINE && !accountLoggedIn) {
            BiuEmptyState(
                icon = Icons.Rounded.AccountCircle,
                title = "登录后查看在线历史",
                actionLabel = "登录 Bilibili",
                onAction = onLogin,
                modifier = Modifier.weight(1f),
            )
        } else if (!loading && entries.isEmpty()) {
            BiuEmptyState(
                icon = Icons.Rounded.History,
                title = if (hasVisibleSource && searchText.isNotBlank()) "没有匹配的历史" else "暂无${source.label}历史",
                message = "播放过的内容会按来源保存在这里",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = listMetrics.contentVerticalPadding),
            ) {
                items(entries, key = UnifiedHistoryItem::key) { entry ->
                    when (entry) {
                        is UnifiedHistoryItem.Online -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    LibraryVideoRow(
                                        item = entry.value,
                                        resolving = resolvingBvid == entry.value.video.bvid,
                                        enabled = resolvingBvid == null && !mutating,
                                        onClick = { onPlay(entry.value.video) },
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteOnline(entry.value) },
                                    enabled = entry.value.historyKey != null && !mutating,
                                    modifier = Modifier.padding(end = 4.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "删除在线历史 ${entry.value.video.title}",
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            MediaDivider(
                                start = 16.dp + listMetrics.mediaThumbnailWidth + listMetrics.mediaRowSpacing,
                            )
                        }
                        is UnifiedHistoryItem.Local -> {
                            LocalHistoryRow(
                                item = entry.value,
                                resolvingBvid = resolvingBvid,
                                onClick = { onPlayLocal(entry.value) },
                            )
                            MediaDivider(
                                start = 16.dp + listMetrics.mediaThumbnailWidth + listMetrics.mediaRowSpacing,
                            )
                        }
                    }
                }
                if (accountLoggedIn && source != HistorySourceFilter.LOCAL && hasMore) {
                    item(key = "unified-history-load-more") {
                        // long: 在线和本地记录按时间混排后，最后一项不一定来自在线历史；在整个列表底部触发才能稳定续页。
                        LaunchedEffect(onlineVideos.size, query, source, hasMore) {
                            if (!loadingMore) onLoadMore()
                        }
                    }
                }
                if (loadingMore) {
                    item(key = "history-loading") {
                        ListLoadingFooter(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

private enum class HistoryClearScope(val label: String) {
    ONLINE("清空在线历史"),
    LOCAL("清空本地历史"),
    ALL("清空全部历史"),
}

@Composable
private fun LocalHistoryRow(
    item: PlaybackHistoryEntity,
    resolvingBvid: String?,
    onClick: () -> Unit,
) {
    val listMetrics = LocalBiuListDensity.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = resolvingBvid == null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = listMetrics.rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(listMetrics.mediaRowSpacing),
    ) {
        AsyncImage(
            model = item.artworkUrl,
            contentDescription = item.title,
            modifier = Modifier
                .size(width = listMetrics.mediaThumbnailWidth, height = listMetrics.mediaThumbnailHeight)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
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
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
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
    val listMetrics = LocalBiuListDensity.current

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
            CompactSearchField(
                value = searchText,
                onValueChange = { searchText = it },
                onSearch = {
                    focusManager.clearFocus()
                    onSearch(searchText)
                },
                onClear = {
                    searchText = ""
                    onSearch("")
                },
                placeholder = "搜索标题或 UP 主",
                loading = loading,
                modifier = Modifier.weight(1f),
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
                contentPadding = PaddingValues(vertical = listMetrics.contentVerticalPadding),
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
                    MediaDivider(
                        start = 16.dp + listMetrics.mediaThumbnailWidth + listMetrics.mediaRowSpacing,
                    )
                }
                if (loadingMore) {
                    item(key = "online-history-loading") {
                        ListLoadingFooter(modifier = Modifier.padding(vertical = 4.dp))
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
    val listMetrics = LocalBiuListDensity.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = listMetrics.rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(listMetrics.mediaRowSpacing),
    ) {
        AsyncImage(
            model = video.coverUrl,
            contentDescription = video.title,
            modifier = Modifier
                .size(
                    width = listMetrics.mediaThumbnailWidth,
                    height = listMetrics.mediaThumbnailHeight,
                )
                .clip(RoundedCornerShape(6.dp))
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
    val listMetrics = LocalBiuListDensity.current
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
                contentPadding = PaddingValues(vertical = listMetrics.contentVerticalPadding),
            ) {
                items(history, key = PlaybackHistoryEntity::mediaId) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = resolvingBvid == null) { onPlay(item) }
                            .padding(horizontal = 16.dp, vertical = listMetrics.rowVerticalPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(listMetrics.mediaRowSpacing),
                    ) {
                        AsyncImage(
                            model = item.artworkUrl,
                            contentDescription = item.title,
                            modifier = Modifier
                                .size(
                                    width = listMetrics.mediaThumbnailWidth,
                                    height = listMetrics.mediaThumbnailHeight,
                                )
                                .clip(RoundedCornerShape(6.dp))
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
                    MediaDivider(
                        start = 16.dp + listMetrics.mediaThumbnailWidth + listMetrics.mediaRowSpacing,
                    )
                }
            }
        }
    }
}

@Composable
internal fun BiuEmptyState(
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
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun ListLoadingFooter(
    label: String = "正在加载更多",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VideoList(
    videos: List<BilibiliVideo>,
    resolvingBvid: String?,
    videoLayout: AppVideoLayout,
    onPlay: (BilibiliVideo) -> Unit,
    onAddFavorite: (BilibiliVideo) -> Unit,
    onLoadMore: (() -> Unit)?,
    hasMore: Boolean,
    loadingMore: Boolean,
    paginationKey: Int,
    modifier: Modifier = Modifier,
) {
    when (videoLayout) {
        AppVideoLayout.LIST -> VideoRowList(
            videos = videos,
            resolvingBvid = resolvingBvid,
            onPlay = onPlay,
            onAddFavorite = onAddFavorite,
            onLoadMore = onLoadMore,
            hasMore = hasMore,
            loadingMore = loadingMore,
            paginationKey = paginationKey,
            modifier = modifier,
        )
        AppVideoLayout.GRID -> VideoGrid(
            videos = videos,
            resolvingBvid = resolvingBvid,
            onPlay = onPlay,
            onAddFavorite = onAddFavorite,
            onLoadMore = onLoadMore,
            hasMore = hasMore,
            loadingMore = loadingMore,
            paginationKey = paginationKey,
            modifier = modifier,
        )
    }
}

@Composable
private fun VideoRowList(
    videos: List<BilibiliVideo>,
    resolvingBvid: String?,
    onPlay: (BilibiliVideo) -> Unit,
    onAddFavorite: (BilibiliVideo) -> Unit,
    onLoadMore: (() -> Unit)?,
    hasMore: Boolean,
    loadingMore: Boolean,
    paginationKey: Int,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    val listMetrics = LocalBiuListDensity.current
    val shouldLoadMore by remember(listState, paginationKey, hasMore) {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            hasMore && videos.isNotEmpty() && lastVisibleIndex >= videos.lastIndex - 3
        }
    }
    // long: 普通列表以可见数量推进，发现筛选则传入来源游标键；始终不把 loadingMore 作为键，避免请求结束后原地重复翻页。
    LaunchedEffect(shouldLoadMore, paginationKey, hasMore) {
        if (shouldLoadMore && !loadingMore) onLoadMore?.invoke()
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(vertical = listMetrics.contentVerticalPadding),
    ) {
        items(videos, key = BilibiliVideo::bvid) { video ->
            VideoRow(
                video = video,
                resolving = resolvingBvid == video.bvid,
                enabled = resolvingBvid == null,
                onClick = { onPlay(video) },
                onAddFavorite = { onAddFavorite(video) },
            )
            MediaDivider(
                start = 16.dp + listMetrics.mediaThumbnailWidth + listMetrics.mediaRowSpacing,
            )
        }
        if (loadingMore) {
            item(key = "recommendation-loading-more") {
                ListLoadingFooter()
            }
        }
    }
}

@Composable
private fun VideoGrid(
    videos: List<BilibiliVideo>,
    resolvingBvid: String?,
    onPlay: (BilibiliVideo) -> Unit,
    onAddFavorite: (BilibiliVideo) -> Unit,
    onLoadMore: (() -> Unit)?,
    hasMore: Boolean,
    loadingMore: Boolean,
    paginationKey: Int,
    modifier: Modifier,
) {
    val gridState = rememberLazyGridState()
    val listMetrics = LocalBiuListDensity.current
    val shouldLoadMore by remember(gridState, paginationKey, hasMore) {
        derivedStateOf {
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo
                .lastOrNull { item -> item.index < videos.size }
                ?.index ?: -1
            hasMore && videos.isNotEmpty() && lastVisibleIndex >= videos.lastIndex - 3
        }
    }
    // long: 续页指示器也会成为 Grid item；只计算真实视频索引，并用外部分页键区分可见数量未变化的发现筛选续页。
    LaunchedEffect(shouldLoadMore, paginationKey, hasMore) {
        if (shouldLoadMore && !loadingMore) onLoadMore?.invoke()
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columnCount = VideoGridLayoutPolicy.columnCount(maxWidth)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = listMetrics.contentVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(listMetrics.mediaRowSpacing),
        ) {
            gridItems(items = videos, key = BilibiliVideo::bvid) { video ->
                VideoGridCard(
                    video = video,
                    resolving = resolvingBvid == video.bvid,
                    enabled = resolvingBvid == null,
                    onClick = { onPlay(video) },
                    onAddFavorite = { onAddFavorite(video) },
                )
            }
            if (loadingMore) {
                item(
                    key = "recommendation-grid-loading-more",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    ListLoadingFooter()
                }
            }
        }
    }
}

@Composable
private fun VideoGridCard(
    video: BilibiliVideo,
    resolving: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onAddFavorite: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
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
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            IconButton(
                onClick = onAddFavorite,
                enabled = enabled,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.FavoriteBorder,
                            contentDescription = "收藏 ${video.title}",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (resolving) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            } else {
                IconButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier.align(Alignment.BottomEnd),
                ) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = "播放 ${video.title}",
                                modifier = Modifier.size(21.dp),
                            )
                        }
                    }
                }
            }
        }
        Text(
            video.title,
            minLines = 2,
            maxLines = 2,
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
}

@Composable
internal fun VideoRow(
    video: BilibiliVideo,
    resolving: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onAddFavorite: () -> Unit,
) {
    val listMetrics = LocalBiuListDensity.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = listMetrics.rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(listMetrics.mediaRowSpacing),
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = listMetrics.mediaThumbnailWidth,
                    height = listMetrics.mediaThumbnailHeight,
                )
                .clip(RoundedCornerShape(6.dp))
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
            .size(48.dp)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
internal fun MediaDivider(start: androidx.compose.ui.unit.Dp = 140.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = start, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
internal fun BiuSheetHeader(
    title: String,
    onClose: () -> Unit,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String? = null,
    onNavigation: (() -> Unit)? = null,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = if (navigationIcon == null) 16.dp else 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationIcon != null && onNavigation != null) {
                IconButton(onClick = onNavigation) {
                    Icon(navigationIcon, contentDescription = navigationContentDescription)
                }
            }
            Text(
                title,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "关闭$title")
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
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
        dragHandle = null,
    ) {
        // long: 内容范围包含可滚动的长关注列表，弹层直接占满可用高度，并把保存动作留在滚动区之外持续可见。
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            BiuSheetHeader(title = "首页内容范围", onClose = onDismiss)
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
                        CompactSearchField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            onSearch = null,
                            onClear = { keyword = "" },
                            placeholder = "按 UP 名称筛选",
                            loading = loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        AsyncImage(
                                            model = creator.faceUrl,
                                            contentDescription = creator.name,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(20.dp))
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
                                            modifier = Modifier.size(24.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.Transparent
                                            },
                                            border = if (selected) null else BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outline,
                                            ),
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
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    MediaDivider(start = 68.dp)
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        if (selectedMids.isEmpty()) "使用默认热门" else "已选 ${selectedMids.size} 位 UP",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onSave(creators.filter { creator -> creator.mid in selectedMids }) },
                        enabled = accountLoggedIn && !loading && !saving,
                        modifier = Modifier
                            .height(40.dp)
                            .widthIn(min = 112.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            when {
                                !accountLoggedIn -> "登录后保存"
                                saving -> "保存中"
                                selectedMids.isEmpty() -> "恢复默认"
                                else -> "保存"
                            },
                        )
                    }
                }
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
    lastPlayed: PlaybackHistoryEntity?,
    loading: Boolean,
    onDismiss: () -> Unit,
    onPlayPage: (Int) -> Unit,
    onAddPage: (Int) -> Unit,
    onResumePage: (Int, PlaybackHistoryEntity) -> Unit,
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
            lastPlayed?.let { history ->
                val resumeIndex = selection.detail.pages.indexOfFirst { page -> page.cid == history.cid }
                if (resumeIndex >= 0) {
                    TextButton(
                        onClick = { onResumePage(resumeIndex, history) },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("从上次 P${selection.detail.pages[resumeIndex].page} 继续")
                    }
                }
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
                                buildString {
                                    append(formatDuration(page.durationSeconds))
                                    if (lastPlayed?.cid == page.cid) append(" · 上次播放")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onAddPage(index) }, enabled = !loading) {
                            Icon(
                                Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = "将 P${page.page} 加入歌单",
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
    onOpenQueue: () -> Unit,
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
            .height(88.dp)
            // long: 迷你播放器跨越内容区与底部导航，使用较高层级的语义表面色，让亮暗主题都能保持清晰但克制的区域分隔。
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 8.dp, vertical = 2.dp),
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
                            .size(44.dp)
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
                            .size(44.dp)
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
            IconButton(onClick = onOpenQueue, enabled = snapshot.queueItems.isNotEmpty()) {
                BadgedBox(
                    badge = {
                        Badge {
                            Text(
                                if (snapshot.queueItems.size > 99) "99+" else snapshot.queueItems.size.toString(),
                            )
                        }
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "播放列表，共 ${snapshot.queueItems.size} 首",
                        modifier = Modifier.size(25.dp),
                    )
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
                .height(36.dp)
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
                height = 36.dp,
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
    player: Player?,
    controllerReady: Boolean,
    mediaModeSwitching: Boolean,
    sleepTimerMode: SleepTimerMode,
    sleepTimerDeadlineEpochMs: Long,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlaybackModeChange: (PlaybackMode) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onPlaybackMediaModeChange: (PlaybackMediaMode) -> Unit,
    onVideoQualityChange: (Int) -> Unit,
    onDownload: () -> Unit,
    onVideoDownload: () -> Unit,
    onSelectQueueItem: (Int) -> Unit,
    onMoveQueueItemNext: (PlaybackQueueItem) -> Unit,
    onMoveQueueItem: (PlaybackQueueItem, Int) -> Unit,
    onReorderQueue: (List<String>) -> Unit,
    onRemoveQueueItem: (PlaybackQueueItem) -> Unit,
    onRemoveQueueItems: (Set<String>) -> Unit,
    onSaveQueue: () -> Unit,
    onClearQueue: () -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onSetSleepTimerAtTrackEnd: () -> Unit,
    onSetSleepTimerAtQueueEnd: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onPrepareLyrics: () -> Unit,
    onSearchLyrics: (String) -> Unit,
    onSelectLyrics: (LyricsSearchResult) -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var showLyricsSearch by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
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
                onMoveQueueItem = onMoveQueueItem,
                onReorderQueue = onReorderQueue,
                onRemoveQueueItem = onRemoveQueueItem,
                onRemoveQueueItems = onRemoveQueueItems,
                onSaveQueue = {
                    showQueue = false
                    onSaveQueue()
                },
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
    if (showSleepTimer) {
        ModalBottomSheet(
            onDismissRequest = { showSleepTimer = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            SleepTimerSheet(
                mode = sleepTimerMode,
                deadlineEpochMs = sleepTimerDeadlineEpochMs,
                onSetMinutes = { minutes ->
                    onSetSleepTimerMinutes(minutes)
                    showSleepTimer = false
                },
                onSetTrackEnd = {
                    onSetSleepTimerAtTrackEnd()
                    showSleepTimer = false
                },
                onSetQueueEnd = {
                    onSetSleepTimerAtQueueEnd()
                    showSleepTimer = false
                },
                onCancel = {
                    onCancelSleepTimer()
                    showSleepTimer = false
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    val isVideoMode = snapshot.mediaMode == PlaybackMediaMode.VIDEO
    val activity = LocalActivity.current
    val exitNowPlaying = {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onBack()
    }
    val switchToAudio = {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onPlaybackMediaModeChange(PlaybackMediaMode.AUDIO)
    }
    val enterFullscreen = {
        // long: 主动全屏只约束到横屏方向；未点击按钮时仍由系统自动旋转决定竖屏小窗或横屏全屏。
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    val exitFullscreen = {
        // long: 从沉浸全屏返回时先切回竖屏，让用户回到同一个播放页的小窗，而不是退出正在播放页面。
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }
    VideoSystemBarsEffect(isVideoMode && isLandscape)
    if (isVideoMode && isLandscape) {
        BackHandler(onBack = exitFullscreen)
        VideoPlaybackScreen(
            snapshot = snapshot,
            player = player,
            controllerReady = controllerReady,
            streamSwitching = mediaModeSwitching,
            snackbarHostState = snackbarHostState,
            onExitFullscreen = exitFullscreen,
            onPrevious = onPrevious,
            onToggle = onToggle,
            onNext = onNext,
            onSeek = onSeek,
            onPlaybackSpeedChange = onPlaybackSpeedChange,
            onSwitchToAudio = switchToAudio,
            onVideoQualityChange = onVideoQualityChange,
            onShowQueue = { showQueue = true },
        )
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("正在播放") },
                navigationIcon = {
                    IconButton(onClick = exitNowPlaying) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showSleepTimer = true }) {
                        Icon(
                            Icons.Rounded.Timer,
                            contentDescription = sleepTimerStatusLabel(sleepTimerMode, sleepTimerDeadlineEpochMs),
                            tint = if (sleepTimerMode == SleepTimerMode.OFF) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                    if (snapshot.bilibiliSource != null) {
                        IconButton(
                            onClick = {
                                if (snapshot.mediaMode == PlaybackMediaMode.VIDEO) {
                                    switchToAudio()
                                } else {
                                    showLyrics = false
                                    showLyricsSearch = false
                                    onPlaybackMediaModeChange(PlaybackMediaMode.VIDEO)
                                }
                            },
                            enabled = controllerReady && !mediaModeSwitching,
                        ) {
                            if (mediaModeSwitching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    if (snapshot.mediaMode == PlaybackMediaMode.VIDEO) {
                                        Icons.Rounded.MusicNote
                                    } else {
                                        Icons.Rounded.SmartDisplay
                                    },
                                    contentDescription = if (snapshot.mediaMode == PlaybackMediaMode.VIDEO) {
                                        "切换到音频播放"
                                    } else {
                                        "切换到视频播放"
                                    },
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    if (!isVideoMode) {
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (isVideoMode) {
            EmbeddedVideoPlayback(
                snapshot = snapshot,
                player = player,
                controllerReady = controllerReady,
                streamSwitching = mediaModeSwitching,
                onPrevious = onPrevious,
                onToggle = onToggle,
                onNext = onNext,
                onSeek = onSeek,
                onPlaybackSpeedChange = onPlaybackSpeedChange,
                onSwitchToAudio = switchToAudio,
                onVideoQualityChange = onVideoQualityChange,
                videoDownloadEnabled = snapshot.videoDownloadRequest != null,
                onVideoDownload = onVideoDownload,
                onShowQueue = { showQueue = true },
                onEnterFullscreen = enterFullscreen,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
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
                onShowQueue = { showQueue = true },
                isLandscape = isLandscape,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

@Composable
private fun SleepTimerSheet(
    mode: SleepTimerMode,
    deadlineEpochMs: Long,
    onSetMinutes: (Int) -> Unit,
    onSetTrackEnd: () -> Unit,
    onSetQueueEnd: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(bottom = 24.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text("睡眠定时", style = MaterialTheme.typography.titleMedium)
                Text(
                    sleepTimerStatusLabel(mode, deadlineEpochMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SleepTimerPolicy.minuteOptions.forEach { minutes ->
            DropdownMenuItem(
                text = { Text("$minutes 分钟后") },
                leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                onClick = { onSetMinutes(minutes) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        DropdownMenuItem(
            text = { Text("当前歌曲结束后") },
            leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
            onClick = onSetTrackEnd,
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenuItem(
            text = { Text("当前队列结束后") },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
            onClick = onSetQueueEnd,
            modifier = Modifier.fillMaxWidth(),
        )
        if (mode != SleepTimerMode.OFF) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DropdownMenuItem(
                text = { Text("取消睡眠定时", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun sleepTimerStatusLabel(mode: SleepTimerMode, deadlineEpochMs: Long): String {
    return when (mode) {
        SleepTimerMode.OFF -> "睡眠定时"
        SleepTimerMode.TRACK_END -> "当前歌曲结束后停止"
        SleepTimerMode.QUEUE_END -> "当前队列结束后停止"
        SleepTimerMode.DEADLINE -> {
            val remainingMinutes =
                (SleepTimerPolicy.remainingMs(deadlineEpochMs, System.currentTimeMillis()) + 59_999L) / 60_000L
            "约 $remainingMinutes 分钟后停止"
        }
    }
}

@Composable
private fun VideoSystemBarsEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val activity = view.context as? ComponentActivity
        val insetsController = activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view)
        }
        if (enabled) {
            // long: 视频画面延伸到物理屏幕边缘；边缘滑动仍可临时唤出系统栏，退出视频模式后立即恢复常规系统栏。
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (enabled) insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
private fun VideoPlaybackScreen(
    snapshot: PlaybackSnapshot,
    player: Player?,
    controllerReady: Boolean,
    streamSwitching: Boolean,
    snackbarHostState: SnackbarHostState,
    onExitFullscreen: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onSwitchToAudio: () -> Unit,
    onVideoQualityChange: (Int) -> Unit,
    onShowQueue: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        VideoPlaybackSurface(
            snapshot = snapshot,
            player = player,
            controllerReady = controllerReady,
            streamSwitching = streamSwitching,
            fullscreen = true,
            onPrevious = onPrevious,
            onToggle = onToggle,
            onNext = onNext,
            onSeek = onSeek,
            onPlaybackSpeedChange = onPlaybackSpeedChange,
            onSwitchToAudio = onSwitchToAudio,
            onVideoQualityChange = onVideoQualityChange,
            onShowQueue = onShowQueue,
            onFullscreenToggle = onExitFullscreen,
            modifier = Modifier.fillMaxSize(),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp),
        )
    }
}

@Composable
private fun EmbeddedVideoPlayback(
    snapshot: PlaybackSnapshot,
    player: Player?,
    controllerReady: Boolean,
    streamSwitching: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onSwitchToAudio: () -> Unit,
    onVideoQualityChange: (Int) -> Unit,
    videoDownloadEnabled: Boolean,
    onVideoDownload: () -> Unit,
    onShowQueue: () -> Unit,
    onEnterFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // long: 竖屏视频直接贴齐屏幕左右边缘，取消外边距和圆角；视频标题和来源信息仍保留正文边距，避免文字贴边影响阅读。
        VideoPlaybackSurface(
            snapshot = snapshot,
            player = player,
            controllerReady = controllerReady,
            streamSwitching = streamSwitching,
            fullscreen = false,
            onPrevious = onPrevious,
            onToggle = onToggle,
            onNext = onNext,
            onSeek = onSeek,
            onPlaybackSpeedChange = onPlaybackSpeedChange,
            onSwitchToAudio = onSwitchToAudio,
            onVideoQualityChange = onVideoQualityChange,
            onShowQueue = onShowQueue,
            onFullscreenToggle = onEnterFullscreen,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = snapshot.pageTitle ?: snapshot.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = snapshot.artist,
                modifier = Modifier.widthIn(max = 132.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "发布时间 · ${formatPublishedDateTime(snapshot.publishedAtEpochSeconds)}",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = onVideoDownload,
                enabled = videoDownloadEnabled,
                modifier = Modifier.size(48.dp),
            ) {
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
        }
    }
}

@Composable
private fun VideoPlaybackSurface(
    snapshot: PlaybackSnapshot,
    player: Player?,
    controllerReady: Boolean,
    streamSwitching: Boolean,
    fullscreen: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onSwitchToAudio: () -> Unit,
    onVideoQualityChange: (Int) -> Unit,
    onShowQueue: () -> Unit,
    onFullscreenToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var controlsVisible by remember(snapshot.mediaId, fullscreen) { mutableStateOf(true) }
    var isDragging by remember(snapshot.mediaId) { mutableStateOf(false) }
    var isSwipeSeeking by remember(snapshot.mediaId) { mutableStateOf(false) }
    var dragFraction by remember(snapshot.mediaId) { mutableFloatStateOf(0f) }
    var swipeStartPositionMs by remember(snapshot.mediaId) { mutableLongStateOf(0L) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    val progress = PlaybackProgressPolicy.normalize(
        positionMs = snapshot.positionMs,
        durationMs = snapshot.durationMs,
        bufferedPositionMs = snapshot.bufferedPositionMs,
        isSeekable = snapshot.isSeekable,
    )
    val isProgressAdjusting = isDragging || isSwipeSeeking
    val sliderValue = if (isProgressAdjusting) dragFraction else progress.fraction
    val displayedPositionMs = if (isProgressAdjusting) {
        PlaybackProgressPolicy.seekPositionMs(dragFraction, progress.durationMs)
    } else {
        progress.positionMs
    }
    LaunchedEffect(
        controlsVisible,
        snapshot.isPlaying,
        isProgressAdjusting,
        showSpeedMenu,
        showQualityMenu,
        snapshot.mediaId,
        fullscreen,
    ) {
        if (controlsVisible && snapshot.isPlaying && !isProgressAdjusting && !showSpeedMenu && !showQualityMenu) {
            delay(VIDEO_CONTROLS_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .background(Color.Black),
    ) {
        if (player == null) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            NowPlayingVideo(player = player, modifier = Modifier.fillMaxSize())
        }

        VideoPlaybackGestureLayer(
            swipeSeekEnabled = controllerReady && progress.isSeekable,
            bottomControlHeight = if (fullscreen) 128.dp else 64.dp,
            onTap = { controlsVisible = !controlsVisible },
            onSwipeSeekStart = {
                val gestureStartPositionMs = progress.positionMs
                controlsVisible = true
                isSwipeSeeking = true
                dragFraction = progress.fraction
                swipeStartPositionMs = gestureStartPositionMs
                gestureStartPositionMs
            },
            onSwipeSeekDrag = { gestureStartPositionMs, dragDistancePx, surfaceWidthPx ->
                VideoSwipeSeekPolicy.targetPositionMs(
                    startPositionMs = gestureStartPositionMs,
                    durationMs = progress.durationMs,
                    isSeekable = progress.isSeekable,
                    dragDistancePx = dragDistancePx,
                    surfaceWidthPx = surfaceWidthPx,
                )?.let { targetPositionMs ->
                    dragFraction = targetPositionMs.toFloat() / progress.durationMs.toFloat()
                    targetPositionMs
                }
            },
            onSwipeSeekFinished = { targetPositionMs ->
                targetPositionMs?.let(onSeek)
                isSwipeSeeking = false
            },
            onSwipeSeekCancelled = { isSwipeSeeking = false },
            modifier = Modifier.fillMaxSize(),
        )

        if (isSwipeSeeking) {
            VideoSwipeSeekPreview(
                startPositionMs = swipeStartPositionMs,
                targetPositionMs = displayedPositionMs,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (controlsVisible) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.62f),
                contentColor = Color.White,
            ) {
                Row(
                    modifier = Modifier
                        .then(
                            if (fullscreen) Modifier.windowInsetsPadding(WindowInsets.displayCutout) else Modifier,
                        )
                        .heightIn(min = if (fullscreen) 60.dp else 48.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (fullscreen) {
                        IconButton(onClick = onFullscreenToggle) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回竖屏小窗")
                        }
                    }
                    Text(
                        text = snapshot.pageTitle ?: snapshot.title,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = if (fullscreen) 0.dp else 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = if (fullscreen) {
                            MaterialTheme.typography.bodyMedium
                        } else {
                            MaterialTheme.typography.bodySmall
                        },
                        color = Color.White,
                    )
                    IconButton(
                        onClick = onSwitchToAudio,
                        enabled = controllerReady && !streamSwitching,
                    ) {
                        if (streamSwitching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Rounded.MusicNote, contentDescription = "切换到音频播放")
                        }
                    }
                    IconButton(onClick = onFullscreenToggle) {
                        Icon(
                            if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                            contentDescription = if (fullscreen) "退出全屏" else "进入全屏",
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (fullscreen) 24.dp else 8.dp),
            ) {
                val sideButtonSize = if (fullscreen) 64.dp else 48.dp
                val sideIconSize = if (fullscreen) 42.dp else 30.dp
                val playButtonSize = if (fullscreen) 68.dp else 52.dp
                val playIconSize = if (fullscreen) 38.dp else 30.dp
                IconButton(
                    onClick = onPrevious,
                    enabled = controllerReady && snapshot.hasPrevious,
                    modifier = Modifier.size(sideButtonSize),
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "上一个视频",
                        modifier = Modifier.size(sideIconSize),
                        tint = Color.White,
                    )
                }
                FilledIconButton(
                    onClick = onToggle,
                    enabled = controllerReady,
                    modifier = Modifier.size(playButtonSize),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.9f),
                        contentColor = Color.Black,
                    ),
                ) {
                    Icon(
                        if (snapshot.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (snapshot.isPlaying) "暂停视频" else "播放视频",
                        modifier = Modifier.size(playIconSize),
                    )
                }
                IconButton(
                    onClick = onNext,
                    enabled = controllerReady && snapshot.hasNext,
                    modifier = Modifier.size(sideButtonSize),
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "下一个视频",
                        modifier = Modifier.size(sideIconSize),
                        tint = Color.White,
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.68f),
                contentColor = Color.White,
            ) {
                if (fullscreen) {
                    FullscreenVideoBottomControls(
                        snapshot = snapshot,
                        progress = progress,
                        sliderValue = sliderValue,
                        displayedPositionMs = displayedPositionMs,
                        controllerReady = controllerReady,
                        streamSwitching = streamSwitching,
                        showSpeedMenu = showSpeedMenu,
                        showQualityMenu = showQualityMenu,
                        onShowSpeedMenuChange = { showSpeedMenu = it },
                        onShowQualityMenuChange = { showQualityMenu = it },
                        onPlaybackSpeedChange = onPlaybackSpeedChange,
                        onVideoQualityChange = onVideoQualityChange,
                        onShowQueue = onShowQueue,
                        onValueChange = { value ->
                            controlsVisible = true
                            isDragging = true
                            dragFraction = value
                        },
                        onValueChangeFinished = { value ->
                            onSeek(PlaybackProgressPolicy.seekPositionMs(value, progress.durationMs))
                            isDragging = false
                        },
                    )
                } else {
                    // long: 小窗底栏把进度和工具合并为单行，最大 52dp，约为手机 16:9 播放器高度的四分之一；按钮仍保留 48dp 触控区。
                    EmbeddedVideoBottomControls(
                        snapshot = snapshot,
                        progress = progress,
                        sliderValue = sliderValue,
                        displayedPositionMs = displayedPositionMs,
                        controllerReady = controllerReady,
                        streamSwitching = streamSwitching,
                        toolbarHeight = (maxHeight * 0.25f).coerceAtMost(52.dp),
                        showSpeedMenu = showSpeedMenu,
                        showQualityMenu = showQualityMenu,
                        onShowSpeedMenuChange = { showSpeedMenu = it },
                        onShowQualityMenuChange = { showQualityMenu = it },
                        onPlaybackSpeedChange = onPlaybackSpeedChange,
                        onVideoQualityChange = onVideoQualityChange,
                        onShowQueue = onShowQueue,
                        onValueChange = { value ->
                            controlsVisible = true
                            isDragging = true
                            dragFraction = value
                        },
                        onValueChangeFinished = { value ->
                            onSeek(PlaybackProgressPolicy.seekPositionMs(value, progress.durationMs))
                            isDragging = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FullscreenVideoBottomControls(
    snapshot: PlaybackSnapshot,
    progress: PlaybackProgress,
    sliderValue: Float,
    displayedPositionMs: Long,
    controllerReady: Boolean,
    streamSwitching: Boolean,
    showSpeedMenu: Boolean,
    showQualityMenu: Boolean,
    onShowSpeedMenuChange: (Boolean) -> Unit,
    onShowQualityMenuChange: (Boolean) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onVideoQualityChange: (Int) -> Unit,
    onShowQueue: () -> Unit,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        BiuPlaybackSlider(
            value = sliderValue,
            bufferedValue = progress.bufferedFraction,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = controllerReady && progress.isSeekable,
            dragSensitivity = VIDEO_PROGRESS_DRAG_SENSITIVITY,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${formatDurationMs(displayedPositionMs)} / ${formatDurationMs(progress.durationMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.82f),
            )
            Spacer(Modifier.weight(1f))
            VideoSpeedMenuButton(
                playbackSpeed = snapshot.playbackSpeed,
                controllerReady = controllerReady,
                expanded = showSpeedMenu,
                onExpandedChange = onShowSpeedMenuChange,
                onPlaybackSpeedChange = onPlaybackSpeedChange,
            )
            IconButton(onClick = onShowQueue) {
                Icon(
                    Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "打开播放列表",
                    tint = Color.White,
                )
            }
            VideoQualityMenuButton(
                snapshot = snapshot,
                controllerReady = controllerReady,
                streamSwitching = streamSwitching,
                expanded = showQualityMenu,
                onExpandedChange = onShowQualityMenuChange,
                onVideoQualityChange = onVideoQualityChange,
            )
        }
    }
}

@Composable
private fun EmbeddedVideoBottomControls(
    snapshot: PlaybackSnapshot,
    progress: PlaybackProgress,
    sliderValue: Float,
    displayedPositionMs: Long,
    controllerReady: Boolean,
    streamSwitching: Boolean,
    toolbarHeight: androidx.compose.ui.unit.Dp,
    showSpeedMenu: Boolean,
    showQualityMenu: Boolean,
    onShowSpeedMenuChange: (Boolean) -> Unit,
    onShowQualityMenuChange: (Boolean) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onVideoQualityChange: (Int) -> Unit,
    onShowQueue: () -> Unit,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(toolbarHeight)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${formatDurationMs(displayedPositionMs)} / ${formatDurationMs(progress.durationMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.82f),
        )
        BiuPlaybackSlider(
            value = sliderValue,
            bufferedValue = progress.bufferedFraction,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = controllerReady && progress.isSeekable,
            dragSensitivity = VIDEO_PROGRESS_DRAG_SENSITIVITY,
            modifier = Modifier.weight(1f),
        )
        VideoSpeedMenuButton(
            playbackSpeed = snapshot.playbackSpeed,
            controllerReady = controllerReady,
            expanded = showSpeedMenu,
            onExpandedChange = onShowSpeedMenuChange,
            onPlaybackSpeedChange = onPlaybackSpeedChange,
            compact = true,
        )
        IconButton(onClick = onShowQueue, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = "打开播放列表",
                modifier = Modifier.size(22.dp),
                tint = Color.White,
            )
        }
        VideoQualityMenuButton(
            snapshot = snapshot,
            controllerReady = controllerReady,
            streamSwitching = streamSwitching,
            expanded = showQualityMenu,
            onExpandedChange = onShowQualityMenuChange,
            onVideoQualityChange = onVideoQualityChange,
            compact = true,
        )
    }
}

@Composable
private fun VideoSpeedMenuButton(
    playbackSpeed: Float,
    controllerReady: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    compact: Boolean = false,
) {
    Box {
        TextButton(
            onClick = { onExpandedChange(true) },
            enabled = controllerReady,
            modifier = if (compact) Modifier.height(48.dp) else Modifier,
            contentPadding = if (compact) PaddingValues(horizontal = 4.dp) else PaddingValues(horizontal = 12.dp),
        ) {
            Text(
                formatPlaybackSpeed(playbackSpeed),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            PlaybackSpeedPolicy.options.forEach { speed ->
                DropdownMenuItem(
                    text = { Text(formatPlaybackSpeed(speed)) },
                    onClick = {
                        onExpandedChange(false)
                        onPlaybackSpeedChange(speed)
                    },
                    leadingIcon = if (speed == playbackSpeed) {
                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun VideoQualityMenuButton(
    snapshot: PlaybackSnapshot,
    controllerReady: Boolean,
    streamSwitching: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onVideoQualityChange: (Int) -> Unit,
    compact: Boolean = false,
) {
    Box {
        TextButton(
            onClick = { onExpandedChange(true) },
            enabled = controllerReady && !streamSwitching && snapshot.videoQualities.isNotEmpty(),
            modifier = if (compact) Modifier.height(48.dp).widthIn(max = 86.dp) else Modifier,
            contentPadding = if (compact) PaddingValues(horizontal = 4.dp) else PaddingValues(horizontal = 12.dp),
        ) {
            Text(
                VideoQualityLabelPolicy.resolutionLabel(snapshot.quality),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            snapshot.videoQualities.forEach { quality ->
                DropdownMenuItem(
                    text = { Text(VideoQualityLabelPolicy.resolutionLabel(quality.label)) },
                    onClick = {
                        onExpandedChange(false)
                        onVideoQualityChange(quality.qualityId)
                    },
                    leadingIcon = if (quality.qualityId == snapshot.selectedVideoQualityId) {
                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
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
            onDownload = onDownload,
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

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun NowPlayingVideo(player: Player, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .background(Color.Black)
            .semantics { contentDescription = "当前视频画面" },
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                keepScreenOn = true
                this.player = player
            }
        },
        update = { view ->
            if (view.player !== player) view.player = player
        },
        onRelease = { view ->
            view.keepScreenOn = false
            view.player = null
        },
    )
}

@Composable
private fun VideoPlaybackGestureLayer(
    swipeSeekEnabled: Boolean,
    bottomControlHeight: androidx.compose.ui.unit.Dp,
    onTap: () -> Unit,
    onSwipeSeekStart: () -> Long,
    onSwipeSeekDrag: (
        startPositionMs: Long,
        dragDistancePx: Float,
        surfaceWidthPx: Float,
    ) -> Long?,
    onSwipeSeekFinished: (targetPositionMs: Long?) -> Unit,
    onSwipeSeekCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnTap = rememberUpdatedState(onTap)
    val latestOnSwipeSeekStart = rememberUpdatedState(onSwipeSeekStart)
    val latestOnSwipeSeekDrag = rememberUpdatedState(onSwipeSeekDrag)
    val latestOnSwipeSeekFinished = rememberUpdatedState(onSwipeSeekFinished)
    val latestOnSwipeSeekCancelled = rememberUpdatedState(onSwipeSeekCancelled)
    Box(
        modifier = modifier.pointerInput(swipeSeekEnabled, bottomControlHeight) {
            val bottomControlHeightPx = bottomControlHeight.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (!VideoPlaybackTouchPolicy.shouldHandleSurfaceSwipe(
                        touchY = down.position.y,
                        surfaceHeight = size.height.toFloat(),
                        bottomControlHeight = bottomControlHeightPx,
                    )
                ) {
                    return@awaitEachGesture
                }
                val startPosition = down.position
                var gestureStartPositionMs = 0L
                var targetPositionMs: Long? = null
                var swipeStarted = false
                var isPressed = true
                try {
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        isPressed = change.pressed
                        val dragDistanceX = change.position.x - startPosition.x
                        val dragDistanceY = change.position.y - startPosition.y
                        val movementExceedsSlop = abs(dragDistanceX) > viewConfiguration.touchSlop ||
                            abs(dragDistanceY) > viewConfiguration.touchSlop
                        if (
                            !swipeStarted &&
                            swipeSeekEnabled &&
                            abs(dragDistanceX) > viewConfiguration.touchSlop &&
                            abs(dragDistanceX) > abs(dragDistanceY)
                        ) {
                            swipeStarted = true
                            // long: 起点属于当前这次手势，必须在协程内锁定；Compose 状态更新晚于同一触摸事件，不能作为 seek 计算来源。
                            gestureStartPositionMs = latestOnSwipeSeekStart.value()
                        }
                        if (swipeStarted) {
                            // long: 手势层位于 PlayerView 之上、控件之下，先消费水平滑动以免原生播放器抢走事件，按钮仍保留正常点击。
                            targetPositionMs = latestOnSwipeSeekDrag.value(
                                gestureStartPositionMs,
                                dragDistanceX,
                                size.width.toFloat(),
                            )
                            change.consume()
                        }
                        if (!change.pressed) {
                            if (swipeStarted) {
                                swipeStarted = false
                                // long: 松手时直接提交本次手势最后计算的目标，避免读取尚未完成重组的预览状态。
                                latestOnSwipeSeekFinished.value(targetPositionMs)
                            } else if (!movementExceedsSlop) {
                                latestOnTap.value()
                            }
                        }
                    } while (isPressed)
                } finally {
                    if (swipeStarted) latestOnSwipeSeekCancelled.value()
                }
            }
        },
    )
}

@Composable
private fun VideoSwipeSeekPreview(
    startPositionMs: Long,
    targetPositionMs: Long,
    modifier: Modifier = Modifier,
) {
    val offsetMs = targetPositionMs - startPositionMs
    val direction = when {
        offsetMs > 0L -> "快进"
        offsetMs < 0L -> "快退"
        else -> "定位"
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.78f),
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(direction, style = MaterialTheme.typography.labelMedium)
            Text(
                formatDurationMs(targetPositionMs),
                style = MaterialTheme.typography.titleMedium,
            )
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
    onDownload: () -> Unit,
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
    onMoveQueueItem: (PlaybackQueueItem, Int) -> Unit,
    onReorderQueue: (List<String>) -> Unit,
    onRemoveQueueItem: (PlaybackQueueItem) -> Unit,
    onRemoveQueueItems: (Set<String>) -> Unit,
    onSaveQueue: () -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val queueIds = snapshot.queueItems.map(PlaybackQueueItem::mediaId)
    val currentMediaId = snapshot.queueItems.getOrNull(snapshot.currentIndex)?.mediaId
    var draftItems by remember(queueIds) { mutableStateOf(snapshot.queueItems) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmBatchRemoval by remember { mutableStateOf(false) }
    var draggingMediaId by remember { mutableStateOf<String?>(null) }
    val dragStepPx = with(LocalDensity.current) { 60.dp.toPx() }
    val currentOnReorderQueue by rememberUpdatedState(onReorderQueue)
    LaunchedEffect(queueIds) {
        selectedIds = selectedIds.intersect(queueIds.toSet())
    }
    if (confirmBatchRemoval) {
        AlertDialog(
            onDismissRequest = { confirmBatchRemoval = false },
            title = { Text("移出所选歌曲？") },
            text = { Text("将从当前播放列表移除 ${selectedIds.size} 首，已下载文件和本地歌单不会受影响。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmBatchRemoval = false
                        onRemoveQueueItems(selectedIds)
                        selectedIds = emptySet()
                        selectionMode = false
                    },
                ) { Text("移除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmBatchRemoval = false }) { Text("取消") }
            },
        )
    }
    Column(modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                if (selectionMode) "已选 ${selectedIds.size} / ${snapshot.queueItems.size}" else "播放列表 · ${snapshot.queueItems.size}",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.weight(1f))
            if (selectionMode) {
                IconButton(
                    onClick = {
                        selectedIds = if (selectedIds.size == queueIds.size) emptySet() else queueIds.toSet()
                    },
                    enabled = queueIds.isNotEmpty(),
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = if (selectedIds.size == queueIds.size) "取消全选" else "全选",
                    )
                }
                IconButton(onClick = { confirmBatchRemoval = true }, enabled = selectedIds.isNotEmpty()) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "移出所选歌曲")
                }
                IconButton(
                    onClick = {
                        selectionMode = false
                        selectedIds = emptySet()
                    },
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "退出批量编辑")
                }
            } else {
                IconButton(onClick = onSaveQueue, enabled = snapshot.queueItems.isNotEmpty()) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "保存为本地歌单")
                }
                IconButton(
                    onClick = { selectionMode = true },
                    enabled = snapshot.queueItems.isNotEmpty(),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "批量编辑播放列表")
                }
                IconButton(onClick = onClearQueue, enabled = snapshot.queueItems.isNotEmpty()) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "清空播放列表")
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(draftItems, key = PlaybackQueueItem::mediaId) { item ->
                val isCurrent = item.mediaId == currentMediaId
                val isSelected = item.mediaId in selectedIds
                val isDragging = item.mediaId == draggingMediaId
                var itemMenuExpanded by remember(item.mediaId) { mutableStateOf(false) }
                var dragDistancePx by remember(item.mediaId) { mutableFloatStateOf(0f) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                isDragging -> MaterialTheme.colorScheme.secondaryContainer
                                isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                isSelected -> MaterialTheme.colorScheme.surfaceContainerHigh
                                else -> Color.Transparent
                            },
                        )
                        .clickable {
                            if (selectionMode) {
                                selectedIds = if (isSelected) selectedIds - item.mediaId else selectedIds + item.mediaId
                            } else {
                                onSelectQueueItem(item.index)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (selectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) selectedIds + item.mediaId else selectedIds - item.mediaId
                            },
                        )
                    }
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
                    if (!selectionMode) {
                        Icon(
                            Icons.Rounded.DragHandle,
                            contentDescription = "按住拖动${item.pageTitle ?: item.title}",
                            modifier = Modifier
                                .size(48.dp)
                                .pointerInput(item.mediaId) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingMediaId = item.mediaId
                                            dragDistancePx = 0f
                                        },
                                        onDragCancel = {
                                            draggingMediaId = null
                                            dragDistancePx = 0f
                                            draftItems = snapshot.queueItems
                                        },
                                        onDragEnd = {
                                            draggingMediaId = null
                                            dragDistancePx = 0f
                                            val reorderedIds = draftItems.map(PlaybackQueueItem::mediaId)
                                            if (reorderedIds != queueIds) currentOnReorderQueue(reorderedIds)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragDistancePx += dragAmount.y
                                            while (abs(dragDistancePx) >= dragStepPx) {
                                                val currentIndex = draftItems.indexOfFirst { it.mediaId == item.mediaId }
                                                if (currentIndex < 0) break
                                                val direction = if (dragDistancePx > 0f) 1 else -1
                                                val targetIndex = (currentIndex + direction).coerceIn(draftItems.indices)
                                                if (targetIndex == currentIndex) {
                                                    dragDistancePx = 0f
                                                    break
                                                }
                                                draftItems = moveQueueDraftItem(draftItems, item.mediaId, targetIndex)
                                                dragDistancePx -= direction * dragStepPx
                                            }
                                        },
                                    )
                                },
                        )
                        Box {
                            IconButton(onClick = { itemMenuExpanded = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "编辑${item.pageTitle ?: item.title}")
                            }
                            DropdownMenu(
                                expanded = itemMenuExpanded,
                                onDismissRequest = { itemMenuExpanded = false },
                            ) {
                                if (!isCurrent) {
                                    DropdownMenuItem(
                                        text = { Text("设为下一首") },
                                        onClick = {
                                            itemMenuExpanded = false
                                            onMoveQueueItemNext(item)
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("上移") },
                                    enabled = item.index > 0,
                                    onClick = {
                                        itemMenuExpanded = false
                                        onMoveQueueItem(item, item.index - 1)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("下移") },
                                    enabled = item.index < draftItems.lastIndex,
                                    onClick = {
                                        itemMenuExpanded = false
                                        onMoveQueueItem(item, item.index + 1)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("移出播放列表") },
                                    onClick = {
                                        itemMenuExpanded = false
                                        onRemoveQueueItem(item)
                                    },
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

private fun moveQueueDraftItem(
    items: List<PlaybackQueueItem>,
    mediaId: String,
    targetIndex: Int,
): List<PlaybackQueueItem> {
    val sourceIndex = items.indexOfFirst { item -> item.mediaId == mediaId }
    if (sourceIndex < 0) return items
    val boundedTarget = targetIndex.coerceIn(items.indices)
    if (sourceIndex == boundedTarget) return items
    return items.toMutableList()
        .apply { add(boundedTarget, removeAt(sourceIndex)) }
        .mapIndexed { index, item -> item.copy(index = index) }
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
    onPlayAudio: (AudioDownloadTaskEntity) -> Unit,
    onPlayVideo: (VideoDownloadTaskEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        // long: 下载任务已经位于账号页独立 Tab，不重复显示页面标题，把纵向空间留给任务状态和控制操作。
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
                onPlay = onPlayAudio,
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
                onPlay = onPlayVideo,
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
    onPlay: (AudioDownloadTaskEntity) -> Unit,
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
                            -> Unit
                            AudioDownloadStatus.COMPLETED -> IconButton(onClick = { onPlay(task) }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "播放已下载音频")
                            }
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
    onPlay: (VideoDownloadTaskEntity) -> Unit,
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
                            VideoDownloadStatus.COMPLETED -> IconButton(onClick = { onPlay(task) }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "播放已下载视频")
                            }
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
    dragSensitivity: Float = 1f,
    height: androidx.compose.ui.unit.Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val normalizedValue = value.coerceIn(0f, 1f)
    val latestValue = rememberUpdatedState(normalizedValue)
    val latestOnValueChange = rememberUpdatedState(onValueChange)
    val latestOnValueChangeFinished = rememberUpdatedState(onValueChangeFinished)
    val normalizedBuffered = bufferedValue.coerceIn(normalizedValue, 1f)
    val activeColor = MaterialTheme.colorScheme.primary
    val bufferedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val inactiveColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    Canvas(
        modifier = modifier
            .height(height)
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
            .pointerInput(enabled, dragSensitivity) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val trackStartPx = 5.dp.toPx()
                    val trackWidthPx = (size.width.toFloat() - trackStartPx * 2f).coerceAtLeast(0f)
                    val startFraction = if (trackWidthPx > 0f) {
                        ((down.position.x - trackStartPx) / trackWidthPx).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    var finalValue = startFraction
                    var dragAnchorFraction = startFraction
                    var dragStarted = false
                    var valueCommitted = false
                    var isPressed = true
                    try {
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            isPressed = change.pressed
                            val dragDistanceX = change.position.x - down.position.x
                            val dragDistanceY = change.position.y - down.position.y
                            val horizontalDrag = abs(dragDistanceX) > viewConfiguration.touchSlop &&
                                abs(dragDistanceX) > abs(dragDistanceY)
                            if (!dragStarted && horizontalDrag) {
                                // long: 只有横向位移越过系统触控阈值才进入拖动，按下时的轻微抖动不会改变播放位置。
                                dragStarted = true
                                dragAnchorFraction = PlaybackSliderDragPolicy.dragAnchorFraction(
                                    currentFraction = latestValue.value,
                                    touchFraction = startFraction,
                                    isDragging = true,
                                )
                                latestOnValueChange.value(dragAnchorFraction)
                            }
                            if (dragStarted) {
                                finalValue = PlaybackSliderDragPolicy.adjustedFraction(
                                    startFraction = dragAnchorFraction,
                                    dragDistancePx = dragDistanceX,
                                    trackWidthPx = trackWidthPx,
                                    sensitivity = dragSensitivity,
                                )
                                latestOnValueChange.value(finalValue)
                                change.consume()
                            }
                            if (!change.pressed) {
                                if (dragStarted) {
                                    latestOnValueChangeFinished.value(finalValue)
                                    valueCommitted = true
                                } else if (
                                    abs(dragDistanceX) <= viewConfiguration.touchSlop &&
                                    abs(dragDistanceY) <= viewConfiguration.touchSlop
                                ) {
                                    // long: 短按仍保留直接定位能力，精细倍率只影响连续拖动，不牺牲快速跳转效率。
                                    latestOnValueChange.value(startFraction)
                                    latestOnValueChangeFinished.value(startFraction)
                                    valueCommitted = true
                                }
                            }
                        } while (isPressed)
                    } finally {
                        // long: 系统手势或窗口切换取消触控时也结束拖动，避免界面持续显示已取消的预览位置。
                        if (dragStarted && !valueCommitted) latestOnValueChangeFinished.value(finalValue)
                    }
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
    return "发布时间 · ${formatPublishedDateTime(epochSeconds)}"
}

private fun formatPublishedDateTime(epochSeconds: Long?): String {
    val instant = epochSeconds?.takeIf { it > 0L }?.let(Instant::ofEpochSecond)
    return instant?.let(PUBLISHED_AT_FORMATTER::format) ?: "未知"
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

private fun MediaController.moveQueueItem(mediaId: String, targetIndex: Int): Boolean {
    val sourceIndex = (0 until mediaItemCount).firstOrNull { getMediaItemAt(it).mediaId == mediaId } ?: return false
    val boundedTarget = targetIndex.coerceIn(0, mediaItemCount - 1)
    if (sourceIndex == boundedTarget) return false
    // long: 直接使用 Media3 原生 move 保留当前媒体实例与播放位置，避免重建整条队列造成声音中断。
    moveMediaItem(sourceIndex, boundedTarget)
    return true
}

private fun MediaController.reorderQueue(mediaIds: List<String>): Boolean {
    val requestedIds = mediaIds.distinct()
    val currentIds = (0 until mediaItemCount).map { index -> getMediaItemAt(index).mediaId }
    if (requestedIds.size != currentIds.size || requestedIds.toSet() != currentIds.toSet()) return false
    requestedIds.forEachIndexed { targetIndex, mediaId ->
        val sourceIndex = (targetIndex until mediaItemCount)
            .firstOrNull { index -> getMediaItemAt(index).mediaId == mediaId }
            ?: return false
        if (sourceIndex != targetIndex) moveMediaItem(sourceIndex, targetIndex)
    }
    return true
}

private fun MediaController.removeQueueItems(mediaIds: Set<String>): Boolean {
    if (mediaIds.isEmpty()) return false
    val indexes = (0 until mediaItemCount)
        .filter { index -> getMediaItemAt(index).mediaId in mediaIds }
        .sortedDescending()
    if (indexes.isEmpty()) return false
    // long: 从末尾删除可避免前方索引收缩；Media3 会在当前项被移除时选择后继，并继续由服务保存新锚点。
    indexes.forEach(::removeMediaItem)
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
private const val VIDEO_CONTROLS_HIDE_DELAY_MS = 3_500L
private const val VIDEO_PROGRESS_DRAG_SENSITIVITY = 0.5f

private val PUBLISHED_AT_FORMATTER: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())
