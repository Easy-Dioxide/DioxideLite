# DioxideLite — Devlog / 版本开发日志（B8）

> 版本：**B8** ｜ Minecraft **26.3** ｜ Fabric **0.19.5** ｜ Java **25**（Temurin 25.0.4.1 LTS）
> 状态：修复 b7 运行期缺陷 · 接线 Krs MovementFix routine · 版本号升级 b7 → b8

---

## 一、版本定位

B8 是一次**缺陷修复版**，不改移植范围，只修 b7 首次实机运行暴露的问题：

1. **Krs routine（MovementFix）未接线** —— 模块完整移植但零调用，移动修正与
   反作弊绕过链路完全失效；
2. **Scaffold2 疾跑逻辑退化** —— telly 背身时始终疾跑、`tryStartSprinting`
   退化为本地简易版；
3. **WTap / MaceExploit 攻击钩子失效** —— 事件身份比较写错，永不触发。

三个问题均已按 Krs 源码逐一比对修复，改动共 6 个文件。

## 二、修复清单

### 1. MovementFix routine 接线（`mixins/MixinKeyboardInput`、`mixins/MixinLocalPlayer`）

b7 把 `MovementFix` 整个模块搬了过来，但**没有任何 Mixin 调用它**：
`updateInput` / `hasForwardImpulseForSprint` / `shouldFixSprintCondition`
全部是死代码，静默旋转下的输入重映射从未发生。

B8 按 Krs `KeyboardInputMixin` / `ClientPlayerEntityMixin` 补上全部钩子：

| 钩子 | 注入点 | 作用 |
| --- | --- | --- |
| `MovementFix.updateInput(input)` | `MixinKeyboardInput#tick` TAIL（POST_MOVE_INPUT 派发**前**） | 把原始按键按 spoof 旋转扇区重映射，覆写 `keyPresses` + `moveVector` |
| `movementFixSprintStartImpulse` | `aiStep` 唯一一处 `ClientInput#hasForwardImpulse()` | 疾跑启动条件读修正后输入 |
| `canStartSprintingHook` | `canStartSprinting()` `@ModifyReturnValue` | 复刻 vanilla 疾跑前置，叠加 NoSlow 豁免与修正脉冲 |
| `movementFixSprintStopImpulse` | `shouldStopRunSprinting` / `shouldStopSwimSprinting` 的 `hasForwardImpulse()` | 疾跑停止条件读修正后输入 |

`MixinLocalPlayer` 同步补齐 shadow：`input`（public 字段）、
`isSlowDueToUsingItem()`、`isSprintingPossible(boolean)`。
三个注入点均在 26.3 字节码中核实存在（`aiStep` 内 `hasForwardImpulse`
仅一次调用；两个 stop 方法各一次）。

### 2. Scaffold2 疾跑与移动修复（`module/player/Scaffold2.java`）

- `tryStartSprinting(player)` 本地简化版（只判 forward 键）删除，
  改为 `Sprint2.tryStartSprinting(player, keyPresses, true)`——
  走完整 Sprint 决策链（auto-sprint 设置、vanilla 式前置、wall check）；
- `requestTellyJump` 中**无条件 `sprinting = true`** 删除，
  恢复 Krs 的 `if (Sprint.tryStartSprinting(...)) sprinting = true;`
  —— 这是 telly 背身始终疾跑的直接原因；
- 三处移动方向读取改为 `MovementFix.shouldFixMovement()
  ? MovementFix.getMovementYaw() : player.getYRot()`
  （`getAutoSneakDirection` / `getAutoInputFixDirection`），
  `getScaffoldMovementDirection` / `scaffoldInputDirection`
  按 Krs 增加 `MovementFix.getRawInput()` 分支；
- `MovementFix` 新增公开静态 `isModuleEnabled()` 供上述分支查询模块状态。

### 3. WTap / MaceExploit 事件比较（`module/combat/WTap.java`、`MaceExploit.java`）

`MixinMultiPlayerGameMode#attack` 每次派发**新的** `EventAttack` 实例，
而两个模块用 `event != Events.ATTACK`（单例身份）比较，永不成立。
改为 `!(event instanceof EventAttack)`。已确认 `EventAttack`
全工程仅此一个派发点，无双发风险。

## 三、影响面

- **KillAura2 / Scaffold2 / 一切依赖静默旋转的模块**：修正后的输入与疾跑条件
  现在真正生效，服务器侧观察到的移动方向与朝向一致（反作弊绕过链路闭环）；
- **WTap / MaceExploit**：攻击钩子恢复触发；
- 其余模块行为不变（`shouldFixSprintCondition` /
  `shouldSprintOmnidirectional` 均为 false 时钩子原样放行）。

## 四、已知限制

- b7 的 `dioxideliteng-b7.jar` 未包含本版修复；请使用 b8 构建产物。
