# DioxideLite v1.8.0 renderer

DioxideLite v1.8.0 uses Minecraft 1.21.11's native GUI rendering path.

## Design goals
- No Skia/Skija dependency.
- No framebuffer capture for LiquidGlass.
- No per-frame image upload.
- No direct OpenGL calls from DioxideLite UI code.
- Transparent LiquidGlass remains available and configurable.
- Visual modules remain independently switchable from ClickGUI.

## Performance policy
`RenderPerformance` samples frame time and automatically enters a reduced-effects path when the frame budget becomes expensive. Performance Mode can also be enabled manually. The reduced path keeps the transparent material and outline while dropping decorative highlight passes.

Sodium/ImmediatelyFast may be installed externally; DioxideLite does not require either one and does not replace Minecraft's renderer with another immediate-mode backend.

## Backgrounds
Main-menu backgrounds are loaded from `DioxideLite/backgrounds`. The client does not embed or redistribute third-party artwork from another client. Place any artwork you have permission to use in that folder and select it from the main-menu background control.
