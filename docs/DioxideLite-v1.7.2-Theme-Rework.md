# DioxideLite v1.7.2 — Main Menu + Signature Theme Pass

## Main menu

- Fresh installs now enter the DioxideLite/Setsuna-style main menu by default (`useMainUI=true`).
- The custom main menu has an explicit top-right `SETSUNA` switch that returns to the vanilla Minecraft title screen.
- The vanilla title screen receives a small `DioxideLite · SETSUNA` switch so the user can return without editing config files.
- The existing main-menu settings surface remains available through the gear icon.
- Built-in GLSL and custom PNG background modes are preserved, including custom background selection, folder opening and mouse-parallax control.

## Signature ClickGUI

- Restored the full Signature ClickGUI route in the v1.7.1 rendering-fix source.
- New/default configurations now open the Signature ClickGUI; the original Liquid Glass layout remains selectable as `ORIGINAL`.
- Reworked the layout around a radial category launcher that morphs into a compact navigation rail + settings workspace.
- Added thinner outlines, restrained teal accents, more negative space, cleaner typography hierarchy and less panel density.
- Preserved DioxideLite's existing pages and setting interactions; the theme is presentation-only.
- Avoids copying third-party assets/source code.

## Performance intent

- Uses the direct GPU Skia path introduced in v1.7.1.
- Does not add per-frame CPU framebuffer readback to the new Signature theme.
- SettingModule static-picture caching remains available for stable settings rows.

## Verification status

The source tree was structurally checked and the resource JSON parses successfully. A full Gradle build could not be executed in this environment because the Gradle 9.3.0 distribution is not cached and external network access to `services.gradle.org` is unavailable.
