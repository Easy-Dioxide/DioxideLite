# DioxideLite v2.1.0 — Devlog

> 2026-09-15 · DioxideLite Team

---

## 中文版

### 背景

v2.1.0 是在 2.0.9 基础上聚焦 **命令 / 歌词 / 聊天 HUD** 的版本：新增客户端命令系统骨架、聊天 → 灵动岛联动、HUD Editor 聊天 overlay，并修复 MC 26.1.2 下新增 Mixin 导致的启动崩溃。

### 本轮开发过程

1. **源码盘点**：确认版本号 2.0.9 → **改为 2.1.0**（gradle.properties + DioxideLite.java）；新增 `command/` 包（CommandManager / CommandBuilder / 参数与补全）、`IrcChatHandler`、`ChatScreenMixin` / `ChatComponentMixin`、`DynamicIslandBridge`。

2. **构建修复 ×2**：
   - `sourceSets` 再次误排除 `tritium/**` 与 `repackage/**` → 移除排除项（与 v2.0.9 同源问题）。
   - `ScaffoldBlockHUD` 第 49 行 `(float) stayTime.get()` 对装箱 `Double` 强转编译失败 → 改为 `((Number) stayTime.get()).floatValue()`。

3. **启动崩溃修复（ChatScreenMixin）**：
   - 运行时报 `Critical injection failure: @Inject ... could not find any targets matching 'charTyped' in ChatScreen`。
   - 反编译 MC 26.1.2 `ChatScreen` 确认：**26.1.2 已无 `charTyped`**（字符输入迁移至 `KeyboardHandler.charTyped`），且 `mouseDragged` / `mouseReleased` 也不存在；有效方法为 `keyPressed(KeyEvent)`、`mouseClicked(MouseButtonEvent, boolean)`、`handleChatInput(String, boolean)`。
   - 移除三个失效注入点（`charTyped`、`mouseDragged`、`mouseReleased`），保留 `keyPressed`（Tab 补全 + 输入跟踪）、`mouseClicked`（HUD Editor 点击）、`handleChatInput` WrapOperation（命令分发）。重新构建后正常启动。

4. **运行验证与截图**：
   - 主菜单：`DIOXIDELITE 2.1.0` + SINGLE PLAYER / MULTI PLAYER 完整。
   - 进游戏：灵动岛显示 `DioxideLite v2.1.0 · FPS · 延迟`。
   - ClickGUI（右 Shift）：六分类环形菜单文字完整；Render 分类含 ESP / Dynamic Island / Global Blur / Watermark HUD / Performance HUD / Array List / Session Info 等。
   - **聊天链路**：T 键打开聊天即唤起 HUD Editor overlay（"HUD EDIT - drag to move" + RESET/DONE），输入 `hello dioxidelite` 回车后聊天栏正常显示 `<Player940> hello dioxidelite`，灵动岛聊天联动生效。
   - 软渲染（llvmpipe）下帧率 6~10 FPS 属正常现象，不代表真实显卡表现。

### 已知说明

- 命令系统为骨架实现：v2 明确不暴露任何 gameplay / cheat 命令，`CommandManager.handle` 默认返回 false 放行至原版聊天；`complete` 返回 null（Tab 补全空操作）。
- IRC 默认开启且本地无服务端时每 30 秒重试一次，属正常提示，本轮未做 IRC 测试。
- HUD Editor 聊天 overlay 的拖拽依赖 `mouseClicked` 注入；`mouseDragged` / `mouseReleased` 因 26.1.2 API 变更已移除，拖拽细节动作由点击 / 按键流程覆盖。

---

## English

### Background

v2.1.0 focuses on **commands / lyrics / chat HUD** on top of 2.0.9: a client command-system skeleton, chat → Dynamic Island linkage, a HUD Editor chat overlay, and a fix for the startup crash caused by the new mixins under MC 26.1.2.

### What happened this cycle

1. **Source audit** — Version bumped 2.0.9 → **2.1.0** (gradle.properties + DioxideLite.java). New packages: `command/` (CommandManager / CommandBuilder / params & completion), `IrcChatHandler`, `ChatScreenMixin` / `ChatComponentMixin`, `DynamicIslandBridge`.

2. **Build fixes ×2**:
   - `sourceSets` again excluded `tritium/**` and `repackage/**` → removed (same root cause as v2.0.9).
   - `ScaffoldBlockHUD:49` — `(float) stayTime.get()` fails on a boxed `Double` → changed to `((Number) stayTime.get()).floatValue()`.

3. **Startup crash fix (ChatScreenMixin)**:
   - Runtime error: `Critical injection failure: @Inject ... could not find any targets matching 'charTyped' in ChatScreen`.
   - Decompiled MC 26.1.2 `ChatScreen`: **26.1.2 no longer has `charTyped`** (character input moved to `KeyboardHandler.charTyped`); `mouseDragged` / `mouseReleased` are also gone. Valid methods: `keyPressed(KeyEvent)`, `mouseClicked(MouseButtonEvent, boolean)`, `handleChatInput(String, boolean)`.
   - Removed the three dead injection points (`charTyped`, `mouseDragged`, `mouseReleased`); kept `keyPressed` (Tab completion + input tracking), `mouseClicked` (HUD Editor clicks), and the `handleChatInput` WrapOperation (command dispatch). Rebuilt and launched cleanly.

4. **Runtime verification & screenshots**:
   - Main menu: `DIOXIDELITE 2.1.0` with SINGLE PLAYER / MULTI PLAYER fully rendered.
   - In-game: Dynamic Island shows `DioxideLite v2.1.0 · FPS · latency`.
   - ClickGUI (Right Shift): six-category ring fully labeled; Render category includes ESP / Dynamic Island / Global Blur / Watermark HUD / Performance HUD / Array List / Session Info.
   - **Chat pipeline**: pressing T opens the chat with the HUD Editor overlay ("HUD EDIT - drag to move" + RESET/DONE); typing `hello dioxidelite` and hitting Enter shows `<Player940> hello dioxidelite` in chat, and the Dynamic Island chat linkage works.
   - 6–10 FPS under llvmpipe software rendering is expected; it does not reflect real GPU performance.

### Known notes

- The command system is a skeleton: v2 intentionally registers no gameplay/cheat commands; `CommandManager.handle` returns false by default (pass-through to vanilla chat); `complete` returns null (Tab completion no-op).
- IRC is enabled by default; without a local server it retries every 30s, which is a normal notice. IRC was not tested this cycle.
- HUD Editor chat-overlay clicks rely on the `mouseClicked` injection; `mouseDragged` / `mouseReleased` were dropped due to the 26.1.2 API change, with drag behavior covered via click/key flows.
