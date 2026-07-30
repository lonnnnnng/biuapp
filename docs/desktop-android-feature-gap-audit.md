# 桌面端与 Android 端功能差异审计

## 审计基准

- 审计日期：2026-07-30
- Git 分支：`main`
- Android Git 提交：`2df4979 chore(android): 发布 BiuApp 0.1.5`
- 桌面端参考基线：`../desktop-app` 的 `7d49920`
- 审计范围：桌面端 `../desktop-app/src/` 与 Android 端当前仓库的代码
- 审计方式：只核对当前代码，不以早期迁移矩阵或开发计划中的状态作为最终依据

## 总体结论

Android 端的“找歌 -> 单 P/多 P 播放 -> 后台和锁屏控制 -> 下载 -> 队列恢复”主链路已经比较完整，但账号互动、社交内容、高级播放和个性化设置仍明显落后于桌面端，暂时不能算功能完全对齐。

当前 Android 端只有“推荐”和“账号”两个主页面。账号音乐库包含收藏夹、在线历史、本地历史和本地音乐。

## 已经基本对齐

- Bilibili H5 登录与账号状态识别。
- 音乐区、音乐榜、视频搜索和自选关注 UP 首页。
- 单 P、多 P、进度显示和拖动、全屏播放、播放列表选择。
- 后台播放、锁屏/通知栏/耳机控制、进程重启后的队列恢复。
- “我创建的”“我收藏的”、普通收藏夹和视频合集浏览。
- 本地音乐和本地播放历史。
- 单曲及收藏夹批量音频/视频下载、断点恢复和网络限制。
- 应用内检查更新、下载和安装新版本。

## 部分实现

| 功能 | Android 当前状态 | 相比桌面端缺少 |
| --- | --- | --- |
| 关注列表 | 只在“设置首页内容范围”中读取关注 UP，支持名称本地筛选 | 独立关注页、分页、分组管理、取消关注 |
| 搜索 | 支持视频搜索 | 用户搜索、搜索历史、搜索排序 |
| 收藏夹 | 支持读取、播放、批量下载，并区分“我创建的/我收藏的” | 新建、重命名、删除收藏夹；将视频添加到收藏夹或从收藏夹移出 |
| 在线历史 | 固定读取最近 20 条 | 游标分页、搜索、单条删除、清空、按类型筛选 |
| 播放队列 | 可以查看、点击切换和持久化恢复 | 删除单项、清空、插入下一首、手动排序 |
| 音质 | 最高音质、省流量两档 | 桌面端的自动、无损、高、中、低五档精细选择 |
| 迷你播放器 | 应用底部迷你播放栏 | 桌面端独立迷你窗口；Android 画中画或悬浮模式尚未实现 |
| 下载 | 音频、视频和批量下载较完整 | 单独下载封面、自定义下载目录 |

## 尚未实现

### 账号与内容

1. 动态流页面。
2. UP 主空间、投稿、动态、系列和合集浏览。
3. 全局用户搜索。
4. 关注、取消关注及关注分组管理。
5. 点赞、取消点赞和一键三连。
6. Bilibili 播放心跳上报。Android 播放目前不会向 Bilibili 同步播放历史进度。

### 播放体验

1. 歌词搜索、翻译歌词、歌词缓存、歌词偏移和字号调整。
2. 音频实时频谱。
3. 全屏播放器的封面、背景、歌词颜色和频谱颜色等外观配置。
4. 单曲循环、列表循环和随机播放。Android 播放器当前固定使用 `Player.REPEAT_MODE_OFF`。
5. 播放倍速。
6. 播放队列删除、清空、插入下一首和排序操作。

### 设置与个性化

1. 浅色/深色/跟随系统主题切换。
2. 强调色、背景色、字体和圆角设置。
3. 列表、网格和紧凑显示模式切换。
4. 菜单显示和隐藏配置。
5. 设置导入和导出。
6. 应用内代理配置。
7. 自定义音频和视频下载目录。

## 稍后再看说明

Android 仓库仍保留读取稍后再看的 API 方法 `BilibiliRepository.watchLater()`，但当前页面没有入口，也没有删除操作。

这符合此前从 Android 账号页删除“稍后再看”的产品决策，但从桌面功能对齐角度仍属于未实现。

2026-07-31 已确认继续保持 Android 端不提供稍后再看入口；API 方法暂时保留供兼容和后续评估使用，迁移矩阵与路线图不再将其标记为已完成能力。

## 不需要机械迁移的平台功能

以下桌面能力不建议按原形式迁移：

- 系统托盘：Android 使用媒体通知和锁屏媒体控件替代。
- 桌面窗口最小化和关闭行为：由 Android Activity 生命周期和系统返回栈替代。
- 桌面全局快捷键：由通知栏、锁屏、耳机和蓝牙媒体按键替代。
- 桌面音量滑块：Android 优先使用系统媒体音量控制。
- Electron 独立 mini 窗口：Android 已有应用内底部迷你播放栏；是否增加画中画或悬浮窗需要单独评估权限和使用价值。

## 现有文档偏差

`feature-migration-matrix.md` 和 `development-roadmap.md` 的部分状态已经落后于当前代码：

- 文档仍把稍后再看列为已完成，但当前 Android 页面没有入口。
- 文档未完整反映“我收藏的”和视频合集读取能力。
- Android 下载、批量任务、队列持久化和更新能力的实际完成度高于早期记录。

后续开发完成一个功能切片后，应同步维护本审计文档和功能迁移矩阵，避免计划状态与实际代码再次分离。

## 建议开发优先级

### P0：补齐账号音乐库闭环

- 在线历史游标分页、搜索、单条删除和清空。
- 收藏夹新建、重命名、删除，以及视频加入/移出收藏夹。
- 保持不提供稍后再看入口，并同步维护代码与迁移文档状态。
- Bilibili 播放心跳上报及用户开关。

### P1：补齐播放器核心能力

- 单曲循环、列表循环和随机播放。
- 播放倍速。
- 播放队列删除、清空和插入下一首。
- 歌词搜索、展示和本地缓存。

### P2：补齐内容发现与互动

- 用户搜索和 UP 主空间。
- 独立关注列表及关注/取消关注。
- 动态流。
- 点赞和一键三连。

### P3：设置和体验完善

- 主题、颜色、字体和显示模式。
- 设置导入/导出。
- 应用内代理。
- 自定义下载目录和封面下载。

## 关键代码位置

### Android

- 主页面与 UI 状态：`app/src/main/java/com/lonnnnnng/biu/ui/BiuViewModel.kt`
- 账号音乐库类型：`app/src/main/java/com/lonnnnnng/biu/data/bilibili/BilibiliModels.kt`
- Bilibili API：`app/src/main/java/com/lonnnnnng/biu/data/bilibili/BilibiliRepository.kt`
- 主界面与全屏播放器：`app/src/main/java/com/lonnnnnng/biu/ui/BiuApp.kt`
- 后台播放和 MediaSession：`app/src/main/java/com/lonnnnnng/biu/playback/PlaybackService.kt`

### 桌面端只读参考

- 页面路由：`../desktop-app/src/routes.tsx`
- 设置中心：`../desktop-app/src/pages/settings/`
- 关注列表：`../desktop-app/src/pages/follow-list/`
- 动态流：`../desktop-app/src/pages/dynamic-feed/`
- 用户空间：`../desktop-app/src/pages/user-profile/`
- 稍后再看：`../desktop-app/src/pages/later/`
- 在线历史：`../desktop-app/src/pages/history/`
- 全屏播放器：`../desktop-app/src/components/full-screen-player/`
- 播放列表：`../desktop-app/src/components/music-playlist-drawer/`
- 收藏操作：`../desktop-app/src/components/favorites-select-modal/`、`../desktop-app/src/components/favorites-edit-modal/`
