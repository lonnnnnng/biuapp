# Biu Android

Biu Android 是桌面版 Biu 的原生 Android 重写项目。目标不是把 Electron 页面塞进 WebView，而是保留 Bilibili 音乐化体验，并使用 Android 原生的后台播放、媒体通知、存储和下载能力。

## 当前进度

- [x] Kotlin + Jetpack Compose 工程
- [x] Media3 `MediaSessionService` 后台播放
- [x] 播放队列主界面和底部迷你播放栏
- [x] Bilibili 请求头隔离器
- [x] WBI 参数签名及单元测试
- [x] M1 示例音轨播放闭环
- [ ] Bilibili 登录与 Cookie 持久化
- [ ] 推荐、搜索、收藏夹和历史接口
- [ ] 播放地址解析与过期刷新
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
- 已在 Redmi Note 8 Pro 验证：安装与冷启动、示例音轨播放、切到后台继续播放、媒体通知、系统媒体暂停/恢复控制。
- 待后续实现：进程重启后的队列和进度恢复；该能力依赖 M3 的 Room 持久化。

## 文档

- [技术方案](docs/technical-solution.md)
- [功能迁移矩阵](docs/feature-migration-matrix.md)
- [开发里程碑](docs/development-roadmap.md)
- [安全与合规边界](docs/security-and-compliance.md)
