# DioxideLite v2.1.1 — Devlog

> 2026-09-21 · DioxideLite Team

---

## 中文版

### 背景

v2.1.1 是在 2.1.0（命令 / 歌词 / 聊天 HUD）基础上，把 **SetsunaClient（上游开源版）的视觉模块与全部视觉功能** 全量并入 DioxideLite 的版本。本次为**全量执行**：含 C 级战斗 / 移动基建（KillAura 体系与 Scaffold 引擎），因此上游原版的 **TargetHud** 与 **ScaffoldBlockHUD** 已可直接使用。

规模：**新增 144 个 java、修改 22 个 java、删除 0**；新注册模块 **43** 个、新登记 mixin **29** 个；编译 0 错误、1530 class；构建脚本与 67 个嵌套库已补齐。

### 本轮开发过程

1. **版本号**：确认源码基底为 2.1.0（含命令系统，IrcChatHandler / ChatScreenMixin / ChatComponentMixin / CommandBuilder / command 包均在），本次在 `gradle.properties` 与 `DioxideLite.java` 中将版本号 **2.0.9 → 2.1.1** 统一（移植包作者漏改的版本串一并修正）。

2. **移植的模块（按分类）**：
   - **世界渲染（本轮新增）**：`HoleESP`、`Tracers`、`OreTracers`、`SpawnerFinder`、`UHCDetector`、`Xray`
   - **战斗**：`KillAura`、`KillAuraPlus`、`AntiBot`、`AutoTotem`、`AutoHitCrystal`、`MaceAura`、`SpearKill`、`Surround`、`Burrow`、`Criticals`、`FakeLag`、`Backtrack`、`ZealotCrystalPlus`
   - **移动**：`Scaffold`、`Velocity`、`KeepSprint`、`MovementFix`、`InvMove`、`NoSlow`、`NoFall`、`NoJumpDelay`、`FlatElytraFly`、`Speed`
   - **玩家**：`ChestStealer`、`InvManager`、`AutoTool`、`AutoMLG`、`FastBreak`、`FastCraftModule`、`GhostHand`、`PacketEat`、`AntiWeb`、`AntiResourcePack`、`BedAura`、`FakePlayer`
   - **其他**：`MiddleClickFriend`、`AltManagerModule`（此前**从未被注册**，永远打不开，本轮补上注册）
   - **HUD**：`TargetHud` 与 `ScaffoldBlockHUD` 改为上游原版实现（走 KillAura→KillAuraPlus→TargetManager 优先级链 / 直接读 Scaffold 方块计数与放置栈）

3. **随附基建**：`KillAuraPlus` 引擎与 12 个配置/枚举类、`AntiBot` 引擎 4 个类、`TargetManager`/`TargetRequest`、`RotationManager`、`HealthManager`、`util/rotation/` 4 个类、`Scaffold` 引擎 8 个类、`InvMove` 引擎 5 个类、`BlinkManager`、`FallingPlayer`，以及 **7 个新事件**（`RaytraceEvent`、`RotationAnimationEvent`、`AfterRotationEvent`、`SendPositionEvent`、`StrafeEvent`、`KeyboardInputEvent`、`FallFlyingEvent`）。ClickGUI 接入音乐色卡预览（`MusicPresetPreview`）；补齐 Sodium / Indigo 兼容路径（4 个 Sodium mixin + `IndigoAltModelBlockRendererMixin`）。

4. **修复的缺陷（8 处，均在移植包内完成并随 2.1.1 生效）**：
   1. **全局翻译层失效**：语言文件全部以 `DioxideLite.` 开头，而代码用 `MOD_ID`（`dioxide-lite`）拼前缀 → 改 4 处键构造点为 `NAME`；移植模块开箱即中文。
   2. **模块开关提示整条链路是死的**：唯一触发调用被注释掉 → 恢复调用（开关默认 `false`，默认表现不变）。
   3. **Block Offset 滑块无读取方**：`CombatVisuals.blockOffset` 全工程只出现 1 次 → 由 `ItemInHandRendererMixin` 新增处理器消费。
   4. **mixin accessor 前缀不一致（编译阻断）**：`DioxideLite$` 大写驼峰 vs Mixin 约定小写 `dioxidelite$` → 统一 6 个 accessor 接口并同步 7 处调用点。
   5. **Constants 目录与包声明不一致**：`com/github/setsuna/Constants.java` 包声明却是 `com.github.DioxideLite` → 文件移到位（内容零改动）。
   6. **音乐界面直接显示 "SETSUNA"**：→ 改为 `DIOXIDELITE SELECTION` / `DIOXIDELITE RECORDS`。
   7. **CommandManager 缺 API 致命令系统无法编译**：缺 `addCommand` / `commands` → 替换为上游完整版（含分词、Levenshtein 纠错提示、自动补全、历史记录）。
   8. **Tracers 设置名笔误**：`"TargetHUD"` → `"Target"`。

5. **启动相关（移植包已在 Windows 实测）**：
   - 补齐 `assets/setsunavia/**` 资源树（77 个文件，10 个 data 映射 + 42 语言 + 粒子/贴图/icon），修复 `DioxideLiteViaMappingDataLoader` 的 NPE 启动崩溃。
   - access widener 追加 `accessible class ...InterpolationHandler$InterpolationData`（PositionInterpolator1_8 需要）。
   - 补齐 `libs/nested` 67 个嵌套库 + `annotations.jar` + `modmenu-18.0.0-alpha.8.jar`。
   - 启动实测通过：Fabric Loader 完整加载 52 mods、430 个 mixin 条目零错误、应用 access widener 成功、引导到 `Setting user`（止于无 GPU 机器的 OpenGL 后端创建，属环境限制）。

6. **本轮构建验证（Linux / JDK 25 / Fabric Loom 1.15.5）**：
   - `gradlew build` **BUILD SUCCESSFUL**（4m10s），`compileJava` 0 错误、`validateAccessWidener` 通过，产出 `DioxideLite-2.1.1.jar`（约 88.8MB）与 `DioxideLite-2.1.1-sources.jar`（约 27.8MB）。
   - 已核对 jar 内 `fabric.mod.json version=2.1.1`、`DioxideLite.class` 含 2.1.1 且无 2.0.9 残留；同时含 2.1.0 命令类（IrcChatHandler / ChatScreenMixin / CommandBuilder 等）与 2.1.1 全部视觉模块（HoleESP / KillAura / Scaffold / Xray / Tracers / TargetHud / LuaScript / MusicPresetPreview 等）。
   - `gradlew runClient` 冒烟测试于 Xvfb + llvmpipe 软渲染下进行，验证客户端可启动、Fabric 完整加载。

### 已知说明

- 本次全量执行含上游战斗 / 移动基建（KillAura 体系与 Scaffold 引擎），**与 v2.0.0「移除自动化模块」的边界不同**——这是移植包的既定交付范围，是否在正式版保留请开发者确认。
- 世界渲染 6 模块只依赖 `Render3DUtils` / `WorldToScreen` / `SkijaUi` 等叶子工具类，没有向核心层循环加边（该环是上游架构缺陷，已在前置审计记录）。
- 回退：所有改动保留原代码，新增内容以 `[DioxideLite 修复]` / `[DioxideLite 移植]` 前缀中文注释标注并写明回退方式，完整前后对比见移植包的 `patches/`。

---

## English

### Background

v2.1.1 builds on 2.1.0 (commands / lyrics / chat HUD) by fully porting **SetsunaClient's visual modules and entire visual feature set** into DioxideLite. This is a **full port** — it includes C-tier combat / movement infrastructure (the KillAura family and the Scaffold engine), so the upstream **TargetHud** and **ScaffoldBlockHUD** now work out of the box.

Scale: **+144 new java, +22 modified, 0 deleted**; **43** newly registered modules, **29** newly registered mixins; 0 compile errors across 1530 classes; build scripts and 67 nested libs provided.

### What happened this cycle

1. **Version** — Confirmed the source base is 2.1.0 (command system present: IrcChatHandler / ChatScreenMixin / ChatComponentMixin / CommandBuilder / command package). Bumped the version string to **2.1.1** in both `gradle.properties` and `DioxideLite.java` (the port pack had left it at 2.0.9).

2. **Ported modules (by category)**:
   - **World rendering (new this release)**: `HoleESP`, `Tracers`, `OreTracers`, `SpawnerFinder`, `UHCDetector`, `Xray`
   - **Combat**: `KillAura`, `KillAuraPlus`, `AntiBot`, `AutoTotem`, `AutoHitCrystal`, `MaceAura`, `SpearKill`, `Surround`, `Burrow`, `Criticals`, `FakeLag`, `Backtrack`, `ZealotCrystalPlus`
   - **Movement**: `Scaffold`, `Velocity`, `KeepSprint`, `MovementFix`, `InvMove`, `NoSlow`, `NoFall`, `NoJumpDelay`, `FlatElytraFly`, `Speed`
   - **Player**: `ChestStealer`, `InvManager`, `AutoTool`, `AutoMLG`, `FastBreak`, `FastCraftModule`, `GhostHand`, `PacketEat`, `AntiWeb`, `AntiResourcePack`, `BedAura`, `FakePlayer`
   - **Other**: `MiddleClickFriend`, `AltManagerModule` (was **never registered** before — the account manager could never be opened; registration added)
   - **HUD**: `TargetHud` and `ScaffoldBlockHUD` switched to upstream originals (KillAura→KillAuraPlus→TargetManager priority chain / direct Scaffold block counts & placement stack)

3. **Supporting infrastructure** — KillAuraPlus engine + 12 config/enum classes, AntiBot engine (4 classes), TargetManager/TargetRequest, RotationManager, HealthManager, util/rotation (4 classes), Scaffold engine (8), InvMove engine (5), BlinkManager, FallingPlayer, and **7 new events** (RaytraceEvent, RotationAnimationEvent, AfterRotationEvent, SendPositionEvent, StrafeEvent, KeyboardInputEvent, FallFlyingEvent). ClickGUI music color-card preview (`MusicPresetPreview`); Sodium / Indigo compatibility path (4 Sodium mixins + `IndigoAltModelBlockRendererMixin`).

4. **Defects fixed (8, done in the port pack, live in 2.1.1)**:
   1. **Whole translation layer dead** — language keys all start `DioxideLite.` but code used `MOD_ID` (`dioxide-lite`) → switched 4 key-construction sites to `NAME`; ported modules are Chinese out of the box.
   2. **Module-toggle notification chain dead** — the only trigger call was commented out → restored (default `false`, unchanged default behavior).
   3. **Block Offset slider had no reader** — `CombatVisuals.blockOffset` appeared once in the whole tree → consumed by a new `ItemInHandRendererMixin` handler.
   4. **Mixin accessor prefix mismatch (compile blocker)** — `DioxideLite$` camel-case vs Mixin's lower-case `dioxidelite$` → unified 6 accessor interfaces + 7 call sites.
   5. **Constants dir/package mismatch** — `com/github/setsuna/Constants.java` declared `com.github.DioxideLite` → file moved (content unchanged).
   6. **Music screen showed "SETSUNA"** → now `DIOXIDELITE SELECTION` / `DIOXIDELITE RECORDS`.
   7. **CommandManager missing API broke the command system** — no `addCommand` / `commands` → replaced with the full upstream version (parsing, Levenshtein suggestions, completion, history).
   8. **Tracers setting-name typo** — `"TargetHUD"` → `"Target"`.

5. **Launch-related (already verified on Windows in the port pack)** — backfilled the `assets/setsunavia/**` resource tree (77 files) fixing the `DioxideLiteViaMappingDataLoader` NPE startup crash; appended `InterpolationHandler$InterpolationData` to the access widener; added the 67 `libs/nested` libs + annotations.jar + modmenu. Launch test passed: Fabric loads 52 mods, 430 mixin entries with zero errors, access widener applied, boots to `Setting user` (stops only at the OpenGL backend on a GPU-less machine — environment limitation).

6. **This cycle's build verification (Linux / JDK 25 / Fabric Loom 1.15.5)** — `gradlew build` **BUILD SUCCESSFUL** (4m10s), `compileJava` 0 errors, `validateAccessWidener` passed, producing `DioxideLite-2.1.1.jar` (~88.8MB) and `DioxideLite-2.1.1-sources.jar` (~27.8MB). Verified the jar carries `fabric.mod.json version=2.1.1`, `DioxideLite.class` has 2.1.1 and no 2.0.9 remnant, and contains both the 2.1.0 command classes and the full 2.1.1 visual set. `gradlew runClient` smoke test runs under Xvfb + llvmpipe software rendering.

### Known notes

- This full port brings back the upstream combat / movement infrastructure (KillAura family and Scaffold engine) — a deliberate scope change vs. v2.0.0's "automation modules removed". Whether to keep it in the release build is up to the developer.
- The 6 world-rendering modules depend only on leaf utilities (Render3DUtils / WorldToScreen / SkijaUi) and add no edge to the core-layer cycle.
- Rollback: all changes keep the original code; new content is marked with `[DioxideLite 修复]` / `[DioxideLite 移植]` Chinese comments with rollback notes; full before/after diffs are under the port pack's `patches/`.
