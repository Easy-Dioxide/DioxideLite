# DioxideLite v2.2.6 Devlog

**发布日期：2026-10-07**

---

## v2.2.6 更新内容

### 新增：主菜单视频背景

- **VideoBackgroundBaker（视频帧烘焙器）**：把随包附带的 `assets/dioxide-lite/textures/mainmenu/mainmenu.mp4` 逐帧解码、缩放到主菜单背景尺寸并编码为 PNG 帧序列缓存（复用 jcodec 解码；解码后的 BufferedImage 走 `org.jcodec.scale.AWTUtil`）。
- **VideoBackgroundPlayer（播放器）**：主菜单背景按帧序列循环播放，代替静态背景。
- **BakeProgressWatcher（烘焙进度）**：首次启动需要烘焙帧序列时，在主菜单展示进度，完成后进入播放。
- 依赖：新增 `org.jcodec:jcodec-javase:0.2.5`（提供 `AWTUtil` 帧转图），与既有 `org.jcodec:jcodec:0.2.5` 一起嵌套进 jar。

### 新增：游戏菜单与主题化控件

- **GameMenuScreen**：游戏内菜单界面，配合 `PauseScreenMixin` 接入暂停菜单。
- 主题化控件扩展：`ThemedEditBoxMixin` / `ThemedSliderMixin` / `AbstractSliderButtonAccessor`，让输入框与滑块统一走 Skija 主题渲染。
- 新增 `MinecraftAccessor`、`AbstractSliderButtonAccessor` 等访问器；`UiText` 提供本地化文本层。

### 重构：Skybox

- 删除 v2.2.5 的 `SkyRendererMixin`（Skybox 的原 mixin 注入实现），Skybox 模块改走新实现；`dioxide-lite.mixins.json` 等配置已同步清理，无残留引用。

### 其他

- 大量 mixin 与模块随 2.2.6 分支源码同步更新（RotationManager / EventBus / 渲染与移动模块等，共 89 个文件）。
- **全平台依赖**：构建含 **Windows x64 / macOS / Linux 全部 Skija 与 WebRTC 原生库**（`-Pskija_platforms` + `-Pwebrtc_platforms` 全平台参数），单 jar 通用包，Windows / macOS / Linux 用户均可直接运行。

---

## 验证

- 构建成功（JDK 25.0.4.1 + Gradle + Fabric Loom 1.15.5）
- 产物 `build/libs/DioxideLite-2.2.6.jar`（约 103 MB，全平台原生库）
- 版本号统一为 2.2.6（`DioxideLite.java` VERSION + `gradle.properties` + fabric.mod.json）
- 启动验证：客户端加载至主菜单阶段（详见交付说明的验证范围）
