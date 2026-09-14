# DioxideLite v2.0.9 — Devlog

> 2026-09-15 · DioxideLite Team

---

## 中文版

### 背景

v2.0.9 是 **Setsuna 视觉迁移**版本：在 2.0.8 base 之上，把 SetsunaClient 的实体视觉面（ESP / Chams / Name Tags / Target HUD / Scaffold HUD / Attack Ring / Combat Visuals / Team Viewer / Music）移植进 DioxideLite，并保持"纯视觉、无自动化"的边界。IRC 按需求不参与本轮测试。

### 本轮开发过程

1. **源码盘点**：确认版本 2.0.9、MC 26.1.2 + Fabric Loader 0.19.2 + Fabric API 0.150.0；随包自带 `V2.0.9_VISUAL_MIGRATION.md` 迁移说明，`tritium` / `repackage` 源码均在 `src/main/java` 下，libs 含完整嵌套依赖（fpsmaster 等）。

2. **构建失败定位（Music 模块依赖不可见）**：
   - 首轮构建报 `package tritium.ncm.music does not exist`（`MusicLyricsHUD` / `MusicScreen`）。
   - 排查发现 `src/main/java/tritium` 文件齐全，但 `build.gradle.kts` 的 `sourceSets.named("main") { java.exclude("repackage/**", "tritium/**") }` 把 tritium 源码排除在编译之外。
   - 移除排除后二次构建仍失败：`AudioPlayer` 缺 `repackage.processing.sound` 与 `JSynFFT`——这两个依赖其实也在 `src/main/java/repackage`（175 个文件，含 com/jsyn 与 processing/sound），但同样被 exclude 排除。
   - 移除全部排除项后构建成功。**根因**：排除项本意可能是减小 jar，但误伤了音乐视觉模块的编译与打包。

3. **运行验证与截图**：
   - 主菜单：暗黑二次元背景 + `DIOXIDELITE 2.0.9` + SINGLE PLAYER / MULTI PLAYER 完整。
   - 进游戏：顶部灵动岛显示 `DioxideLite v2.0.9 · FPS · 延迟`（Dynamic Island 模块默认开启）。
   - ClickGUI（右 Shift）：六分类环形菜单文字完整。
   - Render 分类：完整模块列表，含迁移的 Chams / Name Tags / Attack Ring / Combat Visuals / Target HUD / Dynamic Island / Global Plus；开启 ESP / Chams 后右上角出现状态标识，开关生效。
   - 软渲染（llvmpipe）下帧率 7~11 FPS 属正常现象，不代表真实显卡表现。

### 已知说明

- IRC 按需求未测试；本地无 IRC 服务端时游戏内每 30 秒重试一次，属正常提示。
- Target HUD / Attack Ring / Nametag 等实体视觉在单人无目标环境下无法展示实际效果，仅验证了模块开关与注册正常。

---

## English

### Background

v2.0.9 is the **Setsuna visual migration** release: the entity-visual suite from SetsunaClient (ESP / Chams / Name Tags / Target HUD / Scaffold HUD / Attack Ring / Combat Visuals / Team Viewer / Music) was ported onto the 2.0.8 base, keeping the "visual-only, no automation" boundary. IRC was intentionally excluded from testing.

### What happened this cycle

1. **Source audit** — Confirmed version 2.0.9, MC 26.1.2 + Fabric Loader 0.19.2 + Fabric API 0.150.0. The package ships with `V2.0.9_VISUAL_MIGRATION.md`; `tritium` / `repackage` sources live under `src/main/java`, and `libs/` contains the full nested dependency set (incl. fpsmaster).

2. **Build failure diagnosis (Music module deps invisible)**:
   - First build: `package tritium.ncm.music does not exist` in `MusicLyricsHUD` / `MusicScreen`.
   - Investigation: `src/main/java/tritium` was complete, but `build.gradle.kts` had `sourceSets.named("main") { java.exclude("repackage/**", "tritium/**") }`, excluding tritium from compilation.
   - After removing the exclusion, a second failure surfaced: `AudioPlayer` needs `repackage.processing.sound` and `JSynFFT` — both also present under `src/main/java/repackage` (175 files incl. com/jsyn and processing/sound), but likewise excluded.
   - Removing all exclusions fixed the build. **Root cause**: the excludes (likely intended to slim the jar) accidentally broke the Music visual module's compile + packaging.

3. **Runtime verification & screenshots**:
   - Main menu: dark anime backdrop, `DIOXIDELITE 2.0.9`, SINGLE PLAYER / MULTI PLAYER fully rendered.
   - In-game: Dynamic Island at top shows `DioxideLite v2.0.9 · FPS · latency` (module enabled by default).
   - ClickGUI (Right Shift): six-category ring fully labeled.
   - Render category: full module list including migrated Chams / Name Tags / Attack Ring / Combat Visuals / Target HUD / Dynamic Island / Global Plus; enabling ESP / Chams shows status indicators top-right, toggles work.
   - 7–11 FPS under llvmpipe software rendering is expected; it does not reflect real GPU performance.

### Known notes

- IRC not tested per request; without a local IRC server the client retries every 30s, which is a normal notice.
- Target HUD / Attack Ring / Nametag entity visuals cannot be demonstrated without a target in singleplayer; module registration and toggles were verified.
