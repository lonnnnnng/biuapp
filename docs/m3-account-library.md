# M3 账号音乐库第一阶段

## 范围

本阶段完成账号音乐库的只读纵向切片：

- `/x/web-interface/nav` 识别登录态、昵称、头像和账号 `mid`。
- `/x/v3/fav/folder/created/list` 获取用户创建的收藏夹。
- `/x/v3/fav/resource/list` 获取收藏夹中的可播放普通视频。
- `/x/v2/history/toview/web` 获取稍后再看列表；请求沿用桌面版 WBI 签名链路。
- `/x/web-interface/history/cursor` 以 `type=archive` 获取最近视频历史。
- Room 保存本机实际开始播放的条目、播放次数、最后进度和播放时间。

账号页提供“收藏夹、稍后再看、在线历史、本地历史”四个分段。未登录时前三项显示登录提示，本地历史不依赖账号。

## 数据规则

- 收藏夹仅展示 `type=2`、`attr=0` 且具有 `bvid` 的普通视频；音频、合集和失效稿件暂不进入 DASH 播放链路。
- 在线历史仅展示具有 `history.bvid` 的视频稿件，直播、文章等其他业务记录会被忽略。
- 本地数据库不保存 DASH URL。历史重播使用 `bvid + cid + 音质偏好` 重新解析，避免链接过期。
- 播放进度距离结尾不足 30 秒时从头开始，否则从最后保存位置继续。

## 当前边界

- 在线列表目前加载首屏，不提供无限滚动和游标续页。
- 本阶段不执行删除稍后再看、删除在线历史、收藏、点赞、投币或三连等有副作用操作。
- WebView Cookie 仍使用 M2 原型方案；正式凭据加密和 refresh token 持久化继续列入 M3 后续工作。
- 真实账号数据必须由用户在应用内完成登录后验收；测试和日志不得导出 Cookie。

## 模拟器验收

2026-07-29 使用可见 `Pixel_9` AVD（`emulator-5554`）完成：

- 真实音乐区推荐加载、DASH 播放和 MediaSession `PLAYING` 状态。
- 暂停时写入 Room 的进度、总时长和播放次数。
- 强制停止应用后冷启动，本地历史仍可见。
- 从本地历史点击重播时重新解析 URL，并从保存进度继续。
- 登录 WebView 使用 `https://passport.bilibili.com/h5-app/passport/login` 和系统移动 UA，手机号、验证码及账号密码表单均按手机宽度展示。

H5 登录页在当前 Android System WebView 中会把 CSS `100vh` 计算为 `0px`，导致页面根容器只有内容高度，底部协议文案覆盖登录按钮。应用只在该 H5 登录路径注入实际 `visualViewport.height`，并在软键盘或视口变化时更新根容器最小高度；补丁不勾选协议、不填写凭据，也不修改登录按钮状态。H5 通过 XHR 写入 `SESSDATA` 时不会重新加载页面，应用仅以 Cookie 内存指纹触发账号接口复核，服务端确认登录后自动关闭 WebView 并刷新账号库。

播放复测发现移动端伪装 UA 会让当前 Web DASH CDN 返回 403，因此 Bilibili 专用客户端统一使用桌面版相同的 Chrome 141 Web UA。CDN 请求只携带 Referer、Origin 和 UA，不携带账号 Cookie；失败日志只记录主机和状态码，不输出完整签名 URL。

## 接口证据

- 桌面版服务实现：`src/service/fav-resource.ts`、`fav-folder-created-list.ts`、`history-toview-list.ts`、`web-interface-history-search.ts`。
- 2026-07-29 检索参考：<https://github.com/pskdje/bilibili-API-collect/blob/main/docs/fav/list.md>
- 2026-07-29 检索参考：<https://github.com/pskdje/bilibili-API-collect/blob/main/docs/historytoview/toview.md>
- 2026-07-29 检索参考：<https://github.com/pskdje/bilibili-API-collect/blob/main/docs/historytoview/history.md>

公开接口文档属于社区收集资料，不视为 Bilibili 官方稳定契约；最终可用性以本项目真机或模拟器真实响应为准。
