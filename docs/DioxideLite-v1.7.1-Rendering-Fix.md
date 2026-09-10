# DioxideLite v1.7.1 — Rendering Pipeline Fix

## Why this update exists

v1.7 could become extremely slow when opening ClickGUI because the UI used a CPU raster Skia surface/texture upload path and the glass system could allocate framebuffer capture resources repeatedly. The main-menu shader also rendered into a private framebuffer and copied the entire frame back through `glReadPixels`, forcing a GPU/CPU synchronization every frame.

## v1.7.1 changes

- Skia screen rendering now uses the native OpenGL/Skia backend directly for the two ClickGUI screens.
- Region rendering also uses the active GPU framebuffer when the GPU path is enabled, avoiding raster-surface uploads.
- Liquid Glass blur uses a persistent, downsampled GPU capture texture instead of allocating a texture/FBO for every glass panel.
- The glass capture is reused for multiple panels within the same frame window.
- MainUI GLSL shaders render directly into Minecraft's active framebuffer. The old `glReadPixels -> DynamicTexture` round trip was removed.
- Fresh installations now default to the DioxideLite custom main menu (`useMainUI=true`). Existing explicit configuration values are preserved.
- Signature ClickGUI no longer starts a second blur/capture pass from inside its Skia canvas.
- Global Liquid Glass composites the blur before starting the Skia overlay, preventing nested GL/Skia contexts.

## Expected effect

The main objective is to remove the synchronization points that can turn a 300 FPS game into a 1 FPS UI. Exact FPS depends on GPU, resolution, shader complexity, and the number of enabled visual modules.

## Verification note

The source was structurally checked after the changes. A local Gradle build could not be run in this environment because Gradle 9.3.0 is not cached and the build host cannot reach `services.gradle.org`.
