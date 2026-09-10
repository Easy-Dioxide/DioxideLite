# DioxideLite 更新日志

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
