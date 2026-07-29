package com.lonnnnnng.biu.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.WebChromeClient
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lonnnnnng.biu.appContainer
import com.lonnnnnng.biu.data.bilibili.BilibiliCookieStore
import java.net.URI
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginWebViewDialog(
    onDismiss: () -> Unit,
    onSessionAvailable: () -> Unit,
) {
    val context = LocalContext.current
    val cookieStore = context.appContainer.cookieStore
    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // long: H5 登录页依赖移动 UA 和移动视口适配表单；播放请求仍单独使用桌面 UA，二者不能共用配置。
            settings.allowFileAccess = false
            settings.allowContentAccess = false
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
                    // long: Android System WebView 会把该 H5 页的 100vh 计算为 0，需用实际可视高度保持协议文案位于页面底部。
                    view?.evaluateJavascript(BILIBILI_LOGIN_VIEWPORT_FIX, null)
                    // long: 登录页和主站跨域写入同一 .bilibili.com Cookie；每次页面完成后立刻落盘供 OkHttp 使用。
                    cookieStore.flush()
                }
            }
            loadUrl(BilibiliCookieStore.BILIBILI_LOGIN_URL)
        }
    }

    LaunchedEffect(cookieStore) {
        var checkedSessionVersion: Int? = null
        while (true) {
            val currentSessionVersion = cookieStore.sessionVersion()
            if (currentSessionVersion != null && currentSessionVersion != checkedSessionVersion) {
                // long: H5 通过 XHR 写入登录 Cookie 时不会重新加载页面，只在 Cookie 变化后请求服务端确认登录态。
                checkedSessionVersion = currentSessionVersion
                cookieStore.flush()
                onSessionAvailable()
            }
            delay(LOGIN_SESSION_POLL_INTERVAL_MS)
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

private const val LOGIN_SESSION_POLL_INTERVAL_MS = 750L

internal val BILIBILI_LOGIN_VIEWPORT_FIX =
    """
    (() => {
        if (location.hostname !== "passport.bilibili.com" ||
            !location.pathname.startsWith("/h5-app/passport/login")) return;

        const applyViewportHeight = () => {
            const viewportHeight = Math.round(window.visualViewport?.height || window.innerHeight || 0);
            if (viewportHeight <= 0) return;

            let style = document.getElementById("biu-mobile-login-viewport");
            if (!style) {
                style = document.createElement("style");
                style.id = "biu-mobile-login-viewport";
                document.head.appendChild(style);
            }
            style.textContent =
                "html,body,#app,.login-wrap{min-height:" + viewportHeight + "px!important}";
        };

        applyViewportHeight();
        if (!window.__biuLoginViewportBound) {
            window.__biuLoginViewportBound = true;
            window.addEventListener("resize", applyViewportHeight);
            window.visualViewport?.addEventListener("resize", applyViewportHeight);
        }
    })();
    """.trimIndent()

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
