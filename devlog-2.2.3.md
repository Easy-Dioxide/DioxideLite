# DioxideLite v2.2.3 Devlog

**发布日期：2026-10-01**
**Commit：6fb26d9**

---

## v2.2.3 更新内容

### Vape 移植：SilentAura（静默自动攻击）
- 移植自 Vape-v4 `gg/vape/module/combat/SilentAura.java`，用于战斗分类。
- **重写适配 26.1.2**：
  - 去掉 Vape wrapper 层，改用 Mojang 官方映射（`mc.player` / `LivingEntity` / `Vec3` / `AABB`）。
  - 去掉 1.7.10 legacy 分支、ForgeVersion 版本分支。
  - 去掉 AI 模型分支（Vape ModelManager / MLP），仅保留 PID 旋转引擎。
  - Value 系统改为 DioxideLite Setting；事件 `@EventHandler` → `@Listen`。
  - 旋转交由 RotationManager（setRotations + Rot2f + Priority），移动修正 / 视觉转头由 MovementFix + renderAnimation 接管。
- **保留 Vape 核心语义**：目标选择（Distance/Yaw/Armor/Threat/Health 排序）、PID 旋转微调（pitchProportionalGain + pitchIntegral）、Perfect Swing（攻速刻度==1.0 才攻击）、瞄准抖动（SilentAuraAimJitter）、自适应最近可见点（RotationUtils.calculate adaptive）。

### Vape 移植：Clutch（掉落自动放方块）
- 移植自 Vape-v4 `gg/vape/module/blatant/Clutch.java`（1.7.10 → 26.1.2 语义重写），用于移动分类。
- **按 Vape 语义重写**：1.7.10 的 `PlayerControllerMP.processRightClickBlock` / `ItemStack.tryPlaceItem` / `Block.canPlaceBlockAt` 在 26.x 签名全变（走 `BlockPlaceContext` / `useOn`），故非逐行移植。
- 落点预测改用 DioxideLite `FallingPlayer`（等价 Vape PlayerSimulationUtil）；方块放置改用 `BlockPlaceHelper.findPlaceInfo + place`（原生 `useItemOn`）；旋转改用 RotationManager + RotationUtils.calculate(BlockPos)。
- **保留 Vape 核心语义**：掉落检测（预测落点，掉入虚空或低于阈值高度触发）、自动放方块接住、旋转到放置点（可选 Silent）、物品栏自动切换到可放置方块。

### MovementFix：Vape movementCorrection 三种模式
- 新增三个选项：**VapeNone**（ClientSettings.NO_MOVEMENT_CORRECTION，完全不修正）、**VapeSlow**（SLOW_MOVEMENT_CORRECTION：yaw+180° 反向 + 重映射按键，输入幅度减半，减速防不规则速度）、**VapeProper**（PROPER_MOVEMENT_CORRECTION：yaw=静默 yaw + 重映射 W/A/S/D 到 45° 桶，等价 Silent）。
- 新增 `quantizeToAngle()` 复用核心算法（原 fixMovement），Vape Proper/Slow 与原 Silent/Setting 共用。

### Lua 内置脚本自动释放
- **LuaScriptManager**：打包内置脚本到 jar 资源（`/dioxide-lite/scripts/`），首次运行自动释放到用户配置 `scripts/` 目录，同名文件不覆盖（用户可编辑 / 重命名 / 停用）。
- **SpeedTelly.lua 适配新版 Lua 沙箱**：事件 `render` → `render2d`（用 `draw` 参数绘制）；luaj 无 `math.sign` 本地实现 `sign()`；右键改用 `input:mouse_down(1)`；`world:raycast + player:eye_pos` 改用 `world:block`（眼高 1.62）；`draw:line` 参数顺序 `(x1,y1,x2,y2,color,thickness)`。

### 网易云扫码登录修复
- **MusicScreen**：修复扫码已确认（qrLoginState==AUTHORIZED）但 `loadNCM` 拉不到 profile（cookie 无效 / 网络异常）时界面卡死、按钮不可点的问题——重置为 FAILED 并清理二维码图片，可重新扫码。
- **CloudMusic cookie 处理**：`803 响应` 的 cookie 字段保留全部条目，不再做 MUSIC_U / __csrf 白名单过滤，避免因凭证不全（__remember_me / NMTID 丢失）导致 `loginStatus()` / `loadUserPlaylists()` 失败、profile 变成 null。

---

## 验证

- 构建成功（JDK 25.0.4.1 + Gradle 9.2.1 + Fabric Loom 1.15.5）
- 产物 `build/libs/DioxideLite-2.2.3.jar`（约 54 MB）
- 游戏启动正常（Minecraft 26.1.2 Fabric），窗口标题 `DioxideLite 2.2.3`
- 主菜单 / 欢迎界面正常渲染
