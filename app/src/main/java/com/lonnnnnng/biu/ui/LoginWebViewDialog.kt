package com.lonnnnnng.biu.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lonnnnnng.biu.appContainer
import com.lonnnnnng.biu.data.bilibili.BilibiliCookieStore
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginWebViewDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val cookieStore = context.appContainer.cookieStore
    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            val loginWebView = this
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(loginWebView, false)
            }
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return true
                    return if (BilibiliLoginNavigation.isAllowed(uri.toString())) {
                        false
                    } else {
                        true
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    // long: 登录页和主站跨域写入同一 .bilibili.com Cookie；每次页面完成后立刻落盘供 OkHttp 使用。
                    cookieStore.flush()
                }
            }
            loadUrl(BilibiliCookieStore.BILIBILI_LOGIN_URL)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            cookieStore.flush()
            webView.stopLoading()
            webView.destroy()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("登录 Bilibili") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭登录页")
                        }
                    },
                )
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

internal object BilibiliLoginNavigation {
    fun isAllowed(url: String): Boolean {
        return runCatching {
            val uri = URI(url)
            if (uri.scheme != "https") return@runCatching false
            val host = uri.host.orEmpty().lowercase()
            host == "bilibili.com" || host.endsWith(".bilibili.com")
        }.getOrDefault(false)
    }
}
