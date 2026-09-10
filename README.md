# DioxideLite（pvputils1.4 分支 · 中文说明）

> Minecraft Java Edition **1.21.11** · **Fabric** · 客户端视觉向模组
> 构建环境：Java 21 · Fabric Loader 0.19.5+

DioxideLite 是基于 **PVPUtils** 的**非商业开源衍生客户端**（源自 `pvputilsRename` 重命名分支），在保留原视觉与 Skija 渲染栈的基础上，对界面视觉进行了**二次修改与扩充**。

---

## 开源许可声明（必读）

本项目依据 **PVPUtils 源代码可得非商业性许可证 v1.0** 授权，许可证全文见 [`LICENSE-PVPUTILS.txt`](LICENSE-PVPUTILS.txt)。使用、修改或分发本项目，即表示你同意该许可证的全部条款。

### 署名（许可证第 8 条）

本项目使用了 PVPUtils 的代码，由 **Nachoneko_miao** 及 **PVPUtils 贡献者**编写。

- 原始项目：https://github.com/bakabaicai/PVPUtils
- 原始作者：Nachoneko_miao
- 本仓库同时保留原始 [`LICENSE`](LICENSE)（GPLv3 + 附加非商业条款，Copyright © 2024 Nachoneko_miao）

### 非商业限制（许可证第 5 条）

本项目及其任何衍生作品**禁止任何形式的商业使用**，包括但不限于：销售、付费下载、付费整合包、付费客户端、广告收入、赞助收入、订阅访问、付费捆绑包等。

### 源代码要求（许可证第 6 条）

任何公开或私下分发的衍生作品，都必须提供完整的对应源代码；**禁止分发基于本项目的闭源衍生作品**。

### 许可证继承（许可证第 7 条）

任何衍生作品必须继续使用本许可证或**同等更严格**（限制性更强）的非商业、源代码可得、署名及披露要求的许可证授权；不得在更宽松的许可证下再授权。

### 修改披露（许可证第 9 条）

本项目在 PVPUtils 基础上的重大更改如下：

| 变更项 | 说明 | 衍生位置 |
|---|---|---|
| 客户端身份 | 由 PVPUtils 重命名为 DioxideLite（`pvputilsRename` 分支） | 全局 |
| ClickGUI 主题化 | 重构为主题化体系：**ORIGINAL**（Liquid Glass）/ **MINIMAL_POP** / **SIGNATURE** 三主题 | `src/client/java/com/dioxidelite/client/gui/clickgui/` |
| 主题热切换 | 新增 `ClickGuiThemeController` 统一主题路由，切换主题**无需重启游戏**，重新打开 ClickGUI 即生效 | `ClickGuiThemeController.java` |
| Signature 主题 | 新增径向/分类式 Signature ClickGUI（独立实现，青色强调） | `DioxideLiteSignatureClickGuiScreen.java` |
| Liquid Glass 视觉 | 新增玻璃材质视觉系统（Skija 实现，含透明度/模糊/边缘高光/圆角/性能模式） | `src/client/java/com/dioxidelite/client/render/skia/` |
| 全局玻璃 | 新增 `liquidGlassAllVisuals` 一键全局玻璃（HUD/快捷栏/聊天/物品栏/界面卡片） | `src/client/java/com/dioxidelite/mixin/client/` |
| 自定义主菜单 | 新增 GLSL 着色器背景主菜单与启动揭示动画 | `src/client/java/com/dioxidelite/client/render/MainUI/` |
| HUD 模块 | 新增 Dynamic Island、Signature HUD 等 HUD 信息组件 | `src/client/java/com/dioxidelite/client/hud/` |
| 设置模型 | 保留 PVPUtils 的 BasePage / SettingModule 设置模型，主题切换只改变呈现、不改变功能逻辑 | `gui/clickgui/pages/`、`gui/clickgui/widget/` |

> 来源版本：基于 PVPUtils 原始仓库（https://github.com/bakabaicai/PVPUtils）的重命名/衍生分支开发；若你的发行版包含具体上游 commit/版本号，请在 README 或 NOTICE 中补充。

### 保留通知（许可证第 10 条）

不得删除、隐藏或歪曲本项目的版权声明、许可证声明、作者姓名、贡献者声明或署名声明。分发本项目时**必须随附本许可证副本**（`LICENSE-PVPUTILS.txt` 与 `LICENSE`）。

### 第三方组件（许可证第 12 条）

本项目使用或包含的第三方代码、库、着色器与资产仍受其自身许可证约束，包括但不限于：

- **NoSneakAnim**（作者 chromonym，Apache License 2.0）——潜行动画功能的设计参考与实现细节，见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)
- Skija（渲染库）、Fabric API 等，均遵守各自许可证

### 免责声明（许可证第 14 条）

本项目按"原样"提供，不提供任何形式的担保。原始作者与贡献者不对因使用、修改、分发或依赖本项目或任何衍生作品所引起的崩溃、数据丢失、账号封禁、服务器处罚、兼容性问题、安全问题、法律纠纷、利润损失或任何其他损害承担责任。

---

## 功能特性

- **自定义主界面**：GLSL 着色器动态背景 + CLICK TO START 交互 + 非阻塞启动揭示动画
- **ClickGUI 三主题**（RSHIFT 打开）：
  - `ORIGINAL`：Liquid Glass 卡片式原版布局
  - `MINIMAL_POP`：紧凑深色 Minimal 呈现
  - `SIGNATURE`：径向分类菜单 + 分段面板过渡 + hover 插值 + 滑入动画
- **主题热切换**：在 Theme 页切换主题**无需重启游戏**，立即生效/重开生效
- **Liquid Glass 视觉体系**：可调玻璃透明度、背景模糊强度、边缘柔和高光、圆角与性能模式
- **全局玻璃** `liquidGlassAllVisuals`：一键将玻璃材质应用到游戏内 HUD、聊天、物品栏与界面卡片
- **HUD 模块**：Dynamic Island（可独立调宽高/模糊/透明度）、Signature HUD 等
- 仅包含**合法客户端侧视觉/便利功能**：无战斗自动化、无自瞄辅助、无反作弊绕过等作弊逻辑

## 安装

1. 安装 **Minecraft 1.21.11** + **Fabric Loader 0.19.5+**；
2. 将 `DioxideLite-v1.7.jar` 放入 `.minecraft/mods/`；
3. 启动游戏即可。

## 使用

- 按 **RSHIFT** 打开/关闭 ClickGUI；
- ClickGUI → **Theme** 页 → 右键展开 **ClickGUI Theme** 模块 → 点击 **Layout** 子项循环切换主题（无需重启游戏）；
- 游戏内按 **E** 打开物品栏可查看全局玻璃效果（开启 `liquidGlassAllVisuals` 后）。

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/`（`DioxideLite-v1.7.jar` 与 `-sources.jar`）。

## 致谢

感谢 **Nachoneko_miao** 与 **PVPUtils 贡献者** 的开源工作（https://github.com/bakabaicai/PVPUtils）；感谢 NoSneakAnim 作者 **chromonym** 的潜行动画设计参考。
