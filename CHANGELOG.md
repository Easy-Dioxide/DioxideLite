# Changelog

## B11（2026-10-21）

- 修复 Scaffold2 转头不完整导致的 GrimAC 大量 simulation VL——Krs 移动模拟闭环三处断链全部修复：
  - `MixinEntity#moveRelative` 补 `MovementFix` / `TargetStrafe` 分支（Krs `EntityMixin#updateVelocity`）：移动向量改用 spoofed yaw，客户端移动系与服务端模拟系对齐（主因）。
  - `MixinLivingEntity#jumpFromGround` 补全 directionYaw 链（Krs `LivingEntityMixin#jumpEvent`）：冲刺跳跃 0.2 加成沿 spoofed 系施加，接线 `Sprint2#getOmnidirectionalJumpYaw`，加 LocalPlayer 守卫。
  - `MixinLocalPlayer#preMotion` 补发包前沿 rotationManager 现读（Krs `ModuleManager#onMotion`）：挂 `Events.UPDATE` 的模块转头不再旧一跳。
- 补 `MixinEntity` 的 `targetStrafeCollisionGuard`（Krs `Entity#move` 变量守卫）。
- 审计留档：26.3 协议已移除 `ServerboundSwingPacket`（vanilla `useItemOn` 同样不发），`Scaffold2#tryPlaceBlock` 的 `player.swing(...)` 即 26.3 等价物，无需改动。
- 版本号 `b10 → b11`，构建产物 `dioxideliteng-b11.jar`、`dioxideliteng-b11-sources.jar`。

详细记录见 [devlog-b11.md](devlog-b11.md)。

## B10（2026-10-20）

- 修复 KillAura2 `"no order"` 摆动模式多发 `ServerboundPunchPacket`（Krs 该模式不产生摆动包）。
- 修复 KillAura2 hypixel ncp 自动格挡门控查错模块：legacy `NoSlow` → Krs 对等的 `NoSlow2`。
- 修复 Blink2 缺失的 Freecam 门控（Krs `!getModuleState(Freecam.class)`），`FeatureManager.freecam` 补为静态字段。
- 移植 Krs `Rotations` 渲染模块：新增 `Rotations`、`GuiEntityRenderGuard`、`MixinAvatarRenderer`、`MixinGuiEntityRenderer`，重写 `MixinLivingEntityRenderer` 的三个 vanilla 旋转替换钩子。
- 审计留档：`swingHandWithoutPacket` 守卫由 26.3 原生的 `SwingState#startIfAble` 提供，无需改动；`preferTargetId` 未移植（无 UI 对应，行为等价）。
- 版本号 `b9 → b10`，构建产物 `dioxideliteng-b10.jar`、`dioxideliteng-b10-sources.jar`。

详细记录见 [devlog-b10.md](devlog-b10.md)。

## B9（2026-10-19）

- 打滑模块全面对照 Krs 审计：Scaffold2 数学放置评分 16 处还原、RotationManager 身体摆动条件精确化、Disabler2 Verus KeepAlive 走 silent 通道、BehaviorUtils `isBot` 语义修复。

详细记录见 [devlog-b9.md](devlog-b9.md)。

## B8（2026-10-18）

- 接线 Krs MovementFix routine（`updateInput` / 疾跑启停条件）；修复 Scaffold2 疾跑逻辑退化；修复 WTap / MaceExploit 攻击钩子事件身份比较。

详细记录见 [devlog-b8.md](devlog-b8.md)。

## B7（2026-10-15）

- 以 B3 为基座重建，全量移植 Krs 非可视化模块：102 个功能模块、9 个新事件与全部分发点、35 个 utils。

详细记录见 [devlog-b7.md](devlog-b7.md)。

## B3（2026-10-13）

- 主菜单与终端页立绘更换为明日方舟「维什戴尔 · 绝对主角」皮肤。

详细记录见 [devlog-b3.md](devlog-b3.md)。

## B2（2026-10-09）

- 视觉改版：重绘启动动画（LaunchIntro 四阶段，轮回标识 / TERMINAL SERVICE）。
- 主菜单干员主题视觉升级（风暴瞭望 STORMWATCH / 干员立绘 / 终端数值面板）。
- ClickGUI / HUD 视觉调整，HUD 编辑器入口优化。
- 全平台构建（Windows / Linux / macOS）验证通过，246 项单元测试全绿。

详细记录见 [devlog-b2.md](devlog-b2.md)。
