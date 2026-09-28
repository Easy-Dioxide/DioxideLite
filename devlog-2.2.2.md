# DioxideLite v2.2.2 Devlog

**日期：** 2026-09-28
**版本：** v2.2.2
**主题：** 多平台移植（Windows / Linux / macOS）+ Render 模块重组 + ClickGUI 双模式 + 品牌清理

---

## 核心变更

### 多平台移植（单 jar 四平台）
- 打包 Skija 原生库：Windows x64 / Linux x64 / macOS arm64 / macOS x64 四平台 + `skija-shared` / `types`；webrtc-java 原生库同样覆盖四平台。
- 新增 `PlatformSupport`（`util/client/PlatformSupport.java`）替换 Windows 专属代码路径：
  - PowerShell / AWT 字体发现
  - 无 JNA 的设备 ID 生成
  - `Desktop.open` → `open` / `explorer` / `xdg-open` 跨平台打开链接
  - tinyfd 文件对话框、字体回退
- `build.gradle.kts` 新增 `-Pskija_platforms` / `-Pwebrtc_platforms` 平台开关；新增 `scripts/build-macos.sh` 与 `PORTING-macos.md` 移植文档。
- 验证状态：Windows / Linux 构建静态验证（结构、链接、类文件检查）；macOS arm64 已在真实游戏会话冒烟测试；macOS x64 打包入 jar 但未在真机启动。

### Render 模块重组（27 退役 → 35 移植）
- 27 个内置 render 模块运行时退役，替换为 `module/modules/render/advanced/` 下的 **35 个移植模块**，经 `RenderModuleRegistry` 统一注册：ESP / Chams / NoRender / Fullbright / Camera / Wings / Trails / TargetESP / BedESP / BlockOverlay / ItemPhysics / Particles / Trajectories / Zoom / Freelook / SkeletonESP / GlowESP / Boxes / Ambience / Skybox / FogBlur / FogRemove / Hurtcam / Hand / Crosshair / SkinChanger / CapeChanger / ChinaHat / JumpCircles / Arrows / TNTTimer / ParticleLimiter / SeeInvisibles / ContainerESP / Animations。
- 配套 mixin 挂钩：`CameraMixin` / `GameRendererMixin` / `GuiMixin` / `ItemInHandRendererMixin` / `ParticleEngineMixin` / `AbstractClientPlayerMixin` / `EntityRendererMixin` 等。
- 新增 `core/` 引擎目录（combat / movement / player / client engines）。

### 界面
- **ClickGUI 双模式**：`LegacyStyle`（默认，重制面板：圆角、粒子光晕、展开动画）与 `Setsuna`（环形）。
- **HUD**：单一右对齐 **ArrayList**（纯文本、白色、右对齐、默认无背景；`Style` / `Colors` / `Row Spacing` / `Screen Margin` / `Scale` 可配置）、通知栈、药水列表。
- **ESP 名牌**：可选客户端 Logo 图标显示在名字旁（`ESP → Names → Icon`）；ESP 名牌开启时自动抑制原版名牌。
- Watermark、ClickGUI 角块与主菜单统一读取 `DioxideLite.VERSION`（= 2.2.2）。
- Watermark 模块 `Rename` 设置默认值改为 `DioxideLite`。

### 修复
- **Camera 不再锁定视角**：平滑 yaw/pitch 改为跟随玩家，而非作为衰减偏移使用。
- **Wings 不再糊屏**：三角形缓冲上载时不再排序，并应用姿势锚定，翅膀贴合玩家背部。
- Wings / Trails / ContainerESP 轮廓改用细四边形绘制，弃用 `GL_LINES`。
- **HUD 名牌 / Logo 图标漂移**：改为从插值位置投影，移动时不再漂移。

### 品牌清理
- 移除客户端内全部外部品牌字样（类、资源、模块名、语言文件）；仅保留少量遗留配置键，用于一次性迁移旧配置文件后即从配置中移除。
- 源码注释保留 `[DioxideLite 修复]` 历史标记。

---

## 已知限制

- Skybox / Ambience / Fog Blur 后处理 shader、Item Physics 地面变换、Hand 挥动曲线、Camera 位置平滑 4 个上游 hook 尚未实现（Camera 的旋转平滑可用）。
- 发布 jar 由 patch 上游 2.2.1 archive + 本源码树关键类（HUD / ModuleManager / ConfigManager / DioxideLiteClient / Halo / 移植模块）构建，少量 release 类比本源码旧。
- IRC 客户端期待 OpticsValleyIRC 服务器（默认端口 16688）；无服务器时仅记录连接警告。

---

## 验证

- [x] `DioxideLite.java` VERSION = `2.2.2`，`gradle.properties` version = `2.2.2`
- [x] `fabric.mod.json` version = `2.2.2`（发布 jar 实测）
- [x] `module/modules/render/advanced/` 35 个移植模块 + `RenderModuleRegistry`
- [x] `PlatformSupport` 多平台路径就位
- [x] Watermark `Rename` 默认 `DioxideLite`
- [x] ClickGUI `LegacyStyle` / `Setsuna` 双模式

## 文件变更

| 文件 | 说明 |
| --- | --- |
| `src/main/java/com/dioxidelite/core/` | 新增，combat / movement / player / client 引擎 |
| `src/main/java/com/dioxidelite/module/modules/render/advanced/` | 新增，35 个移植 render 模块 + 注册表 |
| `src/main/java/com/dioxidelite/util/client/PlatformSupport.java` | 新增，跨平台支持层 |
| `src/main/java/com/dioxidelite/ui/hud/ArraylistHUD.java` | 新增，右对齐 ArrayList |
| `src/main/java/com/dioxidelite/ui/hud/NotifStackHUD.java` / `EffectListHUD.java` | 新增，通知栈 / 药水列表 |
| `src/main/java/com/dioxidelite/ui/clickgui/GuiEffects.java` / `GuiPalette.java` | 新增，ClickGUI 特效 / 调色板 |
| `src/main/java/com/dioxidelite/mixin/AbstractClientPlayerMixin.java` | 新增，名牌 / 实体渲染挂钩 |
| `scripts/build-macos.sh` | 新增，macOS 构建脚本 |
| `PORTING-macos.md` | 新增，macOS 移植文档 |
| `build.gradle.kts` / `gradle.properties` | 修改，多平台打包开关 + 版本 2.2.2 |
| `src/main/java/com/dioxidelite/`（其余） | 修改，品牌清理与修复 |
| `src/main/java/com/dioxidelite/onyx/` 等 | 删除，品牌清理移除 Onyx 旧模块 |
