# v1.7.5 Native Renderer Notes

## Visual direction
The UI is an independent recreation of the reference client's visual language:
- large negative space
- dark monochrome background
- thin cyan/white geometry
- staged entrance animation
- transparent layered panels
- compact typography
- radial category launcher
- two-stage workspace layout

The reference source was used as a design/behavior reference; its original source/assets are not bundled into
DioxideLite. The bundled main-menu image is an original DioxideLite asset.

## ClickGUI interaction fix
`NewSettingsScreen` and `DioxideLiteSignatureClickGuiScreen` now delegate module hit-testing directly to
`BasePage.onClick(...)`. The previous second layout walk could disagree with the actual page layout and
swallow right-clicks.

`SettingModule.onClick(...)` reserves right-click on the module header for expansion/collapse before the
main widget is processed.

## Rendering
All ClickGUI and native glass surfaces use Minecraft `GuiGraphics`. The visual system does not require
Skia, HumbleUI, framebuffer capture or a CPU-side TTF rasterizer.
