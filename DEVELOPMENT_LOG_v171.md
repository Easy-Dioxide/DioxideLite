# DioxideLite v1.7.1 渲染修复 — 开发日志

> 版本：v1.7.1（Rendering Fix）
> 目标环境：Minecraft 1.21.11 + Fabric Loader 0.18.4
> 开发周期：2026-09-10
> 分支定位：**纯渲染修复分支**（不含 ClickGUI 主题系统，主题系统于 v1.7.2 回归）

---

## 一、背景与目标

v1.7 在实机验证过程中暴露出一组**渲染层问题**，集中在 Skia 绘制后端与 Liquid Glass 模糊系统的稳定性与性能上：

1. **Skia 帧绘制后端不稳**：帧（Frame）渲染走 Surface/Texture 捕获路径，在部分 GL 环境下出现画面撕裂、纹理残留；
2. **模糊与 Skia 双上下文冲突**：Liquid Glass 的模糊 pass 与 Skia overlay 各自开启 GL 上下文，嵌套到同一 framebuffer 时存在崩溃/渲染错乱风险；
3. **模糊预算偏高**：每帧最多 8 个模糊槽位、且每个控件独立分配捕获纹理/FBO，内存与带宽开销大；
4. **主界面 Shader 路径复杂**：`MainUIShader` 依赖 Framebuffer + NativeImage + DynamicTexture 多级回读，兼容性差；
5. **自定义主界面默认关闭**：`useMainUI` 默认 `false`，用户需手动开启。

本次修复目标：**统一渲染后端、拆分模糊与 Skia 绘制上下文、简化 Shader 路径、默认启用自定义主界面**。

---

## 二、修复内容

### 2.1 Skia 帧绘制切换 GL 后端（核心修复）

`SkiaRenderer.java`：

```java
// 旧：Surface/Texture 捕获后端
private static final boolean USE_GL_BACKEND_FOR_FRAME = false;

// 新：统一走 GL 后端
private static final boolean USE_GL_BACKEND_FOR_FRAME = true;
```

- 帧绘制改为 `GL_BACKEND.begin()` / `clipRect` / `end()` 三段式，先裁剪到目标区域再平移绘制；
- `isReady()` 判定改为 `USE_GL_BACKEND_FOR_FRAME || (regionSurface != null && regionTexture != null)`；
- 消除 Surface/Texture 双轨并行带来的状态污染。

### 2.2 模糊与 Skia 绘制顺序分离

`LiquidGlassVisualSystem.java`：

- **绘制顺序调整**：模糊 pass 在 Skia overlay 开始绘制**之前**完成合成（`renderBlur → drawSurface` 两段分离），避免在同一个 framebuffer 上嵌套两个 Skia GL 上下文；
- **纹理复用**：模糊渲染器复用**单张 GPU 捕获纹理**，不再为每个控件分配独立纹理/FBO；
- **模糊预算下调**：`blurBudget` 由 `8` → `4`，配合性能模式控制大表面模糊、密集模块列表走缓存/分层卡片；
- 玻璃本体保留真实半透明填充（`withAlpha(0x101720, .34f * alpha)`），无模糊槽位时视觉不塌陷。

### 2.3 主界面 Shader 路径简化

`MainUIShader.java`：

- 移除 `Framebuffer` / `NativeImage` / `GpuTexture` / `DynamicTexture` / `ByteBuffer` 相关字段与多级回读实现；
- 移除按尺寸动态分配 FBO/纹理的 `ensureFramebuffer` 逻辑，`textureId` 复用计数简化；
- Shader 只保留核心合成路径，降低 GL 兼容性门槛。

### 2.4 自定义主界面默认开启

`Config.java`：

```java
// 旧：useMainUI 默认 false
// 新：useMainUI 默认 true
```

- 默认启用 DioxideLite 自定义主界面（星云 GLSL 背景），无需手动开关。

### 2.5 主题路由回退（分支定位）

- 移除 `ClickGuiThemeController` 与 `DioxideLiteSignatureClickGuiScreen`；
- `KeyInputHandler` 恢复内联判断：`MINIMAL_POP / SIGNATURE` 走 `DioxideLiteMinimalClickGuiScreen`，其余走 `NewSettingsScreen`；
- **注意**：v1.7.1 为渲染修复分支，不提供三主题热切换；该能力在 **v1.7.2（Setsuna Theme Rework）** 中回归并升级。

---

## 三、验证

- 环境：Minecraft 1.21.11 · Fabric Loader 0.18.4 · Java 21；
- 渲染修复涉及文件全部通过编译（`RenderPage` / `ThemePage` / `SkiaRenderer` / `LiquidGlassVisualSystem` / `MainUIShader` / 三个 Mixin / `Minecraft.frag.glsl` 等）；
- 模糊预算、绘制顺序、纹理复用等性能路径在源码层确认无回归。

---

## 四、涉及文件清单

| 类别 | 文件 |
| :--- | :--- |
| 渲染后端 | `SkiaRenderer.java`、`SkiaScreen.java`、`SkiaBlurRenderer.java`、`LiquidGlassRenderer.java` |
| 视觉系统 | `LiquidGlassVisualSystem.java` |
| 主界面 | `DioxideLiteMainUI.java`、`MainUIShader.java` |
| 配置 | `Config.java`（useMainUI 默认 true） |
| Mixin | `AbstractContainerScreenGlassMixin`、`ChatScreenMixin`、`ScreenGlassMixin` |
| Shader | `Minecraft.frag.glsl` |
| 界面 | `NewSettingsScreen`、`DioxideLiteMinimalClickGuiScreen`、`RenderPage`、`ThemePage`、`TermsScreen`、`KeyInputHandler` |
