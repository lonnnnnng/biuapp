package com.lonnnnnng.biu

import android.app.Application
import android.content.Context
import com.lonnnnnng.biu.data.bilibili.BilibiliCookieStore
import com.lonnnnnng.biu.data.bilibili.BilibiliRepository
import com.lonnnnnng.biu.data.bilibili.BilibiliRequestHeadersInterceptor
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
}

val Context.appContainer: AppContainer
    get() = (applicationContext as BiuApplication).container
