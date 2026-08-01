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
- [x] 收藏夹、在线历史和 Room 本地播放历史
- [x] 单 P / 多 P 播放队列、进度显示与拖动
- [x] 后台播放、锁屏媒体控制和全屏播放页
- [x] 自定义关注 UP 首页与投稿时间线
- [x] GitHub Release 在线检查、系统下载与安装恢复
- [x] MediaStore 本地音乐扫描、权限、队列和后台播放
- [x] Android 10+ SAF 音乐目录筛选、持久授权和进程重启恢复
- [x] Room 音频/视频下载任务持久化
- [x] 前台下载服务、MediaStore 保存和音视频合并
- [x] 收藏夹批量下载、失败任务批量重试和网络约束
- [x] Room 播放队列持久化与进程重启恢复
- [x] 四种播放模式、七档倍速和队列编辑持久化
- [x] 小号、标准、大号应用字号与标准、紧凑媒体列表密度持久化

## 本地构建

```zsh
cd android-app
ANDROID_HOME=/Users/long/Library/Android/sdk \
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
bash ./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

调试 APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`。正式版本与校验文件从 [GitHub Releases](https://github.com/lonnnnnng/biuapp/releases) 下载。

## 验证状态

- 已验证：JVM 单元测试、Android Lint、Debug/Release APK 构建。
- 已在 Redmi Note 8 Pro（`wsvwypiz7xwslvl7`）验证：真实推荐及封面、`Jay Chou` 搜索、推荐与搜索结果 DASH 音频播放、切到后台继续播放、媒体通知、系统媒体暂停/恢复控制。
- 已在同一设备验证顶栏音质菜单可在“最高音质”和“省流量”之间切换；选择对下一次播放生效，并随 MediaItem 传入后台失效刷新链路。
- 已验证 Bilibili H5 手机号登录、账号态与 Cookie 回传、收藏夹、在线历史和本地历史。
- 实机当时没有可用默认网络，联网验收通过临时 ADB reverse HTTP CONNECT 代理完成；验证后已删除设备系统代理并移除端口转发。
- 已在 Pixel_9 模拟器验证：首页关注范围配置、名称搜索、投稿时间倒排、保存后切换“我的关注”，以及重启后配置恢复。
- 已在 Pixel_9 模拟器验证：Android 音频权限、本地 MediaStore 列表、`content://` 播放、后台媒体通知和本地队列切歌。
- 已在 Pixel_9 模拟器验证：SAF 目录选择、所选目录及子目录过滤、DataStore 持久恢复、筛选队列播放和清除筛选。
- 已在 Pixel_9 模拟器验证：收藏夹全量加载、单 P 批量音频任务创建、Room 断点、仅非计费网络等待、解除约束自动恢复和冷启动暂停恢复。
- 已在 Pixel_9 模拟器验证：本地 8 项队列和在线 Bilibili 队列在强制停止后恢复当前曲目、索引与进度，冷启动保持暂停且可继续切歌。
- 已在 Pixel_9 模拟器验证：顺序、列表循环、随机和单曲循环切换，`0.5x` 至 `2.0x` 倍速，设为下一首、移除、清空，以及重启后的模式、倍速和编辑队列恢复。
- 已在 Redmi Note 8 Pro（`wsvwypiz7xwslvl7`）验证：账号菜单“界面显示”可切换小号、标准、大号字号及标准、紧凑媒体列表密度；强制停止后选择恢复，大字号与紧凑列表组合无文字重叠，验收后已恢复默认设置。

## 文档

- [技术方案](docs/technical-solution.md)
- [功能迁移矩阵](docs/feature-migration-matrix.md)
- [开发里程碑](docs/development-roadmap.md)
- [安全与合规边界](docs/security-and-compliance.md)
- [版本记录](CHANGELOG.md)
