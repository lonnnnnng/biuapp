# M24 Intent 安全对齐记录

### Best practices and security alignment update: 小组件 Receiver 与媒体 PendingIntent

- **改进说明**：桌面小组件必须由系统 Launcher 发现，因此使用标准导出 `AppWidgetProvider`；Receiver 只声明系统 `APPWIDGET_UPDATE`，不接收自定义广播。小组件控制通过 Media3 构造显式指向 `PlaybackService` 的播放 Intent，页面跳转使用显式、不可变的 Activity PendingIntent。
- **优先级**：中。避免自定义导出广播、隐式 PendingIntent 或可变 Intent 被其他应用篡改，同时保持 Launcher 和系统媒体控件可用。
- **对齐动作**：
  - 不新增自定义广播 action 或动态导出 Receiver。
  - 页面入口使用 `PendingIntent.FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`。
  - 播放控制使用 `PlaybackPendingIntentBuilder`，目标服务类型固定为 `PlaybackService`。
  - `PlaybackService` 继续通过 `ControllerTrustPolicy` 允许系统可信控制器与本应用 UID，拒绝普通第三方控制器。

#### 修改文件

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/lonnnnnng/biu/widget/PlaybackWidgetProvider.kt`
- `app/src/main/java/com/lonnnnnng/biu/playback/PlaybackService.kt`

#### 实现差异

```diff
+<receiver
+    android:name=".widget.PlaybackWidgetProvider"
+    android:exported="true">
+    <intent-filter>
+        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
+    </intent-filter>
+    <meta-data
+        android:name="android.appwidget.provider"
+        android:resource="@xml/playback_widget_info" />
+</receiver>

+val intent = Intent(context, MainActivity::class.java)
+PendingIntent.getActivity(
+    context,
+    REQUEST_OPEN_APP,
+    intent,
+    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
+)

+PlaybackPendingIntentBuilder(context, command, PlaybackService::class.java)
+    .setStartAsForegroundService(true)
+    .build()
```

#### 测试与验证

1. `:app:lintDebug` 通过，未发现导出组件或 PendingIntent 安全错误。
2. `:app:testDebugUnitTest` 与 `:app:assembleDebug` 通过。
3. 设备验收时仍需检查 Launcher 添加/缩放、三项控制、内容跳转，以及应用进程退出后的显式服务唤起。
