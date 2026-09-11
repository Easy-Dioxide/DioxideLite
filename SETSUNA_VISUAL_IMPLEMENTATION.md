# DioxideLite 1.6 — Minimal-inspired visual pass

This pass is based on direct inspection of the uploaded Minimal visual-design/devlog video. It does not copy the video's assets or source code; it translates the observed design principles into DioxideLite's own rendering system.

## Visual principles extracted

- Large negative space and restrained composition.
- Predominantly black / near-black surfaces with cool white typography.
- Thin cyan/blue hairline outlines as the main accent instead of heavy filled controls.
- Small, compact controls with generous spacing.
- Outline-first buttons and HUD blocks.
- In-game HUD is aligned around clear anchor zones rather than many large opaque panels.
- HUD elements use compact typography and thin separators.
- Settings/editor screens expose visual customization instead of hard-coding one layout.
- Motion is short and physical: fade, slide and spring-like interpolation instead of abrupt state changes.
- Background imagery remains visible and becomes part of the UI composition.
- Glass should add depth and separation, not cover the whole screen with an opaque blur layer.

## Implemented in this pass

### ClickGUI
- Reworked module cards to a dark translucent / hairline-outline language.
- Reworked toggle, cycle, slider and button controls to use the same material language.
- Added page transition fade/slide interpolation.
- Reworked sidebar hierarchy to `DIOXIDE / LITE` with compact navigation.
- Active navigation uses a small cyan accent line instead of a large saturated block.
- Close/reset controls are now outline controls rather than bright white blocks.

### Liquid Glass
- Added shared `DioxideLiteVisuals` design-system renderer.
- Uses a native translucent material layer with a restrained top optical plane and bottom rim.
- The default path avoids framebuffer capture so the UI remains responsive on lower-end systems.
- Dense module cards use cheap layered fills/outlines rather than per-card blur passes.

### Dynamic Island
- Added a compact dark-glass body with a cool hairline rim.
- Added a slow moving specular band.
- Existing width/height scaling remains supported and now participates in the animated layout.
- Tab-list expansion keeps the same material instead of switching to a separate opaque panel.

### Main UI
- Main title is now DioxideLite.
- Menu buttons use thin outlines and a small hover accent rather than thick bars.
- Startup reveal remains non-blocking.
- Background mouse-parallax remains available.
- Settings panel is retained as the customization surface.

## Performance strategy

The visual system distinguishes between large surfaces and dense lists. The native path uses cached/layered cards and avoids repeated framebuffer capture. `Config.performanceMode` remains the global quality switch.

## Source / build note

The project targets Minecraft 1.21.11 Fabric and retains the existing DioxideLite source tree. A real JAR was not generated in the current environment because the Gradle wrapper distribution could not be downloaded from `services.gradle.org`. No unverified JAR is included or represented as a build artifact.
