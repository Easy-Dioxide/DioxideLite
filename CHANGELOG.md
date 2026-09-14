# 更新日志（CHANGELOG）

本文件汇总 DioxideLite 各版本更新记录。最新版本见顶部。

---

## v2.0.9（2026-09-15）

### 视觉（Setsuna 迁移）
- 基于 2.0.8 base 迁移 SetsunaClient 完整实体视觉面：**ESP**（玩家/容器描边）、**Chams**（忽略深度渲染 + 发光描边）、**Name Tags**（姓名/生命/队伍旗帜标签，Logo 锚定左缘随相机贴合）、**Target HUD**（可配置 Player Search Distance，纯视觉）、**Scaffold HUD**（方块图标/数量/BPS，无自动化放置）、**Attack Ring**（最近玩家视觉环，不自动攻击）、**Combat Visuals**（第一人称挥剑/格挡动画，仅渲染）、**Team Viewer**（Apollo 队伍 HUD/标记/消息视觉）、**Music**（网易云/QQ 音乐屏幕 + 歌词 HUD）。
- 新 HUD 组件接入 DioxideLite 模块管理器与 HUD 控制器，可在 ClickGUI 与 HUD Editor 中编辑；HUD Editor 在聊天界面打开时也可通过按键唤起。
- Watermark 保持原设计意图：Logo 光栅路径改为默认避免 Mitchell 过滤与非必要的抗锯齿 blit，保留可选 Logo / 阴影 / 发光 / 渐变 / 文字行为。
- 灵动岛（Dynamic Island）实现保持不变，未迁移替换。
- **边界**：未复制任何战斗/移动自动化逻辑；所有"桥接"功能均为对既有客户端状态/输入/事件的只读视觉反应。

### 修复
- **构建失败（Music 模块）**：`build.gradle.kts` 的 `sourceSets` 误排除 `tritium/**` 与 `repackage/**`，导致 `MusicLyricsHUD` / `MusicScreen` 引用的 `tritium.ncm.music` 包与音频播放依赖（`repackage.processing.sound`、`JSynFFT`）不可见 —— 移除排除项，音乐视觉模块恢复编译。
- 保留 v2.0.8 的 FastGlState 渲染状态守卫与单次 Skija 提交优化。

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
