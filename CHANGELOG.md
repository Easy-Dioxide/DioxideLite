# 更新日志（CHANGELOG）

本文件汇总 DioxideLite 各版本更新记录。最新版本见顶部。

---

## v2.2.6（2026-10-07）

### 新增：主菜单视频背景
- 新增 **VideoBackgroundBaker**（视频帧烘焙器）：将随包附带的 `mainmenu.mp4` 逐帧解码/缩放/编码为 PNG 帧序列缓存，供主菜单背景渲染。
- 新增 **VideoBackgroundPlayer** 与 **BakeProgressWatcher**：播放视频帧序列，并展示烘焙进度（首次启动时）。
- 依赖新增 `org.jcodec:jcodec-javase:0.2.5`（AWTUtil 帧解码），与既有 jcodec 一起嵌套入包。

### 新增：游戏菜单与主题化控件
- 新增 **GameMenuScreen**（游戏内菜单界面）与 PauseScreenMixin 接入。
- 主题化控件扩展：`ThemedEditBoxMixin` / `ThemedSliderMixin` / `AbstractSliderButtonAccessor`，输入框与滑块走统一 Skija 主题。
- 新增 `MinecraftAccessor` / `AbstractSliderButtonAccessor` 等访问器，`UiText` 本地化文本层。

### 重构：Skybox
- 删除原 `SkyRendererMixin`（v2.2.5 的 Skybox 注入实现），Skybox 模块改走新实现，mixin 配置同步清理（无残留引用）。

### 其他
- 大量 mixin 与模块同步更新（RotationManager / EventBus / 渲染与移动模块等，共 89 个文件）。
- 全平台依赖：本次构建含 **Windows x64 / macOS / Linux 全部 Skija 与 WebRTC 原生库**，单 jar 通用包。
- 验证：构建成功（JDK 25 + Gradle + Fabric Loom 1.15.5），产物 `DioxideLite-2.2.6.jar`（约 103 MB，全平台原生库）。

---

### 补全渲染模块真实实现（修复 v2.2.4 空壳）
此前 Animations / FogBlur / PostProcessing / Skybox 四个渲染模块仅有设置界面而无实际效果，本次全部接入 26.1.2 渲染管线，功能真实生效：

- **Animations（渲染）**：接入 `ItemInHandRenderer`。Block 模式把剑举高并随挥动扫过；Swing 模式把物品晃动驱动从挥动进度改为装备进度（equip progress）。
- **Skybox（渲染）**：接入 `SkyRenderer.renderSkyDisc`。按 Preset 驱动动画天空色——CLOUDS 亮度波动 / THUNDER 压暗 + 周期闪电 / PULSAR 脉动。
- **PostProcessing（渲染）**：接入 `GameRenderer` 世界渲染后处理阶段，启用 Blur 应用原版 blur 后处理链（blurRadius 1..20 → 处理 1..3 次）；Bloom 叠加二次模糊得到柔和泛光。
- **FogBlur（渲染）**：启用即对主渲染目标应用 blur 后处理链，实现整体雾状模糊（Distance/Fade 作为强度参考）。

### 全平台依赖
- 本次构建含 **Windows / macOS / Linux 全部 Skija 与 WebRTC 原生库**，Windows 用户可正常运行（修复 2.2.4 早期发布包仅含 Linux 原生库导致 Windows 无法启动的问题）。

### 新增 BedDefender（Combat，修复空壳）
- **BedDefender**（护床防御）此前仅有 4 个设置项、无任何执行逻辑（空壳），本次补全真实实现：
  - 每 tick 定位最近一张床（优先复用 `BedTracker` 缓存，否则本地扫描），检测防御半径内靠近的敌人。
  - 对威胁目标旋转瞄准并真实攻击（`RotationManager` 静默旋转 + `mc.gameMode.attack`，默认 8 CPS），每 tick 最多处理 `Targets Per Tick` 个目标。
  - `Bedwars Only` 开启时仅在床战服务器生效（Hypixel 计分板标题含 "BED WARS" 或 IP 含床战关键词）；目标过滤排除自己 / 朋友 / 旁观者 / 不在射程者；`Debug Logs` 打印命中信息。

### 验证
- 构建成功（JDK 25 + Gradle 9.2.1 + Fabric Loom 1.15.5），产物 `DioxideLite-2.2.5.jar`（约 94 MB，全平台原生库）。
- 游戏启动正常（Minecraft 26.1.2 Fabric），窗口标题 `DioxideLite 2.2.5`，主菜单 / 欢迎界面正常渲染；Combat 分类下 BedDefender 模块加载成功。

---

## v2.2.4（2026-10-02）

### 静默旋转引擎重构（Vape PID）
- **RotationManager 改用 Vape 风格 PID 加速步进**（移植自 `gg.vape.rotation.FixedRotationController`），替换原简单 lerp：按鼠标灵敏度单位步进 + angle-based 加速度 + 容差直瞄 + 微抖动，绕过 Grim / Matrix / NCP 的旋转检测。
- 新增 **AdaptiveRotationController**（移植自 Vape-v4 PID 旋转控制器，支持 angle-based/linear/cubic 加速度、轴比例缩放、aim jitter）。

### SilentAura 重构
- 改用 TargetManager / TargetRequest / FriendManager 统一目标管理；新增 **Show Target + Target Color**；设置项整理（Attack Cps→Attack Speed CPS、Players/Mobs/Animals、新增 Randomize CPS / Invisibles）。

### 新模块
- **LegitScaffold（移动）**：合法搭桥（Place Check、Require Sneak + Sneak Delay、Auto Sprint）。
- **AttackEffects（渲染）**：攻击粒子 / 音效自定义，配 LevelMixin 取消被禁用的攻击音效。
- **BreakProgress（渲染）**：方块破坏进度可视化。
- **NoFOV（渲染）**：锁定 FOV。
- **NoHurtCamera（渲染）**：取消受伤镜头摇晃 / 模型红光。
- **PostProcessing（渲染）**：后处理模糊 / Bloom。
- **StreamerMode（渲染）**：隐藏服务器 ID / 用户名，自定义显示名，过滤聊天。
- **TitleChanger（渲染）**：修改窗口标题。
- 新增 **utility 目录**：Clutch 由 movement 移入（分类仍为 PLAYER）。

### 移除装饰模块
- 移除 ChinaHat、Wings、JumpCircles、Trails（纯装饰，不符合 Opal 简洁风格）及 Halo。

### ESP 增强
- 新增 Box / Box Stroke / Box Color / Health Bar / Health Bar Stroke / Health Bar Color 等选项。

### 视角与聊天适配
- LivingEntityMixin：头部 / 身体跟随静默旋转（Vape 风格「打滑」）。
- ChatScreenMixin：聊天打开时 HUD 覆盖层同步开关；ChatComponentMixin 支持 StreamerMode 过滤。

### 修复
- **LevelMixin**：`playSound` 注入首参 `Player` → `Entity`（MC 26.1.2 实际签名），修复混入注入崩溃。
- **NoFOV**：FOV 锁定从 `GameRenderer.getFov` 移入 `Camera.getFov`（26.1.2 实际位置）。

### 验证
- 构建成功（JDK 25 + Gradle 9.2.1 + Fabric Loom 1.15.5），产物 `DioxideLite-2.2.4.jar`（约 51 MB）。
- 游戏启动正常（Minecraft 26.1.2 Fabric），窗口标题 `DioxideLite 2.2.4`，主菜单 / 欢迎界面正常渲染。

---

## v2.2.3（2026-10-01）

### Vape 移植
- **SilentAura（静默自动攻击）**：移植自 Vape-v4，重写适配 26.1.2，保留目标选择（Distance/Yaw/Armor/Threat/Health 排序）、PID 旋转微调、Perfect Swing、瞄准抖动、自适应最近可见点。
- **Clutch（掉落自动放方块）**：移植自 Vape-v4，1.7.10 → 26.1.2 语义重写，保留掉落检测、自动放方块接住、旋转到放置点（可选 Silent）、自动切换物品栏。

### MovementFix
- 新增 Vape movementCorrection 三种模式：**VapeNone**（完全不修正）/ **VapeSlow**（yaw+180° 反向 + 输入减半）/ **VapeProper**（静默 yaw + 重映射到 45° 桶，等价 Silent）。
- 新增 `quantizeToAngle()` 复用核心算法。

### Lua 内置脚本
- LuaScriptManager 打包内置脚本（`/dioxide-lite/scripts/`），首次运行自动释放到配置 `scripts/` 目录，同名不覆盖。
- SpeedTelly.lua 适配新版沙箱（render→render2d、本地 sign、mouse_down、world:block、draw:line 参数顺序）。

### 网易云扫码登录修复
- MusicScreen：扫码已确认但拉不到 profile 时重置为 FAILED，避免界面卡死，可重新扫码。
- CloudMusic：cookie 保留全部条目（不再白名单过滤），避免凭证不全导致 profile 为 null。

### 验证
- 构建成功（JDK 25 + Gradle 9.2.1 + Fabric Loom 1.15.5），产物 `DioxideLite-2.2.3.jar`。
- 游戏启动正常（Minecraft 26.1.2 Fabric），窗口标题 `DioxideLite 2.2.3`。

---

## v2.2.2（2026-09-28）

### 多平台移植
- **PlatformSupport 统一 OS 分发层**：所有 OS 相关行为（打开文件夹、读取 CPU 名称、桌面集成）按平台分发，不再硬编码 Windows 路径。
- **支持平台**：Windows x64、Linux x64、macOS arm64（Apple Silicon）、macOS x64——一个 jar 全平台通用。
- **零额外 native 依赖**：全部用 JDK API + OS 自带 helper 进程实现。
- **构建参数**：`-Pskija_platforms` / `-Pwebrtc_platforms` 可按需选择打包平台。

### ClickGUI 双模式
- **LegacyStyle**（默认）：传统窗口式 ClickGUI。
- **Setsuna**：径向/轮盘式 ClickGUI。
- **共享强调色**：Accent 颜色统一作用于所有客户端 UI。
- **GUI Scale**：65%–125% 可调。
- **背景模糊**：Setsuna 模式下可调（0–10）。

### Render 模块重组
- **RenderModuleRegistry**：统一注册入口。
- **大量渲染模块移入 `render/advanced/` 子包**：ESP、Boxes、BedESP、GlowESP、SkeletonESP、TargetESP、ContainerESP、Chams、ChinaHat、Wings、CapeChanger、SkinChanger、Trails、Trajectories、JumpCircles、Camera、Freelook、Zoom、FogBlur、FogRemove、Fullbright、Crosshair、Skybox、Animations、Ambience 等。

### 品牌清理
- 移除所有 `setsuna` 字样，统一为 DioxideLite 品牌。
- 窗口标题、任务栏图标、About 信息全部 DioxideLite。

### 其他
- **Tritium NCM**：DeviceIdGenerator 更新。
- **语言文件**：en_us / zh_cn 更新。
- **IRC 心跳**：客户端内置 5 分钟心跳保活，断线自动重连。

---

## v2.2.1（2026-09-27）

### 视觉
- HUD 窗口大小调整修复
- 聊天编辑器功能
- ChatScreenMixin 崩溃修复
- Halo 模块（碧蓝档案光环渲染）
- Onyx HUD 移植（ArrayList / Notifications / PotionHUD）
- 删除原版 ModuleListHUD，OnyxArrayList 右对齐自适应

### 验证
- 构建成功
- 游戏启动正常
- 主菜单砂狼白子背景显示正常
- 游戏内 HUD + ArrayList 显示正常

---

## v2.2.0（2026-09-25）

### 核心变更
- 版本升级至 2.2.0。
- Render 模块优化：ESP / NameTags / ItemTag / SpawnerFinder / TeamViewer / UHCDetector。
- ClickGUI 优化：PopClickGuiScreen / WindowClickGuiScreen。
- SkijaRenderer / DioxideDynamicIsland / DioxideThemeController 优化。
- LuaRender2DContext 优化。
- WorldToScreen 投影修复。

### 修复
- ItemTag.java 语法错误（缺少右括号）。
- NameTagLogoRenderer.java 重复代码块 + 缺少 Vector3f import。

### 保持不变
- 主菜单 UI（砂狼白子背景）
- Dynamic Island 4 样式
- ClickGUI 4 主题
- Lua 脚本沙箱

---

## v2.1.5（2026-09-25）

### 核心变更
- 版本固定为 2.1.5。
- Render 模块稳定性修复：Compass / ESP / NameTags / WorldToScreen。
- referenceTargetScanner 修复 Villager 编译错误（26.1.2 mapping）。
- ClickGui.Mode.Pop → Setsuna 修复。

### 已移植的 reference 模块（全量）

**Combat（18 个）**：KillAura / KillAuraPlus / AntiBot / AutoTotem / Surround / Criticals / Backtrack / Burrow / FakeLag / MaceAura / SpearKill / ZealotCrystalPlus / BedBreaker / BedDefender / BedTracker / JumpReset / AttackRing / CombatVisuals

**Movement（14 个）**：Scaffold / Velocity / NoSlow / Speed / Sprint / MovementFix / InvMove / NoFall / NoJumpDelay / KeepSprint / FlatElytraFly / AutoSprint / SafeWalk / LegacyScaffoldEngine

**Player（16+ 个）**：ChestStealer / InvManager / AutoTool / BedAura / FastBreak / FastCraft / AutoMLG / AntiWeb / GhostHand / PacketEat / FakePlayer / AntiResourcePack / IrcModule / NetEaseMusicModule / Deposit / FastPlace / InventoryManager / NameChanger

**Render（20+ 个）**：ESP / HoleESP / Tracers / OreTracers / SpawnerFinder / UHCDetector / Xray / Chams / BlockHighlight / FullBright / GlobalBlur / Radar / TargetHUD / Watermark / PotionHUD / Notifications / MusicLyrics / Dynamic Island / NameTags / Compass / KillEffect / LegendWatch / TeamViewer / DeltaForceStyle

### reference 引擎层
- CombatEngine — KillAura 目标获取
- MovementEngine — Auto Sprint / Safe Walk
- PlayerEngine — Name Changer
- ClientEngine — 生命周期/上下文边界
- referenceTargetScanner — 目标扫描

### 保持不变
- 主菜单 UI（砂狼白子背景）
- Dynamic Island 4 样式（DIOXIDE / SIGNATURE_DARK / MINIMAL / GLASS）
- ClickGUI 4 主题（LIQUID_GLASS / MINIMAL / SIGNATURE / SIGNATURE_DARK）

---

## v2.1.4（2026-09-24）

### 核心变更
- 版本固定为 2.1.4。
- Combat / Movement / Player / Client 增加统一 reference backend 层。
- KillAura 的目标获取改由 `CombatEngine` 负责，保留 DioxideLite 原有 ClickGUI、TargetHUD 与视觉链。
- Auto Sprint、Safe Walk 改由 `MovementEngine` 负责。
- Name Changer 改由 `PlayerEngine` 负责。
- `ClientEngine` 作为客户端生命周期/上下文边界，渲染仍使用 DioxideLite 的 Skija 链。

### 兼容策略
reference client 的源码包含大量旧 Minecraft mapping/API（旧版 Entity、Packet、Minecraft 类）。直接复制原类会导致 26.1.2 编译失败，因此 2.1.4 使用当前 Minecraft API 重建 reference backend，而不是把旧 mapping 硬塞进主源码。

### 保持不变
- 主菜单 UI 不修改。
- Dynamic Island / OPAI reference 样式体系保留。
- DioxideLite 版本号为 2.1.4。

---

## v2.1.3（2026-09-24）

- 参考客户端表现层集成，对接已有 DioxideLite 功能面。
- 灵动岛样式：DIOXIDE、SIGNATURE_DARK、MINIMAL、GLASS。
- 灵动岛网易云歌词行集成，失败隔离 API 访问。
- 通知 HUD 样式：DIOXIDE、REFERENCEX、OPAI；显示时长可在 ClickGUI 配置。
- 渲染保持在 Skija 路径上，未替换 OpenGL 渲染器。

---

## v2.1.2（2026-09-23）

### Windows 运行失败（关键修复）
**问题**：v2.1.1 的 jar 中缺少 Skija、ViaVersion、Luaj、WebRTC、Cadence、Kotlin 等运行时依赖，Windows 上启动直接报 `NoClassDefFoundError`。

**原因**：`build.gradle.kts` 中这些依赖用的是 `implementation`（只编译时可用），没有用 `include` 打包进 jar。

**修复**：全部改为 `include`，Fabric Loom 自动打包成 nested jar：
- nested jars 从 67 个 → 94 个
- jar 体积从 85M → 96M
- 新增打包：Skija (Windows + Linux)、ViaVersion 全系列、Luaj、WebRTC、Cadence、Kotlin stdlib、JJWT、MinecraftAuth、ViaLegacy、ViaBedrock 等

### 品牌清理
全量替换所有 setsuna/setsunavia 字样为 dioxidelite/dioxidelitevia：
- 注释中的移植来源说明
- 包名 `com.viaversion.setsunavia` → `com.viaversion.dioxidelitevia`
- `fabric.mod.json` entrypoint / provides / custom 字段
- `mixins.json` package 名
- lang 文件中的翻译键和用户可见文本
- 资源目录 `assets/setsunavia/` → `assets/dioxidelitevia/`

### 自定义
- 主菜单背景替换为自定义角色背景（cover 模式，居中裁剪不拉伸）
- 支持用户通过 Options → Import 导入自定义背景图
- 支持 Reset 恢复默认

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
- **ChatScreenMixin 崩溃**：`@Inject(method = "charTyped")` 在 MC 26.1.2 的 `ChatScreen` 中不存在（该方法已迁移至 `KeyboardHandler`），导致 Mixin 注入失败无法启动 —— 移除失效的 `charTyped` / `mouseDragged` / `mouseReleased` 注入点，保留有效的 `keyPressed` / `mouseClicked` / `handleChatInput` 注入。
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
