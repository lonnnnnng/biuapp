# Biu Android

Biu Android 是桌面版 Biu 的原生 Android 重写项目。目标不是把 Electron 页面塞进 WebView，而是保留 Bilibili 音乐化体验，并使用 Android 原生的后台播放、媒体通知、存储和下载能力。

## 当前进度

- [x] Kotlin + Jetpack Compose 工程
- [x] Media3 `MediaSessionService` 后台播放
- [x] 播放队列主界面和底部迷你播放栏
- [x] Bilibili 请求头隔离器
- [x] WBI 参数签名及单元测试
- [x] M1 示例音轨播放闭环
- [x] WebView 登录页与 Cookie 同步原型
- [x] 音乐区推荐、音乐榜和 WBI 搜索
- [x] 视频详情、分 P 元数据与 DASH 音频解析
- [x] 最高音质/省流量全局选择
- [x] 播放地址过期刷新与备用 CDN 切换
- [x] M2 真实 Bilibili 播放闭环
- [ ] 收藏夹、稍后再看和历史接口
- [ ] Room 队列、历史和下载任务持久化
- [ ] 前台下载服务、MediaStore 保存和音视频合并

## 本地构建

```zsh
cd android-app
ANDROID_HOME=/Users/long/Library/Android/sdk \
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
bash ./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

调试 APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`。

## 验证状态

- 已验证：JVM 单元测试、Android Lint、Debug APK 构建。
- 已在 Redmi Note 8 Pro（`wsvwypiz7xwslvl7`）验证：真实推荐及封面、`Jay Chou` 搜索、推荐与搜索结果 DASH 音频播放、切到后台继续播放、媒体通知、系统媒体暂停/恢复控制。
- 已在同一设备验证顶栏音质菜单可在“最高音质”和“省流量”之间切换；选择对下一次播放生效，并随 MediaItem 传入后台失效刷新链路。
- 已验证 WebView 可以打开 Bilibili 手机号登录页；本轮未输入用户账号凭据，登录后的账号态与 Cookie 回传保留为人工验收项。
- 实机当时没有可用默认网络，联网验收通过临时 ADB reverse HTTP CONNECT 代理完成；验证后已删除设备系统代理并移除端口转发。
- 待后续实现：进程重启后的队列和进度恢复；该能力依赖 M3 的 Room 持久化。

## 文档

- [技术方案](docs/technical-solution.md)
- [功能迁移矩阵](docs/feature-migration-matrix.md)
- [开发里程碑](docs/development-roadmap.md)
- [安全与合规边界](docs/security-and-compliance.md)
