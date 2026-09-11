# DioxideLite Visual Design Notes

## Liquid Glass

The new glass layer is composed of:
1. optional framebuffer blur;
2. translucent material tint;
3. a low-contrast inner plate;
4. a one-pixel rim;
5. a restrained top specular band.

The implementation intentionally avoids making every component equally bright. Large surfaces provide depth; active controls carry the stronger accent.

## Templates

- **Liquid Glass**: deepest translucency, blur, cool blue highlight.
- **Aurora**: slightly stronger cyan rim and darker blue surface.
- **RISE Clean**: flatter, sharper hierarchy and less material noise.
- **Minimal**: opaque, lowest rendering cost.

## Performance

`Performance Mode` disables live background blur while keeping the visual hierarchy and glass-like surface treatment. This is preferable to globally removing all animation or UI effects.

## Dynamic Island

Width and height are independent settings and are persisted in the DioxideLite config file. The island animates toward its target size so changing the size does not cause a hard snap.

## Copyright / style

The implementation studies broad visual principles from the requested Minimal-style design direction and the supplied LiquidGlass reference. It does not copy proprietary screenshots, exact layouts, or third-party source code.
