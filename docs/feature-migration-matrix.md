# 功能迁移矩阵

| 桌面功能 | Android 实现 | 阶段 | 状态 |
| --- | --- | --- | --- |
| 推荐音乐、搜索 | Compose 列表 + Bilibili API | M2 | 待开发 |
| 收藏夹、稍后再看、历史 | 账号 API + 分页缓存 | M3 | 待开发 |
| 关注、动态、用户空间 | 独立 feature 页面 | M4 | 待开发 |
| 播放队列 | Media3 playlist + Room 镜像 | M1/M2 | 已开始 |
| 后台播放 | MediaSessionService | M1 | 已开始 |
| 快捷键、任务栏 | 通知栏、锁屏、耳机、蓝牙 MediaSession | M1 | 已开始 |
| 全屏播放器 | Compose Now Playing 页面 | M3 | 待开发 |
| mini 播放器 | 应用底部迷你播放栏；视频场景可选画中画 | M1/M4 | 已开始 |
| 歌词 | LRCLIB/网易搜索 + 本地缓存 | M3 | 待开发 |
| 本地音乐 | MediaStore 查询 + SAF 目录授权 | M4 | 待开发 |
| 音频下载 | 前台下载服务 + MediaStore.Audio | M4 | 待开发 |
| 视频下载 | 分轨下载 + Media3 Muxer/Transformer | M5 | 待开发 |
| 批量下载 | Room 队列 + 并发限制 + 网络约束 | M5 | 待开发 |
| 系统托盘 | Android 无对应能力 | - | 不迁移 |
| 桌面窗口控制 | Android Activity/系统返回栈 | - | 平台替代 |
| Electron 自动更新 | 应用商店或受控 APK 更新 | M6 | 平台替代 |

## 不能机械复制的实现

- Electron IPC 必须替换为 Android repository/service 接口。
- `HTMLAudioElement` 必须替换为 Media3。
- `electron-store` 必须替换为 Room、DataStore 和 Keystore。
- Node.js 文件 API 必须替换为 `ContentResolver`、MediaStore 和 SAF。
- FFmpeg 二进制不能直接复用桌面文件，需要重新评估 Android ABI、包体和许可。
