# DioxideLite Changelog

## [2.0.7] - 2026-09-13
### Performance / 性能（根因修复）
- **Root cause fix**: Array List background/icon geometry moved out of the
  per-frame `GlState.capture/restore` path (the biggest iGPU frame-time spike);
  prepared in the vanilla-font pass, composited once in the final Skija overlay.
- Backdrop blur downsampled only for the backdrop source (iGPU 0.67x /
  balanced 0.75x / quality native); text stays at native Skija resolution.
- iGPU/balanced profiles cap blur strength; glow kernel tightened to 80%.
- Dynamic Island / nametag logos: Mitchell sampling + shared Paint;
  island body stays cached. Compass path built once per enable.
- Skija fallback-font detection cached per key.
- **根因修复**：Array List 背景脱离每帧 GL 状态快照路径；背景模糊按档位
  降采样（文字原生分辨率）；灵动岛/nametag logo 复用 Paint；Compass 路径
  构建一次。

### Added / 新增
- IRC private capability frame: DioxideLite announces capability after
  username handshake; companion server forwards add/remove only between
  DioxideLite clients; nametag logos / island tab set only for DioxideLite
  peers (`tools/OpticsValleyIRC-server/`).
- IRC JOIN/LEAVE presence tracking (online-user list sync).
- IRC 私有 capability 帧（仅 DioxideLite 客户端间显示 nametag logo）；新增
  JOIN/LEAVE 在线状态跟踪。

### Fixed / 修复
- Source compile fixes: dangling catch in `SkijaRenderer.drawBlurredBackdrop`;
  missing `trackPresence` in `IRCClient`.

---

## [2.0.5] - 2026-09-13
### Performance / 性能
- Fast path for the final in-frame Skija overlay pass: removed the full OpenGL
  state snapshot/restore from every HUD frame.
- Cached Skija Gaussian blur filters for glow layers (no repeated native
  alloc/destroy).
- 帧内 overlay 末帧快路径；模糊滤镜缓存。

### Fixed / 修复
- **HUD not rendering intermittently**: profiles now load after the client
  starts, so saved module states (incl. Dynamic Island / HUD) are actually
  restored. Verified: fresh profile renders Dynamic Island by default.
- **修复 HUD 偶发不渲染**：配置在客户端启动后加载，模块状态真正恢复。
- HUD fusion size fingerprinting fixed.
- IRC: unknown-host diagnostics; reconnect continues indefinitely with capped
  exponential backoff.

### Added / 新增
- Dynamic Island enabled-by-default (Render category); native resource cleanup.
- HUD Editor entry point restored.
- `Sprint` module (Movement category, Setsuna-compatible).
- IRC transport: TCP_NODELAY / keep-alive / reuse-address.

---

## [2.0.4] - 2026-09-13
### Added / 新增
- Name tag client logo via Skija (`NameTagLogoRenderer`): local player + IRC
  users; FOV-corrected third-person projection, width-adaptive x, vertical
  alignment. `LegendWatch` adds `Client Logo Size` (6–20).
### Changed / 变更
- Module List (`Array List`) moved into ClickGUI **Render** category; default
  enabled. `BACKDROP_DOWNSAMPLE` 0.5 → 1.0.
### Fixed / 修复
- Blurry watermark / Dynamic Island edges (integer-pixel blits); island text
  size raised; third-person logo vertical offset.
### Removed / 移除
- Obsolete bitmap-glyph nametag logo (IrcNameTagUtil, nametag_logo.json,
  dioxide_logo_16.png).

---

## [2.0.3] - 2026-09-12
### Added / 新增
- OpticsValleyIRC integration (Player category, default on; auto-reconnect;
  chat mirroring; online-user detection; island Tab online list).
- F6 theme-switch crash fix; live theme switching (no restart).
- Backdrop downsampling 0.5 for iGPU; removed module toasts.

---

## [2.0.2] - 2026-09-12
### Added / 新增
- Global Blur module (Render category, default off, strength 1–16).
- HUD pipeline optimizations; Perf panel (Skija/Island timings, Profile mode).
### Fixed / 修复
- Watermark rendering pipeline rework (static cache).

---

## [2.0.1] - 2026-09-12
### Added / 新增
- OPAI-style Dynamic Island (compact + expanded, Tab to expand, player list,
  FPS/ping/server, IRC status). Watermark with custom name/logo.

---

## [2.0.0] - 2026-09-11
### Changed / 变更
- Rebased from pvputils-base; removed automation modules (hard constraint).
- D logo identity, main menu rework, Setsuna-style visuals.

---

## [1.8.x] - 2026-09-11
Skija + OpenGL pipeline; Setsuna theme rework; rendering fixes.

## [1.7.x] - 2026-09-10
LiquidGlass theme, Setsuna boot theme, ClickGUI theme switching.

## [1.6.x] - 2026-09-09
pvputils-based visual client foundation.
