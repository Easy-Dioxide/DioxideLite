# DioxideLite

> 基于 Fabric 的 Minecraft 客户端模组，专注**视觉增强与信息可视化**，**不含任何自动化作弊模块**。
>
> 当前版本：**v2.0.0**（Setsuna 视觉移植版）· Minecraft **26.1.2** · Java 25 · Fabric Loader 0.19.2 · Fabric API 0.150.0+

---

## ✨ 功能特性

### 🎨 UI 与 ClickGUI
- **Skija GPU 硬件渲染**：圆角窗口、阴影、半透明玻璃质感、模糊背景
- **多主题 ClickGUI**：Liquid Glass / Aurora / RISE Clean / Minimal / Signature，主题即时切换无需重启
- **HUD 编辑器**：自由拖拽调整各 HUD 组件位置与尺寸
- **Dynamic Island 灵动岛**：宽度、高度、模糊、透明度独立可调并持久化
- **SignatureLogo 品牌标识**："D" 标志，可独立控制开关、不透明度、辉光强度

### 👁️ 视觉渲染模块（纯显示，无自动执行逻辑）
- **ESP / Chams**：实体轮廓透视着色
- **HoleESP**：洞穴空洞高亮
- **NameTags**：自定义实体名牌（血量 / 距离 / 状态显示）
- **OreTracers**：矿物线条追踪
- **SpawnerFinder**：刷怪笼高亮标记
- **Tracers**：实体指向线条
- **TeamViewer**：队伍信息解析展示
- **UHCDetector**：UHC 模式信息显示
- **Xray**：方块透视高亮（仅视觉渲染）
- **TargetHUD**：目标实体状态 HUD（血量 / 距离 / 状态效果）

### 🎵 附加功能
- **网易云音乐**：音乐界面 + 歌词 HUD（tritium 库内置）
- 完整模块开关、快捷键绑定、配置持久化

> ⚠️ **声明**：本客户端所有功能均为**信息可视化**，不包含战斗自动化、自动点击、自动搭建、移动加速等作弊逻辑。请遵守服务器规则，透视类功能仅限单机/自建服使用。

---

## 📦 安装

1. 安装 [Fabric Loader 0.19.2](https://fabricmc.net/)（Minecraft 26.1.2）
2. 安装 [Fabric API 0.150.0+26.1.2](https://modrinth.com/mod/fabric-api)
3. 将 `DioxideLite-2.0.0.jar` 放入 `.minecraft/mods` 目录
4. 启动游戏，使用默认按键打开 ClickGUI 配置视觉模块与主题

---

## 🔧 构建

```bash
# 需要 JDK 25
./gradlew build -x test
# 产物位于 build/libs/DioxideLite-2.0.0.jar
```

---

## ⚠️ 兼容性

- 兼容 Sodium（渲染 Mixin 条件加载，未安装时自动跳过）
- Skija GPU 渲染依赖显卡驱动；无 GPU / 虚拟机 / 软渲染（LLVMPIPE）环境可能出现 UI 黑屏，属环境限制，真机 Windows 正常
- 请勿与大量修改渲染的其他模组同时加载

---

## 📝 更新日志

### [v2.0.0] - Setsuna 视觉移植版（当前版本）
> 基于 v2.0.0 基线，移植 SetsunaClient 视觉体系，**彻底移除全部自动化模块**。底层为 Skija base（MC 26.1.2）。

#### ✨ 新增
- **视觉渲染模块**：ESP / Chams / HoleESP / NameTags / OreTracers / SpawnerFinder / Tracers / TeamViewer / UHCDetector / Xray
- **TargetHUD**：目标状态 HUD（血量检测 HealthManager + 共享目标选择）
- **网易云音乐**：音乐界面 MusicScreen + 歌词 HUD + 音乐预设预览
- **HUD 编辑器**：HudEditorModule，支持拖拽布局
- **Apollo 队伍信息**：队伍消息解析与展示

#### 🛠 修复
- 修复 viaversion 别名缺失导致的启动空指针崩溃（`provides` 加回 `setsunavia`）
- 修复资源命名空间大小写错误导致的 `IdentifierException` 崩溃
- 修复 Skija Linux native 缺失（补充 `skija-linux-x64` 打包，Windows 包保留）

#### 🧹 清理
- **移除全部自动化**：combat / movement / player（除音乐）自动化模块、Lua 脚本系统、FeatureRuntime、RotationManager、对应自动化 mixin
- 保留纯视觉渲染管线

---

### [v1.8.2] - 2026-09-11（旧版，Minecraft 1.21.11 分支）
> 基于 v1.7 基线的完整视觉与渲染重构，仓库文档清理，日志统一收归 CHANGELOG。

#### ✨ 新增
- **GPU Skia 渲染后端**：直通 OpenGL，UI 渲染帧率显著提升
- **SignatureLogo 品牌标识**：ClickGUI 径向菜单 "D" 标志
- **ThemePage 主题设置页**：主题切换 / Liquid Glass / 视觉模板 / 动态岛 / 渲染后端配置
- **三套 ClickGUI 主题**：Liquid Glass / Minimal / Signature（即时切换无需重启）

#### 🛠 修复
- 字体测量、文本居中与字体缓存问题
- Skia GPU 后端 GL 状态修复、纹理参数错误
- 软渲染环境下主菜单被背景覆盖的问题
- Skija 依赖 jar-in-jar 内嵌打包

---

### [v1.8 / v1.8.1]
> GPU 渲染与字体修复的中间迭代版本，功能已被 v1.8.2 完整取代。

- Skia GPU 渲染后端初版（v1.8）
- Skija 渲染优化与字体补丁（v1.8.1）
- 修复打开 ClickGUI 帧率骤降问题

---

### [v1.7] - ClickGUI Theme Rework
> 基线提交 `7039eb2`。

- ClickGUI 主题系统重构：Liquid Glass / Aurora / RISE Clean / Minimal
- 玻璃透明度、模糊、边缘高亮、圆角、性能模式可调
- 非阻塞式启动揭示动画
- Dynamic Island 灵动岛尺寸 / 模糊 / 透明度控制
- 修复多主题切换、渲染动画、字体补丁等系列问题

---

## 📜 License

MIT License
