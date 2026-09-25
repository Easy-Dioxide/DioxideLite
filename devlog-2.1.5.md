# DioxideLite 2.1.5 — RenderStable OnyxPort

> 发布日期：2026-09-25

---

## 核心变更
- 版本固定为 2.1.5。
- Render 模块稳定性修复：Compass / ESP / NameTags / WorldToScreen。
- OnyxTargetScanner 修复 Villager 编译错误（26.1.2 mapping）。
- ClickGui.Mode.Pop → Setsuna 修复。

## 已移植的 Onyx 模块（全量）

### Combat（18 个）
KillAura / KillAuraPlus / AntiBot / AutoTotem / Surround / Criticals / Backtrack / Burrow / FakeLag / MaceAura / SpearKill / ZealotCrystalPlus / BedBreaker / BedDefender / BedTracker / JumpReset / AttackRing / CombatVisuals

### Movement（14 个）
Scaffold / Velocity / NoSlow / Speed / Sprint / MovementFix / InvMove / NoFall / NoJumpDelay / KeepSprint / FlatElytraFly / AutoSprint / SafeWalk / LegacyScaffoldEngine

### Player（16 个）
ChestStealer / InvManager / AutoTool / BedAura / FastBreak / FastCraft / AutoMLG / AntiWeb / GhostHand / PacketEat / FakePlayer / AntiResourcePack / IrcModule / NetEaseMusicModule / Deposit / FastPlace / InventoryManager / NameChanger / ScaffoldOnyx

### Render（20+ 个）
ESP / HoleESP / Tracers / OreTracers / SpawnerFinder / UHCDetector / Xray / Chams / BlockHighlight / FullBright / GlobalBlur / Radar / TargetHUD / Watermark / PotionHUD / Notifications / MusicLyrics / Dynamic Island / NameTags / Compass / KillEffect / LegendWatch / TeamViewer / DeltaForceStyle

## Onyx 引擎层
- OnyxCombatEngine — KillAura 目标获取
- OnyxMovementEngine — Auto Sprint / Safe Walk
- OnyxPlayerEngine — Name Changer
- OnyxClientEngine — 生命周期/上下文边界
- OnyxTargetScanner — 目标扫描

## 保持不变
- 主菜单 UI（砂狼白子背景）
- Dynamic Island 4 样式（DIOXIDE / OPAI_ONYX / ONYX_MINIMAL / ONYX_GLASS）
- ClickGUI 4 主题（LIQUID_GLASS / MINIMAL / SIGNATURE / OPAI_ONYX）
