package com.lonnnnnng.biu

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.lonnnnnng.biu.data.bilibili.BilibiliCookieStore
import com.lonnnnnng.biu.data.bilibili.BilibiliRepository
import com.lonnnnnng.biu.data.bilibili.BilibiliRequestHeadersInterceptor
import com.lonnnnnng.biu.data.local.BiuDatabase
import com.lonnnnnng.biu.data.local.BiuDatabaseMigrations
import com.lonnnnnng.biu.data.local.AudioDownloadRepository
import com.lonnnnnng.biu.data.local.CreatorSelectionRepository
import com.lonnnnnng.biu.data.local.CreatorGroupRepository
import com.lonnnnnng.biu.data.local.DisplayPreferenceRepository
import com.lonnnnnng.biu.data.local.LocalAudioDirectoryRepository
import com.lonnnnnng.biu.data.local.LocalAudioRepository
import com.lonnnnnng.biu.data.local.LyricsCacheRepository
import com.lonnnnnng.biu.data.local.PlaybackHistoryRepository
import com.lonnnnnng.biu.data.local.PlaybackQueueRepository
import com.lonnnnnng.biu.data.local.ThemePreferenceRepository
import com.lonnnnnng.biu.data.local.VideoDownloadRepository
import com.lonnnnnng.biu.data.update.AppUpdateRepository
import com.lonnnnnng.biu.data.lyrics.LrclibRepository
import com.lonnnnnng.biu.download.DownloadNetworkPreferenceRepository
import com.lonnnnnng.biu.playback.PlaybackPreferenceRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import okhttp3.OkHttpClient

class BiuApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val cookieStore = BilibiliCookieStore()
    val bilibiliHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(BilibiliRequestHeadersInterceptor(cookieStore::cookieHeader))
        .build()
    val bilibiliRepository = BilibiliRepository(
        client = bilibiliHttpClient,
        csrfProvider = cookieStore::csrfToken,
    )
    // long: LRCLIB 属于第三方域名，必须使用无 Cookie 拦截器的独立客户端，避免把 Bilibili 登录凭据发送给歌词服务。
    private val lyricsHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    val lrclibRepository = LrclibRepository(lyricsHttpClient)
    // long: GitHub 更新检查必须使用不带 Bilibili Cookie 拦截器的独立客户端，避免账号凭据发往第三方域名。
    private val appUpdateHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    val appUpdateRepository = AppUpdateRepository(appUpdateHttpClient)
    private val database = Room.databaseBuilder(
        context.applicationContext,
        BiuDatabase::class.java,
        "biu.db",
    ).addMigrations(
        BiuDatabaseMigrations.MIGRATION_1_2,
        BiuDatabaseMigrations.MIGRATION_2_3,
        BiuDatabaseMigrations.MIGRATION_3_4,
        BiuDatabaseMigrations.MIGRATION_4_5,
        BiuDatabaseMigrations.MIGRATION_5_6,
        BiuDatabaseMigrations.MIGRATION_6_7,
    ).build()
    val playbackHistoryRepository = PlaybackHistoryRepository(database.playbackHistoryDao())
    val playbackQueueRepository = PlaybackQueueRepository(database.playbackQueueDao())
    val lyricsCacheRepository = LyricsCacheRepository(database.lyricsCacheDao())
    val creatorSelectionRepository = CreatorSelectionRepository(database.creatorSelectionDao())
    val creatorGroupRepository = CreatorGroupRepository(database.creatorGroupDao())
    val audioDownloadRepository = AudioDownloadRepository(database.audioDownloadTaskDao())
    val videoDownloadRepository = VideoDownloadRepository(database.videoDownloadTaskDao())
    val downloadNetworkPreferenceRepository = DownloadNetworkPreferenceRepository(context)
    val playbackPreferenceRepository = PlaybackPreferenceRepository(context)
    val themePreferenceRepository = ThemePreferenceRepository(context)
    val displayPreferenceRepository = DisplayPreferenceRepository(context)
    val localAudioDirectoryRepository = LocalAudioDirectoryRepository(context)
    val localAudioRepository = LocalAudioRepository(context)
    val audioDownloadRecovery: Deferred<Unit> = applicationScope.async {
        audioDownloadRepository.pauseInterruptedTasks()
    }
    val videoDownloadRecovery: Deferred<Unit> = applicationScope.async {
        videoDownloadRepository.pauseInterruptedTasks()
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as BiuApplication).container
