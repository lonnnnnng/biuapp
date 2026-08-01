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

在线内容默认使用音频轨。用户在正在播放页切换视频时，`MediaController` 通过仅向同包可信控制器开放的自定义 SessionCommand 请求 `PlaybackService` 重建当前分 P；服务以 `MergingMediaSource` 合并独立视频轨和标准 AAC 音频轨，并保留队列索引、进度、倍速、循环模式及播放/暂停意图。竖屏 Compose 以 `16:9` 小窗承载同一个 `PlayerView`，横屏根据系统方向切换为沉浸全屏，共用覆盖式控制层；视频模式仅作用于当前分 P，切换媒体项或进程重启后恢复产品默认的音频模式。

视频画质来自同一次 `fnval=4048` DASH 响应中的实际视频轨，不展示仅存在于描述字段但账号无权播放的清晰度。同一清晰度优先 AVC，缺失时依次回退 HEVC、AV1；画质切换继续由 MediaSession 服务按 `bvid + cid + qualityId` 重新解析和替换媒体源。MediaItem 只向 UI 暴露画质 ID 与标签，带签名的音视频 URL 不进入展示状态或普通日志。Redmi Note 8 Pro 的 MTK HEVC 在正常解码和资源切换 flush 时均会使厂商 codec 服务原生崩溃，`c2.android.avc.decoder` 也会在软件颜色转换中崩溃，因此该类设备默认选择最高 720p 的 AVC 厂商硬解轨；用户手动画质选择仍可越过默认上限。

MTK codec 服务死亡后，Media3 可能上报明确的 decoder error，也可能只给出 `ERROR_CODE_UNSPECIFIED`。播放服务保存队列、索引、进度、播放意图、倍速、循环、随机和音量，在 30 秒窗口内最多重建两次 `ExoPlayer`；已有 `MediaSession` 仅通过 `setPlayer` 换绑新 Player，锁屏和现有 Controller 不需要重连。重建位置会限制在已知媒体总时长内，并等待 350ms 让 vendor codec 服务重新拉起后再 `prepare()`，避免损坏实例的 `DEAD_OBJECT` 状态继续污染会话。

播放结束属于一次性业务事件，不能依赖 `message == "Ended"` 这类展示字符串。状态层使用单调递增事件 ID，确保自动下一首不会重复触发或漏触发。

多 P 视频采用渐进式队列：先解析用户选中的 P 并立即起播，随后前置 P 从近到远插入队首、后置 P 按原顺序追加，最多保持两条解析链路并发。播放命令通过有序 Channel 写入 MediaController；ViewModel 同步维护包含当前 P 和最近进度的完整队列快照，控制器重连时校验媒体 ID 顺序，空队列、缺项或旧队列均从快照恢复。每个多 P `Track` 同时把 `P序号 · 分 P 名称` 写入 Media3 `subtitle`，底部播放器以此显示当前 P 并随媒体项切换自动更新；单 P 不设置该字段。选择新内容会取消旧补齐任务，避免大型队列的迟到结果污染新队列。队列最终保持详情接口的页面顺序，前后媒体键沿队列切换；切换到新 P 时按新的 `cid` 独立登记播放历史。单 P 长视频只提供连续 seek，不推断内部歌曲边界。

播放历史由 `PlaybackService` 在 Media3 真正切入媒体项后写入 Room，播放期间每 5 秒保存一次进度，暂停和结束时立即更新。数据库只保存 `bvid/cid`、标题、作者、封面、音质偏好与进度，不保存会过期的 DASH URL；从历史重播时重新解析地址，距离结尾 30 秒以内的记录从头播放。

播放队列由 `PlaybackService` 直接镜像到 Room v5，保存全部 MediaItem、当前索引和当前进度。大型分 P 渐进补齐产生的连续 timeline 变化按 1 秒防抖合并，播放期间每 5 秒以及暂停、seek、切歌时立即保存。队列短期保留当前 URI 以便冷启动立即展示并恢复，本地内容继续使用稳定的 `content://`；Bilibili 内容同时保存 `bvid/cid` 和音质偏好，URI 过期后仍由播放服务重新解析。进程重启只在播放器为空时恢复旧快照，避免覆盖用户刚选择的新内容，并默认保持暂停，禁止冷启动擅自播放。

## 5. 登录方案

优先级：

1. WebView 登录并同步 `.bilibili.com` Cookie，作为同一手机上最符合直觉的路径。
2. 短信验证码登录，处理 Geetest 风控。
3. 二维码登录仅作为平板、双设备或调试备用方案。

Cookie 只提供给 Bilibili 专用 OkHttpClient。禁止复制桌面版“拦截所有 HTTP/HTTPS 请求并统一改写请求头和 Set-Cookie”的实现。

## 6. 下载方案

下载任务拆成解析、分块下载、校验、合并、发布五个阶段。任务状态写入 Room，进程重启后将运行中状态降级为暂停，等待用户恢复。

音频任务优先直接保存服务端音频流；视频任务下载独立音视频轨后合并。只有 Media3 Muxer 无法覆盖的容器或编码组合才引入 FFmpeg，以控制 APK 体积、ABI 数量和许可证复杂度。

M4 首版音频下载只选择 Bilibili 标准 AAC 轨中的最高码率，不直接保存 FLAC 或杜比轨，避免把需要容器转换的流仅改名为 `.m4a`。任务通过独立 `dataSync` 前台服务串行执行，Room 保存 `QUEUED / RESOLVING / DOWNLOADING / PAUSED / PUBLISHING / COMPLETED / FAILED / CANCELLED` 状态、断点字节、音质和最终 URI。

M5 当前 P 视频下载沿用桌面版的 `fnval=4048` 且不额外传 `qn`，在同一次 DASH 响应中选择最高可用 AVC 视频轨；没有 AVC 时依次回退 HEVC、AV1，音频仍选择最高码率标准 AAC。Room v4 使用独立 `video_download_tasks` 保存视频轨、音频轨和合并文件的进度，避免视频状态改动影响已经稳定的音频任务。

音频与视频共用应用级 Range 传输器。临时文件保存在应用私有目录；继续下载时发送 `Range`：`206` 追加，服务端返回 `200` 时覆盖旧文件，`416` 时只允许清空断点一次。视频服务串行下载两条轨，再由 `MediaExtractor` 读取实际音视频轨，使用 `Media3 Mp4Muxer` 按样本时间戳交错写入 MP4，不做解码和转码。当前分 P 的 Media3 标题用于任务名和最终文件名，发布目录为 `Movies/Biu/`。

进程死亡后，解析、双轨下载、合并和发布中的视频任务统一降级为 `PAUSED`，已完成的轨文件保留以便恢复；取消任务会删除视频轨、音频轨和合并临时文件并把 Room 进度归零。音频完成后通过 MediaStore 的 `IS_PENDING` 和 `RELATIVE_PATH=Music/Biu/` 发布，视频完成后使用 `RELATIVE_PATH=Movies/Biu/` 发布，再删除私有临时文件。

MediaProvider 可能在扫描无内嵌标签的 DASH 容器后覆盖 `TITLE/ARTIST`。本地音乐扫描因此以已完成任务的 `publishedUri` 提取 MediaStore ID，用 Room 中的在线标题、作者和封面覆盖系统回退值；播放仍使用 MediaStore `content://` URI。

M5 批量下载严格按收藏夹接口的 `has_more` 继续分页，失效稿件和非普通视频只从候选资源中过滤，不能影响翻页判断。所选资源最多并发 3 路解析视频详情；单 P 生成一个任务，多 P 按每个 `cid` 拆成独立任务。Room 以 400 条为一批查询和写入，跳过已完成、排队中和运行中的任务，暂停或失败任务保留断点后重新排队。音频与视频服务分别维持单任务 FIFO，因此两种类型可独立工作，同类型文件不会并行抢占带宽和存储。

下载网络偏好通过 DataStore 保存。任意网络模式要求默认网络同时具备 `INTERNET` 和 `VALIDATED`；仅 Wi-Fi 模式实际要求 `NOT_METERED`，因此同样允许系统认定为非计费的其他网络。默认网络不满足约束时，前台服务取消当前网络流、将任务降为暂停并立即保留断点重新排队，通知显示等待网络；网络恢复或用户放宽约束后继续 FIFO。任务面板同时提供音频和视频失败任务的一键重新排队。

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
