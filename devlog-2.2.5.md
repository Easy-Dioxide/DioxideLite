# DioxideLite v2.2.5 Devlog

**发布日期：2026-10-05**

---

## v2.2.5 更新内容

### 补全渲染模块真实实现（修复 v2.2.4 空壳）

v2.2.4 中 `Animations / FogBlur / PostProcessing / Skybox` 四个渲染模块仅有完整设置界面、没有任何实际效果（空壳）。本次全部接入 MC 26.1.2 渲染管线，功能真实生效：

- **Animations（渲染）**：接入 `ItemInHandRenderer`。
  - `Block = DIOXIDE`：强制剑使用 BLOCK 格挡姿态，并在 `applyItemArmTransform` 之后叠加「举高 + 随挥动扫过」变换（source 语义："raises the sword higher and lets it sweep along with the swing"）。
  - `Swing = DIOXIDE`：`@ModifyArg` 注入 `swingArm`，把物品晃动驱动从挥动进度（swing progress）改为装备进度（equip progress = 1 - inverseArmHeight）。
- **Skybox（渲染）**：接入 `SkyRenderer.renderSkyDisc`。新增 `animatedSkyColor()`，按 Preset 驱动动画天空色：
  - CLOUDS：基底色上轻微亮度波动（云流动感）；
  - THUNDER：整体压暗 + 按 Strike Interval 周期性闪电闪亮（Strike Glow 控制强度）；
  - PULSAR：亮度随 Animation Speed 脉动。
- **PostProcessing（渲染）**：接入 `GameRenderer` 世界渲染后处理阶段（`doEntityOutline` 之后、GUI 之前）。
  - `Blur`：应用原版 blur 后处理链（`processBlurEffect`），`blurRadius 1..20` → 处理 1..3 次，越大越糊；
  - `Bloom`：在 Blur 基础上再叠加一次模糊，得到柔和泛光感。
- **FogBlur（渲染）**：启用即对主渲染目标应用 blur 后处理链，实现整体雾状模糊（Distance / Fade 作为强度参考）。

> 说明：来源端的精确实现（深度纹理距离衰减 + 自定义 shader 合成 / 球带网格天空着色器）依赖来源客户端特定版本的底层渲染 hook，在 26.1.2 的 FrameGraph 渲染系统中无对应暴露入口，故以上采用「有界但真实」的实现路径——效果真实可见，且不侵入原版 FrameGraph pass。

### 全平台依赖
- 本次构建含 **Windows / macOS / Linux 全部 Skija 与 WebRTC 原生库**（`-Pskija_platforms` + `-Pwebrtc_platforms` 全平台参数）。
- 修复 v2.2.4 早期发布包仅含 Linux 原生库、导致 Windows 用户无法启动的问题；Windows 用户下载本版本可正常运行。

---

## 验证

- 构建成功（JDK 25.0.4.1 + Gradle 9.2.1 + Fabric Loom 1.15.5）
- 产物 `build/libs/DioxideLite-2.2.5.jar`（约 94 MB，全平台原生库）
- 游戏启动正常（Minecraft 26.1.2 Fabric），窗口标题 `DioxideLite 2.2.5`，主菜单 / 欢迎界面正常渲染
