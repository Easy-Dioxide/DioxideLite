# DioxideLite

**Minecraft 26.1.2 · Fabric · PvP / Visual 客户端**

这是 DioxideLite 的开源版本（当前版本：**v2.0.7**）。允许你进行任何操作，甚至魔改后售卖（详见 LICENSE）。

内置完好的开发环境（Gradle + Fabric Loom），可直接构建并游玩；构建产物同时发布在 GitHub Release 中。

## 版本速览

| 版本 | 说明 |
| :--- | :--- |
| **v2.0.7** | 渲染性能根因修复：移除 Array List 每帧 GL 状态快照；模糊按渲染档位降采样（文字保持原生分辨率）；logo Mitchell 采样 + Paint 复用；IRC capability 私有帧（仅 DioxideLite 客户端间显示 nametag logo） |
| v2.0.5 | overlay 末帧快路径 + 模糊滤镜缓存；修复 HUD 偶发不渲染（配置加载时机）；灵动岛默认启用；恢复 HUD Editor；新增 Sprint 模块；IRC 无限重连 |
| v2.0.4 | nametag logo 改 Skija 渲染（自适应宽度 + 垂直对齐）；Module List 移入 Render 分类；全分辨率背景模糊；灵动岛/Watermark 清晰度修复 |
| v2.0.3 | IRC 接入（Player 分类、自动重连、聊天镜像、灵动岛 Tab 在线列表）；F6 主题切换崩溃修复、免重启换肤 |
| v2.0.2 | Global Blur（Render 分类、默认关、强度 1–16）；HUD 管线优化 |
| v2.0.1 | OPAI 风格灵动岛（compact + Tab 展开）；Watermark |
| v2.0.0 | 换 base 重构，移除自动化模块；D 形 logo 主视觉；Setsuna 风格视觉 |

## 功能

- **全 Skija UI 渲染**：ClickGUI / HUD / Watermark / 灵动岛 / nametag logo 均走 Skija 管线
- **灵动岛（Dynamic Island）**：OPAI 风格，compact + Tab 展开（玩家列表 / 服务器 / IRC 状态 / FPS）
- **ClickGUI**：六分类轮盘（Client / Combat / Misc / Render / Movement / Player），主题免重启切换
- **Watermark / HUD**：自定义名称与 logo，HUD Editor
- **Global Blur**：全局毛玻璃开关与强度调节
- **IRC**：接入 OpticsValleyIRC 协议（默认开启），聊天镜像 + 在线用户检测 + nametag logo
- **优化档位**：iGPU / Balanced / Quality 三档渲染配置，核显友好

## 构建

```bash
# 需要 JDK 25
./gradlew build -x test
# 产物在 build/libs/DioxideLite-2.0.7.jar
```

## 运行

将 `DioxideLite-2.0.7.jar` 放入 `.minecraft/mods`（Fabric Loader 0.19.2+，MC 26.1.2），或直接 `./gradlew runClient`。

## 联系方式

QQ：**81622964**

找我优化视觉设计、制作宣传片完全免费。我只想为这个圈子贡献一份自己的力量——尽管它微不足道。少一些争执，多一些包容。

祝你早上好、中午好、晚上好。
