# Changelog

## B13（2026-10-26）

- **改用整套内核移植**：不再逐模块重写成 NG 风格，改为把 KRS 运行时整体搬入，加桥接层接入 NG 界面。
  - KRS 模块类 133 个、Mixin 86 个、accesswidener 1 个，依赖全部内嵌并改包名。
  - 包重映射：`com.instrumentalist.krs` → `com.dioxideliteng.krs`，`com.instrumentalist.mixin.*` → `com.dioxideliteng.mixin.krs`，`org.nvgu` → `com.dioxideliteng.krs.nvgu`，`xyz.breadloaf.imguimc` → `com.dioxideliteng.krs.imguimc`。
- **`org.json` 改用 Gson 兼容层**：org.json 全部版本号都是纯日期（`20240303`），不满足 Fabric semver 校验，嵌套 jar 会被拒绝加载。改写一份保留 `org.json` 包名的 Gson 实现，KRS 业务代码零改动。
- **新增 `KrsModuleAdapter` 桥接层**：把 KRS `Module` 包装成 NG `Feature`，`BooleanValue` / `FloatValue` / `IntValue` / `ListValue` 映射为 NG 设置项并每 tick 双向同步。
- **新增 `KrsModuleRegistry`**：按类名 + 显示名归一化判定重名（KRS 显示名带空格、NG 不带，直接比较永不相等），22 个冲突模块加 `2`。
- **修复分类映射缺陷**：`ModuleCategory` 枚举常量是 `Combat` 而非 `COMBAT`，字符串比较会导致所有模块静默落入 MISC；改为按枚举对象匹配并补 `default`。
- **补注册 `FukumaiPlayerTracker`**：该模块在 KRS 中位于 `devModules`，原实现漏注册，按白名单并入。
- **界面处理**：不搬 KRS 的 NanoVG / ImGui 界面，`NanoVGClickGuiScreen` 改为空占位，`ModuleRenderable` 退化为转发壳，删除「右 Shift 打开 KRS 界面」逻辑（与 NG ClickGui 抢键）。
- **验证**：`clean build` 通过；86 个 Mixin 目标类全部存在（79 个命中 MC 26.3，7 个指向 Sodium / Iris / Indigo）；158 个注入点逐一对过字节码方法表，0 失败；启动日志 `[KRS-Port] bridge ready, 124 modules registered`，无 Mixin 应用错误。
- **历史沿用**：B7–B12 的重写路线被整体替换，B10–B12 的修复未迁入（B13 用 KRS 原版实现，从根上规避了那些重写偏差）。但 B12 的使用结论依然成立：**开 Scaffold2 必须同时开 Movement Fix**。
- 版本号 `b12 → b13`，构建产物 `dioxideliteng-b13.jar`、`dioxideliteng-b13-sources.jar`。

详细记录见 [devlog-b13.md](devlog-b13.md)。

## B12（2026-10-22）

- Scaffold2 全量排查（对照 Krs 逐行）：模块本体无逻辑漏洞——输入钩子、5 处 MovementFix 调用点、11 处 tower 速度操纵、事件订阅、telly 链全部对应。
- 定位"模拟 VL"用户侧根因：**Movement Fix 是独立手动开关，默认关闭，Scaffold2 开启不会自动带开**（Krs 全源码同样无自动启用逻辑）。Scaffold2 转头中而 Movement Fix 关闭时，客户端按真实 yaw 移动、发送 spoofed 旋转，每 tick 位置分歧即 simulation VL。**用法：开 Scaffold2 必须同时开 Movement Fix（Movement 分类）。**
- b10 报 VL 属必然：moveRelative/jumpFromGround 的 MovementFix 分支为 b11 新增，b10 上开不开 Movement Fix 都断链；已升级 b11 并开启 Movement Fix 后链路闭合。
- 修正 Scaffold2 / MovementFix 两处过时头部注释（原"MovementFix dropped / mixin 未接线"描述与现状矛盾），补 B11 反作弊闭环说明。
- 版本号 `b11 → b12`，无语义变化，jar 行为与 b11 相同。

详细记录见 [devlog-b12.md](devlog-b12.md)。

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
