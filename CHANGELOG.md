# DioxideLite 更新日志

## v1.8.1 (Skia Optimized · Glass Theme) — 2026-09-11

> 目标环境：Minecraft 1.21.11 · Fabric Loader 0.18.4
> 定位：**Skia 渲染回归（Setsuna 优化实现）+ 字体补丁**——在 v1.8 原生渲染基础上，重新引入 GPU 玻璃质感，同时规避 v1.7 时代"打开 ClickGUI / 渲染动画帧率≈0"的性能陷阱

### 背景

- v1.8 全面原生化解决了 Windows 空白与性能问题，但玻璃质感（真实背景模糊、半透明材质）随之弱化。
- 本版本以 **Setsuna 客户端的 Skia 实现**为蓝本重新引入 Skia：ClickGUI 直接向 Minecraft 当前帧缓冲绘制（GL 后端），玻璃模糊改为**低分辨率降采样捕获**（0.50× / 性能模式 0.38×，30Hz / 20Hz 节流），不再每帧全分辨率提交——这是帧率不再掉到≈0 的关键。

### 新增

- **GLASS 主题（纯透明玻璃）**：恢复 v1.7 时代的纯透明 Liquid Glass 玻璃 ClickGUI 为**独立主题**（`ClickGuiTheme.GLASS`）。面板使用真正的玻璃材质（`LiquidGlassRenderer.drawSurface`：背景模糊 + 高透明填充 + 折射内层 + 高光描边），与 ORIGINAL 主题在 GL 后端下的深色半透明面板区分开。
- **Minimal 主题独立紧凑布局**：重写 `DioxideLiteMinimalClickGuiScreen`（此前只是 `NewSettingsScreen` 空壳）。新增 900×560 自适应卡片（大屏最高 1.15× 放大）、宽内容区、左侧六页导航、拖拽滚动 + 滚动条、关闭按钮，全屏下布局更大更易操作。
- **ClickGUI 内帧率显示**：ClickGUI 右上角实时显示 `FPS` 数字（等效打开 F3 查看渲染帧率，无需切出）。

### 变更（渲染）

- **Skia 回归**：`SkiaScreen` / `SkiaRenderer`（`USE_GL_BACKEND_FOR_FRAME=true`，直绘 MC 帧缓冲）/ `SkiaGlBackend` / `SkiaBlurRenderer`（降采样 + 节流捕获）/ `LiquidGlassRenderer` / `LiquidGlassVisualSystem` 全量恢复。
- **字体补丁**：`FontRenderer` 使用 skija 字体（harmony.ttf / icon.ttf / MaterialSymbolsRounded.ttf），字形栅格化委托 Minecraft/Blaze3D，避免 ImmediatelyFast 类优化与自绘字体冲突；修复 `drawString` 浮点坐标在 1.21.11 的适配。
- **主题热切换**：`ClickGuiThemeController.apply` 支持运行中即时切换（无需重启游戏，重新打开 ClickGUI 即生效），新 GLASS 主题已注册进主题页 Cycle 控件（4 选项）。

### 修复

- **编译修复（用户源码 4 处）**：`RenderPage` / `ThemePage` 括号失配；`HudEditOverlay.mouseScrolled` 的 switch 箭头 case 后接语句（改为 block case）。
- **1.21.11 API 适配**：`PlayerSkin` import 修正（`net.minecraft.world.entity.player.PlayerSkin`）；`ArmorHudRenderer` 的 `Inventory.armor` 改为 `getItemBySlot(EquipmentSlot)`（boots→helmet 顺序保持）；`TargetHudRenderer` 补 `PlayerFaceRenderer` import、头像改用 `getSkinManager().createLookup` + `PlayerFaceRenderer.draw`；`FontRenderer.drawString` 浮点→整数坐标。
- **Mixin 修复**：`AbstractContainerScreenGlassMixin` 注入点由 `renderBg` TAIL 改为 `renderSlots` HEAD（1.21.11 的 renderBg 末尾无有效 RETURN，TAIL 注入导致游戏启动崩溃）。
- **`SkiaBlurRenderer` 补全**：补齐 `restoreReadBuffer` / `restoreDrawBuffer` 两个 GL 状态恢复方法（源码缺失导致编译失败）。

### 兼容

- ClickGUI 四主题（Liquid Glass / Minimal / Signature / Glass）共享同一套页面与配置模型，主题间热切换。
- 游戏内 HUD / Liquid Glass 全局视觉模块沿用既有实现；主菜单为 Setsuna 风格（DIOXIDELITE · LIQUID GLASS UI）。

### 产物

- `DioxideLite-v1.8.1.jar` / `DioxideLite-v1.8.1-sources.jar`

---

## v1.8 (Native Render Rework) — 2026-09-11

> 目标环境：Minecraft 1.21.11 · Fabric Loader 0.18.4
> 定位：**ClickGUI 底层渲染重构**——彻底移除 Skia + OpenGL 渲染路径，三套主题全部改为 Minecraft 原生 `GuiGraphics` 渲染

### 背景

- v1.7.x 的 ClickGUI（Liquid Glass / Minimal）仍走 Skia + OpenGL：`BackendRenderTarget.makeGL` 直接包装 Minecraft 帧缓冲，硬编码 `framebuffer 0` 与单采样配置，在 Windows 上画进错误的帧缓冲导致 **ClickGUI 打开后一片空白（无报错）**，且每帧 Skia surface 提交 + 玻璃模糊 FBO 捕获造成性能损耗。

### 变更（渲染底层）

- **三主题全面原生化**：`NewSettingsScreen`（Liquid Glass）、`DioxideLiteMinimalClickGuiScreen`（Minimal）、`DioxideLiteSignatureClickGuiScreen`（Signature）均改为继承 `Screen`，使用 `GuiGraphics` 的 `fill` / `renderOutline` / `drawString`（MC 原生字体）绘制，与游戏主菜单（v1.7.3 起）同一条渲染路径。
- **移除 Skia 依赖**：ClickGUI 路径不再调用 `SkiaRenderer` / `SkiaGlBackend` / `SkiaBlurRenderer` / `LiquidGlassRenderer` / `FontRenderer`；删除 `SkiaScreen`，新增 `ClickGuiScreen` 标记接口供 HUD 渲染器 / Mixin 判断 ClickGUI 打开状态。
- **玻璃效果重做**：Liquid Glass 面板由"GPU FBO 模糊捕获"改为**半透明填充 + 细描边 + 顶部高光**（`DioxideLiteVisuals` 新增 `glassFast / cardFast / outlineFast / accentLineFast / dotFast`），保留玻璃观感，零 GPU 捕获。
- **图标退化文本**：MC 原生字体不包含 icon.ttf 字形，ClickGUI 标签图标改为纯文本（中文/英文标签），字体渲染统一为 MC 默认字体（与 Signature 主题 v1.7.3 做法一致）。
- **每帧提交消失**：不再有 Skia surface 提交、`flushAndSubmit`、CPU 像素回读、动态纹理上传；ClickGUI 渲染成本与普通 MC 界面一致。

### 修复

- **修复 Windows 上 ClickGUI 空白**：不再依赖 GL framebuffer 包装，渲染与平台无关（Windows / Linux / macOS 行为一致）。
- 修复 Minimal 屏幕渲染方法签名（5 参数手误）与 1.21.11 `Matrix3x2fStack` 2D pose API 适配。

### 兼容

- 设置模型 / 页面 / 控件（`BasePage.drawFast`、`SettingButton/Cycle/Slider/Toggle/Module.drawFast`）复用原生快速路径，主题切换逻辑不变（关闭重开 ClickGUI 即生效，或经 `ClickGuiThemeController` 热切换）。
- 游戏内 HUD / Liquid Glass 全局视觉模块（`LiquidGlassVisualSystem` 等）暂保持 Skia 实现，未受影响；如需彻底移除 skija 依赖可在后续版本跟进。

### 产物

- `DioxideLite-v1.8.jar` / `DioxideLite-v1.8-sources.jar`

---

## v1.7.4 (Setsuna Boot · Liquid Glass Default) — 2026-09-11

> 目标环境：Minecraft 1.21.11 · Fabric Loader 0.18.4
> 定位：v1.7.3 渲染重构的完善版——**Setsuna 启动流程** + **Liquid Glass 默认主题**，主菜单与 ClickGUI 全面原生渲染

### 新增

- **Setsuna 启动流程（boot flow）**：主菜单默认呈现 Setsuna 风格的进入动效——黑色/背景入场 → `DIOXIDELITE / SETSUNA UI` 字标 → `CLICK TO START` → 点击后分段过渡（中心 / 分割线 / 菜单）→ `SINGLE PLAYER / MULTI PLAYER` 菜单，全程原生 `GuiGraphics` 渲染，无 GLSL / Skia / FBO / 逐帧 CPU 回读。
- **主菜单顶部切换按钮**：`BACKGROUND: SETSUNA` / `VANILLA` 一键在 Setsuna 主界面与原版主界面之间切换。
- **ClickGUI 默认主题回归 Liquid Glass**：`Config.clickGuiTheme` 默认值回到 `ORIGINAL`（Liquid Glass `NewSettingsScreen`），开箱即用液态玻璃界面。
- **KeyInputHandler 免确认门**：Right Shift 直接打开当前所选主题的 ClickGUI，不再被 `TermsScreen` 拦截（协议页仍保留显式入口）。

### 变更

- **Signature 主题渲染重构**：径向 → 检查器（radial-to-inspector）界面改为原生 `GuiGraphics` 快速渲染路径，不再使用 Skia / GPU framebuffer 包装 / 离屏纹理 / 模糊捕获；模块与设置逻辑完全复用。
- **主菜单渲染重构**：移除 GLSL 星云动画及其 shader 资源，改为原生渲染；保留 PVPUtils 风格自定义 PNG 背景（`DioxideLite/backgrounds`）；鼠标视差为可选且仅使用轻量坐标插值。
- **三主题热切换**：Theme 页 `ClickGUI Theme` 循环控件通过 `ClickGuiThemeController.apply` **即时重建界面**，切换后无需重开 ClickGUI 即生效。
- **Liquid Glass 全局视觉默认**：`liquidGlassAllVisuals` 保留一键全局玻璃（HUD / 快捷栏 / 聊天 / 容器界面 / 界面卡片，主菜单除外）。

### 修复

- 修复 `RenderPage` / `ThemePage` 结尾多余右括号导致的编译失败（历史遗留同款问题）。
- 修复 `SkiaBlurRenderer` 缺失 `restoreReadBuffer` / `restoreDrawBuffer` 实现导致的编译失败。
- 修复 `AbstractContainerScreenGlassMixin` 注入点在 1.21.11 下不可用的问题（`renderBg` TAIL → `renderSlots` HEAD），消除容器玻璃在渲染重做后的崩溃风险。

### 产物

- `DioxideLite-v1.7.4.jar` / `DioxideLite-v1.7.4-sources.jar`

---

## v1.7.3 (Render Rework) — 2026-09-10（前序基线，包含于 v1.7.4）

> 说明：v1.7.4 源码基于 v1.7.3 渲染重构，以下为 v1.7.3 的核心变更（v1.7.4 已继承并完善）。

### 变更

- **主菜单原生化**：移除主菜单 GLSL 动画系统与 shader 资源，改为 Minecraft 原生 `GuiGraphics` 路径；保留 Setsuna 信息层级（`CLICK TO START` / `SINGLE PLAYER` / `MULTI PLAYER`），新增主菜单 `VANILLA` 直切按钮。
- **Signature ClickGUI 原生化**：不再使用 Skia、GPU framebuffer 包装、模糊捕获与离屏纹理；新增原生 `GuiGraphics` 快速渲染路径；右键 / ESC 导航与滑块拖拽保持支持。
- **ClickGUI 访问修复**：旧 keybind 流程在 `termsRead=false` 时可能停在 `TermsScreen`，现改为 Right Shift 直接打开所选主题 ClickGUI；协议页保留显式入口。
- **性能目标**：新主菜单与 Signature ClickGUI 不再进行逐帧 CPU 回读、动态纹理上传、Skia surface 提交或模糊捕获；剩余 Liquid Glass 渲染器仅用于显式请求的界面。

---

## v1.7.2 (Setsuna Theme Rework) — 2026-09-10

> 目标环境：Minecraft 1.21.11 · Fabric Loader 0.18.4
> 定位：v1.7.1 渲染修复 + Setsuna 主题体系（主题热切换回归）

### 新增

- **Setsuna 主菜单**：主标题 `SETSUNA` 字标；右上角 `DioxideLite · SETSUNA` 按钮一键在 Setsuna 主界面与原版主界面之间切换（`useMainUI` 默认开启）。
- **主题系统回归**：恢复 `ClickGuiThemeController` 统一路由，三套 ClickGUI 主题（原版 / Minimal / Signature）**关闭后重新打开 ClickGUI 即按新主题渲染，无需重启游戏**。
- **Signature 主题重做**：径向分类节点 → 平滑形变为设置工作区（radial-to-inspector）；负空间、细轮廓、紧凑排版、青色强调；复用 DioxideLite 自身页面/设置模型，与其他主题共享模块与设置状态。
- **Visual Style 扩展**：`Liquid Glass / Aurora / RISE Clean / Minimal / Signature` 五档可选。

### 变更

- **默认主题**：`Config.clickGuiTheme` 默认值 `ORIGINAL` → `SIGNATURE`；重置后同样默认 Signature 主题。
- **主菜单默认启用**：`useMainUI` 默认 `true`（继承 v1.7.1）。

### 修复

- 修复 `RenderPage` / `ThemePage` 结尾多余右括号导致的编译失败（与 v1.7 相同的遗留问题）。
- 完整继承 v1.7.1 渲染修复：Skia 帧绘制 GL 后端、模糊与 Skia 上下文分离（预算 8→4）、主界面 Shader 路径简化。

### 产物

- `DioxideLite-v1.7.2.jar` / `DioxideLite-v1.7.2-sources.jar`

---

## v1.7.1 (Rendering Fix) — 2026-09-10

> 目标环境：Minecraft 1.21.11 · Fabric Loader 0.18.4
> 分支定位：纯渲染修复分支（不含 ClickGUI 主题系统）

### 修复

- **Skia 帧绘制切换 GL 后端**：`SkiaRenderer.USE_GL_BACKEND_FOR_FRAME = true`，帧绘制统一走 GL 后端，消除 Surface/Texture 双轨绘制导致的画面撕裂与纹理残留。
- **模糊与 Skia 绘制上下文分离**：Liquid Glass 模糊 pass 在 Skia overlay 之前完成合成，避免在同一 framebuffer 嵌套两个 Skia GL 上下文；模糊渲染器复用单张 GPU 捕获纹理，不再为每个控件分配纹理/FBO。
- **模糊预算下调**：`blurBudget` 8 → 4，缓解高密度控件列表的模糊开销。
- **主界面 Shader 路径简化**：`MainUIShader` 移除 Framebuffer/NativeImage/DynamicTexture 多级回读，降低 GL 兼容性门槛。

### 变更

- **自定义主界面默认开启**：`Config.useMainUI` 默认 `true`，无需手动开关。
- **主题路由回退**：移除 `ClickGuiThemeController` / Signature 屏幕，ClickGUI 回归单主题布局（`MINIMAL_POP / SIGNATURE` 配置暂走 Minimal 屏）。**三主题热切换将于 v1.7.2（Setsuna Theme Rework）回归并升级**。

### 影响

- 修复 Liquid Glass 在部分 GL 环境下的崩溃/渲染错乱风险；
- 降低渲染内存与带宽占用；
- 本分支不包含 ClickGUI 主题系统，如需要主题切换请使用 v1.7.2。

---

## v1.7 (ClickGUI Theme Rework) — 2026-09-10

> 目标环境：Minecraft 1.21.11 · Fabric Loader 0.19.5+
> 主题文档：`CLICKGUI_THEMES.md`

### 新增

- **主题热切换**：新增 `ClickGuiThemeController` 统一主题路由，切换 ClickGUI 主题**不再需要重启游戏**——关闭后重新打开 ClickGUI 即按新主题渲染；打开状态下切换也会立即生效。
- **Signature ClickGUI 主题**：独立实现的径向/分类式界面——分类菜单、分段面板过渡、hover 插值、滑入动画、青色强调（`#3ED6B4`）。
- **主题文档**：`CLICKGUI_THEMES.md`，说明三套主题的定位与共享机制。

### 主题

- `ORIGINAL`：Liquid Glass 原版设置布局（保留为原始主题）。
- `MINIMAL_POP`：紧凑 Minimal 呈现。
- `SIGNATURE`：独立参考风格（径向/分类式）。
- 三套主题共享同一套 `BasePage` / 设置组件——**切换主题只改变界面表现，不改变任何功能逻辑**，模块与设置状态完全共享。

### 操作方式

1. 游戏内按 **RSHIFT** 打开 ClickGUI；
2. 进入 **Theme** 页；
3. 右键展开 **ClickGUI Theme** 模块，点击 **Layout** 子项的循环控件切换主题；
4. 关闭并重新打开 ClickGUI（或直接查看），新主题已生效，**无需重启游戏**。

### 修复

- 修复 `RenderPage` / `ThemePage` 结尾多余右括号导致的编译失败。
- 修复 `AbstractContainerScreenGlassMixin` 注入点在 1.21.11 下不可用的问题（`renderBg` TAIL → `renderSlots` HEAD），消除容器玻璃崩溃风险。
- 修复 `Minecraft.frag.glsl` 中 `#extension` 指令位置非法导致的 GLSL 编译错误。
- 修复 `DioxideLiteSignatureClickGuiScreen` 引用不存在的 `SkiaUi` 类、缺失 `SettingModule` import 的编译错误。
- 修复 `DioxideLiteMainUI` 中 6 处 `drawRect` 重载调用与 `MenuLayout` 参数缺失问题。

---

## 历史版本

### v1.7 — 2026-09-10（前序基线）

- 新增 **SIGNATURE 视觉模板**（`VisualStyle` 与 `ClickGuiTheme` 均有）。
- 新增 **Signature HUD** 模块。
- 新增 **`liquidGlassAllVisuals` 全局玻璃**：一键将液态玻璃材质应用到游戏内 HUD、快捷栏、聊天、物品栏容器与界面卡片（含 3 个玻璃 Mixin），主菜单除外。

### v1.6 — 2026-09-09（初始基线）

- 自定义主界面（GLSL 星云背景 + CLICK TO START 交互）。
- Liquid Glass 视觉体系（Skia 渲染的玻璃材质 UI）。
- ClickGUI 设置界面（Combat / Render / Tools / Theme / Optimize / Misc 分类页）。
- Dynamic Island 顶部 HUD。
- 首个可运行版本。
