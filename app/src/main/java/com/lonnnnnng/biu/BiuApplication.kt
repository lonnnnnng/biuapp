package com.lonnnnnng.biu

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.lonnnnnng.biu.data.bilibili.BilibiliCookieStore
import com.lonnnnnng.biu.data.bilibili.BilibiliRepository
import com.lonnnnnng.biu.data.bilibili.BilibiliRequestHeadersInterceptor
import com.lonnnnnng.biu.data.local.BiuDatabase
import com.lonnnnnng.biu.data.local.PlaybackHistoryRepository
import java.util.concurrent.TimeUnit
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
    val cookieStore = BilibiliCookieStore()
    val bilibiliHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(BilibiliRequestHeadersInterceptor(cookieStore::cookieHeader))
        .build()
    val bilibiliRepository = BilibiliRepository(bilibiliHttpClient)
    private val database = Room.databaseBuilder(
        context.applicationContext,
        BiuDatabase::class.java,
        "biu.db",
    ).build()
    val playbackHistoryRepository = PlaybackHistoryRepository(database.playbackHistoryDao())
}

val Context.appContainer: AppContainer
    get() = (applicationContext as BiuApplication).container
