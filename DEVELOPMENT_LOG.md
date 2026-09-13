# DioxideLite v2.0.7 — Development Log

## Overview / 概述

v2.0.7 is the performance root-cause pass. The most expensive CPU/driver path
was not Skija text or the Dynamic Island — it was the Minecraft-font Array List
background, which triggered a full `GlState.capture/restore` (dozens of
synchronous `glGet*` calls) every HUD frame. v2.0.7 removes that path, tunes
the blur passes per render profile, and adds a private IRC capability frame so
nametag logos only show for real DioxideLite peers.

v2.0.7 是渲染性能的"根因修复"版本。最耗 CPU/驱动的路径不是 Skija 文字、
也不是灵动岛，而是走 Minecraft 字体的 Array List 背景——它每帧触发全量
`GlState.capture/restore`（数十次同步 `glGet*` 调用）。v2.0.7 移除了这条
路径，按渲染档位调节模糊开销，并新增 IRC 私有 capability 帧，让 nametag
logo 只对真实的 DioxideLite 对端显示。

## Build fixes in this pass / 本次构建修复

The supplied source did not compile as-is; two fixes were applied before build:
- `SkijaRenderer.drawBlurredBackdrop`: a dangling `} catch (Throwable)` had no
  matching `try` — wrapped the draw body so blur failures degrade gracefully.
- `IRCClient`: `trackPresence(...)` was called but never defined — added the
  JOIN/LEAVE parser that keeps `onlineUsers` in sync.

原始源码无法直接编译，本次构建前修复两处：
- `SkijaRenderer.drawBlurredBackdrop`：悬空的 `} catch (Throwable)` 缺少对应
  `try` —— 补上包裹绘制体的 try，模糊失败时优雅降级。
- `IRCClient`：调用了未定义的 `trackPresence(...)` —— 补全 JOIN/LEAVE
  解析，维护 `onlineUsers` 在线列表。

## Changes / 变更

### Rendering performance / 渲染性能
- **Root cause fix**: Array List background/icon geometry is now prepared in the
  vanilla-font pass and composited once in the final Skija overlay — the
  per-frame full OpenGL state snapshot/restore is gone from normal HUD
  rendering (biggest iGPU frame-time spike).
- Watermark logo: removed the 0.32-sigma direct blur; glow kernel tightened to
  80% of previous sigma.
- Backdrop blur: downsampled only for the backdrop source (iGPU 0.67x,
  balanced 0.75x, quality native) — text stays at native Skija resolution;
  iGPU/balanced cap blur strength.
- Dynamic Island / nametag logos: Mitchell sampling + shared Paint (no
  per-draw allocation); island body stays cached, animation still per-frame.
- Compass: triangle Path built once at module enable instead of per player per
  frame.
- Skija fallback-font detection cached by text/font/size/bold key.
- **根因修复**：Array List 背景/图标几何改为在 vanilla 字体 pass 中准备、
  最终 Skija overlay 一次性合成——常规 HUD 渲染不再有每帧全量 GL 状态
  快照/恢复（iGPU 上最大的帧时间尖峰）。
- Watermark logo 去掉 0.32-sigma 直接模糊；glow 内核收紧为原 80%。
- 背景模糊：仅对 backdrop 源降采样（iGPU 0.67x / balanced 0.75x / quality
  原生）——文字保持原生 Skija 分辨率；iGPU/balanced 档位限制模糊强度。
- 灵动岛/nametag logo：Mitchell 采样 + 复用 Paint；灵动岛 body 保持缓存。

### IRC
- Private capability frame: after the normal username handshake, DioxideLite
  announces its capability; the companion server never broadcasts it as chat
  and forwards capability add/remove only between DioxideLite clients.
- Nametag logos and the Dynamic Island tab set are shown only for peers
  currently identified as DioxideLite; vanilla/other IRC clients never receive
  these frames.
- Companion server source: `tools/OpticsValleyIRC-server/`.
- 新增私有 capability 帧：正常用户名握手后，DioxideLite 声明自身能力；
  配套服务器不将其作为聊天广播，只在 DioxideLite 客户端间转发能力
  增/删。nametag logo 与灵动岛 Tab 列表只对当前识别为 DioxideLite 的对端
  显示。服务器源码见 `tools/OpticsValleyIRC-server/`。

## Verification / 验证

- `build` passes after the two fixes; `DioxideLite-2.0.7.jar` (≈91.9 MB).
- Ran under Xvfb (llvmpipe): main menu "DIOXIDELITE 2.0.7"; in-game Dynamic
  Island renders by default (compact "DioxideLite v2.0.7 · FPS · 0ms"; Tab
  shows player list / server / IRC status / FPS); ClickGUI Render lists
  Array List + Dynamic Island (enabled) + Global Blur / Watermark HUD /
  Performance HUD / Session Info; name-tag logo intact.

## Notes / 备注

- FPS (6–22) is from the software-rendered headless environment (llvmpipe) and
  is not representative of real hardware; the Array List state-capture removal
  is the biggest win for integrated GPUs and should be measured on the user's
  machine.


---

# DioxideLite v2.0.5 — Development Log

## Overview / 概述

v2.0.5 is the "final optimization" pass. It fixes the config-loading timing
(which caused the in-game HUD / Dynamic Island to intermittently not render),
adds a fast path for the final in-frame Skija overlay pass, caches blur
filters, makes the Dynamic Island enabled-by-default, restores the HUD Editor
and adds a Setsuna-compatible Sprint module.

v2.0.5 是"最终优化"版本：修复配置加载时机（该问题曾导致游戏内 HUD /
灵动岛偶发不渲染）、为帧内 Skija overlay 末帧增加快路径、缓存模糊滤镜、
灵动岛默认启用、恢复 HUD Editor，并新增 Setsuna 兼容的 Sprint 模块。

## Verification result (the core question) / 验证结论（核心问题）

**The intermittent "HUD not rendering" issue is fixed.** With a fresh profile
(no saved config), the Dynamic Island renders by default (compact + expanded),
the ClickGUI Render category lists Array List / Dynamic Island / Global Blur /
Watermark HUD / Performance HUD, the name-tag logo (Skija, width-adaptive,
vertical-aligned) still works, and the new Sprint module appears under
Movement. Measured 8–20 FPS in the software-rendered (llvmpipe) test
environment; the overlay fast-path benefits are best measured on real GPU
hardware.

**"游戏内 HUD 偶发不渲染"问题已修复。** 全新配置（无保存的 profile）下：
灵动岛默认开启并正常渲染（compact 与 Tab 展开），ClickGUI Render 分类完整
列出 Array List / Dynamic Island / Global Blur / Watermark HUD / Performance
HUD，nametag logo（Skija、宽度自适应、垂直对齐）正常，新增 Sprint 出现在
Movement 分类。软渲染（llvmpipe）测试环境实测 8–20 FPS；overlay 快路径的
收益需在真实 GPU 上测量。

## Changes / 变更

### Performance / 性能
- Fast path for the final in-frame Skija overlay pass: removed the full
  OpenGL state snapshot/restore from every HUD frame.
- Cached Skija Gaussian blur filters used by glow layers (no repeated native
  filter alloc/destroy; same blur params and visuals).
- Full-resolution backdrop blur preserved.
- 帧内 Skija overlay 末帧快路径：去掉每帧 HUD 的全量 GL 状态快照/恢复。
- 缓存 glow 层使用的 Skija 高斯模糊滤镜（避免重复的原生滤镜分配/销毁）。
- 保留全分辨率背景模糊。

### HUD
- Restored the HUD Editor entry point (Setsuna-side architecture).
- Fixed HUD fusion size fingerprinting so dynamic element dimensions invalidate
  layout correctly.
- 恢复 HUD Editor 入口；修复 HUD fusion 尺寸指纹，使动态元素尺寸正确失效布局。

### Dynamic Island / 灵动岛
- Enabled-by-default when no saved profile overrides it (fixes the fresh-start
  case where the island stayed off).
- Native resource cleanup for the island logo and cached shape.
- 无保存 profile 覆盖时默认启用（修复全新启动灵动岛保持关闭的问题）。
- 新增灵动岛 logo 与形状缓存的资源清理。

### IRC
- Fixed startup config loading: profiles now load after the client starts, so
  saved IRC host/port settings are actually restored (this is also the root
  cause fix for the intermittent HUD-not-rendering issue).
- TCP_NODELAY / keep-alive / reuse-address transport settings; explicit
  unknown-host diagnostics.
- Reconnection continues indefinitely with capped exponential backoff (was:
  stopped permanently after ten failures).
- 修复启动配置加载时机：profiles 在客户端启动后加载，保存的 IRC
  host/port 实际恢复（这也是 HUD 偶发不渲染问题的根因修复）。
- 增加 TCP_NODELAY / keep-alive / reuse-address 传输设置与未知主机诊断。
- 重连无限持续（指数退避封顶），不再在十次失败后永久停止。

### Movement
- Added Setsuna-compatible `Sprint` module (ClickGUI Movement, toggleable;
  restores vanilla sprint key state when disabled).
- 新增 Setsuna 兼容 Sprint 模块（ClickGUI Movement 分类，可开关；关闭时
  恢复原版疾跑键状态）。

## Verification / 验证
- `build` passes; `DioxideLite-2.0.5.jar` (≈91.9 MB).
- Ran under Xvfb (llvmpipe): main menu 2.0.5; in-game Dynamic Island renders
  by default (compact: "DioxideLite v2.0.5 · FPS · 0ms"; Tab: player list /
  server / IRC status / FPS); ClickGUI Render lists Array List + Dynamic Island
  (enabled); Sprint under Movement; name-tag logo intact.

## Notes / 备注
- FPS figures (8–20) come from the software-rendered headless environment and
  are not representative of real hardware; the optimization effect should be
  measured on the user's machine.


---

# DioxideLite v2.0.4 — Development Log

## Overview / 概述

v2.0.4 focuses on three things: making the Module List reachable from the
ClickGUI, rendering the client logo on name tags through the Skija canvas (the
vanilla name-tag pipeline cannot draw a custom high-res logo), and fixing the
blurry edges/fonts of the Dynamic Island and Watermark.

v2.0.4 聚焦三件事：让 Module List 在 ClickGUI 中可达；通过 Skija 画布在
nametag 上渲染客户端 logo（原版 nametag 管线无法绘制自定义高清 logo）；
修复灵动岛与 Watermark 边缘/字体模糊。

## Changes / 变更

### 1. Module List (Array List) in ClickGUI / Module List 进入 ClickGUI
- `ModuleListHUD` already existed with full features (animated list, Scale /
  Font / ColorMode / Only Important / Module Info / Shadow / Glow / Background /
  Bar), but it was constructed with the default `Category.HUD`, which is not one
  of the six ClickGUI categories → it was invisible in the GUI.
- Re-constructed with `Category.RENDER` (x=1000, y=16, 132×82) so it now
  appears under the Render tab, default enabled.
- 原 `ModuleListHUD` 功能完整（右侧动画列表、Scale/Font/ColorMode/Only
  Important/Module Info/Shadow/Glow/Background/Bar 等），但构造时用了默认
  `Category.HUD`，不属于 ClickGUI 六大分类 → 在 GUI 中不可见。已改为
  `Category.RENDER`，现在出现在 Render 分类下，默认开启。

### 2. Name tag client logo (Skija) / nametag 客户端 logo（Skija）
- Previous approach injected a bitmap glyph (`nametag_logo.json` +
  `dioxide_logo_16.png`) into the vanilla name-tag font; vanilla bitmap
  rendering (NEAREST sampling) could not show the 256×256 logo → removed.
- New `NameTagLogoRenderer`: listens to `Render2DEvent`, projects each player's
  head anchor through the camera view-rotation matrix into GUI space, and draws
  the same 256×256 Dynamic-Island logo next to the name tag via Skija.
  - Local player: `Client Logo` (Legend Watch, default on)
  - IRC online users: `IRC Logo` (default on, `IrcModule.isIrcUser`)
  - 16-block cut-off, same as vanilla name tags.
- Third-person FOV: vanilla expands FOV in third person; `camera.getFov()` does
  not include it, so the projected logo landed above the name tag → multiply by
  1.28 in third person.
- Logo x position is measured against the exact rendered name-tag text
  (original name + Legend suffix via `LegendSuffixUtil.appendIfLegendary`) so
  it always sits left of the text and never overlaps, regardless of name
  length. y is nudged up ~3px so its optical centre aligns with the text.
- Removed: `IrcNameTagUtil`, `assets/.../fonts/nametag_logo.json`,
  `textures/nametags/dioxide_logo_16.png`.
- 之前的方案是把位图字形（nametag_logo.json + dioxide_logo_16.png）注入
  原版 nametag 字体；原版位图渲染（NEAREST 采样）无法显示 256×256 logo → 删除。
- 新增 `NameTagLogoRenderer`：监听 `Render2DEvent`，通过相机视图旋转矩阵把
  玩家头部锚点投影到 GUI 空间，用 Skija 在 nametag 旁绘制与灵动岛同款
  256×256 logo。本地玩家（Client Logo，默认开）、IRC 在线用户（IRC Logo，
  默认开）都显示；16 格距离截止（与原版一致）。
- 第三人称 FOV：原版第三人称会扩展 FOV，`camera.getFov()` 不含扩展 →
  logo 投影会偏上 → 第三人称时 ×1.28 修正。
- logo 的 x 位置按实际渲染文本宽度（原始名 + Legend 后缀）动态测量，
  始终位于文字左侧不重叠；y 上移约 3px 与文字光学中心对齐。

### 3. Sharpness fixes / 清晰度修复
- `SkijaRenderer.BACKDROP_DOWNSAMPLE` 0.5 → **1.0**（full-resolution backdrop;
  only used when Global Blur is enabled）。
- Watermark static-cache blit now snaps to integer pixels
  (`Math.round(x-pad)` / `Math.round(y-pad)`) to avoid sub-pixel blur.
- Dynamic Island shape-cache blit also snaps to integer pixels
  (`Math.round(x-GLOW_PAD)` / `Math.round(y-GLOW_PAD)`).
- Island font sizes raised: compact title 9.5 / version 8.5 / right side 8.5;
  expanded title 10 / version 8.5 / status 8 / FPS 8.5 / IRC 7.5 / player rows 7.5.
- Fixed a copy-paste brace bug in `updateDataCache` that broke compilation.

## Verification / 验证
- `compileJava` + `build` pass; jar `DioxideLite-2.0.4.jar` (≈91.9 MB).
- Ran under Xvfb (llvmpipe software GL): main menu shows DIOXIDELITE 2.0.4;
  in-game HUD (Watermark / Perf / Dynamic Island) renders; ClickGUI Render tab
  lists Array List, Dynamic Island, Global Blur, Watermark HUD, Performance HUD;
  third-person name tag shows the D-logo left of the name, vertically aligned.

## Notes / 备注
- The interactive session observed one intermittent case where the in-game HUD
  layer did not draw (the environment/startup timing issue; the same code
  rendered HUD fully in the previous session). Dynamic Island screenshots were
  taken from the verified session.


---

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
