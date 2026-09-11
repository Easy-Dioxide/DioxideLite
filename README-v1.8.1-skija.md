# DioxideLite v1.8.1 — Skija Glass UI

This build restores the GPU-backed Skija GUI path used by the supplied Setsuna reference architecture, while keeping DioxideLite's own UI code/assets.

## Main change
- ClickGUI uses a GPU-backed Skija surface instead of the simplified native-only panel renderer.
- Liquid Glass uses one persistent downsampled framebuffer capture rather than a full-resolution capture for every panel.
- Blur capture is throttled to ~30 Hz normally and ~20 Hz in performance mode.
- Performance mode lowers capture resolution to ~38% of the framebuffer and reduces blur sigma.
- The ClickGUI still keeps the existing right-click module expansion behavior.

## Important
Exact FPS must be measured in the actual Minecraft runtime. The source archive can be checked statically here, but this environment cannot download the Gradle distribution/dependencies needed for a full 1.21.11 build.
