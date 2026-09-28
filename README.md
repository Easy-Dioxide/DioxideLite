# DioxideLite 2.2.2 — Buildable Source (Windows / Linux / macOS)

**DioxideLite** is a Fabric client for **Minecraft 26.1.2** whose entire user interface is
drawn with **Skija** (GPU 2D): ClickGUI, HUD widgets, the dynamic island, watermark, notifications
and the in-game overlays. This archive is the **complete, buildable source tree** of the
multi-platform **2.2.2** build.

* Mod version: **2.2.2**
* Minecraft: **26.1.2** (official Mojang mappings)
* Fabric Loader: **0.19.2+** · Fabric API: **0.150.0+26.1.2**
* Toolchain: **JDK 25** (class file major 69), Gradle **9.2.1**, Fabric Loom **1.15.5**
* Platforms: **Windows x64**, **Linux x64**, **macOS arm64**, **macOS x64** — one jar for all four
  (Skija and WebRTC natives for every platform are bundled)

---

## v2.2.2 更新摘要

- **多平台移植**：PlatformSupport 统一 OS 分发层，支持 Windows / Linux / macOS（Intel + Apple Silicon），零额外 native 依赖
- **ClickGUI 双模式**：LegacyStyle（传统窗口）+ Setsuna（径向轮盘），共享强调色，GUI Scale 可调
- **Render 模块重组**：RenderModuleRegistry 统一注册，大量渲染模块移入 `render/advanced/` 子包
- **品牌清理**：移除 setsuna 字样，全部统一 DioxideLite 品牌
- **IRC 心跳**：5 分钟保活 + 断线自动重连
- **Halo 模块**：碧蓝档案头顶光环渲染，默认启用
- **Onyx HUD**：ArrayList 右对齐自适应 + Notifications + PotionHUD

详细更新记录见 [CHANGELOG.md](CHANGELOG.md) 和 [devlog-2.2.2.md](devlog-2.2.2.md)。

---

## 1. Building

Requirements: JDK 25 (`JAVA_HOME` must point at it), an internet connection for the first
dependency resolve.

**Universal (all four platforms) build — this is how the shipped jar was produced:**

```bash
./gradlew clean build \
  -Pskija_platforms=skija-windows-x64,skija-linux-x64,skija-macos-arm64,skija-macos-x64 \
  -Pwebrtc_platforms=windows-x86_64,linux-x86_64,macos-aarch64,macos-x86_64
```

On Windows use `gradlew.bat` with the same arguments.

**Single-platform build (smaller jar):**

```bash
./gradlew clean build -Pskija_platforms=skija-macos-arm64 -Pwebrtc_platforms=macos-aarch64
```

Outputs land in `build/libs/`:

| File | Contents |
|------|----------|
| `DioxideLite-2.2.2.jar` | the mod (drop it into `mods/`) |
| `DioxideLite-2.2.2-sources.jar` | sources jar |

Notes:

* `libs/nested/` ships the embedded third-party libraries the build depends on, and
  `src/main/java/tritium` + `src/main/java/repackage` carry the audio stack — keep them.
* `libs/modmenu-18.0.0-alpha.8.jar` and `libs/annotations.jar` are compile-time helpers.
* The `-Pskija_platforms` / `-Pwebrtc_platforms` switches exist because Skija and
  webrtc-java are native libraries; passing several platforms produces the universal jar.

---

## 2. Repository layout

```
build.gradle.kts / settings.gradle.kts   Gradle build (platform switches live here)
gradle.properties                        version, mod id, versions
src/main/java/com/dioxidelite/           client sources
  module/modules/render/advanced/        the ported render modules (see §3)
  ui/clickgui/, ui/hud/, ui/dioxide/     Skija UI: ClickGUI, HUD widgets, dynamic island
  core/engine/                           combat / movement / player / client engines
src/main/java/{repackage,tritium}/       audio stack (JSyn / JLayer / NetEase Cloud Music client)
src/main/resources/                      fabric.mod.json, mixin configs, lang files, textures
libs/                                    embedded libraries (nested jars)
tools/, scripts/                         helper scripts
docs/, devlog-*.md, CHANGELOG.md         documentation and development history
```

---

## 3. What is in this 2.2.2 source drop

Compared to the upstream 2.2.1 release this tree contains the work that produced the
multi-platform jar:

**Multi-platform port**
* Bundled Skija natives for Windows x64 / Linux x64 / macOS arm64 / macOS x64
  **plus** `skija-shared` and `types`, and webrtc-java natives for all four platforms.
* Replaced Windows-only code paths with `PlatformSupport`: PowerShell/AWT font discovery,
  device-id generation without JNA, `Desktop.open` → `open` / `explorer` / `xdg-open`,
  tinyfd file dialogs, font fallbacks.

**Rendered module set**
* 27 built-in render modules are retired at runtime and replaced by **35 ported modules** under
  `module/modules/render/advanced/` (ESP, Chams, NoRender, Fullbright, Camera, Wings, Trails,
  TargetESP, BedESP, BlockOverlay, ItemPhysics, Particles, Trajectories, Zoom, Freelook, …),
  registered through `RenderModuleRegistry`.
* The click-through/visual hooks those modules need are wired via mixins
  (`CameraMixin`, `GameRendererMixin`, `GuiMixin`, `ItemInHandRendererMixin`,
  `ParticleEngineMixin`, `AbstractClientPlayerMixin`, `EntityRendererMixin`, …).

**Interface**
* Right-Shift ClickGUI has two modes: `LegacyStyle` (default, remade panel look with rounded
  corners, particle glow and expand animations) and `Setsuna` (the wheel).
* HUD: single right-aligned **ArrayList** (plain text, white, right-aligned, no background by
  default; `Style` / `Colors` / `Row Spacing` / `Screen Margin` / `Scale` are configurable),
  notification stack, potion list, and the rest of the HUD set.
* ESP name tags: optional client-logo icon next to the name (`ESP → Names → Icon`), and the
  vanilla name tag is suppressed automatically while the ESP name tag is enabled.
* The watermark, the ClickGUI corner block and the main menu all read
  `DioxideLite.VERSION` (= `2.2.2`).

**Fixes**
* Camera module no longer locks the view: smoothed yaw/pitch now follow the player instead of
  being used as a decaying offset.
* Wings no longer smear across the screen (triangle buffers are not sorted on upload any more)
  and sit on the player's back (pose anchor applied).
* Wings / Trails / ContainerESP outlines are drawn with thin quads instead of `GL_LINES`.
* HUD name tags / logo icons are projected from interpolated positions, so they no longer drift
  while moving.

**Naming**
* All external-brand wording was removed from the client: no brand string remains in classes,
  resources, module names or language files — the only leftovers are a few legacy config keys
  that are used once to migrate an old profile and are then dropped from it.

---

## 4. Known limitations

* Windows and Linux builds are verified **statically** (structure, linking, class/file checks);
  only macOS arm64 has been smoke-tested in a real game session.
* macOS x64 (Intel) is included in the jar but was not launched on real hardware.
* Four upstream hooks are not implemented yet: `Skybox` / `Ambience` / `Fog Blur`
  post-processing shaders, the `Item Physics` ground transform, the `Hand` swing curve, and
  `Camera` position smoothing (the module's rotation smoothing does work).
* The shipped jar is built by patching the upstream 2.2.1 archive with classes from this tree
  (HUD/`ModuleManager`/`ConfigManager`/`DioxideLiteClient`/`Halo` and the ported modules), so a
  handful of release classes are intentionally older than this source.
* The IRC client expects an OpticsValleyIRC server (default port `16688`); without one it only
  logs a connection warning.

---

## 5. Usage quick start

1. Install Fabric Loader for Minecraft 26.1.2 and put `fabric-api-*.jar` into `mods/`.
2. Drop `DioxideLite-2.2.2.jar` into `mods/`.
3. Launch the game. `Right Shift` opens the ClickGUI; the HUD master switch
   (`HUD` module) controls every HUD widget — turning the master off hides them all.

---

## 6. Licences and credits

* Project licence: **GPL-3.0-or-later** — see [LICENSE](LICENSE); Apache-licensed parts use
  [LICENSE-APACHE](LICENSE-APACHE).
* Bundled third-party libraries (Skija, webrtc-java, Fabric API, Sodium/Lithium/FerriteCore
  and the audio stack) remain under their own licences, see the respective jars under `libs/`.
* Documentation and development history: `docs/`, `devlog-*.md`, `CHANGELOG.md`
  (the Chinese original of this README is kept as `README.zh-CN.md`).
