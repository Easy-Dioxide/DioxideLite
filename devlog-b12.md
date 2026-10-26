# Devlog B12 — Scaffold2 全量排查：模拟 VL 根因定位与 MovementFix 接线确认

> 用户报告：b10 的 Scaffold2 依旧报大量模拟 VL；怀疑 Krs 的 MovementFix "没有打滑上去"。
> 任务：全量排查 Scaffold2 逻辑漏洞 + 确认 MovementFix 接线状态。

## 一、结论（TL;DR）

1. **Scaffold2 模块本体与 Krs 逐行一致，未发现逻辑漏洞**（方法级比对 + 全部调用点逐一核对）。
2. **MovementFix 的"打滑"问题答案是：它是独立手动开关，默认关闭，且 Krs 全源码无任何自动启用逻辑**——
   Scaffold2 开启不会带开它。本地行为与 Krs 完全一致（不是本地退化）。
3. **b10 报模拟 VL 是必然的**：moveRelative/jumpFromGround 的 MovementFix 分支是 b11 才补的，
   b10 上即使开了 Movement Fix 也 100% 断链。**升级到 b11 并手动开启 Movement Fix 后链路闭合。**
4. 本轮改动仅为两处过时头部注释的修正（无语义变化），jar 行为与 b11 相同。

## 二、排查过程与证据

### 1. 模块本体全量比对（无漏洞）

- 方法级比对：Krs `Scaffold` 全部方法在本地 `Scaffold2` 均有对应；唯一"缺失"的
  `hookEdgeSafeSneakInput` / `hookAutoInputFix` / `hookTellyJumpInput` 是以实例方法
  `handlePostMoveInput` → `handleTellyJumpInput` / `handleAutoInputFix` 的形式挂在
  `Events.POST_MOVE_INPUT` 上（框架适配，非缺失）。
- 输入钩子：`handleTellyJumpInput` / `requestTellyJump` / `handleAutoInputFix` /
  `getAutoSneakDirection` / `getAutoInputFixDirection` / `getScaffoldMovementDirection` /
  `scaffoldInputDirection` 与 Krs 逐行一致。
- Krs Scaffold 内 5 处 `MovementFix` 调用点本地全部对应（含 `getRawInput` 分支）。
- 直接速度操纵：11 处 `MovementUtil.setVelocityY`（tower 模式）一一对应。
- 事件订阅：`onBlockEdge` / `onTick` / `onUpdate` 对应（本地 TICK 合并 onTick+onUpdate，
  早于 aiStep HEAD 数毫秒，均在 input.tick 之后、移动之前——无实质差异）。
- `Sprint2.tryStartSprinting` 与 Krs `Sprint.tryStartSprinting` 逐行一致（telly 起跳链）。
- `tryPlaceBlock` 与 Krs 逐行一致；`ServerboundSwingPacket` 差异已在 b11 审计中确认为
  26.3 协议移除所致（vanilla 同样不发）。

### 2. MovementFix 接线状态（重点）

- Krs `MovementFix` 构造：`super("Movement Fix", ModuleCategory.Movement, GLFW_KEY_UNKNOWN, false, true)`
  —— `tempEnabled=false`（默认关闭），`showOnArray=true`（UI 可见）。
- 全 Krs 源码 grep：**无任何** `setState(MovementFix)` / 自动启用调用。引用 MovementFix 的只有
  ModuleManager / WTap / Sprint / Scaffold 四个使用方。
- 本地 `MovementFix` 注册于 FeatureManager（非 hidden），默认 `enabled=false`——与 Krs 一致。
- **机制**：Scaffold2 转头中（`rotationManager.isRotating()` = true）但 MovementFix 关闭时：
  - `KeyboardInput#tick` TAIL：`updateInput` 的 `shouldUseRotationFix()` 为 false → 输入不重映射；
  - `moveRelative`：`shouldFixMovement()` 为 false → 客户端按**真实 yaw** 移动；
  - `preMotion`：`isRotating()` 为 true → 发送 **spoofed 旋转**。
  - 客户端移动系 ≠ 服务端模拟系 → 每 tick 位置分歧 → **GrimAC simulation VL**。
  这就是"MovementFix 没打滑上去导致模拟 VL"的精确机制，与用户观察吻合。

### 3. b11 修复链路验证（字节码 + jar 双重确认）

- 时序全景（26.3 字节码核实）：
  1. `Minecraft#tick` → `KeyboardInput#tick` TAIL：`MovementFix.updateInput` 重映射输入
     （`fixStrafeInput` 扇区修正，读 `RotationManager.getRotationYaw()`）；
  2. `handleKeybinds` 后 `Events.TICK`：Scaffold2 `startRotation` 写入 rm；
  3. `LocalPlayer#tick` HEAD：`RotationManager.tick()`（isRotating 时仅快照 prev，保持 clientYaw）；
  4. `aiStep` HEAD `Events.UPDATE`：其他模块 startRotation；
  5. `aiStep` 内 `travel(Vec3(xxa, yya, zza))`（xxa/zza 来自 `LocalPlayer.applyInput` ←
     `input.getMoveVector()`，即重映射后的值）→ `moveRelative` 用 spoofed yaw；
     `jumpFromGround` 的 0.2 加成沿 spoofed 系；
  6. `tick` 尾 `sendPosition` HEAD → `preMotion`：PRE_MOTION.call() 后 rm 现读 → 发送 spoofed 旋转。
- `rotateToward` 语义确认：startRotation 立即推进 clientYaw 一步并置 isRotating；本地
  `rm.tick()` 在 Scaffold2.onUpdate 之后运行但不动 clientYaw（仅快照 prev）——无覆盖风险。
- jar 验证：`dioxideliteng-b11.jar` 的 `MixinEntity.class` 含 MovementFix 引用、
  `MixinLivingEntity.class` 含 getOmnidirectionalJumpYaw、`MixinLocalPlayer.class` 含
  preMotion 现读——修复确实在产物里。

## 三、给用户的操作答案

开 Scaffold2 时必须**同时手动开启 Movement Fix**（Movement 分类）：

| Scaffold2 | Movement Fix | 结果 |
|---|---|---|
| 开 | 开 | 链路闭合，无模拟 VL（Krs 标准用法） |
| 开 | 关 | 客户端真实 yaw 移动 + 发送 spoofed 旋转 → 模拟 VL（Krs 同样如此） |
| 关 | 开/关 | 无转头，正常移动 |

另：如果还在运行 b10，升级到 b11/b12——b10 的 moveRelative/jumpFromGround 没有 MovementFix 分支，
开不开 Movement Fix 都会 VL。

## 四、本轮改动

- `Scaffold2.java` 头部注释：删除过时的 "MovementFix dropped / Sprint 直接切换" 描述，
  补 B11 反作弊闭环说明与 Movement Fix 联动前提。
- `MovementFix.java` 头部注释：删除过时的 "remaining mixin injections are not part of this port"，
  补 B11 三处 mixin 接线清单与手动开关说明。
- 版本号 `b11 → b12`（无语义变化，jar 行为与 b11 相同）。
