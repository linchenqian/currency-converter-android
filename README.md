# Currency

<p align="center">
  <img src="docs/app-icon.png" width="128" alt="Currency App icon">
</p>

Currency 是一个以计算器为核心交互的原生 Android 货币换算应用。输入金额时可以直接进行加、减、乘、除和百分比运算，计算结果会同步换算为目标币种。

## 功能

- 支持 160+ 种货币，并可按币种代码或中文名称搜索
- 加、减、乘、除、百分比、退格和清除操作
- 币种交换与圆形国旗图标
- 金额根据可用宽度自动调整字号，避免长数字破坏排版
- Light / Dark Mode 自动跟随系统
- 启动时获取最新汇率，并缓存最近一次成功结果供离线使用
- 启动时自动检查 GitHub Release 更新，可立即安装、下次提醒或忽略当前版本
- 无广告、无登录、无用户追踪

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Android 8.0（API 26）及以上

## 汇率

应用启动时从 `open.er-api.com` 获取以美元为基准的汇率。界面会显示服务端返回的最近更新时间；网络不可用时优先使用本地缓存，没有缓存时使用内置的有限离线参考值。

## 应用更新

Currency 启动时会通过 GitHub Releases API 检查公开的新版本，并在 API 限流时自动回退到最新 Release 地址。选择立即更新后，APK 会由 Android 系统下载服务保存到应用专属目录；获取到 GitHub 摘要时会完成 SHA-256 校验，并始终校验应用包名，再交给系统安装器。首次使用需要允许 Currency 安装未知应用。“不再提醒”只忽略当前版本，后续更高版本仍会提示。

## 本地构建

需要 JDK 17 和 Android SDK 36。

```bash
./gradlew testDebugUnitTest assembleDebug
```

生成的 APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 第三方资源

圆形国旗资源来自 [HatScripts/circle-flags](https://github.com/HatScripts/circle-flags)，按 MIT License 使用。完整声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
