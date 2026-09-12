# DioxideLite Changelog

## v2.0.1 (2026-09-12) — OPAI Dynamic Island

### 新增 / New
- **OPAI 风格灵动岛重构**：`DioxideDynamicIsland` 全新实现——暗色玻璃胶囊、白色内边描边、顶部光泽与蓝青辉光，顶部居中平滑浮动画（正弦微浮动效）。
- **灵动岛双形态**：
  - 紧凑态：LOGO + 客户端名 + 版本 + FPS + 延迟，单行胶囊；
  - 展开态：按住 Tab（玩家列表键）展开为大面板，显示服务器名、延迟、玩家列表（按 Tab 列表序排序），高度随玩家数自适应。
- 灵动岛接入共享 Skija 覆盖层（`SkijaRenderer.renderOverlay`），仅在游戏内（无 Screen）渲染，与 HUD 模块同画布。

### 修复 / Fixed
- 修复 `Paint.FilterQuality.HIGH` 编译错误（Skija 0.143 无此 API）→ 改用 `SamplingMode.LINEAR` + `drawImageRect(..., true)` 严格采样。
- 继承 v2.0.0 视觉对齐修复：viaversion 别名 `setsunavia`/provides、`skija-linux-x64` native、资源命名空间 `dioxide-lite` 小写、字体/图标资源路径修正、`nested-058` 旧版 mcstructs 类清理。

### 版本 / Version
- gradle.properties `version=2.0.1`；主菜单标题显示 DIOXIDELITE 2.0.1；窗口标题 DioxideLite 2.0.1。

---

## v2.0.0 (2026-09-11) — Visual Parity & New Base

- New base: Skija GPU-backed rendering architecture under the DioxideLite identity.
- ClickGUI: Pop/Drop Skija renderer retained; F6 hot-switches Liquid Glass, Minimal and Signature presentation modes.
- Dynamic Island: DioxideLite visual/QoL implementation added to the shared Skija overlay pass.
- Removed gameplay automation module registration and deleted the corresponding combat/movement/player module trees and injection points.
- Removed cheat-oriented render feature registration.
- No packet-based gameplay manipulation, combat automation, movement automation, anti-cheat bypass, or command/script automation is exposed by v2.
- DioxideLite is the only client-facing brand in the main project.
