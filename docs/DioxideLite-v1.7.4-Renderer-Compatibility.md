# DioxideLite v1.7.4 — Renderer compatibility profile

DioxideLite v1.7.4 does **not** embed Skia/Skija and does not replace Minecraft's renderer with a second heavyweight graphics stack.

## Rendering stack

- **Minecraft 1.21.11 native Blaze3D / GuiGraphics** for DioxideLite UI and HUD drawing.
- **ImmediatelyFast** is the preferred optimization layer for GUI/HUD/text immediate-mode rendering. It batches immediate-mode draw calls and uploads data more efficiently.
- **Sodium** is the preferred world-rendering optimization layer. It is optional and is not bundled into DioxideLite.

This arrangement is intentionally dependency-light: DioxideLite stays compatible with the normal Fabric rendering pipeline while benefiting from established rendering optimizations when the corresponding mods are installed.

## Visual compatibility

The previous DioxideLite visual direction is retained:

- Liquid-glass-inspired dark translucent HUD surfaces.
- Thin cyan/blue outlines and highlights.
- Dynamic Island.
- Target HUD.
- Potion HUD.
- Armor HUD.
- Keystrokes.
- Block-count display.
- HUD editor.
- Setsuna-inspired spacing, hierarchy, negative space and compact outlined controls.
- ClickGUI theme switching at runtime.
- Main-menu `CLICK TO START` flow followed by `SINGLE PLAYER / MULTI PLAYER`.

The glass layer intentionally uses cheap native translucent geometry rather than a per-frame framebuffer capture + blur pass. This prevents the UI from becoming the main frame-time bottleneck on OpenGL systems.

## Recommended runtime

For the best performance profile on Fabric 1.21.11, install DioxideLite together with compatible releases of Sodium and ImmediatelyFast. DioxideLite itself does not require either mod to launch.

## Important

The two optimization mods are external optional dependencies. They are not copied into this source tree and are not redistributed by this project.
