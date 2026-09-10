# DioxideLite v1.7.4 Setsuna Boot · Liquid Glass Default — Development Log

> Version: v1.7.4 (Setsuna Boot · Liquid Glass Default)
> Target: Minecraft 1.21.11 + Fabric Loader 0.18.4
> Cycle: 2026-09-11
> Position: The polish release on top of the v1.7.3 render rework — a full **Setsuna boot flow** plus **Liquid Glass as the default ClickGUI theme**, with the main menu and ClickGUI fully rendered through native GuiGraphics.

---

## 1. Background & Goals

v1.7.3 moved the main menu and Signature ClickGUI to native rendering (no GLSL / Skia / FBO / per-frame CPU readback). v1.7.4 resolves two product-level leftovers:

1. **First-launch experience**: the main menu should present a complete, perceivable Setsuna-style "boot flow" — black/backdrop entrance → `CLICK TO START` → staged transition → `SINGLE PLAYER / MULTI PLAYER` menu — instead of showing everything at once;
2. **Glass out of the box**: the default ClickGUI theme returns to **Liquid Glass (ORIGINAL)** — press RSHIFT in-game and see the liquid-glass settings UI immediately;
3. **No gate on the way in**: Right Shift opens the selected ClickGUI theme directly, no longer blocked by the terms screen.

---

## 2. Core Changes

### 2.1 Setsuna Boot Flow

`DioxideLiteMainUI.java` / `MainUIScreenManager.java`:

- Entrance animation: black/backdrop → `DIOXIDELITE / SETSUNA UI` wordmark fades in → white square glyph → `CLICK TO START`;
- After the click, a staged transition reveals the center mark / divider / menu items, ending with `SINGLE PLAYER` (left) / `MULTI PLAYER` (right) plus `OPTIONS` / `ESC EXIT`;
- Entirely Minecraft-native `GuiGraphics`: no GLSL shaders, no Skia surfaces, no framebuffer, no per-frame CPU texture upload;
- Persistent top bar with `BACKGROUND: SETSUNA` / `VANILLA` one-click switch; the vanilla title screen also gets a `DioxideLite · SETSUNA` return button;
- PVPUtils-style custom PNG backgrounds remain available (`DioxideLite/backgrounds`); mouse parallax is optional and uses lightweight coordinate interpolation only.

### 2.2 Default ClickGUI Theme: Liquid Glass

`Config.java`:

- `clickGuiTheme` default returns to `ORIGINAL`, routing to the Liquid Glass `NewSettingsScreen`;
- Liquid Glass material (blur / opacity / edge highlight / radius / performance mode) fully preserved;
- The Theme page's `ClickGUI Theme` cycle control now calls `ClickGuiThemeController.apply`, rebuilding the screen **immediately** — switching themes takes effect without closing/reopening ClickGUI.

### 2.3 Signature Theme Native Rendering

`DioxideLiteSignatureClickGuiScreen.java`:

- The radial-to-inspector interaction is retained;
- The render path is replaced with a native `GuiGraphics` fast path: module cards and setting widgets no longer go through Skia / GL framebuffer wrapping / blur capture / off-screen textures;
- Right-click / ESC navigation, slider dragging and scrolling all still work;
- Theme routing stays centralized in `ClickGuiThemeController`; all three themes share the same `BasePage` / setting components.

### 2.4 Gate-Free ClickGUI Access

`KeyInputHandler.java`:

- Right Shift directly calls `ClickGuiThemeController.create(Config.clickGuiTheme, null)` to open the selected theme's ClickGUI;
- The `termsRead` hard gate is removed (the terms screen keeps an explicit entry point when needed);
- Game language is applied automatically via `applyGameLanguageDefault`.

---

## 3. Build & Debug Notes

Target: Minecraft 1.21.11 · Fabric Loader 0.18.4 · JDK 21 · Loom 1.15-SNAPSHOT.

### Build errors fixed (3, all resolved before release)

| File | Issue | Fix |
| :--- | :--- | :--- |
| `RenderPage.java` | Trailing extra parenthesis → compile failure | Removed the redundant `)` |
| `ThemePage.java` | Trailing extra parenthesis → compile failure | Removed the redundant `)` |
| `SkiaBlurRenderer.java` | References missing `restoreReadBuffer` / `restoreDrawBuffer` | Implemented both GL-state restore helpers (`glBindFramebuffer` + `glReadBuffer` / `glDrawBuffer`) |

### Runtime issues & fixes

- **Container-glass Mixin crash**: after the v1.7.3 render rework, `AbstractContainerScreenGlassMixin` was back at the `renderBg` TAIL injection point, which cannot locate a RETURN in 1.21.11 and crashed on startup → moved the injection to `renderSlots` HEAD (same fix as v1.7.2); container glass is stable again after the rework.
- **Software-rendering verification**: the full chain (main menu / world join / ClickGUI / theme switching / Liquid Glass global visuals) was verified on llvmpipe (no GPU) and rendered correctly.

---

## 4. Live Verification (Minecraft 1.21.11 Fabric)

All of the following were verified on a real instance:

1. Launch → Setsuna boot screen (`CLICK TO START`) → click to enter the main menu;
2. `BACKGROUND: SETSUNA` / `VANILLA` switch on the main menu works;
3. `SINGLE PLAYER` → world list → join world, HUD renders correctly (DioxideLite info bar);
4. RSHIFT opens ClickGUI with the default Liquid Glass (ORIGINAL) theme;
5. On the Theme page, expand `ClickGUI Theme` → switch to `MINIMAL_POP` / `SIGNATURE` → the UI rebuilds instantly;
6. `Liquid Glass Global Visuals` toggle applies the glass material to the in-game HUD / hotbar immediately.

---

## 5. Artifacts

- `DioxideLite-v1.7.4.jar` (~26.3 MB)
- `DioxideLite-v1.7.4-sources.jar` (~13.4 MB)
- Changelog: `CHANGELOG.md` (includes v1.7.4 / v1.7.3 / v1.7.2 / v1.7.1 / v1.7 / v1.6 sections)

---

## 6. Compliance & License

- Continues to follow the upstream PVPUtils open-source license; `LICENSE` and `THIRD_PARTY_NOTICES.md` are fully retained;
- The client ships no IRC login, account authentication, license-key validation, or launch authorization service;
- Legal client-side visual / QoL features only — no combat automation, aim assistance, anti-cheat bypass, or similar cheating logic.
