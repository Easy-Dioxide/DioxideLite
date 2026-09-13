# DioxideLite Changelog

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
- HUD fusion size fingerprinting fixed (dynamic element dimensions invalidate
  layout).
- IRC: unknown-host diagnostics; reconnect continues indefinitely with capped
  exponential backoff.

### Added / 新增
- Dynamic Island enabled-by-default (Render category) when no profile
  overrides it; native resource cleanup for island logo/shape cache.
- HUD Editor entry point restored.
- `Sprint` module (Movement category, Setsuna-compatible; restores vanilla
  sprint key when disabled).
- IRC transport: TCP_NODELAY / keep-alive / reuse-address.

---

## [2.0.4] - 2026-09-13
### Added / 新增
- Name tag client logo rendered via Skija (`NameTagLogoRenderer`): local player
  (Client Logo, default on) and IRC users (IRC Logo, default on); FOV-corrected
  third-person projection, width-adaptive x, vertical alignment.
- `LegendWatch` adds `Client Logo Size` (6–20, default 10).

### Changed / 变更
- Module List (`Array List`) moved into ClickGUI **Render** category (was
  invisible on non-GUI HUD category), default enabled.
- `BACKDROP_DOWNSAMPLE` 0.5 → 1.0 (full-resolution backdrop for Global Blur).

### Fixed / 修复
- Blurry watermark / Dynamic Island edges (integer-pixel blits).
- Dynamic Island text too small (fonts raised).
- Third-person name-tag logo vertical offset (mirror vanilla FOV expansion).

### Removed / 移除
- Obsolete bitmap-glyph name-tag logo (IrcNameTagUtil, nametag_logo.json,
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
  FPS/ping/server, IRC status).
- Watermark with custom name/logo.

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
