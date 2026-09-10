# DioxideLite v1.7.3 — Main Menu & Fast GUI Render Rework

## Main menu
- Removed the main-menu GLSL animation system and its shader resources.
- Rebuilt the main menu with Minecraft's native `GuiGraphics` path.
- Keeps the Setsuna-inspired information hierarchy: `CLICK TO START`, `SINGLE PLAYER`, `MULTI PLAYER`, minimal branding and large negative space.
- Added a direct `VANILLA` switch on the custom menu.
- Vanilla title screen receives a `DioxideLite · SETSUNA` switch button.
- Preserves PVPUtils-style PNG background selection through `DioxideLite/backgrounds`.
- Mouse parallax is optional and uses only lightweight coordinate interpolation.

## ClickGUI
- Signature theme no longer uses Skia, GPU framebuffer wrapping, blur capture, or off-screen textures.
- Added a native `GuiGraphics` fast rendering path for module cards and setting widgets.
- Existing module/page logic is reused, so this is a presentation-layer replacement rather than a second settings implementation.
- Right-click/ESC navigation and slider dragging remain supported.
- ClickGUI theme routing is centralized through `ClickGuiThemeController`.
- The default persisted fallback for a new configuration is `SIGNATURE`.

## ClickGUI access bug
The old keybind flow could stop at `TermsScreen` when `termsRead=false`, which made the user experience look like ClickGUI could not open. The direct Right Shift path now opens the configured ClickGUI theme immediately. The terms screen remains available where explicitly invoked, but it is no longer a hard gate for the keybind.

## Performance goal
The new main menu and Signature ClickGUI perform no per-frame CPU readback, dynamic texture upload, Skia surface submission, or blur capture. The remaining Liquid Glass renderer is still used only by features/screens that explicitly request it.


## v1.7.3 follow-up: Setsuna-style boot flow
- Default ClickGUI is restored to `ORIGINAL`, which routes to the original Liquid Glass `NewSettingsScreen`.
- The Theme page still hot-switches between all three complete ClickGUI implementations at runtime.
- Right Shift continues to open the currently selected ClickGUI directly, without the old terms-screen gate.
- Main menu boot flow is now: black/background entrance -> `CLICK TO START` -> click -> staged center/divider/menu transition -> `SINGLE PLAYER` / `MULTI PLAYER`.
- The main-menu renderer remains native `GuiGraphics`; no GLSL, Skia, FBO or per-frame CPU texture upload is used by the menu.
- The default backdrop is an original lightweight recreation of the supplied Setsuna visual language rather than bundling the reference project's copyrighted background asset.
- PVPUtils-style custom PNG background switching remains available.
