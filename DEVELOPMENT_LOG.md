# DioxideLite v2.0.1 Development Log / 开发日志

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
