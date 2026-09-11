# DioxideLite v1.7.5 — Native Liquid Glass + Performance Stack

## 目标

v1.7.5 不再使用 Skia/Skija 作为客户端 UI 后端，也不重新引入另一套重量级 CPU 光栅器。

视觉层恢复为 DioxideLite 原来的 Liquid Glass 语言：

- 透明、可透出游戏画面的玻璃材质
- 多层透明面、顶部高光、边缘描边
- Harmony 主字体
- Icon / Material Symbols Rounded 图标字体
- Hover / 开关 / 展开 / 页面切换动画
- ORIGINAL Liquid Glass、MINIMAL、SIGNATURE 热切换
- HUD / Target HUD / Potion / Armor / Keystrokes / Block Count / HUD Editor 保持原有模块入口

## 渲染策略

DioxideLite 自身只使用 Minecraft 1.21.11 的 GuiGraphics / Blaze3D 路径。

性能优化由成熟的外部客户端优化模组承担，而不是在 DioxideLite 内重复实现一套 OpenGL UI 引擎：

1. **Sodium**：负责世界渲染与渲染管线优化。
2. **ImmediatelyFast**：负责 immediate-mode、GUI/HUD、文字等批处理优化。
3. **DioxideLite**：负责视觉、动画、布局和交互，不重复维护第二套 GPU/CPU 光栅器。

fabric.mod.json 对 Sodium 与 ImmediatelyFast 使用 `suggests`，两者不是 DioxideLite 的硬依赖。

## 字体

旧版 Skia FontRenderer 已替换为 Minecraft 原生字体资源提供器：

- `assets/dioxide-lite/font/harmony.json`
- `assets/dioxide-lite/font/icon.json`
- `assets/dioxide-lite/font/material_symbols.json`

TTF 仍然来自项目原有资源，因此视觉字体不会退回 Minecraft 默认字体。

## Liquid Glass

v1.7.5 的 Liquid Glass 不再使用“整块不透明毛玻璃”作为视觉主体，而是使用低 alpha 多层材质：

- 底层透明阴影
- 玻璃面
- 顶部反光线
- 底部暗边
- 青蓝边缘高光
- 内容区域的低 alpha module cards

这样可以保持游戏场景透出，同时避免每帧屏幕捕获、CPU readback、Skia FBO 同步等高开销路径。

## 兼容性

推荐组合：Minecraft 1.21.11 + Fabric + Sodium + ImmediatelyFast。

DioxideLite 本身不强制绑定这两个优化模组，因此没有它们也可以运行，只是不会获得对应的外部渲染优化。
