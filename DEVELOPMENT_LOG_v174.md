# DioxideLite v1.7.4 Setsuna Boot · Liquid Glass Default — 开发日志

> 版本：v1.7.4（Setsuna Boot · Liquid Glass Default）
> 目标环境：Minecraft 1.21.11 + Fabric Loader 0.18.4
> 开发周期：2026-09-11
> 定位：v1.7.3 渲染重构的完善发布——**Setsuna 启动流程**与 **Liquid Glass 默认主题**双落地，主菜单与 ClickGUI 全面原生渲染，性能与兼容性再上台阶。

---

## 一、背景与目标

v1.7.3 完成了主菜单与 Signature ClickGUI 的原生化（移除 GLSL / Skia / FBO / 逐帧 CPU 回读）。v1.7.4 在此基础上解决两个产品层面的遗留问题：

1. **启动即视感**：主菜单要有一个完整、可感知的 Setsuna 风格"启动流程"——从黑场/背景入场，到 `CLICK TO START` 点击过渡，再到 `SINGLE PLAYER / MULTI PLAYER` 菜单，而不是一次全部呈现；
2. **开箱即用的玻璃**：ClickGUI 默认主题回到 **Liquid Glass（ORIGINAL）**，用户装上进游戏按 RSHIFT 看到的就是液态玻璃设置界面；
3. **流程无门槛**：Right Shift 直接打开当前主题 ClickGUI，不再被协议页（TermsScreen）挡住。

---

## 二、核心变更

### 2.1 Setsuna 启动流程（boot flow）

`DioxideLiteMainUI.java` / `MainUIScreenManager.java`：

- 入场动效：黑色/背景 → `DIOXIDELITE / SETSUNA UI` 字标淡入 → 白色方形图标 → `CLICK TO START`；
- 点击后分段过渡：中心标记 / 分割线 / 菜单项按节奏展开，最终呈现 `SINGLE PLAYER`（左）/ `MULTI PLAYER`（右）+ `OPTIONS` / `ESC EXIT`；
- 全程 Minecraft 原生 `GuiGraphics`：无 GLSL shader、无 Skia surface、无 framebuffer、无逐帧 CPU 纹理上传；
- 顶部常驻 `BACKGROUND: SETSUNA` / `VANILLA` 一键切换，原版主界面同样获得 `DioxideLite · SETSUNA` 回流入口；
- PVPUtils 风格自定义 PNG 背景继续可用（`DioxideLite/backgrounds`），鼠标视差为可选轻量坐标插值。

### 2.2 ClickGUI 默认主题：Liquid Glass

`Config.java`：

- `clickGuiTheme` 默认值回归 `ORIGINAL` → 路由到 Liquid Glass `NewSettingsScreen`；
- 液态玻璃材质（模糊 / 透明度 / 边缘高光 / 圆角 / 性能模式）全部保留；
- Theme 页 `ClickGUI Theme` 循环控件通过 `ClickGuiThemeController.apply` **即时重建界面**——切换主题后无需关闭重开 ClickGUI，所见即所得。

### 2.3 Signature 主题原生渲染

`DioxideLiteSignatureClickGuiScreen.java`：

- 径向分类节点 → 设置工作区（radial-to-inspector）交互保留；
- 渲染路径替换为原生 `GuiGraphics` 快速路径：模块卡片与设置控件不再走 Skia / GL framebuffer / 模糊捕获 / 离屏纹理；
- 右键 / ESC 导航、滑块拖拽、滚动全部保持；
- 主题路由统一在 `ClickGuiThemeController`，三主题共享同一套 `BasePage` / 设置组件。

### 2.4 ClickGUI 访问免门槛

`KeyInputHandler.java`：

- Right Shift 直接 `ClickGuiThemeController.create(Config.clickGuiTheme, null)` 打开所选主题 ClickGUI；
- 移除 `termsRead` 硬门（协议页保留显式入口，供需要时查看）；
- 游戏语言默认按游戏语言自动应用（`applyGameLanguageDefault`）。

---

## 三、构建与调试记录

目标环境：Minecraft 1.21.11 · Fabric Loader 0.18.4 · JDK 21 · Loom 1.15-SNAPSHOT。

### 修复的构建错误（3 处，均在提交前修复）

| 文件 | 问题 | 修复 |
| :--- | :--- | :--- |
| `RenderPage.java` | 结尾多一个右括号，编译失败 | 移除多余 `)` |
| `ThemePage.java` | 结尾多一个右括号，编译失败 | 移除多余 `)` |
| `SkiaBlurRenderer.java` | 引用不存在的 `restoreReadBuffer` / `restoreDrawBuffer` | 补全两个 GL 状态恢复方法（`glBindFramebuffer` + `glReadBuffer` / `glDrawBuffer`） |

### 运行时问题与修复

- **容器玻璃 Mixin 崩溃**：`AbstractContainerScreenGlassMixin` 在 v1.7.3 渲染重做后回到 `renderBg` TAIL 注入点，1.21.11 下无法定位 RETURN，启动即崩 → 注入点改回 `renderSlots` HEAD（与 v1.7.2 修复一致），容器玻璃在渲染重做后恢复稳定。
- **软渲染环境验证**：在 llvmpipe（无 GPU）环境下完成主界面 / 进世界 / ClickGUI / 三主题切换 / Liquid Glass 全局视觉全链路实机验证，均正常渲染。

---

## 四、实机验证（Minecraft 1.21.11 Fabric）

以下流程均在本机实测通过：

1. 启动 → Setsuna 启动界面（`CLICK TO START`）→ 点击进入主菜单；
2. 主菜单 `BACKGROUND: SETSUNA` / `VANILLA` 切换正常；
3. `SINGLE PLAYER` → 世界列表 → 进世界，HUD 正常（DioxideLite 信息栏）；
4. RSHIFT 打开 ClickGUI：默认 Liquid Glass（ORIGINAL）主题；
5. Theme 页展开 `ClickGUI Theme` → 切换 `MINIMAL_POP` / `SIGNATURE` → 界面即时重建生效；
6. `Liquid Glass Global Visuals` 开关 → 游戏内 HUD / 快捷栏玻璃即时生效。

---

## 五、产物

- `DioxideLite-v1.7.4.jar`（约 26.3 MB）
- `DioxideLite-v1.7.4-sources.jar`（约 13.4 MB）
- 更新日志：`CHANGELOG.md`（含 v1.7.4 / v1.7.3 / v1.7.2 / v1.7.1 / v1.7 / v1.6 历史章节）

---

## 六、合规与许可

- 继续遵循上游 PVPUtils 开源许可，`LICENSE` 与 `THIRD_PARTY_NOTICES.md` 完整保留；
- 客户端不包含 IRC 登录、账号认证、License Key 验证或启动授权服务；
- 仅实现合法客户端侧视觉 / QoL 功能，无战斗自动化、瞄准辅助、反作弊绕过等作弊逻辑。
