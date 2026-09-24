# OpenOnyx → DioxideLite 2.1.3 Visual Compatibility Audit

## 中文

本表只记录**视觉适配层**。OpenOnyx 中涉及自动战斗、自动移动、自动放置、背包自动管理或其他 gameplay automation 的实现没有在本次视觉适配中重新接入。

| OpenOnyx visual surface | DioxideLite 2.1.3 | 状态 |
|---|---|---|
| ClickGUI / ThemeSupport | `ClickGui` + `DioxideThemeController` + `PopClickGuiScreen` / `WindowClickGuiScreen` | 已适配 |
| TargetHUD | `TargetHud` | 已适配；读取 DioxideLite 当前目标状态 |
| Scaffold HUD | `ScaffoldBlockHUD` | 已适配；读取现有 Scaffold 状态 |
| BlockOverlay | `BlockHighlight` | 已有对应视觉模块 |
| ESP | `ESP` | 已有对应视觉模块 |
| Target ESP / combat visuals | `CombatVisuals` / `AttackRing` / `TargetHud` | 已有视觉拆分 |
| Notifications | `Notifications` / `NotificationManager` | 已有；通知时长继续由配置控制 |
| Watermark | `WatermarkHUD` | 已有；DioxideLite Logo |
| Potion HUD | `PotionHUD` | 已有 |
| Music / lyrics | `MusicLyricsHUD` + `NcmLyrics` | 已适配 |
| Dynamic Island | `DioxideIslandModule` + `DioxideDynamicIsland` | 已适配；4 种 Island style |

### ClickGUI 主题

`ClickGui.Theme` 现在是普通持久化 `EnumSetting`，可直接在 ClickGUI 中选择：

- Liquid Glass
- Minimal
- Signature
- OPAI Onyx

`OPAI Onyx` 使用 DioxideLite 当前 Skija UI，不替换原版主菜单。

### Dynamic Island

`Dynamic Island → Island Style`：

- Dioxide
- OPAI Onyx
- Onyx Minimal
- Onyx Glass

歌词仍通过现有 `NcmLyrics` API 获取，并在接口异常时返回空状态而不是抛出到 HUD 渲染线程。

### 图标

窗口图标现在提供 16/32/48/64/128 五种尺寸。`WindowMixin` 将这些尺寸交给窗口系统，以覆盖窗口标题栏、Windows 任务栏以及高 DPI 缩放场景。

另外在 `fabric.mod.json` 中加入 128px DioxideLite Logo，避免 Fabric/Mod UI 继续引用 Via 的旧图标。

## English

This audit covers the **visual adapter layer only**. OpenOnyx gameplay automation such as automated combat, movement, placement, inventory automation, or similar behavior is not reintroduced through the visual adapter.

The current 2.1.3 mapping is:

- ClickGUI / themes → `ClickGui` + `DioxideThemeController`
- Target HUD → `TargetHud`
- Scaffold HUD → `ScaffoldBlockHUD`
- Block overlay → `BlockHighlight`
- ESP → `ESP`
- Combat presentation → `CombatVisuals` / `AttackRing` / `TargetHud`
- Notifications → `Notifications` / `NotificationManager`
- Watermark → `WatermarkHUD`
- Potion HUD → `PotionHUD`
- Music / lyrics → `MusicLyricsHUD` + `NcmLyrics`
- Dynamic Island → `DioxideIslandModule` + `DioxideDynamicIsland`

ClickGUI themes are persisted through `ClickGui.Theme` and can be changed directly from the GUI. The available themes are Liquid Glass, Minimal, Signature and OPAI Onyx.

Dynamic Island styles are Dioxide, OPAI Onyx, Onyx Minimal and Onyx Glass. Lyrics remain isolated behind the existing `NcmLyrics` API.
