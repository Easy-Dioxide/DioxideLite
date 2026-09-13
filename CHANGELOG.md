# DioxideLite Changelog

## [2.0.4] - 2026-09-13
### Added / 新增
- Name tag client logo now rendered via Skija (`NameTagLogoRenderer`): the
  same 256×256 Dynamic-Island logo is drawn next to the name tag for the local
  player (Client Logo, default on) and IRC online users (IRC Logo, default on).
  Vanilla name tags cannot render the custom bitmap glyph, so the logo is
  projected from the entity head anchor into GUI space every frame.
- nametag 客户端 logo 改为 Skija 渲染：本地玩家（Client Logo，默认开）与
  IRC 在线用户（IRC Logo，默认开）的 nametag 旁绘制灵动岛同款 256×256 logo。
- `LegendWatch` adds `Client Logo Size` (IntSetting, 6–20, default 10).

### Changed / 变更
- Module List (`Array List`) moved into the ClickGUI **Render** category (was
  invisible: defaulted to non-GUI `Category.HUD`), default enabled.
- Module List（Array List）移入 ClickGUI Render 分类（原为不可见的 HUD 分类），
  默认开启。
- `SkijaRenderer.BACKDROP_DOWNSAMPLE` 0.5 → 1.0: full-resolution backdrop for
  Global Blur (was 0.5 for iGPU).
- 背景降采样恢复为全分辨率（Global Blur 开启时生效）。

### Fixed / 修复
- Blurry watermark / Dynamic Island edges: static-cache blits now snap to
  integer pixels (no sub-pixel sampling).
- 修复 Watermark / 灵动岛边缘模糊：静态缓存 blit 对齐整像素。
- Dynamic Island text too small/blurry: font sizes raised (compact 9.5/8.5/8.5;
  expanded 10/8.5/8/8.5/7.5/7.5).
- 灵动岛字号上调，文字更清晰。
- Third-person name-tag logo vertical offset: mirror vanilla FOV expansion
  (×1.28) so the logo aligns with the text.
- 修复第三人称 nametag logo 高度偏移（对齐原版 FOV 扩展）。

### Removed / 移除
- Obsolete bitmap-glyph name-tag logo (IrcNameTagUtil, nametag_logo.json,
  dioxide_logo_16.png).
- 移除失效的位图字形 nametag logo 方案。

---

## [2.0.3] - 2026-09-12
### Added / 新增
- OpticsValleyIRC integration: IRC connection module under ClickGUI **Player**
  category (default on), server auto-reconnect, `#dioxide-lite` channel chat
  mirroring, online-user detection (`isIrcUser`), online list in the Dynamic
  Island expanded view and Tab list.
- 接入 OpticsValleyIRC：ClickGUI Player 分类 IRC 模块（默认开启）、自动重连、
  频道聊天互通、在线用户识别（灵动岛展开与 Tab 列表显示）。
- F6 theme-switch crash fix and ClickGUI live theme switching (no restart).
- F6 主题切换崩溃修复；ClickGUI 实时切换主题（无需重启）。
- Backdrop downsampling 0.5 for iGPU budget; removed module toasts.
- 背景降采样 0.5 以降低核显压力；移除模块 toast。

---

## [2.0.2] - 2026-09-12
### Added / 新增
- Global Blur module (Render category, default off, Blur Strength 1–16).
- HUD pipeline optimizations (backdrop snapshot reuse, blur source reuse).
- Perf panel: Skija / Island render timings, Profile mode.

### Fixed / 修复
- Watermark rendering pipeline rework (static cache).
- Various rendering fixes.

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
### Changed / 变更
- Skija + OpenGL rendering pipeline; Setsuna theme rework; rendering fixes.

---

## [1.7.x] - 2026-09-10
### Added / 新增
- LiquidGlass theme, Setsuna boot theme, ClickGUI theme switching.

---

## [1.6.x] - 2026-09-09
### Added / 新增
- pvputils-based visual client foundation.
