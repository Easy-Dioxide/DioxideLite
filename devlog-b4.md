# DioxideLite — Devlog / 版本开发日志（B4）

> 版本：**B4** ｜ Minecraft **26.3** ｜ Fabric **0.19.5** ｜ Java **25**
> 状态：构建通过 · Krs Hacks 整合 · 60 模块移植

---

## 一、版本定位

B4 在 B3 基础上，从 [Aspw-w/Krs](https://github.com/Aspw-w/Krs) 仓库批量适配并移植了 **60 个功能模块**到 DioxideLiteNG 客户端，覆盖 combat、movement、exploit、level、player、nulling 全部分类（跳过 dev 和 render 分类），并新增 8 个事件类接入客户端事件总线与 Mixin 分发管线。

## 二、新增事件类（8）

为支持 Krs 模块的事件驱动模型，在 `event/impl` 下新增并接入 `Events` 单例原型与 `EVENT_TYPES` 列表：

| 事件 | 用途 |
| --- | --- |
| `EventHandleInput` | 输入处理钩子（对应 Krs `HandleInputEvent`） |
| `EventKey` | 按键事件（key / action） |
| `EventMouseScroll` | 鼠标滚轮事件（horizontal / vertical） |
| `EventAttack` | 攻击实体事件（entity） |
| `EventWorld` | 世界切换事件（previousWorld / world） |
| `EventBlock` | 方块碰撞事件（blockState / blockPos / voxelShape） |
| `EventBlockEdge` | 方块边缘检测事件 |
| `EventCancelBlockBreaking` | 取消方块破坏事件 |

## 三、移植模块清单（60）

### Movement（12）→ `module/movement/`

AutoWalk2、FastFall、AntiLevitation、AutoRocket、AutoSneak、Jesus、NoJumpCooldown、NoWeb、Parkour、SafeWalk、Spider、WaterSpeed

### Combat（14）→ `module/combat/`

AntiBot2、AutoPot、AutoTotem、Backtrack2、Criticals2、CrossbowExploit、FastBow、FastEat、MaceExploit、NoCombatDelay、Reach、Teams、Velocity2、WTap

### Exploit / Nulling → Misc（14）→ `module/misc/`

NoBreakReset、XCarry、PacketDuper、PortalGodMode、LongThrow、ConsoleSpammer、BrandSpoofer、PackSpoofer、EntityDesync、ExploitPatcher、ServerCrasher、PluginsDetector、FakePinger、**Disabler2**（含 Hypixel / Mineplex / Cubecraft Bedrock / Mineland / Miniblox / Verus / Vulcan Scaffold 七模式）

### Level / Player（20）→ `module/player/`

AutoBypass、NoBreakCooldown、Timer2、SneakSpam、SpinBot、AntiAFK、AutoFish、AutoRespawn、AirPlace、AutoLeave、DurabilityAlert、MiddleClick、MurdererDetector、HackerDetector、FastBreak、FastLadder、NoFall2、AutoTool2、ChestStealer2、InvManager2

## 四、适配规则

- **事件模型**：Krs `EventListener` 接口默认方法 → 客户端 `onEvent(Event)` 内按 `Events.XXX` 引用比较分支
- **设置映射**：Krs `ListValue` / `IntValue` / `FloatValue` / `BooleanValue` → 客户端 `ModeSetting` / `NumberSetting` / `BooleanSetting`
- **分类映射**：combat→`COMBAT`、movement→`MOVEMENT`、exploit / nulling→`MISC`、level / player→`PLAYER`
- **命名规则**：与客户端已有模块同名（忽略大小写与空格）→ 客户端名 + 2（如 `KillAura` → `KillAura2`、`Velocity` → `Velocity2`、`NoFall` → `NoFall2` 等）；无碰撞保留原名
- **依赖处理**：Krs 自有 utils 内联；Krs mixin 钩子 / 重依赖模块（KillAura、TargetStrafe、Scaffold2、Phase 等 33 个）延后

## 五、跳过模块（33）

因依赖 Krs 专属工具层（`PlayerUtil` / `MovementUtil` / `ToolUtil` / `PacketUtil` / `BlinkUtil` / `MainPathFinder` 等）或渲染混入（按「不动渲染器」要求跳过），以下模块未移植：

| 分类 | 模块 |
| --- | --- |
| Exploit（6） | Blink2、TransactionConfirmBlinker、PathFinder、TPStealer、TPSurround、PortalScreen |
| Level（11） | AlwaysRiptide、Breaker、CaveFinder、CivBreak、ItemDropChanger、NoteBot、Nuker、QuickMacro、Spammer、Xray、AntiCheatDetector |
| Player（6） | ChatCommands、LookTP、Phase、Scaffold2、Stalker |
| Combat（2） | KillAura、TargetStrafe |
| Movement（8） | AntiVoid、ElytraFly、EntityControl、EntityFly、NoPush、PerfectHorseJump、fly/*、speed/* |

## 六、技术变更

- Minecraft 26.3 / Fabric Loader 0.19.5 / Fabric Loom 1.15.5 / Gradle 9.6.1
- `mixins/MinecraftAccessor` 新增 `@Accessor("missTime")` 用于 NoCombatDelay
- `mixins/LivingEntityAccessor` 用于 NoJumpCooldown
- Disabler2 Verus 模式：因 `ServerboundKeepAlivePacket.id` 为 `private final`，采用 cancel + 重发策略
- Disabler2 Vulcan Scaffold 模式：通过 `FeatureManager.scaffold.isEnabled()` 替代 Krs `ModuleManager.getModuleState`
- Krs Backtrack2 中 `ClientboundEntityPositionSyncPacket.values()` 在 26.3 不存在，替换为 `packet.position().endPosition()` + yRot / xRot
- Krs Teams 中 `DataComponents.DYED` 在 26.3 重命名为 `DYED_COLOR`
- 模块总数由 B3 的 55 个增至 **115 个**

## 七、版本号升级

- `gradle.properties`：`mod_version` 由 `b3` 升至 `b4`
- `fabric.mod.json` 版本号同步为 `${version}`（构建时展开为 `b4`）

## 八、构建环境与产物

| 项目 | 值 |
| --- | --- |
| Gradle Wrapper | 9.6.1 |
| JDK | Java 25 |
| Loom | 1.15.5 |
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5 |

构建命令：

```shell
./gradlew build
```

产物（`build/libs/`）：

```
dioxideliteng-b4.jar          # 主产物（含内嵌依赖与各平台 native）
dioxideliteng-b4-sources.jar  # 源码包
```

构建结果：**BUILD SUCCESSFUL**
