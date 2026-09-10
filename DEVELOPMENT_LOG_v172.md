# DioxideLite v1.7.2 Setsuna Theme Rework — 开发日志

> 版本：v1.7.2（Setsuna Theme Rework）
> 目标环境：Minecraft 1.21.11 + Fabric Loader 0.18.4
> 开发周期：2026-09-10
> 定位：在 v1.7.1 渲染修复的基础上，回归并升级主题系统——新增 **Setsuna 主菜单**，恢复三套 ClickGUI 主题热切换，默认主题改为 **Signature**。

---

## 一、背景与目标

v1.7.1 为纯渲染修复分支，临时移除了 ClickGUI 主题系统；v1.6/v1.7 的"三主题热切换"能力需要回归，并且这次要**整体升级为 Setsuna 视觉体系**：

1. **Setsuna 主菜单**：主菜单改为 Setsuna 风格呈现，标题与入口按钮统一标注 `SETSUNA`；
2. **主题系统回归**：恢复 `ClickGuiThemeController` 统一路由，三套主题（原版 / Minimal / Signature）**关闭 ClickGUI 后重开即按新主题渲染**，无需重启游戏；
3. **Signature 主题重做**：径向分类菜单升级为"径向 → 检查器"交互——负空间、细轮廓、紧凑排版；
4. **默认主题调整**：默认 `clickGuiTheme = SIGNATURE`（此前为 ORIGINAL）；
5. **完整继承 v1.7.1 渲染修复**：Skia GL 帧后端、模糊上下文分离、Shader 路径简化全部保留。

---

## 二、核心变更

### 2.1 Setsuna 主菜单

`DioxideLiteMainUI.java` / `MainUIScreenManager.java`：

- 主标题绘制 `SETSUNA` 字标（保留背景 GLSL 星云）；
- 原版主菜单切换按钮文案改为 `DioxideLite · SETSUNA`，右上角常驻入口，点击可在 Setsuna 主界面与原版主界面之间切换；
- 设置面板保留原有背景切换设置（`useMainUI` 默认开启，v1.7.1 起生效）。

### 2.2 主题路由恢复：`ClickGuiThemeController`

```java
public static Screen create(Config.ClickGuiTheme theme, Screen parent) {
    return switch (theme) {
        case ORIGINAL    -> new NewSettingsScreen(parent);
        case MINIMAL_POP -> new DioxideLiteMinimalClickGuiScreen(parent);
        case SIGNATURE   -> new DioxideLiteSignatureClickGuiScreen(parent);
    };
}
```

- `KeyInputHandler` 打开 ClickGUI 一律走 `ClickGuiThemeController.create(Config.clickGuiTheme, null)`；
- ThemePage 的 Layout 循环控件切换主题后写 `Config` 并落盘，**关闭并重新打开 ClickGUI 即按新主题渲染，无需重启游戏**。

### 2.3 Signature ClickGUI 主题重做

`DioxideLiteSignatureClickGuiScreen.java` 重写为独立实现：

- **交互范式**：径向分类节点 → 平滑形变为设置工作区（radial-to-inspector）；
- **视觉语言**：大面积负空间、细轮廓（hairline）、紧凑排版、青色强调（`#3ED6B4` 体系）；
- **页面模型**：复用 DioxideLite 自身的 `BasePage` / 设置组件（Combat / Render / Theme / Misc / Optimize / Tool），与其它主题共享全部模块与设置状态；
- 不捆绑任何参考客户端的素材与源码，为参考驱动的原创实现。

### 2.4 主题与视觉模板配置

- `Config.clickGuiTheme` 默认值：`ORIGINAL` → `SIGNATURE`；
- ThemePage 的 Visual Style 列表扩展为：`Liquid Glass / Aurora / RISE Clean / Minimal / Signature`；
- `ResetManager` 重置后默认 `SIGNATURE` 主题、`useMainUI = true`。

### 2.5 编译修复（本仓库构建时处理）

与 v1.7 相同的两个历史遗留语法错误在本分支源码中依然存在，构建时修复：

- `RenderPage.java`：环境粒子 `SettingToggle` 结尾多余右括号（`))))` → `)))`）；
- `ThemePage.java`：Layout 循环控件结尾多余右括号（`))))` → `)))`）。

---

## 三、继承自 v1.7.1 的渲染修复（本版本包含）

- `SkiaRenderer`：`USE_GL_BACKEND_FOR_FRAME = true`，帧绘制统一走 GL 后端；
- `LiquidGlassVisualSystem`：模糊预算 8→4，blur 在 Skia overlay 之前合成，复用单张 GPU 捕获纹理；
- `MainUIShader`：移除 Framebuffer/NativeImage/DynamicTexture 多级回读；
- 三个 Mixin 与 `Minecraft.frag.glsl` 的 1.21.11 适配保持。

---

## 四、验证

- 构建：Minecraft 1.21.11 · Fabric Loader 0.18.4 · Fabric API 0.141.3 · Java 21（Gradle 9.3 / Loom 1.15-SNAPSHOT）——`BUILD SUCCESSFUL`；
- 主题热切换行为：ThemePage Layout 循环切换 → Config 落盘 → 重开 ClickGUI 按新主题渲染；
- 产物：`build/libs/DioxideLite-v1.7.2.jar` 与 `-sources.jar`。

---

## 五、涉及文件清单

| 类别 | 文件 |
| :--- | :--- |
| 主题路由 | `ClickGuiThemeController.java`（恢复） |
| Signature 界面 | `DioxideLiteSignatureClickGuiScreen.java`（重写） |
| 主菜单 | `DioxideLiteMainUI.java`、`MainUIScreenManager.java` |
| 页面 | `ThemePage.java`（三主题 UI + Visual Style 扩展）、`TermsScreen.java` |
| 配置 | `Config.java`（默认 SIGNATURE）、`ResetManager.java`、`Version.java`（1.7.2） |
| 输入 | `KeyInputHandler.java`（走主题路由） |
| 渲染（继承 1.7.1） | `SkiaRenderer`、`LiquidGlassVisualSystem`、`MainUIShader`、三个 Mixin、`Minecraft.frag.glsl` |
