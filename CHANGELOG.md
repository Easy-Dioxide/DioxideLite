# DioxideLite Changelog

> 更新日志合并文件。所有版本记录均收录于此，新版本在上，倒序排列。
> v2.0.0 起：Minecraft 26.1.2 · Java 25 · Fabric Loader 0.19.2 · Fabric API 0.150.0+
> v1.x 系列：Minecraft 1.21.11 · Fabric Loader · Fabric API 0.141.3+

---

## [v2.0.0] - Setsuna 视觉移植版

> 基于 v2.0.0 基线，移植 SetsunaClient 视觉体系，**彻底移除全部自动化模块**。底层为 Skija base（Minecraft 26.1.2）。

### ✨ 新增

- **视觉渲染模块**：ESP / Chams / HoleESP / NameTags / OreTracers / SpawnerFinder / Tracers / TeamViewer / UHCDetector / Xray
- **TargetHUD**：目标状态 HUD（HealthManager 血量检测 + TargetManager 共享目标选择）
- **网易云音乐**：MusicScreen 音乐界面 + MusicLyricsHUD 歌词 + 音乐预设预览（tritium 库内置）
- **HUD 编辑器**：HudEditorModule，支持拖拽布局
- **Apollo 队伍信息**：队伍消息解析与展示（TeamViewer 配套）

### 🛠 修复

- 修复 viaversion 别名缺失导致的启动空指针崩溃（`provides` 加回 `setsunavia` / `custom` 加回 `setsunavia:implVersion` / `entrypoints` 加回 `setsunavia` 键）
- 修复资源命名空间大小写错误导致的 `IdentifierException` 崩溃（`DioxideLite:` → `dioxide-lite:`）
- 修复 Skija Linux native 缺失（补充 `skija-linux-x64` 打包，Windows 包保留）

### 🧹 清理

- **移除全部自动化**：combat / movement / player（除音乐）自动化模块、Lua 脚本系统（script/）、FeatureRuntime、RotationManager、对应自动化 mixin（FlowingFluid / ItemInHand / Entity / LocalPlayer / Player / MultiPlayerGameMode / Inventory / FastBreak / StrafeJumpPacket / ClientInput / ClientCommonPacketListener / AbstractRecipeBookScreen / Minecraft / LivingEntity 等）
- 保留纯视觉渲染管线

---

## [v1.8.2] - 2026-09-11

正式版。基于 v1.7 基线（commit 7039eb2）的完整视觉与渲染重构，同时清理仓库文档，日志统一收归本文件。

### ✨ 新增

- **GPU Skia 渲染后端**：`SkiaGlBackend` 直通 OpenGL，移除每元素 `glFlush` 与高频 `glGetError`，UI 渲染帧率显著提升（打开 ClickGUI / 渲染动画不再掉到约 0 FPS）
- **SignatureLogo 品牌标识**：ClickGUI 径向菜单中心的 "D" 品牌标志，可独立控制开关、不透明度、辉光强度（`signatureLogoEnabled` / `signatureLogoOpacity` / `signatureLogoGlow`）
- **ThemePage 主题设置页**：整合 ClickGUI 主题切换、Liquid Glass 全局视觉、视觉模板、动态岛、Signature 标志、渲染后端配置
- **三套 ClickGUI 主题**（主题切换即时生效，无需重启游戏）：
  - `DioxideLite Liquid Glass`（原版玻璃主题）
  - `DioxideLite Minimal`（紧凑极简列表）
  - `DioxideLite Signature`（径向菜单 + D 标志品牌主题）
- **`gpuFrameRendering` 配置开关**：可在 Skia GPU direct 与 CPU raster 之间切换，兼容无硬件 GL 的环境

### 🛠 修复

- 字体测量、文本居中与字体缓存问题（`FontRenderer` 居中计算 + 缓存）
- Skia GPU 后端 GL 状态修复、纹理参数错误（`GL_INVALID_ENUM`）
- 主菜单在软渲染环境下被背景覆盖的问题（关闭 GPU 后端后正常）
- Skija 依赖 jar-in-jar 内嵌打包，用户无需手动安装 skija natives

### ⚠️ 已知问题

- LLVMPIPE 软渲染环境（CI / 无独显）下 GPU direct 渲染会覆盖原版主菜单按钮，需在 `DioxideLite/Config.cfg` 设置 `gpuFrameRendering=false`；Windows 真机 GPU 后端工作正常

### 📦 资产

- `DioxideLite-v1.8.2.jar`：Mod 主文件
- `DioxideLite-v1.8.2-sources.jar`：完整源码

---

## [v1.8 / v1.8.1]

GPU 渲染与字体修复的中间迭代版本，功能已被 v1.8.2 完整取代，历史记录合并于此。

### ✨ 新增

- Skia GPU 渲染后端初版（v1.8）
- Skija 渲染优化与字体补丁（v1.8.1）

### 🛠 修复

- 打开 ClickGUI 时帧率骤降问题（GPU 后端引入后大幅缓解）

---

## [v1.7] - ClickGUI Theme Rework

> 基线提交 `7039eb2`。

### ✨ 新增

- ClickGUI 主题系统重构（Liquid Glass 原版玻璃视觉）
- 主菜单 Skia 渲染（MainUI Shader 背景 + 圆形菜单）
- 设置界面（Combat / Render / Tools / Theme / Optimize / Misc）

### 🛠 修复

- 主菜单按钮渲染与交互

---

## [v1.6] - pvputilsRename 分支

> 由 PVPUtils 派生并重命名，传播需附带完整源码 + 署名 + 同许可（见 LICENSE-PVPUTILS.txt）。

### ✨ 新增

- 项目更名 DioxideLite（原 pvputils 系）
- 基础视觉模块（HUD、Dynamic Island 动态岛、Watermark）

---

## [v1.4 - v1.5]

早期迭代版本（见仓库 `Update` 记录：v1.4 / v1.4-beta.1 / v1.3-alpha.2 时代的产物），功能已被后续版本取代。
