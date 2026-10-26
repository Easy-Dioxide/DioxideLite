# DioxideLiteNG — Devlog / 版本开发日志

> 版本：**B13** ｜ Minecraft **26.3** ｜ Fabric **0.19.5** ｜ Java **25**（Temurin 25.0.4.1 LTS）
> 状态：构建通过 · 158 个注入点静态校验通过 · 运行时初始化验证通过

---

## 一、版本定位

DioxideLiteNG（简称 DLNG）是面向 Minecraft 26.3（Fabric / Java 25）的开源客户端，采用 Mixin 运行时注入，无修改版 Minecraft 分发。

B13 是一个分水岭版本：此前 B7–B12 走的是「读 Krs 源码，逐个模块用 NG 事件体系重写」的路线，累计四个版本都在修重写引入的语义偏差。B13 改为**整套内核移植**——把 KRS 的运行时原样搬进来，只在最外层加一层桥接，让它在 NG 的 ClickGUI 里可开关、可调参。

本版本源码规模：**648 个 Java 文件、182 个功能模块、140 个 Mixin**。

## 二、构建环境与产物

| 项目 | 值 |
| --- | --- |
| Gradle Wrapper | 9.6.1 |
| JDK | Temurin 25.0.4.1 LTS |
| Loom | 1.15.5 |
| Fabric Loader | 0.19.5 |
| 依赖仓库 | Fabric / Maven Central / CCBlueX snapshots / lenni0451 releases |

构建命令：

```shell
./gradlew clean build
```

产物（`build/libs/`）：

- `dioxideliteng-b13.jar`（83.8 MB，含内嵌依赖与各平台 natives）
- `dioxideliteng-b13-sources.jar`（19.9 MB）

预构建产物同步发布在 [Releases](https://github.com/Easy-Dioxide/DioxideLite/releases/tag/b13)。

## 三、主要特性

### 启动与品牌
- 自定义 **LaunchIntro 启动动画**：boot → login → connection → emblem 四阶段，含「轮回」标识、TERMINAL SERVICE 登录窗、SAMSARA 字样（约 13 秒）。
- 主菜单（明日方舟主题）：「风暴瞭望 STORMWATCH」活动入口、干员立绘、终端数值面板、日期横幅。
- 渐变品牌消息前缀（每字符按色相渐变渲染）。

### 模块体系（182 个）

**KRS 内核移植（129 个模块类，界面注册 125 个）**

- **Combat（16）**：KillAura2、Velocity2、Backtrack2、Criticals2、AntiBot2、AutoTotem2、AutoPot2、Reach、WTap、MaceExploit、CrossbowExploit、FastBow、FastEat、NoCombatDelay、TargetStrafe、Teams
- **Movement（24）**：Speed2、LongJump2、Stasis2、Sprint2、NoSlow2、InventoryMove2、ElytraFly、Jesus、Spider、Step、Parkour、SafeWalk、NoWeb、NoPush、FastFall、AntiVoid、AntiLevitation、AutoSneak、AutoRocket、EntityControl、EntityFly、MovementFix、NoJumpCooldown、PerfectHorseJump
- **Player（23）**：Scaffold2、ChestStealer2、InvManager2、NoFall2、AutoTool2、Freecam、Phase、SpinBot、MiddleClick、AirPlace、AntiAFK、AutoFish、AutoLeave、AutoRespawn、ChatCommands、DurabilityAlert、FastBreak、FastLadder、HackerDetector、LookTP、MurdererDetector、Stalker、SneakSpam
- **Render（24）**：NameTags2、FullBright2、ClickGui2、ESP、Rotations、ViewModel、Zoom、PlayerIndicator、Interface、Capes、CustomModel、FPSBobbing、HudHider、KillEffect、NoHurtCam、OldHitting、TrueSight、WorldTime、AntiBlind、CameraNoClip、Imposter、ThunderDetector、WidelyPutin、ImGui
- **Exploit（18）**：Blink2、XCarry、PortalGodMode、PacketDuper、ChestTP、SurroundTP、PathFinder、NoBreakReset、BrandSpoofer、ConsoleSpammer、EntityDesync、ExploitPatcher、FakePinger、LongThrow、PackSpoofer、PortalScreen、ServerCrasher、TransactionConfirmBlinker
- **Level（14）**：Timer2、Nuker、Xray、CaveFinder、NoteBot、Spammer、Breaker、CivBreak、AlwaysRiptide、AntiCheatDetector、AutoBypass、ItemDropChanger、NoBreakCooldown、QuickMacro
- **Dev/Misc（9）**：Disabler2、PluginsDetector、FukumaiPlayerTracker、ChatExcepChecker、BlockBreakSimulator2、Debugga、HatenaPiano、NoMoreAutism、MovementUtilTest

**NG 原生模块（53 个）**

保留 B1–B3 时期的原生实现，与 KRS 模块共存于同一 ClickGUI。名称冲突的 22 个已自动加 `2`。

### 界面与交互
- **ClickGUI**：Opai / Rockstar / Neverlose 三种主题布局，左键开关、右键展开设置。NG 与 KRS 模块统一在此呈现。
- **HUD 编辑器**：铅笔按钮进入，自由拖拽排版。
- **DynamicIsland 灵动岛**：延迟 / 状态悬浮岛。
- **账户管理**：微软登录（JavaFX WebView 内嵌）、Altening 会话、账户保存与切换。
- **终端主题**（TerminalPage / TerminalConfirmScreen / TerminalMultiplayerScreen）。

### 工程与安全
- **双事件总线**：NG 的 `EventBus`（priority 分发）+ KRS 的 `EventManager`，由 `KrsModuleAdapter` 协调模块状态。
- **静默转头**：`RotationManager` 三态（client / server / render），转头值与发包值分离。
- **原子化配置持久化**（AtomicFiles / ClientStateStore）、`.config save/load` 预设。
- **Mixin 注入** 140 个（KRS 86 + NG 原生 54），覆盖渲染 / 输入 / 网络 / 菜单等多个切面。
- **Access Widener**：`krs.accesswidener`（126 行）。

## 四、B13 移植架构

### 4.1 包重映射

```
com.instrumentalist.krs       -> com.dioxideliteng.krs
com.instrumentalist.mixin.*   -> com.dioxideliteng.mixin.krs
org.nvgu                      -> com.dioxideliteng.krs.nvgu
xyz.breadloaf.imguimc         -> com.dioxideliteng.krs.imguimc
com.instrumentalist.Client    -> com.dioxideliteng.krs.KrsClient
```

### 4.2 桥接层

`KrsModuleAdapter` 继承 NG 的 `Feature`，内部持有一个 KRS `Module`：

- `onEnable()` / `onDisable()` → `module.setState(true/false)`
- 反射读取 `@Module.Setting` 标注的字段，逐项映射为 NG 设置项
- 每 tick 做一次双向同步（快照比对，避免互相覆盖）

`KrsModuleRegistry` 负责注册与重名处理，在 NG 模块全部注册完之后执行，以便拿到已占用的名字集合。

### 4.3 不移植的部分

KRS 的 NanoVG / ImGui 界面整体不搬：`NanoVGClickGuiScreen` 改为空占位 `Screen`，`ModuleRenderable`（原 1003 行 ImGui 面板）退化为转发壳。同时删除 `ModuleManager` 中「右 Shift 打开 KRS 界面」的逻辑——NG 的 ClickGui 同样是右 Shift，两者会抢键。

## 五、运行验证

- **构建**：`./gradlew clean build` 通过。
- **Mixin 目标校验**：86 个 Mixin 的目标类全部存在——79 个精确命中 MC 26.3，7 个指向 Sodium / Iris / Indigo（未安装时运行时自动跳过）。
- **注入点校验**：158 个注入点的方法名逐个对照目标类字节码方法表，0 失败。
- **启动验证**：日志输出 `[KRS-Port] bridge ready, 124 modules registered`，无 `InvalidInjectionException`。
- **已知限制**：验证环境无 GPU，游戏在 `Minecraft.<init>` 创建 GL 窗口时因 `Couldn't find matching GLX visual` 中断（Mesa 软渲染亦无法匹配 visual）。此为沙箱环境限制，与移植代码无关。

## 六、使用方式

1. 从 Releases 下载 `dioxideliteng-b13.jar`，或按上述命令自行构建。
2. 为 Minecraft 26.3 安装 Fabric Loader 0.19.5+。
3. 将 jar 放入 `mods` 目录，用 Java 25 启动 Fabric 档位。
4. **Right Shift** 打开 ClickGUI；聊天输入 `.help` 查看指令；`.config save/load <name>` 管理预设。
5. 自动状态存于 `dioxideliteng/state.json`，预设存于 `dioxideliteng/configs/`（游戏目录下）。

> **踩坑提示**：开 `Scaffold2` 必须同时开 `Movement Fix`。KRS 源码无自动启用逻辑，只开 Scaffold2 会导致客户端按真实 yaw 移动、发送欺骗旋转，产生大量 GrimAC simulation VL。

## 七、许可

GNU General Public License v3.0（详见 LICENSE）。第三方依赖与素材保留各自许可证。

绕过逻辑移植自 [Krs](https://github.com/Aspw-w/Krs)（作者 Aspw-w），按 GPL-3.0 兼容方式使用。
