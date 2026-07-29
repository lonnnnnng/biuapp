# Biu Android 技术方案

## 1. 目标

使用 Kotlin 和 Jetpack Compose 重写桌面版核心能力：Bilibili 账号内容浏览、DASH 音频播放、后台媒体控制、播放队列、歌词、本地音乐和下载。

首要验收标准是“真实账号可以稳定播放”，而不是先追求桌面版全部页面的一比一复刻。

## 2. 技术基线

| 领域 | 方案 |
| --- | --- |
| UI | Jetpack Compose + Material 3 |
| 播放 | Media3 ExoPlayer + MediaSessionService |
| 网络 | OkHttp；仅 Bilibili 域名注入 Referer、Origin、UA 和 Cookie |
| 状态 | ViewModel + StateFlow |
| 持久化 | Room 保存队列、历史、下载任务；DataStore 保存轻量设置 |
| 密钥 | Android Keystore 加密登录 Cookie 和 refresh token |
| 下载 | 独立前台下载服务；Room 保存状态；MediaStore 发布最终文件 |
| 音视频处理 | 优先 Media3 Muxer/Transformer，确实需要时再评估 FFmpeg |

官方依据：

- Media3 建议将 `Player` 和 `MediaSession` 放进 `MediaSessionService`，以支持锁屏、通知栏、耳机、蓝牙和后台播放。
- Android 16 会让长时间 WorkManager 消耗 JobScheduler 配额。用户主动的大文件下载因此使用专用前台服务，WorkManager 只承担可延迟的恢复检查和元数据同步。
- Android 10 以上通过 MediaStore 的 `IS_PENDING`、`DISPLAY_NAME` 和 `RELATIVE_PATH` 发布下载文件，避免依赖裸文件路径。

## 3. 模块边界

第一阶段保持单 Gradle module，按包划分职责，稳定后再物理拆分：

```text
com.lonnnnnng.biu
├── core/model        领域对象与播放模式
├── data/bilibili     API、Cookie、WBI、响应模型
├── data/local         Room 数据库、DAO 与本地播放历史
├── playback          Media3 Service、Controller、播放状态
├── download          下载队列、前台服务、合并与存储
└── ui                Compose 页面与主题
```

UI 不直接请求 Bilibili，也不持有 `ExoPlayer`。所有播放命令通过 `MediaController` 发送到 `MediaSessionService`，这样 Activity 被回收后音乐仍可继续。

## 4. 播放链路

```text
推荐/搜索条目
  -> 获取视频详情和分 P cid
  -> 解析 DASH 音频 URL
  -> 创建带标题、UP 主、封面的 MediaItem
  -> MediaController 写入队列
  -> PlaybackService 播放
  -> 通知栏、锁屏、蓝牙和应用 UI 共享同一 MediaSession
```

DASH URL 可能过期。播放失败且响应符合链接失效特征时，解析器必须按 `bvid + cid` 重新请求地址，并限制单曲自动重试次数，防止无限循环。

播放结束属于一次性业务事件，不能依赖 `message == "Ended"` 这类展示字符串。状态层使用单调递增事件 ID，确保自动下一首不会重复触发或漏触发。

多 P 视频采用渐进式队列：先解析用户选中的 P 并立即起播，随后前置 P 从近到远插入队首、后置 P 按原顺序追加，最多保持两条解析链路并发。播放命令通过有序 Channel 写入 MediaController；ViewModel 同步维护包含当前 P 和最近进度的完整队列快照，控制器重连时校验媒体 ID 顺序，空队列、缺项或旧队列均从快照恢复。选择新内容会取消旧补齐任务，避免大型队列的迟到结果污染新队列。队列最终保持详情接口的页面顺序，前后媒体键沿队列切换；切换到新 P 时按新的 `cid` 独立登记播放历史。单 P 长视频只提供连续 seek，不推断内部歌曲边界。

播放历史由 `PlaybackService` 在 Media3 真正切入媒体项后写入 Room，播放期间每 5 秒保存一次进度，暂停和结束时立即更新。数据库只保存 `bvid/cid`、标题、作者、封面、音质偏好与进度，不保存会过期的 DASH URL；从历史重播时重新解析地址，距离结尾 30 秒以内的记录从头播放。

## 5. 登录方案

优先级：

1. WebView 登录并同步 `.bilibili.com` Cookie，作为同一手机上最符合直觉的路径。
2. 短信验证码登录，处理 Geetest 风控。
3. 二维码登录仅作为平板、双设备或调试备用方案。

Cookie 只提供给 Bilibili 专用 OkHttpClient。禁止复制桌面版“拦截所有 HTTP/HTTPS 请求并统一改写请求头和 Set-Cookie”的实现。

## 6. 下载方案

下载任务拆成解析、分块下载、校验、合并、发布五个阶段。任务状态写入 Room，进程重启后将运行中状态降级为暂停，等待用户恢复。

音频任务优先直接保存服务端音频流；视频任务下载独立音视频轨后合并。只有 Media3 Muxer 无法覆盖的容器或编码组合才引入 FFmpeg，以控制 APK 体积、ABI 数量和许可证复杂度。

## 7. 质量门槛

- 领域规则必须有 JVM 单元测试。
- 每个 API 响应必须容忍字段缺失和业务错误码。
- 播放地址、Cookie、手机号等敏感内容不得进入普通日志。
- 每个阶段至少验证 `testDebugUnitTest`、`lintDebug`、`assembleDebug`。
- 真实 Bilibili 播放链路完成后必须在 Android 模拟器进行前后台、锁屏、网络切换和进程恢复测试。

## 8. 参考资料

- https://developer.android.com/media/media3/session/background-playback
- https://developer.android.com/jetpack/androidx/releases/media3
- https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running
- https://developer.android.com/training/data-storage/shared/media
- https://developer.android.com/build/releases/gradle-plugin
