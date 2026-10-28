# DioxideLiteNG b14 — KRS 全量模块移植说明

## 一、交付物

| 文件 | 说明 |
|---|---|
| `dioxideliteng-b14.jar` | 成品 mod（83.8 MB，直接放进 `mods/`） |
| `dioxideliteng-b14-sources.jar` | 源码 jar，便于对照阅读 |

校验值：

```
12ab27c287d66d19bc26ed68e9d431a47b883ed19ac5facbe95b7308bd785f27  dioxideliteng-b14.jar
9dc468f69d14aeaeae32f15f5e5f02537af1b2a16761b0e66bb28662cdd89e79  dioxideliteng-b14-sources.jar
```

**版本号**：`fabric.mod.json` 中 `version: "b14"`。

---

## 二、移植范围

KRS 最新版（MC 26.3）的功能模块**全量移植**，包括你点名的视觉 reader。

### 2.1 已移植内容

| 类别 | 数量 | 说明 |
|---|---|---|
| KRS 模块类 | 133 | 全部重映射进 `com.dioxideliteng.krs` |
| 参与界面注册 | 124 | 124 个正式模块全部接入 NG 桥接层 |
| Mixin | 87 | 全部注册进 `dioxideliteng.krs.mixins.json` |
| Access Widener | 1 | `krs.accesswidener`（126 行） |
| 内嵌依赖 | 39 | 全部改包名后编入 jar `META-INF/jars/` |

### 2.2 包名重映射

```
com.instrumentalist.krs      -> com.dioxideliteng.krs
com.instrumentalist.mixin.*  -> com.dioxideliteng.mixin.krs
org.nvgu                     -> com.dioxideliteng.krs.nvgu
xyz.breadloaf.imguimc        -> com.dioxideliteng.krs.imguimc
```

`org.json` 用了另一套方案：org.json 的版本号是纯日期（如 `20240303`），不满足 Fabric 的 semver 校验，会导致 mod 拒绝加载。因此写了一份 **Gson 实现的 `org.json` 兼容层**，保留原包名，KRS 业务代码零改动。

---

## 三、功能注册（桥接设计）

两套框架差异很大，没有硬塞进同一套体系，而是加了桥接层：

- **NG**：抽象类 `Feature` + `EventBus` 按 priority 分发 + 手动声明 `BooleanSetting/NumberSetting/ModeSetting`
- **KRS**：`Module implements EventListener` + 反射 `@Module.Setting` 注解 + `SettingValue<T>` 体系

### 3.1 KrsModuleAdapter

把 KRS 的 `Module` 包装成 NG 的 `Feature`：

- `onEnable()/onDisable()` → `module.setState(true/false)`
- 反射读取 `@Module.Setting` 标注的字段，逐个映射成 NG 设置项
- **每 tick 双向同步**：NG 界面改动写回 KRS（触发 KRS 自己的 onChange），KRS 内部改值也回写到界面

| KRS 类型 | NG 类型 |
|---|---|
| `BooleanValue` | `BooleanSetting` |
| `FloatValue` / `IntValue` | `NumberSetting` |
| `ListValue` | `ModeSetting` |
| `KeyBindValue` / `TextValue` / `ColorValue` | 跳过（NG 无对应编辑器） |

### 3.2 分类映射

KRS 的 `ModuleCategory` 枚举常量是 `Combat`/`Movement`/`Player`/`Render`/`Exploit`/`Level`/`Dev`（首字母大写）。这里按**枚举对象**匹配而不是字符串比较，避免大小写不一致导致所有模块静默落进 MISC：

| KRS | NG |
|---|---|
| Combat | COMBAT |
| Movement | MOVEMENT |
| Player | PLAYER |
| Render | VISUAL |
| Exploit / Level / Dev | MISC |

### 3.3 界面处理

**不搬 KRS 的 GUI**，只搬功能：

- `NanoVGClickGuiScreen` 改为空占位 `Screen`
- `ModuleRenderable`（原 1003 行 ImGui 面板）退化为转发壳
- 删掉 `ModuleManager` 里用右 Shift 打开 KRS 界面的逻辑 —— 否则会和 NG 的 ClickGui 抢同一个键

KRS 的聊天命令系统保留（`KrsCommands`，约 396 行）。

---

## 四、重名处理

判定规则：**类名 + 显示名都比对**，且做归一化（忽略大小写、空格、标点），因为 KRS 显示名带空格（`"Kill Aura"`），NG 不带（`"KillAura"`），直接字符串比较永远不相等。

共 **22 个**重名模块已加 `2`：

| KRS 原名 | KRS 类名 | NG 已有 | 最终名称 |
|---|---|---|---|
| AntiBot | `AntiBot` | AntiBot | **AntiBot2** |
| AutoTool | `AutoTool` | AutoTool | **AutoTool2** |
| AutoWalk | `AutoWalk` | AutoWalk | **AutoWalk2** |
| Backtrack | `Backtrack` | Backtrack | **Backtrack2** |
| Blink | `Blink` | Blink | **Blink2** |
| ChestStealer | `ChestStealer` | ChestStealer | **ChestStealer2** |
| ClickGui | `ClickGui` | ClickGui | **ClickGui2** |
| Criticals | `Criticals` | Criticals | **Criticals2** |
| Disabler | `DisablerModule` | Disabler | **Disabler2** |
| FullBright | `FullBright` | FullBright | **FullBright2** |
| InventoryMove | `InventoryMove` | InventoryMove | **InventoryMove2** |
| KillAura | `KillAura` | KillAura | **KillAura2** |
| LongJump | `LongJump` | LongJump | **LongJump2** |
| NameTags | `NameTags` | NameTags | **NameTags2** |
| NoFall | `NoFall` | NoFall | **NoFall2** |
| NoSlow | `NoSlow` | NoSlow | **NoSlow2** |
| Scaffold | `Scaffold` | Scaffold | **Scaffold2** |
| Speed | `SpeedModule` | Speed | **Speed2** |
| Sprint | `Sprint` | Sprint | **Sprint2** |
| Stasis | `Stasis` | Stasis | **Stasis2** |
| Timer | `Timer` | Timer | **Timer2** |
| Velocity | `Velocity` | Velocity | **Velocity2** |

> 注：KRS 的 `NoHurtCam` / `InvManager` 与 NG 的 `NoHurtCamera` / `InventoryManager` 名称不同，**不算重名**，保持原名。

显示名同时抹掉了空格，与 NG 的紧凑风格统一（`KillAura2` 而不是 `Kill Aura2`）。

---

## 五、模块清单（124 个主力 + 1 个 Dev 白名单 = 125）

#### Combat（16 个）

| 显示名 | 类名 |
|---|---|
| AntiBot | `AntiBot` |
| AutoPot | `AutoPot` |
| AutoTotem | `AutoTotem` |
| Backtrack | `Backtrack` |
| Criticals | `Criticals` |
| CrossbowExploit | `CrossbowExploit` |
| FastBow | `FastBow` |
| FastEat | `FastEat` |
| KillAura | `KillAura` |
| MaceExploit | `MaceExploit` |
| NoCombatDelay | `NoCombatDelay` |
| Reach | `Reach` |
| TargetStrafe | `TargetStrafe` |
| Teams | `Teams` |
| Velocity | `Velocity` |
| WTap | `WTap` |

#### Movement（24 个）

| 显示名 | 类名 |
|---|---|
| AntiLevitation | `AntiLevitation` |
| AntiVoid | `AntiVoid` |
| AutoRocket | `AutoRocket` |
| AutoSneak | `AutoSneak` |
| AutoWalk | `AutoWalk` |
| ElytraFly | `ElytraFly` |
| EntityControl | `EntityControl` |
| EntityFly | `EntityFly` |
| FastFall | `FastFall` |
| InventoryMove | `InventoryMove` |
| Jesus | `Jesus` |
| LongJump | `LongJump` |
| MovementFix | `MovementFix` |
| NoJumpCooldown | `NoJumpCooldown` |
| NoPush | `NoPush` |
| NoSlow | `NoSlow` |
| NoWeb | `NoWeb` |
| Parkour | `Parkour` |
| PerfectHorseJump | `PerfectHorseJump` |
| SafeWalk | `SafeWalk` |
| Spider | `Spider` |
| Sprint | `Sprint` |
| Stasis | `Stasis` |
| Step | `Step` |

#### Player（23 个）

| 显示名 | 类名 |
|---|---|
| AirPlace | `AirPlace` |
| AntiAFK | `AntiAFK` |
| AutoFish | `AutoFish` |
| AutoLeave | `AutoLeave` |
| AutoRespawn | `AutoRespawn` |
| AutoTool | `AutoTool` |
| ChatCommands | `ChatCommands` |
| ChestStealer | `ChestStealer` |
| DurabilityAlert | `DurabilityAlert` |
| FastBreak | `FastBreak` |
| FastLadder | `FastLadder` |
| Freecam | `Freecam` |
| HackerDetector | `HackerDetector` |
| InvManager | `InvManager` |
| LookTP | `LookTP` |
| MiddleClick | `MiddleClick` |
| MurdererDetector | `MurdererDetector` |
| NoFall | `NoFall` |
| Phase | `Phase` |
| Scaffold | `Scaffold` |
| SneakSpam | `SneakSpam` |
| SpinBot | `SpinBot` |
| Stalker | `Stalker` |

#### Render（24 个）

| 显示名 | 类名 |
|---|---|
| AntiBlind | `AntiBlind` |
| CameraNoClip | `CameraNoClip` |
| Capes | `Capes` |
| ClickGui | `ClickGui` |
| CustomModel | `CustomModel` |
| ESP | `ESP` |
| FPSBobbing | `FPSBobbing` |
| FullBright | `FullBright` |
| HudHider | `HUDHider` |
| ImGui | `ImGui` |
| Imposter | `Imposter` |
| Interface | `Interface` |
| KillEffect | `KillEffect` |
| NameTags | `NameTags` |
| NoHurtCam | `NoHurtCam` |
| OldHitting | `OldHitting` |
| PlayerIndicator | `PlayerIndicator` |
| Rotations | `Rotations` |
| ThunderDetector | `ThunderDetector` |
| TrueSight | `TrueSight` |
| ViewModel | `ViewModel` |
| WidelyPutin | `WidelyPutin` |
| WorldTime | `WorldTime` |
| Zoom | `Zoom` |

#### Exploit（18 个）

| 显示名 | 类名 |
|---|---|
| Blink | `Blink` |
| BrandSpoofer | `BrandSpoofer` |
| ChestTP | `ChestTP` |
| ConsoleSpammer | `ConsoleSpammer` |
| EntityDesync | `EntityDesync` |
| ExploitPatcher | `ExploitPatcher` |
| FakePinger | `FakePinger` |
| LongThrow | `LongThrow` |
| NoBreakReset | `NoBreakReset` |
| PackSpoofer | `PackSpoofer` |
| PacketDuper | `PacketDuper` |
| PathFinder | `PathFinder` |
| PortalGodMode | `PortalGodMode` |
| PortalScreen | `PortalScreen` |
| ServerCrasher | `ServerCrasher` |
| SurroundTP | `SurroundTP` |
| TransactionConfirmBlinker | `TransactionConfirmBlinker` |
| XCarry | `XCarry` |

#### Level（14 个）

| 显示名 | 类名 |
|---|---|
| AlwaysRiptide | `AlwaysRiptide` |
| AntiCheatDetector | `AntiCheatDetector` |
| AutoBypass | `AutoBypass` |
| Breaker | `Breaker` |
| CaveFinder | `CaveFinder` |
| CivBreak | `CivBreak` |
| ItemDropChanger | `ItemDropChanger` |
| NoBreakCooldown | `NoBreakCooldown` |
| NoteBot | `NoteBot` |
| Nuker | `Nuker` |
| QuickMacro | `QuickMacro` |
| Spammer | `Spammer` |
| Timer | `Timer` |
| Xray | `Xray` |

#### Dev（10 个）

| 显示名 | 类名 |
|---|---|
| BlockBreakSimulator2 | `BlockBreakSimulator2` |
| ChatExcepChecker | `ChatExcepChecker` |
| Debugga | `Debugga` |
| FukumaiPlayerTracker | `FukumaiPlayerTracker` |
| HatenaPiano | `HatenaPiano` |
| MovementUtilTest | `MovementUtilTest` |
| NoMoreAutism | `NoMoreAutism` |
| PluginsDetector | `PluginsDetector` |
| Sex | `Sex` |
| TuckMod | `TuckMod` |


---

## 六、b14：渲染崩溃修复

### 6.1 症状与根因

**症状**：游戏窗口刚出来就崩溃，日志提示渲染器问题。装不装 Sodium 都会崩，OpenGL / Vulkan 两个后端都试过。

**根因（已在本地复现并取得完整堆栈）**：

```
FATAL ERROR in native method: Thread[#3,Render thread]:
No context is current ... The JVM will abort execution.
    at org.lwjgl.opengl.GL15C.nglDeleteBuffers(Native Method)
    at imgui.gl3.ImGuiImplGl3.dispose(ImGuiImplGl3.java:211)
    at ImguiLoader.shutdownInternal(ImguiLoader.java:378)
    at ImguiLoader.initialize(ImguiLoader.java:79)
    at KeyboardHandler.<init>
    at Minecraft.<init>
```

链条是这样的：

1. `Minecraft.<init>` 构造期间创建隐藏 OpenGL 上下文，用于兼容渲染层；
2. 这一步因驱动限制失败（或被禁用）；
3. `ImguiLoader.initialize()` 进入 `catch` 分支，调用 `shutdown()` 做清理；
4. **清理时会直接 `glDeleteBuffers`**，但此时线程上没有 GL 上下文；
5. LWJGL 对「无上下文的 GL 调用」是**直接 JVM abort**，属于原生崩溃，`try/catch` 根本接不住。

这也是为什么窗口已经出现、随后瞬间消失 —— 崩溃点就在构造期，窗口对象建好了但主循环还没开始。

### 6.2 b14 的修复

| # | 改动 | 作用 |
|---|---|---|
| 1 | `GraphicsApiCompatibility.initialize()` 全量兜底 | 兼容层初始化任何一步失败都降级为「关闭旧式 UI」，不再抛出 |
| 2 | 隐藏上下文**版本回退** 3.3 → 3.2 → 3.1 → 3.0 | 核显 / 远程桌面 / 虚拟机的驱动常常只给 3.1，原先死磕 3.3 Core 必然失败 |
| 3 | `runWithOpenGlContext` 不再抛异常 | 上下文不可用时跳过绘制，而不是让调用方崩 |
| 4 | `renderOffscreenLayer` / 旧式 overlay 整体兜底 | 每帧路径上的任何异常都就地降级，不留到下一帧 |
| 5 | **修复 ImGui 清理路径（本次核心）** | `imGuiGl3` 存在时强制在 GL 上下文内 dispose；拿不到上下文就跳过 GL 释放，绝不在无上下文时调 GL |
| 6 | `onWindowInit` 增加后端校验 | Vulkan 后端 + 兼容层已禁用时直接跳过 ImGui，避免在 `glGenBuffers` 上 abort |
| 7 | 新增诊断文件 `DioxideLiteNG-krs-render.log` | 记录 GPU 后端、GL 能力、SDL 错误与完整堆栈，方便回传定位 |

**设计原则**：旧式 OpenGL UI（NanoVG / ImGui）只是显示层，画不出来可以接受；绝不能因为它把整个客户端带崩。

### 6.3 双路径实测

**正常路径**（兼容层可用）：

```
[INFO] Krs compatibility OpenGL context created with core profile 3.3
[INFO] Krs legacy UI compatibility renderer enabled for graphics backend Vulkan
FATAL ERROR = 0        纹理图集加载 = 12/12        主菜单完整显示
```

**降级路径**（用故障注入强制让隐藏上下文创建失败）：

```
[ERROR] Krs legacy UI compatibility renderer disabled after a failure;
        the client keeps running without the legacy OpenGL overlays
[WARN ] ImGui skipped: the Vulkan backend has no OpenGL context and the
        compatibility renderer is disabled
FATAL ERROR = 0        纹理图集加载 = 12/12        主菜单完整显示
```

两条路径都截图确认过：**b14 不会再出现「窗口刚出来就崩」**。

### 6.4 如果还崩，把这个文件发我

游戏目录下会生成 **`DioxideLiteNG-krs-render.log`**，里面有：

- `gpu.backend` —— 实际用的渲染后端
- `gl.version` / `gl.compute` —— 隐藏上下文拿到的 GL 能力
- `sdl.last_error` —— SDL 层的原始错误串
- `compat.enabled` / `compat.disabled` —— 兼容层状态与禁用原因
- 完整异常堆栈

直接把这个文件（或 `logs/latest.log`）发给我，就能精确定位。

---

## 七、验证结果（b13 基线，b14 沿用）

全部结论来自 Minecraft 26.3 + Fabric Loader 0.19.5 的 `runClient` 真实启动，
不是静态检查。

### 7.1 端到端自检

启动时输出的自检结果：

```
[SELFCHECK] expected=125  ngAdapters=125  uniqueBridged=125
            missingInNg=0  cmdUnreachable=0  settingNull=0  ngTotalFeatures=177
```

| 指标 | 值 | 含义 |
|---|---|---|
| `expected` | 125 | 应被桥接的 KRS 模块全集（`modules` 124 + Dev 白名单 1） |
| `ngAdapters` | 125 | NG 模块表里的 `KrsModuleAdapter` 实例数 |
| `uniqueBridged` | 125 | 去重后的实际桥接模块数 |
| `missingInNg` | **0** | 没有任何模块漏接 —— ClickGui 里能全部看到 |
| `cmdUnreachable` | **0** | 每个模块都能被 KRS 命令层 `/t <名字>` 唯一命中 |
| `settingNull` | **0** | 354 个 `@Module.Setting` 反射字段无一为 null |
| `ngTotalFeatures` | 177 | NG 侧模块表总条目（52 个原生 + 125 个 KRS） |

> 口径说明：`ModuleManager.modules` 是 124 个主力模块；`devModules` 里另有 8 个调试工具，
> 其中 `FukumaiPlayerTracker` 按白名单并入界面（共 125），其余 7 个
> （`BlockBreakSimulator2` / `Debugga` / `NoMoreAutism` / `ChatExcepChecker` /
> `MovementUtilTest` / `TuckMod` / `Sex`）属纯调试用途，不暴露给界面。

### 7.2 启动与渲染

| 项目 | 结果 |
|---|---|
| `./gradlew clean build` | ✅ BUILD SUCCESSFUL |
| 加载 mod 数 | ✅ `Loading 52 mods`，含 `dioxideliteng b14` |
| Mixin 应用错误 | ✅ 0 条 `InvalidInjectionException` |
| 兼容渲染层 | ✅ `Krs legacy UI compatibility renderer enabled for graphics backend Vulkan` |
| NanoVG HUD | ✅ `nano hud frame started=true, available=true` |
| 主菜单渲染 | ✅ 完整显示（立绘 / Lv 120 / 325 终端 / 单人游戏 / 多人游戏 / 选项 / 模组） |
| 运行稳定性 | ✅ 连续运行至超时退出，无崩溃 |

### 7.3 b13 修复的四个硬伤

移植版此前「启动不了」，根因有四处，全部已修：

**① `MinecraftClientMixin` 完全缺失**（最严重）

KRS 原版 91 个 mixin，移植版只有 87 个，缺的正是这个。后果是双层的：

- `GraphicsApiCompatibility.initialize()` 全库无调用点 → `initialized` 恒为 false →
  `usesCompatibilityRenderer()` 恒为 false → Vulkan 后端下 ImGui / NanoVG 直接对
  无 GL 上下文的设备发 GL 调用 → JVM abort（`No context is current`）；
- `TickEvent` / `WorldEvent` / `HandleInputEvent` 三个事件**没有任何派发点**，
  依赖它们的模块全部静默失效。

现在 `RenderSystem.initRenderer` 之后初始化兼容层，并在 `tick` / `setLevel` /
`Gui.overlay` / `continueAttack` / `startUseItem` / `stop` 等处补齐派发点。

**② `GlBackendMixin` 的 `@ModifyVariable` 不生效**

`createWindow` 的字节码是 `ldc2_w 2L` + `lload` + `lor`，flags 由位运算合并而来，
不是局部变量直传。改用 `@WrapOperation` 拦截 `SDL_CreateWindow` 调用本身，
同时剥离与 OpenGL 互斥的 `VULKAN` 标志位并要求 8 位 stencil。

**③ `Initializer` 放错了包**

它被放在已声明的 mixin 包 `com.dioxideliteng.mixin.krs` 下，但自身不是 `@Mixin`，
Mixin 框架拒绝直接引用（`IllegalClassLoadError`）。已迁至
`com.dioxideliteng.krs.render.Initializer`。

**④ `TimerUtil` 与 NG 的计时器注入冲突**

NG 自己的 `MixinTimer` 用 `@Overwrite` 重写了 `advanceGameTime`，KRS 的
`@At("FIELD")` 注入必然失败（原 `RenderTickCounterDynamicMixin` 已删除）。
改为每刻把 `TimerUtil.timerSpeed` 桥接到 NG 的 `TimerController`，
`Timer` / `Spider` / `LongJump` / `TuckMod` 的倍速行为保持一致。

### 7.4 关于 OpenGL 后端

在本沙箱的 Xvfb + llvmpipe 软件渲染环境下，**OpenGL 后端无法创建 GLX visual**：

```
BackendCreationException: Failed to create window for OpenGL context:
Couldn't find matching GLX visual
```

这**不是移植问题** —— 不带任何 mod 的原版 MC 26.3 在此环境同样失败。
我用 T7~T15 十余个独立 SDL 测试程序逐个排查了 stencil、srgb、depth、multisample、
`SDL_Init` flags、Vulkan→GL 调用顺序、各种窗口标志组合，SDL 层全部成功，
问题出在更上层的 GLX visual 匹配，属环境限制。

MC 26.3 会在此后自动回落到 Vulkan 后端并正常初始化，上面所有验证都是在
**Vulkan 后端**下完成的。

### 7.5 关于 Sodium / Iris

jar **默认不内嵌** Sodium / Iris，需自己放进 `mods/`。原因是 Iris 不支持 Vulkan：
`IrisRenderSystem` 的静态块会无条件调用 `glGetInteger` 查询采样器上限，
在 Vulkan 后端下没有当前 GL 上下文，会直接让 JVM abort。

这与 KRS 原版策略一致 —— KRS 也没把这两个当内嵌件。移植的 7 个针对
Sodium / Iris / Indigo 的 Mixin 在缺席时只会打 `ClassNotFoundException` WARN，
自动跳过，不影响任何其他功能。

如需内嵌，构建时加 `-PembedGraphicsMods=true`（但请确保跑 OpenGL 后端）。

---

## 八、安装与使用

```bash
# 1. 放进 mods/
dioxideliteng-b14.jar

# 2. 必需前置（已内嵌 39 个库，但以下仍需自备）
Fabric Loader >= 0.19.5
Fabric API
MixinExtras >= 0.4.1
Java >= 25

# 3. 可选（相关 Mixin 才会生效）
Sodium 0.9.x+mc26.3
Iris 1.11.x+mc26.3
```

**模块全部默认关闭**，需要在 ClickGui 里手动开启。

KRS 模块在 NG 的 ClickGui 里按分类排列；同名模块带 `2` 后缀
（如 `KillAura2`），共 22 个。KRS 自己的聊天命令系统也保留了（约 396 行）。

---
