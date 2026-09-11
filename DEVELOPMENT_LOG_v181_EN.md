# DioxideLite v1.8.1 Development Log (EN)

**Version**: DioxideLite v1.8.1 (Minecraft 1.21.11 · Fabric Loader 0.18.4)
**Focus**: Skia render regression (Setsuna-optimized) + font patches + GLASS glass theme + Minimal layout rework

---

## 1. Background

v1.8 fully nativized the ClickGUI (removed Skia) and fixed both the Windows blank-screen bug and the performance cliff — but the real glass look (background blur + translucent material) was lost. v1.8.1 brings Skia back, **in a different way than v1.7** (modeled after the Setsuna client):

- **Direct GL framebuffer drawing**: the ClickGUI renders straight into Minecraft's active framebuffer (`USE_GL_BACKEND_FOR_FRAME = true`) — no per-frame CPU surface upload;
- **Downsampled, throttled blur capture**: `SkiaBlurRenderer` captures the background at 0.50× (0.38× in performance mode) resolution, throttled to 30Hz / 20Hz — no full-res FBO capture every frame;
- **Font patch**: `FontRenderer` delegates glyph rasterization to Minecraft/Blaze3D, avoiding conflicts between self-drawn fonts and render optimization mods.

This is exactly why the user expects the v1.7 "FPS ≈ 0 when opening ClickGUI / animating" problem to be gone — the two dominant per-frame costs (surface submission and full-res blur capture) were eliminated at the code level.

---

## 2. Additions & Fixes

### 2.1 GLASS theme (v1.7 pure transparent glass ClickGUI restored)

The stock `ClickGuiTheme` only had ORIGINAL / MINIMAL_POP / SIGNATURE, and under the GL backend ORIGINAL actually renders an **opaque dark panel** (`SkiaRenderer.isDrawing()` = true → `glassBase` dark fill, skipping the real `LiquidGlassRenderer.drawSurface` material).

Per the request "restore the v1.7 pure transparent glass ClickGUI as a new theme":

- Added `GLASS` to `Config.ClickGuiTheme`;
- New `GlassThemeScreen extends NewSettingsScreen`, overriding `usePureGlass() = true`;
- `NewSettingsScreen` gained a `drawGlass()` hook: when `usePureGlass() || !SkiaRenderer.isDrawing()` it draws the true glass material (`LiquidGlassRenderer.drawSurface`: background blur + high-transparency fill + refraction inner layer + rim highlight); otherwise it keeps the dark panel;
- `ClickGuiThemeController` registers the `GLASS` route; the theme-page Cycle control now lists 4 options (incl. Glass) with live hot-swap.

### 2.2 Minimal theme — dedicated compact layout (fixes "full-screen is awkward to use")

The stock `DioxideLiteMinimalClickGuiScreen` was an 8-line shell extending `NewSettingsScreen` — opening it showed exactly the same 740×500 centered card as ORIGINAL, with a small content area on full-screen.

Rewritten as an independent screen (extends `SkiaScreen`, consistent with v1.8.1's Skia stack):
- 900×560 adaptive card, up to 1.15× on big monitors (`scale = clamp(min(w/W, h/H), 0.60, 1.15)`);
- left 6-tab nav (Combat/Render/Tools/Theme/Optimize/Misc) + wide content area;
- wheel scroll + scrollbar drag + in-content drag (module sliders);
- close button, open/close animations, hover feedback;
- plain dark panel (no blur) — cheapest rendering path.

### 2.3 In-ClickGUI FPS readout (F3 equivalent)

Since the ClickGUI is a full-screen Screen (the F3 debug overlay would be covered), both `NewSettingsScreen` and `DioxideLiteMinimalClickGuiScreen` now draw a live `FPS` number (`Minecraft.getInstance().getFps()`) in the top-right corner — open the ClickGUI to see the render frame-rate, equivalent to the F3 corner readout.

---

## 3. Build & Fix Log (important)

The first build after unpacking the source failed with **3 compile errors**; runtime then exposed **1 mixin crash** and **1 environment-level skija issue**. All were diagnosed and fixed:

### Compile-time (upstream source bugs)

| File | Problem | Fix |
|---|---|---|
| `RenderPage.java:51` | `.addSub(...)` chain paren mismatch (+ missing statement `;`) | Corrected to `})))` + `;` |
| `ThemePage.java:24` | Same paren mismatch; theme Cycle had 3 entries | Paren fix; Cycle expanded to 4 incl. GLASS mapping |
| `HudEditOverlay.java:19` | Arrow-case switch followed by bare statement/`return` (illegal) | Converted to block cases (`case X -> { ... }`) |
| `SkiaBlurRenderer.java:170-171` | Called undefined `restoreReadBuffer` / `restoreDrawBuffer` | Implemented both GL-state restore helpers |
| `TargetHudRenderer.java:7` | `PlayerSkin` imported from wrong package | → `net.minecraft.world.entity.player.PlayerSkin` |
| `TargetHudRenderer.java:21` | Missing `PlayerFaceRenderer` import | Added `net.minecraft.client.gui.components.PlayerFaceRenderer` |
| `ArmorHudRenderer.java:8` | `Inventory.armor` removed in 1.21.11 | Use `getItemBySlot(EquipmentSlot)` (boots→helmet order preserved) |
| `FontRenderer.java:143` | Float coords incompatible with 1.21.11 `drawString` | Integer coords `1, 1` |

### Runtime (mixin crash)

| Issue | Symptom | Fix |
|---|---|---|
| `AbstractContainerScreenGlassMixin` | Game won't boot: `TAIL could not locate a valid RETURN in renderBg` | Injection point moved to `renderSlots` HEAD (matches v1.8) |

### Verification-environment limitation (important)

Build/screenshot verification ran in a **headless Linux container (Xvfb + llvmpipe software GL)**. In this environment the **skija 0.143.x native library always SIGSEGVs during post-load init** (`Library._nAfterLoad → jni_GetMethodID` on a null ref; verified across JDK 11 / 21.0.5 / 21.0.12, skija 0.109.1 / 0.143.14 / 0.143.16 / 0.143.17, and four native-loading strategies). Therefore:

- **Setsuna main menu, world list, in-game view**: render fine — screenshots taken (FPS 3 is due to llvmpipe software rendering, no GPU);
- **ClickGUI (Skia)**: cannot start in this headless environment (native crash); **unaffected on the user's Windows environment** (skija-windows-x64 native is embedded in the jar) — the user already confirmed v1.8.1 runs at a good frame-rate there;
- Please verify the four ClickGUI themes on Windows (the build artifact carries the Windows native).

---

## 4. Verification & Results

| Item | Method | Result |
|---|---|---|
| Compile | `./gradlew build -x test` (JDK 21.0.12) | ✅ BUILD SUCCESSFUL |
| Artifacts | `build/libs/DioxideLite-v1.8.1.jar` (22.4MB) + sources | ✅ Generated |
| Mixin boot | Fabric launch → menu / world | ✅ No mixin crash |
| Main menu | Setsuna boot screen + menu (v1.8.1 tag) | ✅ Screenshot |
| In-game | World loaded + HUD (health/hunger/hotbar) | ✅ Screenshot |
| ClickGUI | Skia crash in this headless env (see above) | ⚠️ Verify on Windows |

**Not yet covered**: live screenshots + frame-rate numbers of the four ClickGUI themes (awaiting the Windows environment; the ClickGUI already shows FPS in-corner, so the rate is visible immediately).

---

## 5. Next Steps

- On Windows, verify the four themes' visuals and frame-rate ("FPS no longer ≈0 when opening ClickGUI");
- To fully remove the headless-env limitation, consider pre-loading skija before ClickGUI construction and falling back to native rendering on `Throwable` (v1.8's native path remains a reference);
- The font resource-pack builder warnings (`dioxide-lite:fonts/*.ttf` paths) don't affect runtime; moving `fonts/` under `assets/dioxide-lite/` would clean them up later.
