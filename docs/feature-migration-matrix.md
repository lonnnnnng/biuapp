# 功能迁移矩阵

| 桌面功能 | Android 实现 | 阶段 | 状态 |
| --- | --- | --- | --- |
| Bilibili 登录 | WebView + CookieManager + 专用 OkHttpClient | M2 | 原型完成，账号态待人工验收 |
| 热门推荐、自选 UP 首页、搜索 | Compose 列表 + 热门/每周必看/排行/入站必刷 API + UP 投稿 WBI | M2/M12.1/M12.2/M18 | 空配置展示四个默认来源；配置后支持最新发布/未播放/最近播放，并按全部、本地分组或单个 UP 筛选；下拉刷新、聚合续载和视频搜索已完成 |
| 视频详情、分 P | `view` 详情与 cid 解析；分 P 选择与渐进队列 | M2/M3 | 单 P、多 P 选择、顺序播放和大型队列渐进解析已完成 |
| 收藏夹与视频合集 | 账号 API + 普通收藏夹/视频合集统一模型 | M3/M9 | 浏览、播放、批量下载、收藏夹 CRUD 及视频加入/移出已完成 |
| 在线历史、本地历史 | 账号 API + Room 本地历史 + 统一历史筛选页 | M3/M8/M18 | 统一历史入口支持全部/在线/本地来源筛选、在线分页搜索、时间排序、单条在线删除，以及明确范围的在线/本地/全部清空 |
| 稍后再看 | 保留 API，不提供 Android 页面入口 | - | 产品决策为不迁移入口 |
| 关注、动态、用户空间 | UP 主中心 + 动态主页面 + Room 本地 UP 分组 | M11/M12/M12.2/M15 | 用户搜索、空间投稿、关注切换、本地分组及首页分组聚合、视频动态下拉刷新与游标分页、播放、点赞/取消点赞及确认三连已实现；真实点赞往返已验收，三连真实扣币待授权 |
| DASH 音频播放 | Media3 + 最高可用音质自动降级 + URL 刷新 + 备用 CDN | M2 | 已完成 |
| DASH 视频播放 | Media3 `MergingMediaSource` + SessionCommand + 实际轨画质菜单 | M10.1 | 默认音频、竖屏小窗、横屏沉浸全屏、自动旋转控制和画质切换已实现；真机最终验收进行中 |
| 播放队列 | Media3 playlist + Room 镜像 | M1/M3/M7/M18 | 完整队列、当前索引与进度持久化、进程重启恢复、设为下一首、单项/批量移除、清空、上下移动与长按拖动排序已完成；整条队列可保存为本地歌单 |
| 播放模式与倍速 | Media3 repeat/shuffle/playback parameters + DataStore | M7 | 顺序、列表循环、随机、单曲循环及七档倍速已完成并持久化 |
| 后台播放 | MediaSessionService | M1 | 已完成 |
| 快捷键、任务栏 | 通知栏、锁屏、耳机、蓝牙 MediaSession | M1 | 已完成 |
| 全屏播放器 | Compose 音频 Now Playing + 小窗/沉浸式视频控制层 | M3/M10.1 | 音频封面/歌词、竖屏视频小窗与横屏视频全屏模式已完成 |
| mini 播放器 | 应用底部迷你播放栏；视频场景可选画中画 | M1/M4 | 音频迷你栏已完成 |
| 歌词 | LRCLIB 手动搜索、时间轴展示与 Room 缓存 | M10 | 已完成；单 P/多 P 使用各自歌曲名称 |
| 主题 | Material 亮色、暗色、跟随系统 + 系统栏同步 | M10 | 已完成基础三态切换；自定义强调色和字体待后续 |
| 显示密度与字号 | Preferences DataStore + Compose Typography/CompositionLocal | M13 | 小号、标准、大号应用字号与标准、紧凑媒体列表密度已完成并跨冷启动恢复；推荐与搜索结果支持列表/两列网格，自定义字体待后续 |
| 本地音乐 | MediaStore 查询 + SAF 目录授权（Android 10+） | M4/M18 | 扫描、目录筛选与重启恢复已完成；点击歌曲可明确选择仅播放此曲或播放当前目录，并在操作前显示实际入队数量 |
| 音频下载 | Room 任务 + 前台下载服务 + Range 续传 + MediaStore.Audio | M4 | 标准 AAC 下载、暂停/继续/取消、通知进度与发布已完成 |
| 视频下载 | Room 视频任务 + 双轨 Range 下载 + Media3 Mp4Muxer + MediaStore.Video | M5 | 当前单 P/当前分 P 下载、暂停恢复、取消、合并与 `Movies/Biu/` 发布已完成 |
| 批量下载 | 收藏夹全量分页 + 分 P 独立任务 + Room FIFO + 网络约束 | M5 | 音频/视频批量选择、失败重试和仅非计费网络等待恢复已完成 |
| 系统托盘 | Android 无对应能力 | - | 不迁移 |
| 桌面窗口控制 | Android Activity/系统返回栈 | - | 平台替代 |
| Electron 自动更新 | GitHub Release + DownloadManager + 系统安装器 | M6 | 已完成首版 |

## 不能机械复制的实现

- Electron IPC 必须替换为 Android repository/service 接口。
- `HTMLAudioElement` 必须替换为 Media3。
- `electron-store` 必须替换为 Room、DataStore 和 Keystore。
- Node.js 文件 API 必须替换为 `ContentResolver`、MediaStore 和 SAF。
- FFmpeg 二进制不能直接复用桌面文件，需要重新评估 Android ABI、包体和许可。
