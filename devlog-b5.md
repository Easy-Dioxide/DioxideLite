# DioxideLite — Devlog / 版本开发日志（B5）

> 版本：**B5** ｜ Minecraft **26.3** ｜ Fabric **0.19.5** ｜ Java **25**
> 状态：构建通过 · Krs Hacks 重依赖模块移植 · 绕过链路修复

---

## 一、版本定位

B5 在 B4 的基础上，完成了两个此前被标记为「重依赖延后」的模块移植：**KillAura2** 与 **Scaffold2**，并补全了 Krs 反作弊绕过的核心链路——RotationManager 全局单例与 Mixin send 钩子。

此前 B4 跳过的 33 个模块中，这两个是依赖链最深、功能最复杂的代表。本次移植为后续解锁更多重依赖模块（TargetStrafe、AntiVoid、ElytraFly 等）打下了工具层基础。

## 二、新增工具层（14 类）→ `util/krs/`

为支持 KillAura2 / Scaffold2 对 Krs `utils` 包的依赖，将整套工具层移植到 `com.dioxideliteng.util.krs` 包，移除所有 `com.instrumentalist.krs.*` 依赖：

| 文件 | 行数 | 说明 |
| --- | --- | --- |
| `RotationManager.java` | 944 | 旋转管理器核心，管理 clientYaw/Pitch、serverYaw/Pitch、renderYaw 等 |
| `PlayerUtil.java` | 214 | 玩家辅助：blockHitResult、destroyBlock、nukeBlockWithPacket、spoof 槽位 |
| `BehaviorUtils.java` | 103 | 行为工具：isBot、isTeammate、getTargetList、isTarget |
| `ToolUtil.java` | 168 | 工具查找：findBestTool、findBestSword、isArmor、isBetterArmor |
| `MovementUtil.java` | 501 | 移动工具：hasMotion、stopMoving、setVelocityY、getMovementVector |
| `InputUtil.java` | 106 | 输入工具：反射访问 ClientInput.moveVector |
| `PacketUtil.java` | 100 | 包工具：sendPacket、sendPacketAsSilent、consumeSilentPacket |
| `BlinkUtil.java` | 219 | 独立闪烁队列：doBlink、sync、stopBlink |
| `EntityExtension.java` | 102 | 实体扩展：距离、护甲颜色、isFallingToVoid |
| `RotationScraper.java` | 47 | 旋转抓取：getRotationsEntity |
| `RandomUtil.java` | 56 | 随机工具 |
| `MSTimer.java` | 30 | 毫秒计时器 |
| `Tuple.java` | 24 | 二元组 |
| `TimerUtil.java` | 12 | 计时器速度 |

### 关键适配

- `IMinecraft` 接口 → `Minecraft.getInstance()` 静态字段
- `ModuleManager.getModuleState(X.class)` → `FeatureManager.xxx.isEnabled()`
- `Client.rotationManager` → 全局单例 `RotationManager.INSTANCE`
- `ViewModel.hookSwingSpeed` → 直接返回 baseSpeed
- 26.3 API：`ServerboundSwingPacket` 不存在 → `player.swing()`；`KeyboardInput.calculateImpulse` 私有化 → 内联；`ServerboundUseItemPacket` 记录访问器 → `yRot()/xRot()/hand()/sequence()`

## 三、新增模块（2）

### KillAura2 → `module/combat/KillAura2.java`（1739 行）

从 Krs `KillAura` 移植，分类 COMBAT。客户端原有 `KillAura` 不受影响，两者独立并存。

**保留功能：**
- Single / Multi 双目标模式、优先目标 ID、距离/偏好排序、多目标上限
- 智能旋转：完整保留 Krs 的 `findSmartRotationTarget` 算法（面心 + 1/32 网格采样评分）
- APS 四模式：Cooldown / Quick Cooldown / Randomized CPS / Hurt Math / No Delay
- Swing Order 三模式：1.8 / 1.9.x / No Order
- 暴击粒子
- 自动格挡 14 种模式：Vanilla、Blink1、Blink2、Hypixel NCP、Swap、Legit、Packet、Interact1/2、Basic、Spoof、Delayed、Post Attack、No Order
- TP Reach / Random TP

**简化：**
- MainPathFinder 寻路 → 直接位置包（客户端无寻路工具）
- Krs NoSlow 跨模块调用 → 移除

### Scaffold2 → `module/player/Scaffold2.java`（约 2700 行）

从 Krs `Scaffold` 移植，分类 PLAYER。客户端原有 `Scaffold` 不受影响。

**保留功能：**
- Mode：Normal / Telly（含 jump input handling、clutch、normal/non-upwards 变体）
- Tower：Vanilla / NCP / Hypixel（含 center alignment）
- Rotation Mode：Math / Simple / Backwards / None
- 旋转速度抖动、搜索范围、自定义 timer
- KeepY、Edge Safe（Safewalk / Sneak / Auto Input Fix）、Down
- Math rotation：humanized aim、jitter、pre-rotation、ray-trace 放置搜索
- 方块候选排序、放置目标评分

**简化：**
- Krs MovementFix / SpeedModule / Sprint 跨模块调用 → 移除或用客户端等价物

## 四、反作弊绕过链路修复（核心）

B4 移植时，`RotationManager` 被简化为「模块内实例」，`PacketUtil.normalizeRotationPacket` 直接返回 packet，且没有 Mixin send 钩子——导致旋转计算有了，但旋转一致性绕过失效。B5 补全了这条链路。

### 1. RotationManager 全局单例

```java
// util/krs/RotationManager.java
public static final RotationManager INSTANCE = new RotationManager();
```

所有 Krs 移植模块共享同一旋转状态，保证 `serverYaw/serverPitch` 记录的一致性。

### 2. PacketUtil 恢复归一化

```java
// util/krs/PacketUtil.java
private static Packet<?> normalizeRotationPacket(Packet<?> packet) {
    return RotationManager.INSTANCE.normalizeOutgoingPacket(packet);
}
```

对 `ServerboundMovePlayerPacket` 和 `ServerboundUseItemPacket` 的旋转字段应用鼠标灵敏度归一化。

### 3. KillAura2 / Scaffold2 改用单例

```java
private final RotationManager rotationManager = RotationManager.INSTANCE;
```

### 4. MixinClientPacketListener send 钩子

在 `ClientPacketListener.send(Packet)` 链路注入 `@Inject HEAD` 钩子，对**每个** outgoing packet 自动执行：

1. **归一化**：`RotationManager.INSTANCE.normalizeOutgoingPacket(packet)`
2. **记录**：`RotationManager.INSTANCE.recordOutgoingRotation(normalized)`
3. **Silent 包跳过**：`PacketUtil.consumeSilentPacket(packet)`
4. **重入 guard**：`ThreadLocal<Boolean> REENTRY_GUARD`，归一化后通过 `conn.getConnection().send(normalized)` 走底层 `Connection.send`，绕过 `ClientPacketListener.send` 本身避免递归

### 工作原理

- KillAura2/Scaffold2 调用 `rotationManager.faceEntity/startRotation` 设置目标旋转
- 通过 `Events.ROTATION.setYaw/setPitch` 应用到玩家移动包
- 玩家自己发的 `ServerboundMovePlayerPacket`、格挡包、使用物品包都会经过 Mixin 钩子被自动归一化
- 服务器看到的旋转始终与 RotationManager 的服务器旋转状态一致 → 反作弊无法检测到旋转跳变/不一致

## 五、灵动岛视觉适配

- **KillAura2**：`onEnable/onDisable` 调用 `DynamicIslandManager.notifyInfo`，攻击命中目标时 `notifySuccess`
- **Scaffold2**：`onEnable` 调用 `notifyInfo("已开启 Scaffold2 搭路")`，事件中调用 `sampleScaffoldMovement()`
- **DynamicIslandManager** 更新：`updateScaffold()` 和 `onModuleToggled()` 同时识别 `FeatureManager.scaffold2`，BPS 显示对 Scaffold2 生效

## 六、构建信息

- 产物：`dioxideliteng-b5.jar`（含内嵌依赖与各平台 native）
- 产物：`dioxideliteng-b5-sources.jar`
- `./gradlew build -x test` → **BUILD SUCCESSFUL**

## 七、累计移植统计

| 版本 | 新增模块 | 累计模块 | 新增事件 | 新增工具类 |
| --- | --- | --- | --- | --- |
| B4 | 60 | 115 | 8 | 0 |
| B5 | 2 | 117 | 0 | 14 |

## 八、仍延后的模块（31）

以下模块仍因依赖链过深或混入驱动而延后：

- **Combat（2）**：TargetStrafe（依赖 PlayerUtil + BehaviorUtils 跨模块）
- **Movement（8）**：AntiVoid、ElytraFly、EntityControl、EntityFly、NoPush、PerfectHorseJump + fly/* + speed/*
- **Exploit（6）**：Blink2、PathFinder、TPStealer、TPSurround、PortalScreen、TransactionConfirmBlinker
- **Level（11）**：AlwaysRiptide、ItemDropChanger、Breaker、CivBreak、Nuker、CaveFinder、Xray、NoteBot、QuickMacro、Spammer、AntiCheatDetector
- **Player（4）**：ChatCommands、LookTP、Stalker、Phase

后续可基于本次移植的 `util.krs` 工具层继续解锁。
