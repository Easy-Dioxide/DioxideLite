# DioxideLite — Devlog / 版本开发日志（B9）

> 版本：**B9** ｜ Minecraft **26.3** ｜ Fabric **0.19.5** ｜ Java **25**（Temurin 25.0.4.1 LTS）
> 状态：打滑（绕过）模块全面对照 Krs 审计 · 消除简化/改写 · 版本号升级 b8 → b9

---

## 一、版本定位

B9 的唯一目标：**将全部打滑相关模块与 Krs 原版逐行对照，消除 b3 移植时的
一切简化、改写与缺失，保证绕过功能完整可用**。重点对象是绕过链路的核心依赖
RotationManager，以及 Scaffold2 / KillAura2 / Disabler2 / BehaviorUtils。

审计方式：对每个文件做归一化 diff（屏蔽包名 / 设置框架 / 事件总线等 API 适配
噪声），逐 hunk 与 Krs 源码比对，语义不一致处一律还原为 Krs 精确实现。

## 二、修复清单

### 1. RotationManager —— 身体朝向摆动条件精确还原（`util/krs/RotationManager.java`）

Krs `updateVanillaRenderBodyYaw` 中身体朝向摆动的 vanilla 条件是
`entity.attackAnim > 0.0F`（摆动动画进度）。26.3 移除了 `attackAnim` 公开字段
（重构为私有 `LivingEntity$SwingState.animation`），此前用 `isSwinging()` 近似
替代——但 `isSwinging()` 在摆动首 tick 即为 true，与 vanilla 的
`animation > 0` 存在时序偏差。

B9 新增 `mixins/SwingStateAccessor`（`@Accessor("animation")`）并在
`LivingEntityAccessor` 上补 `getSwingState()`，用
`swingAnimationProgress(player) > 0.0F` **精确还原** vanilla 条件。
`dioxideliteng.mixins.json` 同步注册。

### 2. Scaffold2 —— 数学放置评分子系统全面还原（`module/player/Scaffold2.java`）

b3 移植时该子系统被大面积简化/改写，本次逐项还原为 Krs 精确实现：

| 方法 | b8 状态（错误） | B9 还原 |
| --- | --- | --- |
| `findRotationPriorityPlacementTarget` | 丢失 reach 终检 | 补回 `eyesPos.distanceToSqr(hitPos) > reachSqr + 1e-4` 拒绝 |
| `reusableMathPlacementTarget` | 丢失 rotationMode 门、玩家空判、reach 校验、searchDistance 重建、`isIdealPlacementHitReachable` 终检 | 全部补回 |
| `selectMathPositionTarget` | 自创"分数 + ORIGIN_PRIORITY"比较 | 还原 samePlacementTarget → preferMovementAligned、searchDistance 比较、currentPlacementHit 就绪判断、`10.0D` 边际 |
| `selectAdaptiveMathSearchTarget` | 自创 score 比较 + telly 分支 | 还原 Krs 自适应算法：distanceGap、rotationTimeSaving、requiredRotationSavingTicks（BASE + d×0.42 + d²×0.12 + ORIGIN 0.30 + 路径惩罚 + 距离损失 + 空中下落惩罚） |
| `findRayTracePlacementTargetForCandidate` | score 公式被改（distanceSqr×0.12、丢 3 项、误加 backwards 守卫） | 还原 `rotationScore + distanceSqr×0.01 + |ΔeyesToSqr|×0.001 + placementSideScore + lastPlacedBridgePlacementScore` |
| `placementTargetScore`（双参/单参） | 被改成 ticks 混合组成 | 还原 score 族：`placementRotationScore + minimumPlacementTargetScore + placementSideScore`，双参加 `mathTellyConservationScore + lastPlacedBridgePlacementScore` |
| `minimumPlacementTargetScore` | `×0.42` | `×4.0` |
| `placementSideScore` | `DOWN?1:0` | `UP?0 : 横向?0.05 : 0.1` |
| `isDownwardsPlacementSupport` | `side==UP` | `side==UP && isDownScaffoldActive()` |
| `mathSearchPathPenaltyTicks` | 误用 lastPlacedBridgePathPenalty | 还原 getPreRotationDirection 对齐惩罚 |
| `placementRotationTicks` | 丢 movementAlignmentTicks | 补回 `mathMovementAlignmentScore(yaw) / max(1, expectedYawSpeed)` |
| `rotationTicks` | 阈值 0 / MAX_VALUE | Krs 阈值 `1e-3` / `POSITIVE_INFINITY` |
| `horizontalDistanceToBlock` | 到方块中心距离 | 还原到方块 AABB 的轴距离（列内为 0） |
| `mathPitchSpeedBounds` | 丢 safeMin 下限钳制 | 补回 `safeMin = max(min(1, safeMax), safeMin)` |
| `movementFixMathInputYaw` | 丢 MovementFix 模块门控 | 补回 `!MovementFix.enabled → null`（关闭时禁用整套移动对齐评分） |
| `clearMathPlacementTarget` | 只清 locked target | 补回 humanized aim 三字段重置 |

### 3. Disabler2 —— Verus KeepAlive silent 语义（`module/misc/Disabler2.java`）

Verus disabler 的 KeepAlive 重发此前走 `mc.getConnection().send` + re-entry
守卫（会过完整发送管线并触发递归风险）。按 Krs silent 语义改写：
cancel 原包 + `PacketUtil.sendPacketAsSilent(new ServerboundKeepAlivePacket(随机化 id))`
——silent 包不发 SendPacketEvent，但仍过 ModifyPacketEvent 与
recordOutgoingRotation，与 Krs `consumeSilentPacket` 行为一致。

### 4. BehaviorUtils —— isBot 语义修复（`util/krs/BehaviorUtils.java`）

`isBot` 此前接错模块（旧 AntiBot，语义相反：把 tab 列表外玩家当 bot）。
按 Krs 结构修复：仅当 AntiBot2 开启且实体在 `AntiBot2.inBotList` 中才判 bot；
`fillTargetList` / `isTarget` 改引 KillAura2 的 `closestEntity` / `multiTargets`，
恢复 Krs 的 multiTargets 循环。该修复同时解决 KillAura2 的目标筛选。

### 5. 审计结论（无改动确认一致）

- **KillAura2**：onHandleInput / attackCooldownMath / faceSmartRotation /
  performAttack / attackToEntity / finishAttackCycle / autoBlock 序列 / reset /
  teleport 序列 / helpers 全部与 Krs 语义一致，差异仅为 API 适配
  （`.getValue()`、26.3 swing 签名、`getLast()`→`get(size()-1)`）。
- **RotationManager**：除 §2.1 一处外全文逐行一致。
- **MovementFix / Sprint2 / NoSlow2 / WTap / MaceExploit / Velocity2 /
  Backtrack2 / LongJump2 / AntiBot2**：b8 已修复接线，本次复核一致。

## 三、构建

- `gradle.properties`：`mod_version=b8 → b9`
- `./gradlew build` 通过，产物 `dioxideliteng-b9.jar`（含 sources jar）。
