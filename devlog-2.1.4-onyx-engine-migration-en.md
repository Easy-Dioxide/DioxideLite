# DioxideLite 2.1.4 — OpenOnyx Engine Migration (English)

## Core changes
- Version fixed at 2.1.4.
- Added a unified Onyx backend layer for Combat / Movement / Player / Client.
- KillAura target acquisition is now routed through `OnyxCombatEngine`, while the existing ClickGUI, TargetHUD and visual pipeline remain intact.
- Auto Sprint and Safe Walk are routed through `OnyxMovementEngine`.
- Name Changer is routed through `OnyxPlayerEngine`.
- `OnyxClientEngine` defines the client lifecycle/context boundary; rendering remains on DioxideLite's Skija pipeline.

## Compatibility strategy
OpenOnyx contains extensive legacy Minecraft mappings/APIs (for example legacy Entity, Packet and Minecraft classes). Copying those classes verbatim would break compilation on 26.1.2. Therefore 2.1.4 rebuilds the Onyx backend against the current Minecraft API instead of injecting the old mappings directly.

## Preserved
- Main menu UI is untouched.
- Dynamic Island / OPAI Onyx styles remain available.
- DioxideLite version is 2.1.4.
