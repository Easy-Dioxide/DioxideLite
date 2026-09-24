# DioxideLite 2.1.4 — OpenOnyx Engine Migration（中文）

## 核心变更
- 版本固定为 2.1.4。
- Combat / Movement / Player / Client 增加统一 Onyx backend 层。
- KillAura 的目标获取改由 `OnyxCombatEngine` 负责，保留 DioxideLite 原有 ClickGUI、TargetHUD 与视觉链。
- Auto Sprint、Safe Walk 改由 `OnyxMovementEngine` 负责。
- Name Changer 改由 `OnyxPlayerEngine` 负责。
- `OnyxClientEngine` 作为客户端生命周期/上下文边界，渲染仍使用 DioxideLite 的 Skija 链。

## 兼容策略
OpenOnyx 的源码包含大量旧 Minecraft mapping/API（例如旧版 Entity、Packet、Minecraft 类）。直接复制原类会导致 26.1.2 编译失败，因此 2.1.4 使用当前 Minecraft API 重建 Onyx backend，而不是把旧 mapping 硬塞进主源码。

## 保持不变
- 主菜单 UI 不修改。
- Dynamic Island / OPAI Onyx 样式体系保留。
- DioxideLite 版本号为 2.1.4。
