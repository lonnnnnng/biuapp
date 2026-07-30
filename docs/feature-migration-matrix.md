# 功能迁移矩阵

| 桌面功能 | Android 实现 | 阶段 | 状态 |
| --- | --- | --- | --- |
| Bilibili 登录 | WebView + CookieManager + 专用 OkHttpClient | M2 | 原型完成，账号态待人工验收 |
| 推荐音乐、搜索 | Compose 列表 + Bilibili API + WBI | M2 | 已完成 |
| 视频详情、分 P | `view` 详情与 cid 解析；列表默认第一 P | M2/M3 | M2 数据链路完成，选择器待 M3 |
| 收藏夹、稍后再看、历史 | 账号 API + Room 本地历史 | M3 | 首屏只读浏览与本地续播完成，分页/删除待开发 |
| 关注、动态、用户空间 | 独立 feature 页面 | M4 | 待开发 |
| DASH 音频播放 | Media3 + 最高音质/省流量选择 + URL 刷新 + 备用 CDN | M2 | 已完成 |
| 播放队列 | Media3 playlist + Room 镜像 | M1/M3 | 本地播放历史完成，持久队列待开发 |
| 后台播放 | MediaSessionService | M1 | 已完成 |
| 快捷键、任务栏 | 通知栏、锁屏、耳机、蓝牙 MediaSession | M1 | 已完成 |
| 全屏播放器 | Compose Now Playing 页面 | M3 | 已完成 |
| mini 播放器 | 应用底部迷你播放栏；视频场景可选画中画 | M1/M4 | 音频迷你栏已完成 |
| 歌词 | LRCLIB/网易搜索 + 本地缓存 | M3 | 待开发 |
| 本地音乐 | MediaStore 查询 + SAF 目录授权（Android 10+） | M4 | 扫描、播放、目录筛选与重启恢复已完成 |
| 音频下载 | 前台下载服务 + MediaStore.Audio | M4 | 待开发 |
| 视频下载 | 分轨下载 + Media3 Muxer/Transformer | M5 | 待开发 |
| 批量下载 | Room 队列 + 并发限制 + 网络约束 | M5 | 待开发 |
| 系统托盘 | Android 无对应能力 | - | 不迁移 |
| 桌面窗口控制 | Android Activity/系统返回栈 | - | 平台替代 |
| Electron 自动更新 | GitHub Release + DownloadManager + 系统安装器 | M6 | 已完成首版 |

## 不能机械复制的实现

- Electron IPC 必须替换为 Android repository/service 接口。
- `HTMLAudioElement` 必须替换为 Media3。
- `electron-store` 必须替换为 Room、DataStore 和 Keystore。
- Node.js 文件 API 必须替换为 `ContentResolver`、MediaStore 和 SAF。
- FFmpeg 二进制不能直接复用桌面文件，需要重新评估 Android ABI、包体和许可。
