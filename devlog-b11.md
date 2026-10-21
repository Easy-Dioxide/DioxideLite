# Devlog B11 — Scaffold2 转头闭环修复（GrimAC simulation VL）

> 问题：Scaffold2 转头不完整，GrimAC 大量 simulation VL。
> 要求：完全对照 Krs 的转头与 Scaffold 逻辑，不简化。

## 根因分析

Krs 的反作弊一致性建立在一条完整闭环上：**客户端移动系与服务端模拟系必须同系**。

1. `KeyboardInput#tick` TAIL（`KeyboardInputMixin`）：`MovementFix.updateInput` 把 WASD 按键
   重映射进 spoofed 旋转系（`fixStrafeInput` 扇区修正 + 疾跑条件修正）。
2. `Entity#moveRelative`（`EntityMixin#updateVelocity`）：移动向量必须用 **spoofed yaw**
   （`MovementFix.getMovementYaw()`）计算——因为输入已被重映射进 spoofed 系。
3. `LivingEntity#jumpFromGround`（`LivingEntityMixin#jumpEvent`）：冲刺跳跃 0.2 加成必须沿
   spoofed 系方向施加（`MovementFix.getJumpMovementYaw()`，TargetStrafe / Sprint 全向跳收尾）。
4. `LocalPlayer#sendPosition`（`ClientPlayerEntityMixin#krs$sendPosition`）：MotionEvent 派发时
   `ModuleManager#onMotion` **现读** `rotationManager.getRotationYaw()/getRotationPitch()`
   覆写事件，再经事件级归一化（`normalizeRotation`，基准 = 上次已发送旋转）发 PosRot/Pos/Rot。

本地移植在（1）上完整，但（2）（3）（4）三处断链，导致客户端按真实 yaw 移动、
服务端按发送的 spoofed 旋转模拟——每 tick 位置分歧累积，即 GrimAC 的 simulation VL。

## 修复清单（逐行对照 Krs）

### 1. `mixins/MixinEntity.java` — moveRelative 三分支（主因）

- 修复前：只认旧 `Events.ROTATION.hasMovementCorrection()`（仅旧 MoveFix/AutoRod 设置），
  Scaffold2 的 MovementFix 路径完全缺失 → `moveRelative` 永远用真实 yaw。
- 修复后：照搬 `EntityMixin#updateVelocity` 分支结构——
  `MovementFix.shouldFixMovement()` → `MovementFix.getMovementYaw()`；
  `TargetStrafe.targetStrafeHook()` → `MovementUtil.getPlayerDirection()`；
  旧 `hasMovementCorrection` 分支降级为 else-if 收尾（本地旧模块兼容）。
  带 `LocalPlayer` 守卫，不影响其他实体。
- 补 `EntityMixin#targetStrafeCollisionGuard`（`@ModifyVariable(method="move", HEAD, argsOnly)`
  → `TargetStrafe.preventHorizontalCollision`），26.3 `move(MoverType, Vec3)` 签名与 Krs 一致。

### 2. `mixins/MixinLivingEntity.java` — jumpFromGround 冲刺跳跃方向

- 修复前：`hasMovementCorrection` 单分支、无 LocalPlayer 守卫、Sprint2 全向跳从未接线。
- 修复后：完整 directionYaw 链——`getYRot()` → `MovementFix.getJumpMovementYaw()` →
  `TargetStrafe`/`MovementUtil.getPlayerDirection()` → 旧分支 → 
  `Sprint2.getOmnidirectionalJumpYaw()` 无条件收尾（与 Krs `Sprint#getOmnidirectionalJumpYaw` 逐字一致）。
  字节码核实 26.3 `jumpFromGround` 仅一处 `getYRot()`（冲刺加成），
  `@ModifyExpressionValue` 命中该点与 Krs 整体重写语义等价（Krs 重写还需处理
  `hurtMarked → needsSync` 改名，无必要）。

### 3. `mixins/MixinLocalPlayer.java` — preMotion 现读 rotationManager

- 修复前：`Events.ROTATION` 快照在 `tick` HEAD；挂 `Events.UPDATE`（`Player#aiStep`）的模块
  （AutoPot/Step/AntiAFK2/SpinBot2/CivBreak/Nuker/Breaker）其 `startRotation` 之后
  没有新鲜读取点 → 转头旧一跳。
- 修复后：`dioxideliteng$preMotion` 在 `PRE_MOTION.call()` 之后、取 motionYaw/Pitch 之前，
  补 `if (RotationManager.INSTANCE.isRotating()) { Events.ROTATION.setYaw/setPitch(...) }`，
  等价于 Krs `ModuleManager#onMotion` 在 MotionEvent 派发时的现读覆写。
  Scaffold2/KillAura2（`Events.TICK`，早于玩家 tick）不受影响，该读取幂等。

### 4. `Scaffold2#tryPlaceBlock` — 审计结论：无需改动

- 26.3 协议已移除 `ServerboundSwingPacket`（javap 核实 jar 内不存在该类，
  serverbound 包列表无 swing 包）。`LivingEntity#swing(hand, anim, boolean)` 反编译证实
  其仅驱动客户端 `SwingState` + 服务端 `ClientboundSwingAnimationPacket` 广播，
  客户端调用不产生任何 serverbound 流量。
- vanilla `MultiPlayerGameMode#useItemOn` 自身也不发 swing（放置通知由
  `ServerboundUseItemOnPacket` 承载）。
- 本地 `player.swing(hand, SwingAnimation.DEFAULT, true)` 即 26.3 完全等价物；
  `tryPlaceBlock` 其余部分与 Krs 逐行一致（diff 仅此一行协议差异）。

## 修复后时序全景（每玩家 tick）

1. `Minecraft#tick` → `KeyboardInput#tick` TAIL：`MovementFix.updateInput` 重映射输入。
2. `handleKeybinds` 后 `Events.TICK`：Scaffold2 `startRotation` 写入 RotationManager。
3. `LocalPlayer#tick` HEAD：`RotationManager.tick()` + `Events.ROTATION` 快照 + rm 现读覆写。
4. `aiStep` HEAD `Events.UPDATE`：其他模块 `startRotation`。
5. `aiStep` 内 travel → `Entity#move` → **`moveRelative`（修复1）**：spoofed yaw 计算移动向量；
   `jumpFromGround`（**修复2**）：0.2 加成沿 spoofed 系。
6. `tick` 尾 `sendPosition` HEAD → **`preMotion`（修复3）**：PRE_MOTION.call() 后 rm 现读 →
   发送 spoofed 旋转 + 归一化。

客户端移动系（spoofed yaw + 重映射输入）与服务端模拟系（spoofed 旋转 + 同一输入）完全同系。

## 验证

- `JAVA_HOME=jdk-25 ./gradlew build` 通过（compileJava + mixin AP + jar）。
- 版本号 `b10 → b11`。
