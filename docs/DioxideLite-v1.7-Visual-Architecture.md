# DioxideLite v1.7 Visual Architecture

## Liquid Glass

DioxideLite v1.7 introduces a single global **Liquid Glass** switch for gameplay visuals.
The switch affects HUD surfaces, the vanilla hotbar, container/inventory screens, chat, Dynamic Island,
and DioxideLite presentation widgets. The main menu is deliberately excluded.

The supplied LiquidGlassShader reference was used as a visual/material reference. Its older Minecraft
1.8 framebuffer/shader plumbing is not copied into the 1.21.11 renderer. Instead, its useful material
ideas are reimplemented with the current Skija/OpenGL path: local framebuffer capture, blur, adaptive
tint, optical edge, restrained refraction/chromatic softness and a moving specular highlight.

A four-surface-per-frame blur budget prevents a screen with many HUD widgets from performing an
unbounded number of framebuffer captures. Additional surfaces fall back to the lightweight glass plate.
Performance Mode also falls back to the lightweight material.

## Signature visual theme

The ClickGUI includes a **DioxideLite Signature** theme. It uses the same SettingModule and BasePage
model as the original DioxideLite ClickGUI, so all existing settings remain available and theme changes
do not duplicate configuration state. The theme uses a compact dark surface, hairline accents, smooth
entrance motion, hover response and scroll interpolation.

The main menu uses a staged entrance inspired by the supplied visual reference: background reveal,
central dot, expanding interaction geometry, split Single Player / Multi Player destinations, and a
**CLICK TO START** gate. The implementation is original DioxideLite code and contains no third-party
client branding or combat functionality.

## Deliberate exclusions

No combat automation, packet manipulation, anti-cheat bypass, Xray, Tracers, KillAura or other
non-visual advantage features are introduced by the v1.7 visual work.
