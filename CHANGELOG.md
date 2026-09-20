# 更新日志（CHANGELOG）

本文件汇总 DioxideLite 各版本更新记录。最新版本见顶部。

---

## v2.1.1（2026-09-21）

### 视觉（SetsunaClient 全量移植）
- **全量执行**：把 SetsunaClient 上游的视觉模块与全部视觉功能并入 DioxideLite，含 C 级战斗 / 移动基建（KillAura 体系与 Scaffold 引擎）。规模：新增 144 java、修改 22、删除 0；新注册模块 43、新登记 mixin 29；编译 0 错误 / 1530 class。
- **世界渲染（本轮新增）**：`HoleESP`、`Tracers`、`OreTracers`、`SpawnerFinder`、`UHCDetector`、`Xray`。
- **战斗**：`KillAura`、`KillAuraPlus`、`AntiBot`、`AutoTotem`、`AutoHitCrystal`、`MaceAura`、`SpearKill`、`Surround`、`Burrow`、`Criticals`、`FakeLag`、`Backtrack`、`ZealotCrystalPlus`。
- **移动**：`Scaffold`、`Velocity`、`KeepSprint`、`MovementFix`、`InvMove`、`NoSlow`、`NoFall`、`NoJumpDelay`、`FlatElytraFly`、`Speed`。
- **玩家**：`ChestStealer`、`InvManager`、`AutoTool`、`AutoMLG`、`FastBreak`、`FastCraftModule`、`GhostHand`、`PacketEat`、`AntiWeb`、`AntiResourcePack`、`BedAura`、`FakePlayer`。
- **其他**：`MiddleClickFriend`、`AltManagerModule`（此前从未被注册、永远打不开，本轮补上注册）。
- **HUD**：`TargetHud` 与 `ScaffoldBlockHUD` 改为上游原版实现。
- **随附基建**：KillAuraPlus 引擎、AntiBot 引擎、TargetManager / RotationManager / HealthManager、Scaffold / InvMove 引擎、BlinkManager、FallingPlayer，以及 7 个新事件（Raytrace / RotationAnimation / AfterRotation / SendPosition / Strafe / KeyboardInput / FallFlying）。ClickGUI 接入音乐色卡预览；补齐 Sodium / Indigo 兼容路径。

### 修复
- **全局翻译层失效**：语言文件前缀 `DioxideLite.` 与 `MOD_ID`（`dioxide-lite`）不一致，所有模块名/设置名回退英文 → 改 4 处键构造点为 `NAME`，移植模块开箱即中文。
- **模块开关提示链路是死的**：唯一触发调用被注释掉 → 恢复（开关默认关，默认表现不变）。
- **Block Offset 滑块无读取方**：`CombatVisuals.blockOffset` 全工程仅 1 处声明 → 由 `ItemInHandRendererMixin` 新处理器消费。
- **mixin accessor 前缀不一致（编译阻断）**：`DioxideLite$` → 统一为小写 `dioxidelite$`（6 个 accessor + 7 处调用点）。
- **Constants 目录与包声明不一致**：文件移入匹配目录（内容零改动）。
- **音乐界面显示 "SETSUNA"**：改为 `DIOXIDELITE SELECTION` / `DIOXIDELITE RECORDS`。
- **CommandManager 缺 `addCommand` / `commands`**：替换为上游完整版（分词 / 纠错提示 / 补全 / 历史）。
- **Tracers 设置名笔误**：`"TargetHUD"` → `"Target"`。
- **启动崩溃**：补齐 `assets/setsunavia/**` 资源树（77 文件）修复 `DioxideLiteViaMappingDataLoader` NPE；access widener 追加 `InterpolationHandler$InterpolationData`。

### 构建
- `gradlew build` BUILD SUCCESSFUL（JDK 25 / Loom 1.15.5），产物 `DioxideLite-2.1.1.jar` 与 `-sources.jar`；已核对 jar 内版本 2.1.1，无 2.0.9 残留。

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
