# 更新日志（CHANGELOG）

本文件汇总 DioxideLite 各版本更新记录。最新版本见顶部。

---

## v2.1.0（2026-09-15）

### 功能
- **命令系统（Command）**：新增客户端命令注册表骨架（`CommandManager` / `CommandBuilder` / 参数构建 / Tab 补全提供器）。v2 明确不注册任何 gameplay / cheat 命令，未匹配命令默认放行至原版聊天。
- **聊天 → 灵动岛联动**：聊天输入实时同步到灵动岛（`DynamicIslandBridge.onChatInput` / `onCommandSubmitted`）。
- **HUD Editor 聊天 overlay**：聊天界面打开时可通过按键唤起 HUD Editor，直接拖拽 / 右键调整 HUD 布局（RESET / DONE）。
- **IRC 命令分发**：`IrcChatHandler` 接入聊天发送链路，`.` 前缀命令在客户端本地处理，其余消息走原版发送。

### 修复
- **ChatScreenMixin 崩溃**：`@Inject(method = "charTyped")` 在 MC 26.1.2 的 `ChatScreen` 中不存在（该方法已迁移至 `KeyboardHandler`），导致 Mixin 注入失败无法启动 —— 移除失效的 `charTyped` / `mouseDragged` / `mouseReleased` 注入点（`mouseDragged` / `mouseReleased` 在 26.1.2 `ChatScreen` 中亦不存在），保留有效的 `keyPressed` / `mouseClicked` / `handleChatInput` 注入。
- **ScaffoldBlockHUD 编译错误**：`(float) stayTime.get()` 对装箱 `Double` 强转不合法 —— 改为 `((Number) stayTime.get()).floatValue()`。
- **构建排除修复**：`sourceSets` 移除对 `tritium/**` 与 `repackage/**` 的误排除（音乐视觉依赖与音频库参与编译打包）。

---

## v2.0.9（2026-09-15）

### 视觉（Setsuna 迁移）
- 基于 2.0.8 base 迁移 SetsunaClient 完整实体视觉面：**ESP**、**Chams**、**Name Tags**（Logo 锚定左缘）、**Target HUD**（可配置 Player Search Distance，纯视觉）、**Scaffold HUD**（无自动化放置）、**Attack Ring**（纯视觉）、**Combat Visuals**（仅渲染）、**Team Viewer**、**Music**（网易云/QQ 音乐屏幕 + 歌词 HUD）。
- 新 HUD 组件接入 DioxideLite 模块管理器与 HUD 控制器。
- Watermark Logo 光栅路径优化（默认避免 Mitchell 过滤与额外抗锯齿 blit）。
- **边界**：未复制任何战斗/移动自动化逻辑；所有"桥接"功能均为只读视觉反应。

### 修复
- 构建失败（Music 模块）：移除 sourceSets 对 tritium/repackage 的误排除。

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

---

## v2.0.1（2026-09-11）

- **OPAI Dynamic Island**：灵动岛视觉 + 内存优化。
- **Skija 管线优化**：优化渲染流水线，改善核显下的帧率。
- **GC / 对象池优化**：减少渲染期分配与垃圾回收停顿。
- 内置主流优化模组（Sodium / Lithium / FerriteCore）。

---

## v2.0.0（2026-09-11）

- 迁移至自有 Skija GPU 渲染 base（不再基于 pvputils）。
- ClickGUI：Pop / Drop 双形态，F6 热切换 Liquid Glass / Minimal / Signature 主题。
- 灵动岛视觉与 IRC 联动接入共享 Skija overlay pass。
- **移除自动化模块**：删除战斗 / 移动 / 玩家自动化模块树与注入点，不含任何 gameplay 自动化、移动自动化、反作弊绕过或命令脚本自动化。
- DioxideLite 成为主项目唯一客户端品牌。
