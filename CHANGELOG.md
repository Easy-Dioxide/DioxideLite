# DioxideLite Changelog

## v2.0.3 (2026-09-13) — IRC Link & Name Tag Module

### 新增 / New
- **IRC 接入（Player 分类，默认开启）**：移植 OpticsValleyIRC 1.20 客户端协议至 26.1.2（明文 TCP、`localhost:16688` 默认配置、`Host`/`Port` 设置项）。模块在 ClickGUI → Player 分类中可开关，**默认启用**；连接状态、在线用户名单实时维护。**无任何自动化能力**：纯聊天桥接，且刻意忽略远程 CRASH 控制指令（防远程杀进程）。
- **Nametag IRC Logo（挂在 Nametag 视觉模块）**：IRC 在线玩家的 nametag 前缀显示客户端 Logo 字形（bitmap 字体 ``），第三人称下自己的 nametag 同样显示；Logo 显示开关位于 ClickGUI → Render → Legend Watch（Nametag 模块）→ `IRC Logo`，默认开启。
- **灵动岛 Tab 状态行**：按住 Tab 展开灵动岛时，顶部新增 IRC 状态行——`IRC Off` / `IRC Connecting...` / `IRC Online · N online`（灰/琥珀/绿），展开面板高度随玩家数自适应。
- **聊天栏 IRC 消息与 `/irc` 命令**：IRC 服务器消息经颜色转换进入聊天栏；`/irc connect|disconnect|status|send <消息>` 聊天命令（走聊天栏拦截链路，不依赖 Fabric command API）。

### 优化 / Optimised
- **恢复 backdrop 降采样**：`BACKDROP_DOWNSAMPLE` 回到 0.5，全局模糊/背景模糊在软渲染与核显场景下压力更小（HUD 本体仍全分辨率矢量渲染，不糊字）。

### 修复 / Fixed
- **F6 主题切换崩溃**：ClickGUI 打开时按 F6 热切换主题不再崩溃（多主题往返切换实测稳定）。
- **模块状态通知残留**：彻底移除 `Module.setEnabled` 自动弹右上角 "Dynamic Island / Enabled" 通知的调用，开关模块不再刷屏。
- **26.1.2 自己 nametag 不显示**：MC 26.1.2 不再为本地玩家填充 `EntityRenderState.nameTag`，Nametag 模块装饰无法生效——现在由 `EntityRendererMixin` 补填（含位置附件），nametag 模块（传奇后缀 + IRC Logo）可正常装饰自己的名字。

### 版本 / Version
- gradle.properties `version=2.0.3`；`DioxideLite.VERSION=2.0.3`；窗口标题 DioxideLite 2.0.3。

---

# DioxideLite Changelog

## v2.0.2 (2026-09-13) — Global Blur & Built-in Optimisers

### 新增 / New
- **全局模糊控制（Global Blur）**：ClickGUI → Render 分类新增 `Global Blur` 模块，**默认关闭**。关闭时完全不绘制任何 HUD 背景模糊并跳过每帧 backdrop 快照（视觉全锐利、零模糊开销）；开启后统一接管所有 HUD 背景模糊，`Blur Strength` 滑块（1–16，默认 6）覆盖各 HUD 自身强度。
- **内置主流优化模组（jar-in-jar）**：随主 jar 打包 Sodium 0.8.12 + Lithium 0.24.7 + FerriteCore 9.0.0（fabric.mod.json `jars` 自动注入），免去单独安装，实测三模组正常加载、无 mixin 冲突。
- **视觉 HUD 三件套模块化**：Performance HUD、Watermark、灵动岛全部纳入 ClickGUI Render 分类，可独立开关，**默认全部关闭**。

### 优化 / Optimised
- **HudFusionManager 布局指纹缓存**：按屏幕尺寸与各 HUD enabled/位置/尺寸异或指纹缓存融合布局，消除每帧 O(n²) 碰撞 BFS，所有带背景融合的 HUD 受益。
- **文本缓存 LRU 上限**：`TEXT_WIDTH_CACHE`/`TEXT_RUN_CACHE` 由无界 HashMap 改为有界 LRU（4096/2048），杜绝 FPS/Ping 文本长期换 key 造成的内存无限增长。
- **弃用分辨率/降采样手段**：blur 背景降采样（BACKDROP_DOWNSAMPLE）彻底停用并条件化（1.0 时不执行降采样），HUD 保持全分辨率矢量渲染，字体不糊。

### 修复 / Fixed
- 修复模块启用/关闭时右上角通知文字 **baseline 错位字符重叠**（"Dynamic Island" 渲染成乱码）：fallback 分段绘制统一使用主字体单一 baseline。
- **模块状态通知默认关闭**：启用/关闭模块不再弹出右上角 "Dynamic Island / Enabled" 等通知（ClickGUI 可重新开启）。
- 修复用户自改源码的全量编译错误：`DioxideDynamicIsland` 重复字段、`repackage/**`（javazoom mp3、processing sound）与 `tritium/**`（ncm 解密）第三方死代码编译失败（146+ 错误）→ 编译排除，源码保留。

### 版本 / Version
- gradle.properties `version=2.0.2`；`DioxideLite.VERSION=2.0.2`；窗口标题 DioxideLite 2.0.2。

---

## v2.0.1 (2026-09-12) — OPAI Dynamic Island

### 新增 / New
- **OPAI 风格灵动岛重构**：`DioxideDynamicIsland` 全新实现——暗色玻璃胶囊、白色内边描边、顶部光泽与蓝青辉光，顶部居中平滑浮动画（正弦微浮动效）。
- **灵动岛双形态**：
  - 紧凑态：LOGO + 客户端名 + 版本 + FPS + 延迟，单行胶囊；
  - 展开态：按住 Tab（玩家列表键）展开为大面板，显示服务器名、延迟、玩家列表（按 Tab 列表序排序），高度随玩家数自适应。
- 灵动岛接入共享 Skija 覆盖层（`SkijaRenderer.renderOverlay`），仅在游戏内（无 Screen）渲染，与 HUD 模块同画布。

### 修复 / Fixed
- 修复 `Paint.FilterQuality.HIGH` 编译错误（Skija 0.143 无此 API）→ 改用 `SamplingMode.LINEAR` + `drawImageRect(..., true)` 严格采样。
- 继承 v2.0.0 视觉对齐修复：viaversion 别名 `setsunavia`/provides、`skija-linux-x64` native、资源命名空间 `dioxide-lite` 小写、字体/图标资源路径修正、`nested-058` 旧版 mcstructs 类清理。

### 版本 / Version
- gradle.properties `version=2.0.1`；主菜单标题显示 DIOXIDELITE 2.0.1；窗口标题 DioxideLite 2.0.1。

---

## v2.0.0 (2026-09-11) — Visual Parity & New Base

- New base: Skija GPU-backed rendering architecture under the DioxideLite identity.
- ClickGUI: Pop/Drop Skija renderer retained; F6 hot-switches Liquid Glass, Minimal and Signature presentation modes.
- Dynamic Island: DioxideLite visual/QoL implementation added to the shared Skija overlay pass.
- Removed gameplay automation module registration and deleted the corresponding combat/movement/player module trees and injection points.
- Removed cheat-oriented render feature registration.
- No packet-based gameplay manipulation, combat automation, movement automation, anti-cheat bypass, or command/script automation is exposed by v2.
- DioxideLite is the only client-facing brand in the main project.
