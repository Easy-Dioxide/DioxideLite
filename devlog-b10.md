# DioxideLite — Devlog / 版本开发日志（B10）

> 版本：**B10** ｜ Minecraft **26.3** ｜ Fabric **0.19.5** ｜ Java **25**（Temurin 25.0.4.1 LTS）
> 状态：只读审计收口 · KillAura2 / Blink2 三处语义偏差修复 · 移植 Krs `Rotations` 渲染模块

---

## 一、版本定位

B10 建立在 B9 的审计基础上，完成三项工作：

1. **修复 B9 审计中发现、但当时按「只检查不改」要求未动的三处偏差**；
2. **移植 Krs 的 `Rotations` 渲染模块**（`hacks/features/render/Rotations`），
   把转头系统的第三人称渲染链路补全；
3. 审计中确认「疑似问题但实际正确」的两项，补上结论与依据，避免后续误改。

本轮所有改动均与 Krs 源码逐行对照，并用 26.3 字节码核实注入点。

---

## 二、修复清单

### 1. KillAura2 — `"no order"` 摆动模式多发包（`module/combat/KillAura2.java`）

Krs `performAttack` 的三个分支语义：

| 模式 | Krs 行为 | 26.3 等价 |
| --- | --- | --- |
| `1.8` | `swing()` 后 `attack()` | `swing(hand, DEFAULT, false)` + `attack` + `ServerboundPunchPacket` |
| `1.9.x` | `attack()` 后 `swing()` | `attack` + `swing(hand, DEFAULT, false)` + `ServerboundPunchPacket` |
| `no order` | `attack()` + `swingHandWithoutPacket()` | **仅 `attack`，不发** `ServerboundPunchPacket` |

26.3 的 `LivingEntity#swing` 只更新客户端摆动状态、不发包，因此 `1.8` / `1.9.x`
必须补发 `ServerboundPunchPacket`（等价于旧版 `ServerboundSwingPacket`）才能让
服务端看到挥击；而 `no order` 的设计意图就是**不产生摆动包**，B9 版在该分支
多补了一个 `ServerboundPunchPacket`，B10 删除。

### 2. KillAura2 — hypixel ncp 自动格挡门控查错模块（`module/combat/KillAura2.java`、`module/movement/NoSlow2.java`）

Krs `finishAutoBlock` 的 `hypixel ncp` 分支：

```java
if (ModuleManager.getModuleState(NoSlow.class))
    spoofHypixelNoSlowSlot();
```

Krs 的 `NoSlow` 是模式为 `Vanilla` / `Hypixel NCP` 的减速绕过模块，对应本项目的
**`NoSlow2`**（B7 移植）。B9 版却接到了 legacy `FeatureManager.noSlow`（倍速型旧模块），
后果是：开 `NoSlow2` 时不发换槽欺骗包、开旧 `NoSlow` 时反而会发。

B10 改为 `NoSlow2.inst()` 的空值与启用判断，并把 `NoSlow2.inst()` 由 `private`
提升为 `public` 供跨包调用，与 `Scaffold2` 等移植模块的实例解析方式一致。

### 3. Blink2 — 缺失 Freecam 门控（`module/movement/Blink2.java`、`module/FeatureManager.java`）

Krs `ModuleManager.onSendPacket` 的 blink 队列条件是：

```java
BlinkUtil.INSTANCE.getBlinking() && Blink.shouldBlinkOutgoing()
    && !BlinkUtil.INSTANCE.getLimiter() && !getModuleState(Freecam.class)
```

B7 移植时注释里写着「本客户端没有 Freecam」，但 `Freecam` 实际存在且已注册，
B10 补上该门控：`FeatureManager.freecam` 补为静态字段并在注册时赋值，Blink2
在 Freecam 启用时整段跳过 blink 发包拦截（与 Krs 一致）。同步更正了过期注释。

---

## 三、新移植：Krs `Rotations` 渲染模块

### 3.1 原模块

`com.instrumentalist.krs.hacks.features.render.Rotations` —— 转头系统的
**第三人称渲染开关**，只有一个 `Vanilla` 布尔设置（默认 `true`），模块默认启用。

作用是决定静默转头（silent aim）期间，本地玩家的第三人称模型怎么画：

- **Vanilla 开**（默认）：把 RotationManager 的插值结果喂进 vanilla 的状态提取
  路径，让头部/身体/俯仰按欺骗朝向走原生动画；
- **Vanilla 关**：在 `AvatarRenderer#extractRenderState` 末尾直接覆写渲染状态
  （转头时身体跟着头转，回正时走 `isReturning` 动画）。

### 3.2 移植产物

| 文件 | 类型 | 说明 |
| --- | --- | --- |
| `module/visual/Rotations.java` | 新增 | `VISUAL` 分类（Krs `Render` 对应本项目 `VISUAL`），`Vanilla` 设置，构造时 `setEnabled(true)` 对齐 Krs `tempEnabled = true`；提供 `shouldUseVanilla()` / `isModuleEnabled()` |
| `util/krs/GuiEntityRenderGuard.java` | 新增 | 逐字移植的深度计数器，标记 GUI 纸人渲染期，渲染钩子在此期间不介入 |
| `mixins/MixinLivingEntityRenderer.java` | 重写 | Vanilla 模式下三个 `@ModifyExpressionValue`：`solveBodyRot` → 插值身体 yaw、`Mth.rotLerp`(ordinal 0) → 插值头部 yaw、`LivingEntity#getXRot` → 插值俯仰；三人均只对本地玩家、且模块启用 + `Vanilla` 开启时生效 |
| `mixins/MixinAvatarRenderer.java` | 新增 | Vanilla 关闭时在 `extractRenderState` RETURN 覆写 `bodyRot` / `yRot` / `xRot`（含 returning 分支）；`extractCapeState` 的 `rotLerp` 替换为披风身体朝向跟随转头 |
| `mixins/MixinGuiEntityRenderer.java` | 新增 | `@WrapMethod renderToTexture` 包一层 `GuiEntityRenderGuard` |
| `dioxideliteng.mixins.json` | 修改 | 注册 `MixinAvatarRenderer`、`MixinGuiEntityRenderer` |
| `module/FeatureManager.java` | 修改 | 注册 `Rotations` |

### 3.3 注入点字节码核实

26.3 `LivingEntityRenderer#extractRenderState` 的实际顺序：

```text
offset  8  getfield  LivingEntity.yHeadRotO
offset 13  getfield  LivingEntity.yHeadRot
offset 16  invokestatic Mth.rotLerp          <- ordinal 0，头部 yaw
offset 26  invokestatic solveBodyRot          <- 身体 yaw
offset 40  invokestatic Mth.wrapDegrees       <- yRot = head - body
offset 49  invokevirtual LivingEntity.getXRot <- 俯仰
```

确认 `rotLerp` 序号 0 是**头部**而非身体，Krs 的替换目标与本项目一致。
另核实 `AvatarRenderer#extractRenderState(Avatar, AvatarRenderState, float)` 首条指令
即为 `invokespecial LivingEntityRenderer.extractRenderState`，因此
`@At("RETURN")` 覆写发生在父类提取之后，不会被后续覆盖。

### 3.4 行为矩阵

| Rotations 模块 | Vanilla 设置 | 转头时第三人称模型 |
| --- | --- | --- |
| 关 | — | 纯 vanilla 渲染（服务端看到的朝向不可见） |
| 开 | 开（默认） | vanilla 提取路径注入插值旋转 |
| 开 | 关 | `AvatarRenderer` 末尾直接覆写，身体跟随头部 |

---

## 四、审计结论（不改动，留档）

### 4.1 `PlayerUtil.swingHandWithoutPacket` —— 疑似缺守卫，实际不需要改

Krs 版本手工实现摆动画守卫：

```java
if (!player.swinging || player.swingTime >= duration / 2 || player.swingTime < 0) {
    player.swingTime = -1; player.swinging = true; player.swingingArm = hand;
}
```

本项目直接调用 26.3 的 `player.swing(hand, SwingAnimation.DEFAULT, false)`。
反编译 `LivingEntity$SwingState#startIfAble` 后确认 26.3 **原生内置了同样的守卫**：

```text
currentSwing == null  -> start()
ticks > duration / 2  -> start()
ticks <= 0            -> start()
否则不重启
```

且 `getModifiedSwingDuration` 的急迫/疲劳计算与 `PlayerUtil#getSnglSwingHandDuration`
完全一致（本项目仅移除了 Krs 的 ViewModel 调速钩子并注释说明）。边界条件
`>=` 与 `>` 差一个 tick，仅影响第一人称手臂动画，**不影响发包**，因此不改。

### 4.2 `preferTargetId` 未移植

Krs `KillAura` 有 `TextValue "Prefer Target ID"` 设置，本项目无对应 UI 组件。
该值在 `compareTargets` 中只作为距离比较**之前**的 tiebreaker，无配置时
恒为默认值、与不设置行为等价，故不移植（已在 `KillAura2` 头部注释记录）。

---

## 五、构建

- `gradle.properties`：`mod_version=b9 → b10`
- `./gradlew compileJava` / `./gradlew build` 通过
- 产物：`build/libs/dioxideliteng-b10.jar`、`dioxideliteng-b10-sources.jar`
- jar 内容校验：`Rotations.class`、`GuiEntityRenderGuard.class`、
  `MixinAvatarRenderer.class`、`MixinGuiEntityRenderer.class` 均已打包，
  `dioxideliteng.mixins.json` 中三个渲染 Mixin 均已注册
