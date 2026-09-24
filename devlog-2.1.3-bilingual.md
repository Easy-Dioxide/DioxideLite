# DioxideLite 2.1.3 Devlog / 开发日志

## 中文

### ClickGUI / 点击 GUI
- 在现有 ClickGUI 主题系统中加入 `OPAI_ONYX` 主题。
- Theme 现在是标准 `EnumSetting`，可直接在 ClickGUI 中切换并保存配置。
- Onyx 主题沿用 DioxideLite 的 Skija 渲染链，不替换主界面，也不修改单人游戏/多人游戏等原版菜单。
- 保留原有主题与 Drop/Pop 两种 GUI 模式。

### Dynamic Island / 灵动岛
- 保持现有 DioxideLite、OPAI Onyx、Onyx Minimal、Onyx Glass 四种 Island 样式。
- Island 样式继续作为 `Dynamic Island` 视觉模块设置，可在 ClickGUI 直接切换。
- 网易云歌词继续通过现有 `NcmLyrics` API 接入，并对 API 异常进行隔离。
- 修复 Island 渲染缓存中的重复局部变量问题。

### Visual compatibility / 视觉适配
- Target HUD 继续读取现有目标状态并进行纯视觉呈现；不在 HUD 层新增攻击/自动化逻辑。
- Scaffold Block HUD 继续读取 Scaffold 的方块计数与当前放置物品，仅负责 HUD/视觉展示。
- 现有 ESP、Block Highlight、Combat Visuals、Target HUD、Scaffold Block HUD 保持独立注册，可通过 ClickGUI 控制。
- 对 Combat / Movement / Player 的 OpenOnyx 移植进行视觉侧核对：视觉模块只消费 DioxideLite 当前状态/API，不直接依赖旧版 OpenOnyx Minecraft 类。

### Window / Taskbar icon / 游戏窗口与任务栏图标
- 重新生成 DioxideLite D Logo 的 16/32/48/64/128 多分辨率图标。
- WindowMixin 改为向 GLFW/窗口系统提供完整多尺寸图标集合，减少 Windows 缩放、任务栏及窗口边框使用错误尺寸图标的情况。
- Via 资源中的旧 `VIA` 图标不再作为 DioxideLite 主窗口图标。

### Build / 构建
- 版本保持 `2.1.3`。
- 本次没有修改主菜单 UI 或单人/多人游戏入口。
- 构建检查同时保留源码级 API 检查；最终 Gradle 构建仍取决于本机是否已有项目所需的 Loom/依赖缓存。

## English

### ClickGUI
- Added the `OPAI_ONYX` theme to the existing ClickGUI theme system.
- Theme selection is now a normal persisted `EnumSetting`, so it can be switched directly from ClickGUI.
- The Onyx presentation keeps DioxideLite's Skija rendering path and does not replace the vanilla main menu or Singleplayer/Multiplayer screens.
- Existing themes and both Drop/Pop GUI modes remain available.

### Dynamic Island
- Kept four visual styles: Dioxide, OPAI Onyx, Onyx Minimal and Onyx Glass.
- Island style remains a setting of the `Dynamic Island` visual module and can be changed directly in ClickGUI.
- NetEase Cloud Music lyrics continue to use the existing `NcmLyrics` API with failure isolation.
- Fixed a duplicated local declaration in the island render cache path.

### Visual compatibility
- Target HUD continues to consume the existing target state for presentation only; no attack or automation logic is added to the HUD layer.
- Scaffold Block HUD continues to consume Scaffold block-count/placement state for presentation only.
- Existing ESP, Block Highlight, Combat Visuals, Target HUD and Scaffold Block HUD remain independently registered and configurable.
- OpenOnyx Combat/Movement/Player migration was checked from the visual-adapter side: visual modules consume DioxideLite's current APIs rather than old OpenOnyx Minecraft classes.

### Window / Taskbar icon
- Regenerated the DioxideLite D logo at 16/32/48/64/128 resolutions.
- WindowMixin now supplies a multi-resolution icon set to the window system, reducing incorrect icon selection on Windows scaling, taskbar and window borders.
- The old Via `VIA` resource is no longer used as the DioxideLite main window icon.

### Build
- Version remains `2.1.3`.
- No main-menu UI or Singleplayer/Multiplayer entry UI was changed.
- Source/API checks were performed; the final Gradle build still depends on the required Loom/dependency caches being available locally.
