# DioxideLite v2.0.3 Development Log / 开发日志

## English

### v2.0.3 — IRC Link & Name Tag Module

**Goal**: Bring the OpticsValleyIRC 1.20 chat link into 26.1.2 (Player-category module, ON by default), surface the client logo on name tags of IRC-online players (via the existing name-tag module), add IRC status to the Dynamic Island tab, keep the client free of automation, and close out the F6 theme-switch crash plus the leftover module-state toasts.

**What changed**
- `irc/` package ported from `opticsvalleyirc-fabric-1.20.1`: `IRCProtocol`, `IRCClientConfig`, `IRCClient` (socket client with reconnect backoff, online-user set maintained from the server's join/leave frames, state listeners), `IrcNameTagUtil`, `IrcChatHandler`. The legacy remote CRASH control is deliberately ignored.
- `IrcModule` (ClickGUI → Player, `setEnabled(true)` by default) with `Host`/`Port` settings; registered in `ModuleManager`.
- Name tags: `EntityRendererMixin` fills the local player's `nameTag`/attachment (26.1.2 leaves it null), then `LegendSuffixUtil.appendIfLegendary` → `IrcNameTagUtil.appendLogoIfIrc` (bitmap font glyph `\uE101` prepended with a space). The logo toggle lives on the name-tag module (Render → Legend Watch → `IRC Logo`, default ON).
- Dynamic Island expanded state gained an IRC status line (`IRC Off` / `Connecting...` / `Online · N online`) and a taller panel.
- `ChatScreenMixin` intercepts `/irc ...` before sending to the server (Fabric client command v2 API is unavailable in this environment).
- `Module.setEnabled` no longer auto-posts module-state toasts.
- `SkijaRenderer.BACKDROP_DOWNSAMPLE` restored to 0.5.

**Bugs fixed**
- F6 theme cycling while ClickGUI is open no longer crashes (verified over several cycles).
- Right-top "Dynamic Island / Enabled" toasts gone.
- Own name tag now renders in third person with the IRC logo (MC 26.1.2 leaves local `nameTag` null — forced fill + attachment).

**Verification**
- Built `DioxideLite-2.0.3.jar` + `-sources.jar`; ran under Xvfb (`:98`) with a local IRC simulator (`16688`): `Player540 joined`, chat bridge alive.
- Screenshots: main menu 2.0.3 / single player / multi player / in-game watermark / ClickGUI (Player → IRC ON, Render → Dynamic Island) / island Tab with `IRC Online · 1 online` / third-person name tag `logo Player540` / chat `[OpticsValleyIRC] 已连接到IRC服务器`.
- F6 ×3 while ClickGUI open: no crash. Module toggles: no top-right toasts.

---

## 中文

### v2.0.3 — IRC 接入与 Nametag 模块

**目标**：把 OpticsValleyIRC 1.20 的聊天链路移植到 26.1.2（Player 分类模块、默认开启）；通过现有 Nametag 视觉模块在 IRC 在线玩家 nametag 上显示客户端 Logo；灵动岛 Tab 增加 IRC 状态；保持客户端无自动化能力；收尾 F6 主题切换崩溃与模块状态通知残留。

**改动**
- 新建 `irc/` 包（移植自 opticsvalleyirc-fabric-1.20.1）：协议、配置、`IRCClient`（Socket 客户端，重连退避、由服务端 join/leave 帧维护在线名单、状态监听）、nametag 工具、聊天命令处理。**刻意忽略远程 CRASH 控制指令**。
- `IrcModule`（ClickGUI → Player，构造即 `setEnabled(true)` 默认开）带 `Host`/`Port` 设置，注册进 `ModuleManager`。
- Nametag：`EntityRendererMixin` 补填本地玩家 `nameTag` 与位置附件（26.1.2 默认留空），随后传奇后缀 → IRC Logo（bitmap 字形 `` + 空格前置）；Logo 开关在 Nametag 模块（Render → Legend Watch → `IRC Logo`，默认开）。
- 灵动岛展开态新增 IRC 状态行（`IRC Off` / `Connecting...` / `Online · N online`）并加高面板。
- `ChatScreenMixin` 拦截 `/irc ...`（本环境 Fabric client command v2 API 不可用）。
- `Module.setEnabled` 不再自动弹模块状态通知。
- `SkijaRenderer.BACKDROP_DOWNSAMPLE` 恢复 0.5。

**修复**
- ClickGUI 打开时 F6 循环切换主题不再崩溃（多轮实测）。
- 右上角 "Dynamic Island / Enabled" 通知消失。
- 第三人称自己的 nametag 正常渲染并带 IRC Logo（26.1.2 本地玩家 nameTag 为 null → 强制填充 + 附件）。

**验证**
- 构建 `DioxideLite-2.0.3.jar` + `-sources.jar`；Xvfb `:98` + 本地 IRC 模拟服务器（`16688`）运行：`Player540 joined`，聊天桥接正常。
- 截图：主菜单 2.0.3 / 单人 / 多人 / 游戏内 watermark / ClickGUI（Player → IRC 开，Render → Dynamic Island 开）/ 灵动岛 Tab `IRC Online · 1 online` / 第三人称 nametag `logo Player540` / 聊天栏 `[OpticsValleyIRC] 已连接到IRC服务器`。
- ClickGUI 打开时 F6 ×3：不崩溃；模块开关：右上角无通知。

---
# DioxideLite v2.0.2 Development Log / 开发日志

## English

### v2.0.2 — Global Blur & Built-in Optimisers

**Goal**: Kill the last "resolution/downsampling" shortcuts, give users a proper global blur switch, ship mainstream optimiser mods inside the jar, and keep every HUD sharp at full resolution.

**What changed**
- `GlobalBlurModule` (ClickGUI → Render): global master switch for HUD backdrop blur. OFF by default → `drawBlurredBackdrop` returns immediately, which also prevents the per-frame backdrop snapshot; ON → one `Blur Strength` slider (1–16, default 6) overrides every HUD's own blur radius. All HUD blur funnels through this single choke point.
- Bundled Sodium 0.8.12 + Lithium 0.24.7 + FerriteCore 9.0.0 via Fabric jar-in-jar (`include(implementation(...))`; Loom injects `fabric.mod.json` `jars` automatically). Verified all three load with zero mixin conflicts.
- Module-state toasts (`"Dynamic Island / Enabled"`) are now OFF by default; module enable/disable no longer spams the top-right corner (re-enable in ClickGUI).
- Downsampling retired: `BACKDROP_DOWNSAMPLE` forced to 1.0 and `downsampleBackdrop()` only runs below 1.0 — full-resolution blur, no visual tradeoff.
- Bounded LRU caches for text metrics (`TEXT_WIDTH_CACHE` 4096, `TEXT_RUN_CACHE` 2048) to stop unbounded growth from churning FPS/ping keys.
- `HudFusionManager` layout fingerprint cache: screen size + per-HUD enabled/x/y/w/h XOR fingerprint; unchanged screens reuse the fused layout instead of re-running the O(n²) BFS every frame.

**Bug fixed**
- Top-right "Dynamiodsland" garbage text: mixed-font fallback runs each computed their own baseline, so segments of one line stacked vertically. `drawTextWithFallback` now uses a single primary-font baseline for every run.
- Full-clean build errors from the user-edited tree: duplicate `cachedCompactRightText` in `DioxideDynamicIsland`, and 146+ errors in dead third-party code (`repackage/**` javazoom mp3 + processing sound, `tritium/**` ncm) → excluded from compilation (sources kept).

**Verification**
- Main menu / single-player / multi-player screenshots captured (DIOXIDELITE 2.0.2).
- In-game: watermark + island + performance HUD all on; island render ~0.03–0.1 ms; no toasts; ClickGUI ring + Global Blur settings panel (Blur Strength slider) verified.
- 13–16 FPS under llvmpipe software rendering (optimiser gains appear on real GPUs).

---

## 中文

### v2.0.2 — 全局模糊与内置优化模组

**目标**：彻底弃用"降分辨率/降采样"取巧手段，提供正式的全局模糊开关，内置主流优化模组，所有 HUD 保持全分辨率锐利渲染。

**改动**
- **GlobalBlur 模块**（ClickGUI → Render）：HUD 背景模糊总开关，**默认关闭** → `drawBlurredBackdrop` 直接返回（同时跳过每帧 backdrop 快照）；开启后 `Blur Strength` 滑块（1–16，默认 6）统一覆盖各 HUD 模糊强度。所有 HUD 模糊收敛到唯一入口。
- **jar-in-jar 内置** Sodium 0.8.12 + Lithium 0.24.7 + FerriteCore 9.0.0（`include(implementation(...))`，Loom 自动注入 `fabric.mod.json` 的 `jars`）；实测三模组正常加载、零 mixin 冲突。
- **模块状态通知默认关闭**：启用/禁用模块不再在右上角弹出 "Dynamic Island / Enabled" 等提示（可在 ClickGUI 重新打开）。
- **降采样退役**：`BACKDROP_DOWNSAMPLE` 固定 1.0，且仅 <1.0 时才执行 `downsampleBackdrop()`——全分辨率模糊，不牺牲画质。
- **文本缓存加 LRU 上限**（宽度 4096 / 分段 2048），杜绝 FPS/Ping 换 key 导致的无界增长。
- **HudFusionManager 布局指纹缓存**：屏幕尺寸 + 各 HUD enabled/位置/尺寸异或指纹；画面未变直接复用融合布局，不再每帧跑 O(n²) BFS。

**修复的 Bug**
- 右上角 "Dynamiodsland" 乱码文字：混合字体 fallback 分段各自计算 baseline 导致同一行文字上下堆叠；`drawTextWithFallback` 现统一使用主字体单一 baseline。
- 用户改版源码的全量编译错误：`DioxideDynamicIsland` 重复字段；`repackage/**`（javazoom mp3、processing sound）与 `tritium/**`（ncm）第三方死代码 146+ 错误 → 编译排除（源码保留）。

**验证**
- 主菜单 / 单人 / 多人界面截图（DIOXIDELITE 2.0.2）。
- 游戏内 watermark + 灵动岛 + 性能 HUD 同屏，灵动岛渲染 0.03–0.1ms，无右上角通知；ClickGUI 环形菜单 + Global Blur 设置面板（强度滑块）验证通过。
- llvmpipe 软渲染下 13–16 FPS（优化模组收益在真实 GPU 上体现）。

---


## English

### v2.0.1 — OPAI Dynamic Island

**Goal**: Redesign the dynamic island in the OPAI style (dark glass pill with white inner edge and glow) and ship it inside the shared Skija overlay pass, while keeping the client free of any automation/cheat modules.

**What changed**
- `DioxideDynamicIsland` rewritten: animated pill (sine float), dark glass gradient body, white inner outline, top gloss line, cyan-blue glow layers, and the client logo.
- Compact mode: logo + `DioxideLite v2.0.1` + FPS + latency in one pill, top-center.
- Expanded mode (hold Tab): panel grows to show server name, latency and the player list, sorted by tab-list order; width/height ease smoothly between states.
- Hooked into `SkijaRenderer.renderOverlay()` — renders only in-game (no screen open), on the same canvas as HUD modules.

**Build/run issues found and fixed**
1. `Paint.FilterQuality.HIGH` does not exist in Skija 0.143 → replaced with `SamplingMode.LINEAR` and the strict `drawImageRect(image, src, dst, SamplingMode, paint, true)` overload.
2. First launch kept the v2.0.0 runtime data; world saves were reused from the parity build to verify in-game rendering.
3. Xvfb instability on the test box (3 GB RAM, llvmpipe software GL) — the client was relaunched with a stable `setsid` Xvfb; rendering verified at 3–4 FPS software path.

**Verification**
- Main menu shows `DIOXIDELITE 2.0.1`.
- In-game: compact island visible top-center (log confirmed `render called: w=176 h=30 x=125.5 y=7`), expanded island on Tab (`w=340 h=72` with server/latency/player list).
- ClickGUI Pop ring opens on right Shift; all six categories (Client/Combat/Misc/Render/Movement/Player) render.
- No crash reports; `Skija renderer failed` absent; no automation modules registered (module list remains pure visual/QoL).

---

## 中文

### v2.0.1 — OPAI 灵动岛

**目标**：以 OPAI 风格（暗色玻璃胶囊 + 白色内边 + 辉光）重做灵动岛，接入共享 Skija 覆盖层发布，同时保持客户端无任何自动化/作弊模块。

**改动**
- 重写 `DioxideDynamicIsland`：动画胶囊（正弦浮动）、暗色玻璃渐变主体、白色内描边、顶部光泽、蓝青辉光层、客户端 LOGO。
- 紧凑态：LOGO + `DioxideLite v2.0.1` + FPS + 延迟，单行胶囊，顶部居中。
- 展开态（按住 Tab）：面板平滑扩展，显示服务器名、延迟、玩家列表（按 Tab 列表序排序），宽高在两态间缓动过渡。
- 挂载到 `SkijaRenderer.renderOverlay()`——仅游戏内（无 Screen）渲染，与 HUD 模块同画布。

**构建/运行中发现并修复的问题**
1. Skija 0.143 无 `Paint.FilterQuality.HIGH` → 改为 `SamplingMode.LINEAR` + 严格模式 `drawImageRect(image, src, dst, SamplingMode, paint, true)` 重载。
2. 首次启动沿用 v2.0.0 运行时数据；复用对齐版世界存档验证游戏内渲染。
3. 测试机 Xvfb 不稳定（3GB 内存、llvmpipe 软件 GL）——用 `setsid` 稳定 Xvfb 后重启客户端，软件渲染 3–4 FPS 下验证通过。

**验证**
- 主菜单显示 `DIOXIDELITE 2.0.1`。
- 游戏内：顶部中央紧凑胶囊可见（日志确认 `render called: w=176 h=30 x=125.5 y=7`）；按 Tab 展开（`w=340 h=72`，含服务器/延迟/玩家列表）。
- 右 Shift 打开 ClickGUI Pop 环形菜单，六大分类（Client/Combat/Misc/Render/Movement/Player）正常渲染。
- 无崩溃报告；无 `Skija renderer failed`；无自动化模块注册（模块列表保持纯视觉/QoL）。
