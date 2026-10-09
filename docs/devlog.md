# DioxideLiteNG — Devlog / 版本开发日志

> 版本：**B1** ｜ Minecraft **26.3** ｜ Fabric **0.19.5** ｜ Java **25**（Temurin 25.0.4.1 LTS）
> 状态：构建通过 · 246 项单元测试全绿 · 运行时启动验证通过

---

## 一、版本定位

DioxideLiteNG（简称 DLNG）是面向 Minecraft 26.3（Fabric / Java 25）的**全新一代开源自研客户端**，采用 Mixin 运行时注入，无修改版 Minecraft 分发。本版本 B1 为整体重构后的首发版，源码规模约 **288 个 Java 文件、55 个功能模块**。

相较于旧代 DioxideLite 代码树，B1 按新包名 `com.dioxideliteng` 重建，统一品牌、终端身份与启动体验，并同步完成 UI 渲染管线（NanoVG / JavaFX WebView / LWJGL 3.4.3）适配。

## 二、构建环境与产物

| 项目 | 值 |
| --- | --- |
| Gradle Wrapper | 9.6.1 |
| JDK | Temurin 25.0.4.1 LTS |
| Loom | 1.15.5 |
| 依赖仓库 | Fabric / Maven Central / CCBlueX snapshots / lenni0451 releases |

构建命令：

```shell
./gradlew build
```

产物（`build/libs/`）：

- `dioxideliteng-b1.jar`（71 MB，含内嵌依赖与各平台 JavaFX/LWJGL native）
- `dioxideliteng-b1-sources.jar`（12.9 MB）

测试：**246 项**（JUnit 5 / Jupiter），skipped 0，failures 0，errors 0。

## 三、主要特性

### 启动与品牌
- 自定义 **LaunchIntro 启动动画**：boot → login → connection → emblem 四阶段，含「轮回」标识、TERMINAL SERVICE 登录窗、SAMSARA 字样（约 13 秒）。
- 主菜单（明日方舟主题）：「风暴瞭望 STORMWATCH」活动入口、干员立绘（莫斯提马）、终端数值面板、日期横幅。
- 渐变品牌消息前缀（每字符按色相渐变渲染）。

### 模块体系（55 个）
- **Combat（10）**：KillAura、AimAssist、AutoClicker、AutoRod、Criticals、TriggerBot、Velocity、AntiBot、SprintReset、TargetSettings
- **Movement（13）**：Speed、Flight、LongJump、Blink、Timer、Stasis、Sprint、KeepSprint、InventoryMove、NoSlow、AntiSwim、AutoWalk、MovementCorrection
- **Player（13）**：Scaffold、BedAura、ChestStealer、Backtrack、Eagle、AutoHead、AutoTool、FastMine、FastPlace、InventoryManager、LagRange、NoFall、NoJumpDelay
- **Visual（13）**：ClickGui、Hud、NameTags、PlayerEsp、Scoreboard、Ambience、Animations、AntiFire、BedPlates、Cape、FullBright、NoHurtCamera、Theme
- **Misc（3）**：Disabler、Whitelist、WindCharge

### 界面与交互
- **ClickGUI**：Opai / Rockstar / Neverlose 三种主题布局，左键开关、右键展开设置。
- **HUD 编辑器**：铅笔按钮进入，自由拖拽排版。
- **DynamicIsland 灵动岛**：延迟 / 状态悬浮岛。
- **账户管理**：微软登录（JavaFX WebView 内嵌）、Altening 会话、账户保存与切换。
- **终端主题**（TerminalPage / TerminalConfirmScreen / TerminalMultiplayerScreen）。

### 工程与安全
- 分布式事件总线（EventBus）、模块生命周期管理（FeatureManager）。
- 原子化配置持久化（AtomicFiles / ClientStateStore）、`.config save/load` 预设。
- Mixin 注入覆盖渲染 / 输入 / 网络 / 菜单等多个切面。

## 四、运行验证

- 环境：Xvfb（DISPLAY=:99）+ llvmpipe 软渲染（Mesa 23.2.1，OpenGL 4.5 Core）。
- 结果：客户端成功加载 5 个 mod（dioxideliteng b1 / fabricloader 0.19.5 / minecraft 26.3 / mixinextras 0.5.5），进入主菜单并正常渲染启动动画与明日方舟主题界面（截图见 `shots/`）。
- 已知：软渲染下帧率受限（约 4~7 FPS），属 llvmpipe 环境正常现象；无声卡设备存在 ALSA 音频告警，不影响功能。

## 五、使用方式

1. 按 `./gradlew build` 构建得到 `build/libs/dioxideliteng-b1.jar`。
2. 为 Minecraft 26.3 安装 Fabric Loader 0.19.5+。
3. 将 jar 放入 `mods` 目录，用 Java 25 启动 Fabric 档位。
4. **Right Shift** 打开 ClickGUI；聊天输入 `.help` 查看指令；`.config save/load <name>` 管理预设。
5. 自动状态存于 `dioxideliteng/state.json`，预设存于 `dioxideliteng/configs/`（游戏目录下）。

## 六、许可

GNU General Public License v3.0（详见 LICENSE）。第三方依赖与素材保留各自许可证。
