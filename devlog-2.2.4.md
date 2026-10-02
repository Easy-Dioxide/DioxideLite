# DioxideLite v2.2.4 Devlog

**发布日期：2026-10-02**

---

## v2.2.4 更新内容

### 静默旋转引擎重构（Vape PID）
- **RotationManager 改用 Vape 风格 PID 加速步进**（`vapeSmooth`，移植自 `gg.vape.rotation.FixedRotationController`），替换原 `RotationUtils.smooth` 的简单 lerp：
  - 每 tick 以「鼠标灵敏度单位」步进（与游戏内 mouseSensitivity 换算一致），剩余角度越大加速度越高（angle-based），模拟真人甩枪轨迹。
  - 绕过 Grim（rotation velocity/acceleration）、Matrix（snap/angle 检测）、NCP（rotation speed/head-body consistency）的旋转检测。
  - 容差内直接对准（避免在目标点抖动）+ 微抖动（避免每 tick 完美瞄准被检测）。
  - yaw/pitch 按误差比例缩放步进，限制单 tick 最大步数避免被判定为 snap。
- **新增 `AdaptiveRotationController`**（移植自 Vape-v4 PID 旋转控制器 `AdaptiveRotationController + FixedRotationController`）：支持 angle-based / linear / cubic 三种加速度、轴比例缩放、容差、aim jitter，可与 RotationManager 静默发包链路集成。

### SilentAura 重构
- 改用 **TargetManager / TargetRequest / FriendManager** 统一目标管理（好友豁免、目标请求优先级）。
- 新增 **Show Target + Target Color**：高亮当前攻击目标。
- 设置项整理：
  - `Attack Cps` → `Attack Speed (CPS)`；`Target Players/Mobs/Animals` → `Players/Mobs/Animals`。
  - 移除 `Require Mouse Down` / `Disable On Death` / `Through Walls`；新增 `Invisibles`、`Randomize CPS`、`Show Target`。
- 转头统一走 RotationManager（内置 Vape PID 加速步进）。

### 新模块
- **LegitScaffold（移动）**：合法搭桥——`Place Check` 落点校验、`Require Sneak` + `Sneak Delay`、`Auto Sprint`、可调 `Aim Speed`。
- **AttackEffects（渲染）**：自定义攻击特效——暴击/锋利粒子、暴击/击退音效开关；配 `LevelMixin` 取消被禁用的玩家攻击音效。
- **BreakProgress（渲染）**：方块破坏进度可视化。
- **NoFOV（渲染）**：锁定 FOV，禁用速度/缓行等效果带来的视角变化。
- **NoHurtCamera（渲染）**：取消受伤镜头摇晃，附 `No Player Model Hurt` 选项。
- **PostProcessing（渲染）**：后处理模糊（Blur）/ Bloom，可调半径。
- **StreamerMode（渲染）**：隐藏服务器 ID / 用户名，支持自定义显示名，过滤聊天消息（ChatComponentMixin）。
- **TitleChanger（渲染）**：修改窗口标题。
- **新增 utility 目录**：`Clutch` 由 movement 移入 utility（分类仍为 PLAYER）。

### 移除装饰模块
- 移除 **ChinaHat（彩虹锥帽+光晕拖尾）、Wings（翅膀）、JumpCircles（跳跃光圈）、Trails（实体拖尾）**——纯装饰，不符合 Opal 简洁风格；**Halo** 一并移除。
- 如需启用，将对应 `INSTANCE` 加回 `RenderModuleRegistry` / `ModuleManager` 数组即可。

### ESP 增强
- 新增 **Box / Box Stroke / Box Color / Health Bar / Health Bar Stroke / Health Bar Color** 等选项。

### 视角与聊天适配
- **LivingEntityMixin**：头部跟随静默旋转；身体也跟随静默旋转（Vape 风格「打滑」），第三人称下玩家模型身体对准目标方向。
- **ChatScreenMixin**：聊天界面打开/关闭时 HUD 覆盖层同步开关。
- **MultiPlayerGameModeAccessor**：补充方块操作访问器（配合放置类模块）。

### 修复
- **LevelMixin**：`playSound` 注入目标首参 `Player` → `Entity`（MC 26.1.2 实际签名），修复 AttackEffects 音效取消导致的混入注入崩溃。
- **NoFOV**：FOV 锁定注入从 `GameRenderer.getFov`（该方法在 26.1.2 中不存在）移入 `Camera.getFov`（MC 26.1.2 的实际位置）。

---

## 验证

- 构建成功（JDK 25.0.4.1 + Gradle 9.2.1 + Fabric Loom 1.15.5）
- 产物 `build/libs/DioxideLite-2.2.4.jar`（约 51 MB）
- 游戏启动正常（Minecraft 26.1.2 Fabric），窗口标题 `DioxideLite 2.2.4`，主菜单 / 欢迎界面正常渲染
