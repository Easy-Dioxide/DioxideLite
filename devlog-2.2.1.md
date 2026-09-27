# DioxideLite v2.2.1 Devlog

**日期：** 2026-09-27
**版本：** v2.2.1
**基于：** v2.2.2-fixed（GPT 修复版）

---

## 新增功能

### Halo 模块（碧蓝档案光环渲染）
- 从 [blue-archive-halo](https://github.com/opai-client/blue-archive-halo)（MC 1.8.9）移植到 DioxideLite
- 支持 4 种角色光环：砂狼白子 / 黑见芹香 / 小鸟游星野 / Opai Logo
- 可调节参数：Size、Spacing、X/Y Rot、Follow Pitch、Render In First Person、Floating Animation
- 注册在 ClickGUI → Render 分类最底部

### Onyx HUD 移植
- `OnyxArraylistHUD`：Onyx 风格 ArrayList
- `OnyxNotifsHUD`：Onyx 风格通知
- `OnyxPotionHUD`：Onyx 风格药水状态显示

---

## 修复

### ChatScreenMixin 崩溃修复
- **问题：** MC 26.1.2 的 `ChatScreen` 中 `mouseDragged` / `mouseReleased` / `mouseScrolled` / `charTyped` 方法签名变更，导致 Mixin 注入失败，游戏启动崩溃
- **修复：** 所有 HUD Editor 相关注入点添加 `require = 0`，找不到目标方法时跳过注入而非崩溃
- **影响文件：** `src/main/java/com/dioxidelite/mixin/ChatScreenMixin.java`

### HUD 窗口大小调整
- 修复窗口更改大小时 HUD 偏移或拉伸的问题

### 聊天编辑器
- HUD Editor 聊天界面唤起功能修复

---

## 验证

- [x] 构建成功（60MB jar）
- [x] 游戏启动正常
- [x] 主菜单砂狼白子背景显示正常
- [x] 游戏内 HUD + ArrayList 显示正常
- [x] 竹林地形渲染正常

---

## 文件变更

| 文件 | 说明 |
| --- | --- |
| `module/modules/render/Halo.java` | 新建，光环渲染模块 |
| `ui/hud/OnyxArraylistHUD.java` | 新建，Onyx ArrayList |
| `ui/hud/OnyxNotifsHUD.java` | 新建，Onyx 通知 |
| `ui/hud/OnyxPotionHUD.java` | 新建，Onyx 药水 HUD |
| `mixin/ChatScreenMixin.java` | 修复，注入点加 require=0 |
| `resources/assets/dioxidelite/halo/` | 新建，4 组光环纹理 |
