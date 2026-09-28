# DioxideLite v2.2.2 Devlog

**发布日期：2026-09-28**
**Commit：30bb9c0**

---

## v2.2.2 更新内容

### 多平台移植
- **PlatformSupport 统一 OS 分发层**：此前客户端直接调用 `powershell.exe` / `explorer.exe` / JNA `Advapi32Util`，仅支持 Windows。现在所有 OS 相关行为（打开文件夹、读取 CPU 名称、桌面集成）统一通过 `PlatformSupport` 按平台分发。
- **支持平台**：Windows x64、Linux x64、macOS arm64（Apple Silicon）、macOS x64（Intel）——一个 jar 全平台通用。
- **零额外 native 依赖**：全部用 JDK API + OS 自带 helper 进程实现，macOS 上不再依赖 JNA-platform。
- **构建参数**：`-Pskija_platforms` 和 `-Pwebrtc_platforms` 可按需选择打包平台的 native 库，减小单平台 jar 体积。

### ClickGUI 双模式
- **两种 ClickGUI 风格**：
  - **LegacyStyle**（默认）：传统窗口式 ClickGUI
  - **Setsuna**：径向/轮盘式 ClickGUI
- **共享强调色**：`Accent` 颜色设置统一作用于所有客户端 UI 表面。
- **GUI Scale**：65%–125% 可调。
- **背景模糊**：Setsuna 模式下可调节背景模糊强度（0–10）。
- **Daylight Mode**：日间模式开关。

### Render 模块重组
- **RenderModuleRegistry**：统一注册入口，替代散落的模块注册。
- **大量渲染模块移入 `render/advanced/` 子包**：
  - 世界渲染：`ESP`、`Boxes`、`BedESP`、`HoleESP`、`Tracers`、`GlowESP`、`SkeletonESP`、`TargetESP`、`ContainerESP`、`SpawnerFinder`、`OreTracers`、`Xray`、`XraySectionCompilerHooks`
  - 视觉效果：`Chams`、`ChinaHat`、`Wings`、`CapeChanger`、`SkinChanger`、`Trails`、`Trajectories`、`JumpCircles`、`ItemPhysics`、`Particles`、`ParticleLimiter`
  - 相机/视角：`Camera`、`Freelook`、`Zoom`、`NoRender`、`FogBlur`、`FogRemove`、`Fullbright`、`SeeInvisibles`
  - 其他：`Crosshair`、`Skybox`、`Arrows`、`TNTTimer`、`Animations`、`Ambience`、`BlockOverlay`、`Hand`、`Hurtcam`

### 品牌清理
- 移除所有 `setsuna` 字样，统一为 DioxideLite 品牌。
- 窗口标题、任务栏图标、About 信息全部使用 DioxideLite。

### 其他
- **Tritium NCM**：`DeviceIdGenerator` 更新，设备 ID 生成逻辑优化。
- **语言文件**：`en_us.json` / `zh_cn.json` 更新，新增模块翻译。
- **Mixin 配置**：`dioxide-lite.mixins.json` 更新注册。
- **IRC 心跳**：客户端内置 5 分钟心跳保活，断线自动指数退避重连。

---

## 验证

- 构建成功（JDK 25 + Gradle 9.2.1 + Fabric Loom 1.15.5）
- 游戏启动正常（Minecraft 26.1.2 Fabric）
- ClickGUI 双模式可切换
- IRC 连接成功（`IRC Online · 1 online`）
- Halo 模块默认启用
- Onyx ArrayList 右对齐自适应
