# Changelog

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
