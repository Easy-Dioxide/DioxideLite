# DioxideLite v1.7.5 — Native Renderer Migration

## Goal

Remove the external Skia rendering stack from the client UI/HUD path and use Minecraft's native `GuiGraphics` pipeline throughout.

## Changes

- Removed all Skia renderer/backend classes and Skia dependencies from Gradle.
- Replaced the custom font rasterizer with a compatibility facade backed by Minecraft's native font renderer.
- Replaced Liquid Glass material compositing with lightweight native translucent plates, outlines and highlights.
- Reworked Liquid Glass ClickGUI to the native renderer while retaining the existing page/module/setting model.
- Kept in-game ClickGUI theme hot-switching through `ClickGuiThemeController`.
- Default ClickGUI remains `ORIGINAL`, which now uses the native Liquid Glass presentation.
- Reworked Dynamic Island, Target HUD, Keystrokes, Armor HUD, Potion Status, Notification and Block Count displays to draw directly with `GuiGraphics`.
- Reworked HUD Editor to use native outlines/grid/snap guides and preserved drag + scroll-to-scale behavior.
- Removed frame-end rendering passes that existed only to support external off-screen rendering.
- Main menu remains on the native renderer and keeps the Setsuna-inspired interaction structure.

## Performance model

The new UI path avoids:

- Skia GL contexts
- framebuffer capture for blur
- CPU raster surfaces
- per-frame CPU→GPU texture uploads for UI
- nested external rendering passes

The Liquid Glass look is intentionally material-like rather than blur-dependent: translucent depth, thin borders, cool edge accents and highlight strips are used to preserve hierarchy without forcing a full-screen capture/blur operation.

## Build status

Source/static checks were performed. A full Gradle build could not be completed in the current environment because Gradle 9.3.0 is not cached and network access to `services.gradle.org` is unavailable.
