# DioxideLite

<div align="center">
    <img width="128" src="docs/dioxide-lite-logo.png" alt="DioxideLite logo">
</div>

**DioxideLite** 是一个基于 Mixin 运行时注入的开源 Minecraft 客户端（Fabric），为 Minecraft **26.3** 打造：保留流畅的 HUD 视觉、ClickGUI 与启动体验，并以 **KRS 全量内核移植**的方式提供完整的战斗 / 移动 / 世界 / 杂项能力。

> 当前版本：**B13** · 平台：Fabric · 游戏版本：Minecraft 26.3 · Java 25 · 支持 Windows / Linux / macOS

---

## 版本沿革

| 版本 | 定位 |
| --- | --- |
| B2 | 视觉改版：启动动画、主菜单干员主题、ClickGUI / HUD 视觉调整 |
| B3 | 主菜单立绘更换为「维什戴尔 · 绝对主角」 |
| B7 | 以 B3 为基座重建，首次移植 Krs 非可视化模块（102 个）+ 事件分发层 |
| B8 | 修复 B7 运行期缺陷：MovementFix routine 接线、Scaffold2 疾跑、WTap / MaceExploit 攻击钩子 |
| B9 | 打滑模块全面对照 Krs 审计：Scaffold2 数学放置评分还原、RotationManager 摆动条件精确化 |
| B10 | 修复 B9 遗留偏差；移植 Krs `Rotations` 渲染模块 |
| B11 | 修复 Scaffold2 转头导致的 GrimAC simulation VL（移动模拟闭环三处断链） |
| B12 | Scaffold2 全量排查，定位「模拟 VL」用户侧根因为 Movement Fix 未手动开启 |
| **B13** | **改用整套内核移植**：搬入 KRS 运行时（133 模块类 + 86 Mixin），以桥接层接入 NG 界面 |

> B7–B12 走的是「按模块逐个重写成 NG 风格」的路线，B13 改为「保留 KRS 内核 + 加适配层」。两条路线的差异见 [devlog-b13.md](devlog-b13.md) 第二节。

## 特性

### 界面与品牌

- **启动动画**：LaunchIntro 四阶段（boot → login → connection → emblem），「轮回」品牌标识 + TERMINAL SERVICE 登录窗，约 13 秒。
- **主菜单（干员主题）**：「风暴瞭望 STORMWATCH」活动入口、干员立绘、终端数值面板（理智 / 等级 / 关卡进度 / 日期横幅）。
- **渐变品牌消息前缀**：聊天每字符按色相渐变渲染。
- **ClickGUI**：多主题布局，左键开关模块、右键展开设置，GUI Scale 可调。
- **HUD 编辑器**：ClickGUI 右下角铅笔按钮进入，自由拖拽排版（RESET / DONE）。
- **DynamicIsland 灵动岛**：顶部胶囊信息面板，实时显示客户端版本 / FPS / 延迟 / 模块开关提示。
- **账户管理**：微软登录（内嵌 WebView）、Altening 会话、账户保存与切换。

### 模块体系

**KRS 内核移植（129 个模块类，界面注册 125 个）**

| 分类 | 数量 | 代表模块 |
| --- | --- | --- |
| Combat | 16 | KillAura2、Velocity2、Backtrack2、Criticals2、AntiBot2、AutoTotem2、Reach、WTap、MaceExploit、CrossbowExploit |
| Movement | 24 | Speed2、LongJump2、Stasis2、Sprint2、NoSlow2、InventoryMove2、ElytraFly、Jesus、Spider、Step、Parkour |
| Player | 23 | Scaffold2、ChestStealer2、InvManager2、NoFall2、AutoTool2、Freecam、Phase、SpinBot、MiddleClick |
| Render | 24 | NameTags2、FullBright2、ClickGui2、ESP、Rotations、ViewModel、Zoom、PlayerIndicator |
| Exploit | 18 | Blink2、XCarry、PortalGodMode、PacketDuper、ChestTP、PathFinder、NoBreakReset |
| Level | 14 | Timer2、Nuker、Xray、CaveFinder、NoteBot、Spammer、Breaker、CivBreak |
| Dev/Misc | 9 | Disabler2、PluginsDetector、FukumaiPlayerTracker、ChatExcepChecker、BlockBreakSimulator2 |

**NG 原生模块（53 个）**

保留 B1–B3 时期的全部原生模块，与 KRS 模块共存于同一 ClickGUI。名称冲突的 22 个已自动加 `2`，例如 `KillAura`（NG 原生）与 `KillAura2`（KRS 版）。

### 工程与安全

- **静默转头（silent aim）**：`RotationManager` 三态（client / server / render）旋转引擎，转头值与发包值分离。
- **事件系统**：NG 的 `EventBus`（priority 分发）+ KRS 的 `EventManager` 双总线并存，由适配层协调。
- **配置持久化**：原子化写入（AtomicFiles / ClientStateStore）、`.config save/load` 预设。
- **Mixin 注入**：NG 原有 + KRS 移植共 **140 个**，覆盖渲染 / 输入 / 网络 / 菜单多个切面。
- **Access Widener**：`krs.accesswidener`（126 行）。

## 截图

| 启动动画 | 主菜单 |
| --- | --- |
| ![启动动画](docs/screenshots/b2-loading.png) | ![主菜单](docs/screenshots/b2-main-menu.png) |

| ClickGUI | HUD 游戏界面 |
| --- | --- |
| ![ClickGUI](docs/screenshots/b2-clickgui.png) | ![HUD 游戏界面](docs/screenshots/b2-hud.png) |

## 使用方式

1. 从 [Releases](https://github.com/Easy-Dioxide/DioxideLite/releases) 下载 `dioxideliteng-b13.jar`，或自行构建。
2. 为 Minecraft 26.3 安装 Fabric Loader 0.19.5+。
3. 将 jar 放入 `mods` 目录，用 Java 25 启动 Fabric 档位。
4. **Right Shift** 打开 ClickGUI；聊天输入 `.help` 查看指令；`.config save/load <name>` 管理预设。
5. 自动状态存于 `dioxideliteng/state.json`，预设存于 `dioxideliteng/configs/`（游戏目录下）。

> **踩坑提示**：开 `Scaffold2` 时必须同时开启 `Movement Fix`（Movement 分类）。KRS 源码无自动启用逻辑，若只开 Scaffold2，客户端按真实 yaw 移动、发送欺骗旋转，每 tick 位置分歧会产生大量 GrimAC simulation VL。

## 构建

使用 JDK 25（Temurin 25.0.4.1 LTS）：

```bash
./gradlew clean build
```

产物位于 `build/libs/`：

- `dioxideliteng-b13.jar`（83.8 MB，含内嵌依赖与各平台 natives）
- `dioxideliteng-b13-sources.jar`（19.9 MB）

## 源码规模

| 项目 | 数量 |
| --- | --- |
| Java 源文件 | 648 |
| KRS 移植部分 | 396 |
| 功能模块 | 182（KRS 内核 129 + NG 原生 53） |
| Mixin | 140（KRS 86 + NG 原生 54） |

## 更新日志

| 版本 | Devlog |
| --- | --- |
| B13 | [devlog-b13.md](devlog-b13.md) — KRS 全量内核移植（133 模块类 + 86 Mixin），桥接层接入 NG 界面，22 个重名模块加 `2` |
| B12 | [devlog-b12.md](devlog-b12.md) — Scaffold2 全量排查，定位模拟 VL 根因为 Movement Fix 未开启 |
| B11 | [devlog-b11.md](devlog-b11.md) — 修复 Scaffold2 转头导致的 GrimAC simulation VL（移动模拟闭环三处断链） |
| B10 | [devlog-b10.md](devlog-b10.md) — 修复 no order 摆动包 / hypixel ncp 门控 / Blink2 Freecam 门控；移植 Krs `Rotations` |
| B9 | [devlog-b9.md](devlog-b9.md) — 打滑模块全面对照 Krs 审计，Scaffold2 数学放置评分还原 |
| B8 | [devlog-b8.md](devlog-b8.md) — 接线 Krs MovementFix routine，修复 Scaffold2 疾跑与 WTap / MaceExploit 钩子 |
| B7 | [devlog-b7.md](devlog-b7.md) — 以 B3 为基座全量移植 Krs 非可视化模块（102 个） |
| B3 | [devlog-b3.md](devlog-b3.md) — 主菜单立绘更换为「维什戴尔 · 绝对主角」 |
| B2 | [devlog-b2.md](devlog-b2.md) — 视觉改版：启动动画与主菜单视觉升级 + ClickGUI / HUD 视觉调整 |

详细更新记录见 [CHANGELOG.md](CHANGELOG.md)。

## 致谢

绕过逻辑参考并移植自 [Krs](https://github.com/Aspw-w/Krs)（作者 Aspw-w）。B13 起采用整套内核移植，仅做包名重映射与事件系统适配，模块逻辑保持原样。

## 许可

本项目基于 **GPL-3.0-or-later** 开源（详见 [LICENSE](LICENSE)）。

## 联系

QQ：**81622964**
