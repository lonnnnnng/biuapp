# 安全与合规边界

## 账号数据

- Cookie、refresh token 和手机号属于敏感数据，不写入日志、Crash 报告或普通 Room 表。
- 正式账号态使用 Android Keystore 派生密钥加密本地账号数据。
- M2 登录原型只使用 WebView `CookieManager` 的应用沙箱存储，不把 Cookie 复制到 Room、DataStore、日志或导出文件；进入 M3 账号音乐库前必须完成 Keystore 凭据存储并清理 WebView 冗余 Cookie。
- 导出设置时默认排除所有登录凭据。

## 网络

- 仅 Bilibili 域名请求附加 Bilibili `Referer`、`Origin`、User-Agent 和 Cookie。
- 登录 WebView 禁止第三方 Cookie，只允许导航到 HTTPS 的 `bilibili.com` 主域及子域。
- 第三方歌词、更新和图片请求使用独立客户端，禁止共享登录 Cookie。
- HTTPS 证书校验保持系统默认，不实现忽略证书错误的调试后门。

## 下载与存储

- 用户发起下载后显示持续通知，并提供暂停和取消操作。
- 下载中的文件保持 `IS_PENDING=1`，完成校验和合并后再对其它应用可见。
- 删除非本应用创建的媒体必须走 Android 系统授权，不绕过 scoped storage。

## 许可与平台规则

- 原项目使用 PolyForm Noncommercial 1.0.0，Android 派生实现仍需保留许可和 Required Notice，不能直接用于商业分发。
- Bilibili 接口调用、下载和账号能力必须遵守平台协议，不实现绕过会员、DRM 或风控限制的功能。
- 引入 FFmpeg 前必须完成 LGPL/GPL 配置、动态链接、源码提供义务和 ABI 清单评审。
