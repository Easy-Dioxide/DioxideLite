# DioxideLite v1.7.5 — Setsuna-inspired visual rebuild

This build keeps DioxideLite's existing feature/config architecture and the three ClickGUI themes,
while rebuilding the presentation around Minecraft's native GUI renderer.

## Changes
- Restored the original Liquid Glass ClickGUI as the default theme.
- Kept Minimal and Signature themes available through the existing hot-switch controller.
- Reworked module cards to use layered transparent Liquid Glass material.
- Fixed ClickGUI right-click expansion: right-clicking a module header now reliably expands/collapses it.
- Removed the duplicate outer ClickGUI hit-test that could swallow module interactions.
- Fixed `NewSettingsScreen` extensibility so the Minimal theme can compile as its existing subclass.
- Kept custom Harmony / icon / Material Symbols font providers on Minecraft's native font path.
- Added a new original monochrome visual background for the main menu and enabled image backgrounds by default for fresh configs.
- Main menu keeps the staged `CLICK TO START` -> `SINGLE PLAYER / MULTI PLAYER` flow.
- Existing visual features remain individually configurable; no presentation feature is forced on permanently.
- Better Chat, Liquid Glass HUD/screen/container surfaces, HUD overlays and existing visual modules remain config-gated.
- Native renderer path contains no Skia/HumbleUI dependency or references.

## Build note
The source was statically checked, but a full Gradle compile could not be completed in this environment because
the Gradle 9.3.0 wrapper distribution is not cached and outbound access to `services.gradle.org` is unavailable.
