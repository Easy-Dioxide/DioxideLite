# DioxideLite v1.8.1 开发日志

**版本**：DioxideLite v1.8.1（Minecraft 1.21.11 · Fabric Loader 0.18.4）
**定位**：Skia 渲染回归（Setsuna 优化实现）+ 字体补丁 + GLASS 玻璃主题 + Minimal 布局重写

---

## 一、版本背景

v1.8 做了全面原生化（ClickGUI 移除 Skia），解决了 Windows 上 ClickGUI 空白与性能问题，但玻璃质感（真实背景模糊、半透明材质）也随之弱化——用户希望在保留性能的同时找回玻璃观感。

v1.8.1 用户源码重新引入 Skia，但**实现方式与 v1.7 不同**（用户以 Setsuna 客户端为蓝本做了优化）：
- **GL 直绘帧缓冲**：ClickGUI 直接向 Minecraft 当前帧缓冲绘制（`USE_GL_BACKEND_FOR_FRAME = true`），不再经 CPU surface 上传；
- **玻璃模糊降采样 + 节流**：`SkiaBlurRenderer` 以 0.50×（性能模式 0.38×）分辨率、30Hz / 20Hz 间隔捕获背景，避免每帧全分辨率 FBO 捕获；
- **字体补丁**：`FontRenderer` 把字形栅格化委托给 Minecraft / Blaze3D，规避自绘字体与渲染优化 mod 的冲突。

这些正是用户声称"不会再像 v1.7 那样打开 ClickGUI / 渲染动画时帧率≈0"的依据——代码层面成立（消除了每帧 surface 提交与全分辨率模糊捕获两个最大开销）。

---

## 二、本版本新增与修复

### 1. GLASS 主题（恢复 1.7 纯透明玻璃 ClickGUI）

用户源码的 `ClickGuiTheme` 只有 ORIGINAL / MINIMAL_POP / SIGNATURE 三枚，而 v1.8.1 在 GL 后端下 ORIGINAL 面板实际是**深色半透明**（`SkiaRenderer.isDrawing()` 为 true 时走 `glassBase` 深色填充，跳过 `LiquidGlassRenderer.drawSurface` 玻璃材质）。

按用户要求"恢复 1.7 版本那个 clickgui 有纯透明玻璃效果的 clickgui 作为新的主题"：

- `Config.ClickGuiTheme` 新增 `GLASS` 枚举；
- 新增 `GlassThemeScreen extends NewSettingsScreen`，覆盖 `usePureGlass() = true`；
- `NewSettingsScreen` 抽出 `drawGlass()` 钩子：`usePureGlass() || !SkiaRenderer.isDrawing()` 时走 `LiquidGlassRenderer.drawSurface`（真正的玻璃材质：背景模糊 + 高透明填充 + 折射内层 + 高光描边），否则维持深色面板；
- `ClickGuiThemeController` 注册 `GLASS` 路由；主题页 Cycle 控件扩为 4 选项（含 Glass），支持运行中热切换。

### 2. Minimal 主题独立紧凑布局（修复"全屏不好操作"）

用户源码的 `DioxideLiteMinimalClickGuiScreen` 只是 `extends NewSettingsScreen` 的 8 行空壳——打开后与 ORIGINAL 完全一样（740×500 居中卡片），全屏下内容区小、操作不便。

重写为独立屏幕（继承 `SkiaScreen`，与 v1.8.1 的 Skia 体系一致）：
- 900×560 自适应卡片，大屏最高 1.15× 放大（`scale = clamp(min(w/W, h/H), 0.60, 1.15)`）；
- 左侧六页导航（战斗/视觉/工具/主题/优化/其他）+ 宽内容区；
- 滚轮滚动 + 滚动条拖拽 + 内容区拖拽（模块滑块）；
- 关闭按钮、打开/关闭动画、hover 反馈；
- 纯深色面板（无模糊），渲染开销最低。

### 3. ClickGUI 内帧率显示（等效 F3）

用户要求"打开 F3 的时候打开 clickgui，方便查看渲染帧率"。由于 ClickGUI 是全屏 Screen（F3 调试屏会被覆盖），在 `NewSettingsScreen` / `DioxideLiteMinimalClickGuiScreen` 的右上角绘制实时 `FPS` 数字（`Minecraft.getInstance().getFps()`），打开 ClickGUI 即可看到帧率，等效 F3 左上角帧率查看。

---

## 三、构建与修复记录（重要）

源码解压后**首次构建即报 3 处编译错误**，随后运行期又暴露 1 处 mixin 崩溃与 1 处 skija 运行期问题，全部定位并修复：

### 编译期（用户源码 bug）

| 文件 | 问题 | 修复 |
|---|---|---|
| `RenderPage.java:51` | `.addSub(...)` 链尾括号失配（多 2 个 `)`、丢语句分号） | 修正为 `})))` + `;` |
| `ThemePage.java:24` | 同上括号失配；主题 Cycle 只有 3 项 | 括号修正；Cycle 扩为 4 项并接入 GLASS 映射 |
| `HudEditOverlay.java:19` | switch 箭头 case 后直接接语句/`return`（非法） | 全部改为 block case（`case X -> { ... }`） |
| `SkiaBlurRenderer.java:170-171` | 调用未定义的 `restoreReadBuffer` / `restoreDrawBuffer` | 补齐两个 GL 状态恢复方法 |
| `TargetHudRenderer.java:7` | `PlayerSkin` import 错误包名 | 改为 `net.minecraft.world.entity.player.PlayerSkin` |
| `TargetHudRenderer.java:21` | 引用不存在的 `PlayerFaceRenderer`（缺 import） | 补 `net.minecraft.client.gui.components.PlayerFaceRenderer` |
| `ArmorHudRenderer.java:8` | 1.21.11 已无 `Inventory.armor` 字段 | 改用 `getItemBySlot(EquipmentSlot)`（boots→helmet 顺序保持） |
| `FontRenderer.java:143` | `drawString` 浮点坐标不匹配 1.21.11 签名 | 改为整数坐标 `1, 1` |

### 运行期（mixin 崩溃）

| 问题 | 现象 | 修复 |
|---|---|---|
| `AbstractContainerScreenGlassMixin` | 游戏启动即崩：`TAIL could not locate a valid RETURN in renderBg` | 注入点改为 `renderSlots` HEAD（与 v1.8 一致） |

### 验证环境限制（重要告知）

本构建/截图验证运行在**无头 Linux 容器（Xvfb + llvmpipe 软件渲染）**。该环境中 **skija 0.143.x native 库在加载后初始化阶段必然 SIGSEGV**（`Library._nAfterLoad → jni_GetMethodID` 空引用；已穷尽 JDK 11 / 21.0.5 / 21.0.12、skija 0.109.1 / 0.143.14 / 0.143.16 / 0.143.17、四种 native 加载方式，均复现），因此：

- **主菜单（Setsuna）、世界列表、游戏内画面**：正常渲染，已截图（FPS 3 为 llvmpipe 软件渲染无 GPU 所致）；
- **ClickGUI（Skia 渲染）**：在本无头环境无法启动（native 崩溃）；**在用户 Windows 环境（内嵌 skija-windows-x64 native）不受影响**——用户已实测 v1.8.1 帧率正常；
- ClickGUI 四主题的视觉验证请在 Windows 上进行（构建产物内置 Windows native）。

---

## 四、验证方式与结果

| 项 | 方式 | 结果 |
|---|---|---|
| 编译 | `./gradlew build -x test`（JDK 21.0.12） | ✅ BUILD SUCCESSFUL |
| 产物 | `build/libs/DioxideLite-v1.8.1.jar`（22.4MB）+ sources | ✅ 已生成 |
| mixin 启动 | Fabric 启动进主菜单/世界 | ✅ 无 mixin 崩溃 |
| 主菜单 | Setsuna 启动屏 + 主菜单（v1.8.1 版本号） | ✅ 截图 |
| 游戏内 | 世界加载 + HUD（生命/饥饿/物品栏） | ✅ 截图 |
| ClickGUI | 本环境 skija 崩溃（见上） | ⚠️ 需 Windows 验证 |

**仍未覆盖**：ClickGUI 四主题的实机截图与帧率数字（等待用户 Windows 环境；ClickGUI 内已内置 FPS 显示，打开即可看到帧率）。

---

## 五、下一步建议

- Windows 上验证四主题视觉与帧率，确认"打开 ClickGUI 帧率不再≈0"；
- 若需彻底摆脱验证环境限制，可考虑在 ClickGUI 构造前预加载 skija native 并捕获 `Throwable` 降级到原生渲染（v1.8 的原生路径仍保留参考价值）；
- 字体资源包 builder 警告（`dioxide-lite:fonts/*.ttf` 路径）不影响运行，可后续把 `fonts/` 目录移入 `assets/dioxide-lite/` 消除。
