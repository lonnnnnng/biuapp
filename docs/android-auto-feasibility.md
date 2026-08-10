# Android Auto 可行性评估

评估日期：2026-08-10  
适用仓库：`android-app`  
结论状态：可实现，建议作为独立后续切片；M24 只完成评估，不直接迁移现有播放服务

## 1. 结论

BiuApp 具备接入 Android Auto 的良好基础，但当前还不能向车机提供可浏览的音乐库。

已经具备的基础：

- `PlaybackService` 使用 Media3 `MediaSessionService`，播放、队列、通知、锁屏、耳机和蓝牙控制共享同一状态。
- 当前媒体项已经包含标题、当前分 P、UP 主、封面和稳定曲目身份；多 P 使用 `bvid + cid`，适合直接映射为车载媒体项。
- Room 已保存最近播放、本地歌单、下载、本地音乐与队列，能够构造不依赖手机 Compose 页面的媒体树。
- `ControllerTrustPolicy` 已允许系统可信控制器，同时拒绝普通第三方应用直接连接播放会话。

尚缺的关键能力：

- 当前服务继承 `MediaSessionService`，不是 `MediaLibraryService`，也没有 `MediaLibrarySession`。
- 当前没有实现 `onGetLibraryRoot()`、`onGetChildren()`、`onGetItem()`、搜索结果和车载播放请求解析。
- Manifest 虽保留 `android.media.browse.MediaBrowserService` action，但仅声明 action 不会自动生成可浏览内容树。
- 尚未添加 Android Auto 发现所需的汽车能力描述、单色 attribution icon、车载错误状态和 DHU 验收。

因此推荐结论为：**技术上好实现，改造量中等，主要工作是把现有音乐库聚合为只读媒体树，而不是重写播放器。**

## 2. 官方 API 依据

Media3 官方文档说明，`MediaLibraryService` 在 `MediaSessionService` 的基础上提供标准媒体库 API，并通过 `MediaLibrarySession` 暴露单根节点的可浏览树。Android Auto 和 Android Automotive OS 会通过根节点与子节点回调读取内容。

- [Serve content with a MediaLibraryService](https://developer.android.com/media/media3/session/serve-content)
- [Build your content hierarchy](https://developer.android.com/training/cars/media/create-media-browser/content-hierarchy)
- [Configure manifest files](https://developer.android.com/training/cars/media/configure-manifest)
- [Media apps for cars overview](https://developer.android.com/training/cars/media)

官方车载层级还要求考虑根节点数量和支持类型提示。常见车机根层级通常最多展示四个可浏览入口，因此 BiuApp 不应把手机端全部页面平铺到车机。

## 3. 推荐媒体树

```text
Biu
├── 最近播放
│   └── Room 最近播放曲目
├── 本地歌单
│   └── 歌单 -> 单 P / 具体分 P / 本地音乐
├── 已下载
│   └── 可直接读取的 MediaStore 音频或视频音轨
└── 音乐来源
    └── 本地分组 / UP 主 -> 最新投稿或合集曲目
```

设计原则：

1. 根节点控制为四项，全部使用短名称和可浏览节点。
2. `最近播放`、`本地歌单`、`已下载` 优先使用本地数据，弱网或未登录时仍可浏览。
3. `音乐来源` 才读取在线 UP 主内容；未登录、风控、断网和接口失败时返回明确但不阻断其他节点的错误。
4. 叶子媒体项使用现有稳定 `mediaId`。Bilibili 多 P 必须继续使用 `bvid:cid`，不能退回主视频 ID。
5. 在线曲目被选中后才解析临时 DASH 地址；媒体树本身只保存稳定身份和展示元数据。
6. 视频能力不进入驾驶中主链路。Android Auto 只暴露音频播放，手机端手动视频模式保持独立。

## 4. 推荐实施方案

### 阶段 A：服务迁移

- 将 `PlaybackService` 从 `MediaSessionService` 迁移为 `MediaLibraryService`。
- 将 `MediaSession` 替换为 `MediaLibrarySession`，保留现有 Player、恢复、淡入淡出、音量平衡、下载回退和自愈逻辑。
- Manifest 同时声明：
  - `androidx.media3.session.MediaLibraryService`
  - `android.media.browse.MediaBrowserService`
- 保持当前 `ControllerTrustPolicy`，并对媒体库回调中的 `ControllerInfo` 使用同一可信调用方边界。

### 阶段 B：媒体树仓库

- 新增只读 `CarMediaLibraryRepository`，复用现有 Room Repository 和 Bilibili Repository，不在 Service 中直接拼接数据库/API 细节。
- 实现根节点、分页子节点、单项查询和刷新通知。
- 对在线 UP 主内容设置并发和超时上限；车机请求不能触发无限合集解析或 200P 全量 DASH 解析。
- 将媒体项分为 `browsable` 与 `playable`，所有可播放项补全标题、UP 主、封面、时长和内容类型。

### 阶段 C：播放与搜索

- 在 `onSetMediaItems()` 或等价回调中将稳定媒体 ID 解析为现有 `Track`，再进入当前 Media3 队列链路。
- 支持最近播放、本地歌单和已下载内容立即起播；在线内容显示解析中并保留失败重试语义。
- 增加受限搜索：优先搜索本地歌单、最近播放和已选音乐来源，不在驾驶场景提供开放式社区搜索。
- 语音播放必须映射到同一稳定曲目身份，避免多 P 命中主标题后总是播放 P1。

### 阶段 D：平台声明与验收

- 添加 Android Auto 媒体能力描述 XML 和 Manifest meta-data。
- 提供单色 attribution icon，并检查车载强调色和媒体卡片展示。
- 使用 Desktop Head Unit 验证浏览、播放、暂停、上一首、下一首、队列、搜索、断网和登录失效。
- Android Automotive OS 作为独立发布目标另行评估；当前不在手机 APK 中引入车载 Activity 或视频模板。

## 5. 风险与边界

| 风险 | 影响 | 处理方式 |
| --- | --- | --- |
| Bilibili DASH 地址短期失效 | 车机选择在线曲目后可能无法立即播放 | 只保存稳定身份，选中时解析；继续复用地址刷新、本地副本和有限重试 |
| 账号 Cookie 失效 | 在线来源不可浏览 | 本地节点保持可用，在线节点返回登录失效；登录仍只允许在手机完成 |
| 大型多 P / 合集 | 车机等待过久或请求过多 | 媒体树只分页展示元数据，不在浏览阶段解析全部播放地址 |
| 车载控制器包名差异 | 错误拒绝 AAOS 或 Assistant | 以系统可信身份和签名校验为主，不维护脆弱的固定包名白名单 |
| 驾驶分心限制 | UI 或功能不符合车载质量要求 | 只提供音频、短标签和系统渲染界面，不迁移视频、下载管理或账号设置 |
| 手机播放回归 | 服务基类迁移影响现有通知和控制器 | 先建立 MediaLibraryService 回归测试，再验证手机、锁屏、耳机和小组件后才启用车载声明 |

## 6. 验收清单

- [ ] DHU 能发现 Biu 并连接媒体库服务。
- [ ] 根节点不超过车机提示的限制，四个入口均可打开。
- [ ] 最近播放、本地歌单和下载在断网时仍可浏览并播放。
- [ ] 在线 UP 主内容支持分页，加载失败不影响本地节点。
- [ ] 单 P 和多 P 均显示正确曲名；多 P 选中后播放对应 `cid`，不是固定 P1。
- [ ] 播放、暂停、上一首、下一首、进度和队列与手机 MediaSession 同步。
- [ ] 语音搜索只返回允许范围内的音频内容。
- [ ] 手机通知、锁屏、耳机、蓝牙、桌面小组件和进程恢复无回归。
- [ ] 驾驶中不出现视频、登录、下载管理或复杂编辑入口。

## 7. M24 决策

M24 只保留上述评估文档，不修改服务基类和车载 Manifest 声明。原因是该迁移会同时影响手机端所有媒体控制入口，需要独立切片完成 DHU 和手机回归后再启用。

建议后续立项条件：用户有明确 Android Auto 使用场景，并能提供 DHU 或真实车机验收环境。满足后按“服务迁移 -> 本地媒体树 -> 在线来源 -> 搜索和平台声明”四步推进。
