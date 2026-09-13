# 更新日志（CHANGELOG）

本文件汇总 DioxideLite 各版本更新记录。最新版本见顶部。

---

## v2.0.8（2026-09-13）

### 修复
- **渲染异常（"都扁了"）**：SkijaRenderer 引入紧凑 GL 状态守卫 `FastGlState`，外层绘制不再做每帧全量 GL 快照（旧 `GlState.capture()` 在部分驱动上会残留错误的视口/投影状态，导致画面比例异常）。
- ClickGUI 打开时每帧只保留一次 Skija 提交，避免重复提交造成的性能损耗。

### IRC（关键改动）
- **握手协议对齐原版 OpticsValleyIRC**：连接后仅发送玩家名（不再发送 capability 私有帧），兼容原版服务器，解决"无法连接"问题。
- 新增 JOIN/LEAVE 在线状态解析：从服务器广播的加入/离开消息维护 IRC 在线玩家列表（Nametag Logo 与灵动岛在线状态依赖此列表）。
- `trackCapability` 接收处理保留原样（收到私有帧仍可解析，兼容 companion 服务器）。
- 安全策略不变：忽略远程 CRASH 控制帧，服务器无法远程关闭客户端。

### 性能
- 保留 v2.0.7 的根因修复（移除 Array List 路径每帧 GL 状态捕获）。
- 内嵌 Sodium / Lithium / FerriteCore 优化模组。

---

## v2.0.7（2026-09-13）

### 修复
- **性能根因**：移除渲染热路径中每帧全量 `GlState.capture()`，改为按需保存，解决核显/软渲染下 HUD 渲染导致的帧率骤降。
- README 重写为客户端介绍（联系方式：QQ 81622964）。
- 清理仓库杂项文档，更新日志合并为单一 `CHANGELOG.md`。

### 功能
- IRC capability 私有帧（服务端需配套支持）。
- HUD/Watermark/灵动岛锐化优化。

---

## v2.0.5（2026-09-13）

- **最终优化**：Overlay 快速路径、HUD 渲染修复、灵动岛默认开启、Sprint 优化。
- 修复 Watermark 无内容问题（默认显示 DioxideLite 品牌）。
- 修复 Nametag Logo 模糊问题（改为 Skija 渲染）。
- 优化 GC / 对象池，减少渲染期分配。

---

## v2.0.4（2026-09-12）

- **Nametag Logo**：IRC 在线玩家名字左侧渲染 DioxideLite Logo（Skija）。
- Module List 加入 ClickGUI 视觉模块。
- 锐化修复：灵动岛 / Watermark 边缘与字体清晰度优化。
- IRC 联动完善：灵动岛 Tab 面板展示 IRC 在线用户。

---

## v2.0.3（2026-09-11）

- **IRC Link**：接入 OpticsValleyIRC 聊天联动（Player 分类，默认开启）。
- **Nametag 模块**：原版 Nametag 无法渲染客户端 Logo，改为 Skija 渲染层。

---

## v2.0.2（2026-09-11）

- 优化渲染管线，降低 HUD 分辨率依赖。
- 修复 ClickGUI 主题切换崩溃（F6 热切换）。
- 灵动岛右上角多余文字移除。

---

## v2.0.1（2026-09-11）

- **OPAI Dynamic Island**：灵动岛视觉重构。
- 核显性能优化（Skija 管线优化）。
- 全局 Blur 调节（ClickGUI 视觉模块，默认关闭）。

---

## v2.0.0（2026-09-10）

- **全量重构**：脱离 pvputilsbase，独立 Base。
- D Logo 品牌视觉。
- 移除 pvputils 开源协议依赖，改为 GPL-3.0 / Apache-2.0 双许可。
- 多主题 ClickGUI（含 Setsuna 主题 / LiquidGlass）。

---

## 历史版本（节选）

### v1.8.2
- Skija + OpenGL 渲染适配，修复 Windows 下 ClickGUI 空白问题。
- 字体补丁（Setsuna 字体渲染适配）。

### v1.7.4
- Setsuna 启动主题、LiquidGlass 默认主题。
- ClickGUI 主题热切换（无需重启游戏）。

### v1.6
- pvputilsbase 初始版本，视觉模块基础框架。
