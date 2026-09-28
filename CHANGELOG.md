# 更新日志（CHANGELOG）

本文件汇总 DioxideLite 各版本更新记录。最新版本见顶部。

---

## v2.2.2（2026-09-28）

### 多平台移植（单 jar 四平台）
- 打包 Skija 原生库：Windows x64 / Linux x64 / macOS arm64 / macOS x64 + `skija-shared` / `types`；webrtc-java 原生库覆盖四平台。
- 新增 `PlatformSupport`（`util/client/PlatformSupport.java`）替换 Windows 专属代码路径：PowerShell / AWT 字体发现、无 JNA 设备 ID 生成、`Desktop.open` → `open` / `explorer` / `xdg-open`、tinyfd 文件对话框、字体回退。
- `build.gradle.kts` 新增 `-Pskija_platforms` / `-Pwebrtc_platforms` 平台开关；新增 `scripts/build-macos.sh` 与 `PORTING-macos.md`。
- 验证：Windows / Linux 构建静态验证；macOS arm64 真实会话冒烟测试；macOS x64 打包入 jar 未真机启动。

### Render 模块重组（27 退役 → 35 移植）
- 27 个内置 render 模块运行时退役，替换为 `module/modules/render/advanced/` 下 **35 个移植模块**，经 `RenderModuleRegistry` 注册：ESP / Chams / NoRender / Fullbright / Camera / Wings / Trails / TargetESP / BedESP / BlockOverlay / ItemPhysics / Particles / Trajectories / Zoom / Freelook / SkeletonESP / GlowESP / Boxes / Ambience / Skybox / FogBlur / FogRemove / Hurtcam / Hand / Crosshair / SkinChanger / CapeChanger / ChinaHat / JumpCircles / Arrows / TNTTimer / ParticleLimiter / SeeInvisibles / ContainerESP / Animations。
- 配套 mixin：`CameraMixin` / `GameRendererMixin` / `GuiMixin` / `ItemInHandRendererMixin` / `ParticleEngineMixin` / `AbstractClientPlayerMixin` / `EntityRendererMixin` 等。
- 新增 `core/` 引擎目录（combat / movement / player / client engines）。

### 界面
- **ClickGUI 双模式**：`LegacyStyle`（默认，圆角面板 + 粒子光晕 + 展开动画）与 `Setsuna`（环形）。
- **HUD**：单一右对齐 ArrayList（纯文本 / 白色 / 右对齐 / 默认无背景；`Style` / `Colors` / `Row Spacing` / `Screen Margin` / `Scale` 可配置）、通知栈、药水列表。
- **ESP 名牌**：可选客户端 Logo 图标（`ESP → Names → Icon`）；开启时自动抑制原版名牌。
- Watermark、ClickGUI 角块、主菜单统一读取 `DioxideLite.VERSION`（= 2.2.2）。
- Watermark `Rename` 设置默认值改为 `DioxideLite`。

### 修复
- Camera 不再锁定视角：平滑 yaw/pitch 跟随玩家而非衰减偏移。
- Wings 不再糊屏：三角形缓冲上载不再排序 + 姿势锚定。
- Wings / Trails / ContainerESP 轮廓改用细四边形绘制（弃用 `GL_LINES`）。
- HUD 名牌 / Logo 图标从插值位置投影，移动时不再漂移。

### 品牌清理
- 移除客户端全部外部品牌字样（类 / 资源 / 模块名 / 语言文件）；仅保留少量遗留配置键用于一次性迁移旧配置。

### 已知限制
- Skybox / Ambience / Fog Blur 后处理、Item Physics 地面变换、Hand 挥动曲线、Camera 位置平滑 4 个上游 hook 未实现（Camera 旋转平滑可用）。
- 发布 jar 由 patch 上游 2.2.1 archive + 本源码树关键类构建，少量 release 类比本源码旧。

---

## v2.2.1（2026-09-27）

### 新增
- **Halo 模块（碧蓝档案光环渲染）**：从 [blue-archive-halo](https://github.com/opai-client/blue-archive-halo) 移植，支持 砂狼白子 / 黑见芹香 / 小鸟游星野 / Opai Logo 四种角色光环；可调 Size、Spacing、X/Y Rot、Follow Pitch、第一人称渲染、浮动动画；注册于 ClickGUI → Render 分类最底部。
- **Onyx HUD 移植**：`OnyxArraylistHUD`（Onyx 风格 ArrayList，右对齐 + 自动边距）、`OnyxNotifsHUD`（Onyx 风格通知）、`OnyxPotionHUD`（Onyx 风格药水状态）。
- **IRC 心跳保活**：IRC 连接每 5 分钟心跳健康检查，断线自动重连。
- 移除 `ModuleListHUD`；Halo 模块默认开启。

### 修复
- **ChatScreenMixin 启动崩溃**：MC 26.1.2 的 `ChatScreen` 中 `mouseDragged` / `mouseReleased` / `mouseScrolled` / `charTyped` 方法签名变更导致 Mixin 注入失败 —— 所有 HUD Editor 相关注入点添加 `require = 0`，找不到目标方法时跳过注入而非崩溃。
- **HUD 窗口大小调整**：修复窗口更改大小时 HUD 偏移或拉伸的问题。
- **聊天编辑器**：修复 HUD Editor 聊天界面唤起功能。

### 构建
- 构建成功（约 60MB jar），游戏启动正常，主菜单背景、HUD + ArrayList 显示正常。

---

## v2.2.0（2026-09-25）

### 优化
- Render 模块优化：ESP / NameTags / ItemTag / SpawnerFinder / TeamViewer / UHCDetector。
- ClickGUI 优化：`PopClickGuiScreen` / `WindowClickGuiScreen`。
- `SkijaRenderer` / `DioxideDynamicIsland` / `DioxideThemeController` / `LuaRender2DContext` 优化。
- WorldToScreen 投影修复。

### 修复
- `ItemTag.java` 语法错误（缺少右括号）。
- `NameTagLogoRenderer.java` 重复代码块 + 缺少 `Vector3f` import。

### 保持不变
- 主菜单 UI（砂狼白子背景）、Dynamic Island 4 样式、ClickGUI 4 主题、Lua 脚本沙箱。

---

## v2.1.5（2026-09-25）

### 核心变更（RenderStable OnyxPort）
- Render 模块稳定性修复：Compass / ESP / NameTags / WorldToScreen。
- `OnyxTargetScanner` 修复 Villager 编译错误（26.1.2 mapping）。
- `ClickGui.Mode.Pop → Setsuna` 修复。

### 已移植的 Onyx 模块（全量）
- **Combat（18）**：KillAura / KillAuraPlus / AntiBot / AutoTotem / Surround / Criticals / Backtrack / Burrow / FakeLag / MaceAura / SpearKill / ZealotCrystalPlus / BedBreaker / BedDefender / BedTracker / JumpReset / AttackRing / CombatVisuals
- **Movement（14）**：Scaffold / Velocity / NoSlow / Speed / Sprint / MovementFix / InvMove / NoFall / NoJumpDelay / KeepSprint / FlatElytraFly / AutoSprint / SafeWalk / LegacyScaffoldEngine
- **Player（16）**：ChestStealer / InvManager / AutoTool / BedAura / FastBreak / FastCraft / AutoMLG / AntiWeb / GhostHand / PacketEat / FakePlayer / AntiResourcePack / IrcModule / NetEaseMusicModule / Deposit / FastPlace / InventoryManager / NameChanger / ScaffoldOnyx
- **Render（20+）**：ESP / HoleESP / Tracers / OreTracers / SpawnerFinder / UHCDetector / Xray / Chams / BlockHighlight / FullBright / GlobalBlur / Radar / TargetHUD / Watermark / PotionHUD / Notifications / MusicLyrics / Dynamic Island / NameTags / Compass / KillEffect / LegendWatch / TeamViewer / DeltaForceStyle

### Onyx 引擎层
- `OnyxCombatEngine`（KillAura 目标获取）、`OnyxMovementEngine`（Auto Sprint / Safe Walk）、`OnyxPlayerEngine`（Name Changer）、`OnyxClientEngine`（生命周期/上下文边界）、`OnyxTargetScanner`（目标扫描）。

### 保持不变
- 主菜单 UI（砂狼白子背景）、Dynamic Island 4 样式（DIOXIDE / OPAI_ONYX / ONYX_MINIMAL / ONYX_GLASS）、ClickGUI 4 主题（LIQUID_GLASS / MINIMAL / SIGNATURE / OPAI_ONYX）。

---

## v2.1.4（2026-09-24）

### 核心变更（OpenOnyx Engine Migration）
- Combat / Movement / Player / Client 增加统一 **Onyx backend 层**：
  - KillAura 目标获取改由 `OnyxCombatEngine` 负责，保留 DioxideLite 原有 ClickGUI、TargetHUD 与视觉链。
  - Auto Sprint、Safe Walk 改由 `OnyxMovementEngine` 负责。
  - Name Changer 改由 `OnyxPlayerEngine` 负责。
  - `OnyxClientEngine` 作为客户端生命周期/上下文边界，渲染仍使用 DioxideLite 的 Skija 链。

### 兼容策略
- OpenOnyx 源码含大量旧 Minecraft mapping/API（旧版 Entity、Packet、Minecraft 类），直接复制会导致 26.1.2 编译失败 —— 2.1.4 使用当前 Minecraft API 重建 Onyx backend，而非把旧 mapping 硬塞进主源码。

### 保持不变
- 主菜单 UI、Dynamic Island / OPAI Onyx 样式体系。

---

## v2.1.3（2026-09-24）

### 功能
- **OpenOnyx 视觉适配**：在现有 ClickGUI 主题系统加入 `OPAI_ONYX` 主题；Theme 变为标准持久化 `EnumSetting`，可在 ClickGUI 中直接切换保存；Onyx 主题沿用 Skija 渲染链，不替换原版主菜单。
- **Dynamic Island**：保留 DIOXIDE / OPAI_ONYX / ONYX_MINIMAL / ONYX_GLASS 四种 Island 样式，作为 `Dynamic Island` 视觉模块设置；网易云歌词继续经 `NcmLyrics` API 接入并对异常隔离；修复 Island 渲染缓存中的重复局部变量。
- **Notification HUD 样式**：DIOXIDE / ONYX / OPAI，显示时长可配置并持久化在 ClickGUI。
- **窗口 / 任务栏图标**：重新生成 D Logo 16/32/48/64/128 多分辨率图标；`WindowMixin` 向窗口系统提供完整多尺寸图标集合，减少 Windows 缩放 / 任务栏 / 窗口边框错误图标；Via 旧 `VIA` 图标不再作为主窗口图标。

### 视觉适配核对
- Target HUD / Scaffold Block HUD 继续只读现有目标/放置状态做纯视觉呈现；ESP、Block Highlight、Combat Visuals、Target HUD、Scaffold Block HUD 保持独立注册；OpenOnyx 移植的视觉模块只消费 DioxideLite 当前状态/API，不直接依赖旧版 OpenOnyx Minecraft 类。

### 构建
- 版本保持 2.1.3，未修改主菜单 UI 或单人/多人游戏入口。

---

## v2.1.2（2026-09-23）

### 修复
- **Windows 运行失败（关键）**：v2.1.1 的 jar 缺少 Skija、ViaVersion、Luaj、WebRTC、Cadence、Kotlin 等运行时依赖，Windows 启动报 `NoClassDefFoundError`。原因：`build.gradle.kts` 中这些依赖用了 `implementation`（仅编译期）未 `include` 打包。修复：全部改为 `include`，由 Fabric Loom 打包为 nested jar —— nested jars 67 → 94，jar 体积 85M → 96M；新增打包 Skija（Windows + Linux）、ViaVersion 全系列、Luaj、WebRTC、Cadence、Kotlin stdlib、JJWT、MinecraftAuth、ViaLegacy、ViaBedrock 等。

### 清理
- **去除 setsuna 字样**：全量替换所有 setsuna/setsunavia 为 dioxidelite/dioxidelitevia（注释、包名 `com.viaversion.setsunavia` → `com.viaversion.dioxidelitevia`、`fabric.mod.json` entrypoint/provides/custom、`mixins.json` package、lang 翻译键与用户可见文本、资源目录 `assets/setsunavia/` → `assets/dioxidelitevia/`）。

### 自定义
- 主菜单背景替换为自定义角色背景（cover 居中裁剪不拉伸），支持 Options → Import 导入自定义背景图，支持 Reset 恢复默认。

### 验证
- 5/5 关键类加载 OK；游戏启动正常，窗口标题 `DioxideLite 2.1.2`；ClickGUI 右 Shift 正常；水牌左上角 `DIOXIDELITE 2.1.2` 正常显示。

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
- **Lua 脚本沙箱**：内置 Luaj 沙箱，支持游戏内动态加载 / 运行 / 停止自定义 Lua 脚本。
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
- **启动崩溃**：补齐 `assets/setsunavia/**` 资源树（77 文件）修复 `DioxideLiteViaMappingDataLoader` NPE；access widener 追加 `InterpolationHandler$InterpolationData`；补齐 67 个 `libs/nested` 嵌套库。

### 构建
- `gradlew build` BUILD SUCCESSFUL（JDK 25 / Loom 1.15.5），产物 `DioxideLite-2.1.1.jar` 与 `-sources.jar`；已核对 jar 内版本 2.1.1，无 2.0.9 残留。游戏内 Music 模块实测：网易云 / QQ 音乐双平台二维码登录界面正常渲染。

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
