# DioxideLite（中文说明）

DioxideLite is a **Minecraft Java Edition 1.21.11 Fabric client-side visual/PvP utility project**, based on the supplied upstream source.

## This iteration

- Renamed the client-facing identity to `DioxideLite`.
- Retained Minecraft `1.21.11` + Fabric and the existing Skija rendering stack.
- Added four ClickGUI visual templates:
  - Liquid Glass
  - Aurora
  - RISE Clean (original implementation inspired by clean PvP-client layouts)
  - Minimal
- Added adjustable glass opacity, blur, edge highlight, radius and performance mode.
- Reworked ClickGUI surfaces around a translucent layered-glass system with soft rims and restrained highlights.
- Added a non-blocking startup reveal animation to the custom main UI.
- Fixed Dynamic Island sizing controls: width and height are now independently adjustable and persisted.
- Added Dynamic Island blur and opacity controls.
- Kept the project focused on legal client-side visual/QoL functionality; no combat automation, aim assistance, anti-cheat bypass or similar cheating logic was added.

## Build

The project is configured for Java 21 and Minecraft 1.21.11.

```bash
./gradlew build
```

The distributable JAR is produced under `build/libs/`.

## Visual direction

The UI uses layered depth, restrained translucency, rounded surfaces, soft motion and clear visual hierarchy. The Liquid Glass implementation is an original Skija-based treatment rather than a copy of a third-party implementation.

Apple's Liquid Glass design guidance emphasizes hierarchy, translucency, responsive transformation and keeping content visually primary; those principles informed the new surface system.

## Attribution

This project contains code derived from the supplied upstream source. Please retain the original license and attribution requirements contained in `LICENSE` and `THIRD_PARTY_NOTICES.md`. DioxideLite does not require an IRC account, client login, or external authorization to start.

Original upstream source:


## IRC / 登录验证
DioxideLite 不包含 IRC 客户端、IRC 登录流程、账号认证门槛、License Key 验证或启动授权服务。客户端启动不依赖外部登录服务。
