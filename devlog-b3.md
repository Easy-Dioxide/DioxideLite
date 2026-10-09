# DioxideLite — Devlog / 版本开发日志（B3）

> 版本：**B3** ｜ Minecraft **26.3** ｜ Fabric **0.19.5** ｜ Java **25**
> 状态：构建通过 · 卫戍协议内嵌验证通过 · 主菜单立绘与版本号更新

---

## 一、版本定位

B3 在 B2 视觉改版的基础上，新增**卫戍协议（Stronghold Protocol）内嵌游戏入口**，并将主菜单干员立绘更换为明日方舟「维什戴尔 · 绝对主角」皮肤，同步版本号升级至 B3。

本版本核心目标：在客户端主菜单内提供一个可直接进入的同人游戏入口，让玩家在启动客户端的同时即可体验卫戍协议的联机塔防玩法。

## 二、卫戍协议内嵌

### 入口改造
- 主菜单原「采购中心」装饰卡片改造为可交互的「卫戍协议」入口。
- `LaunchLayout.ACTIONS` 新增 `stronghold` 点击热区（994, 520, 570, 128），与卡片渲染区域精确对应，不与现有按钮重叠。
- 卡片文字更新：`采购中心` → `卫戍协议`，`招募` → `进入`，`公开招募` → `单机`，`干员寻访` → `联机`。
- 鼠标悬停有上浮动画与「卫戍协议」提示，键盘方向键聚焦后按回车亦可触发。

### 内嵌实现
- 新增 `com.dioxideliteng.ui.stronghold.StrongholdProtocolWindow`，复用项目已集成的 JavaFX + WebView（与微软登录窗口同一套机制）。
- 点击卡片后弹出 1280×720 的 JavaFX 窗口，加载官方网页版 `https://weishuxieyi.icu/_build/67c6f52a2530-11/`。
- 网页版本身支持 1–4 人联机合作，因此**联机功能直接可用**，无需额外后端。
- 单例防重复打开：窗口已存在时再次点击将置顶而非新开。
- 关闭窗口时清理 WebView 资源，`Platform.setImplicitExit(false)` 避免关窗杀掉 FX 工具包。
- 设置桌面端 User-Agent，确保游戏加载桌面版构建。

### 事件接入
- `DioxideLiteNGTitleScreen.activate()` 新增 `case "stronghold" -> StrongholdProtocolWindow.open();`。

## 三、主菜单立绘更换

- 主菜单与终端页面背景共用的 `operator.png` 立绘更换为明日方舟干员**维什戴尔（Wiš'adel）「绝对主角」皮肤**立绘。
- 来源：PRTS Wiki `立绘_维什戴尔_skin2.png`（2026-04-01 上架，对应愚人节「成就之星 / 绝对主角」系列皮肤）。
- 图片尺寸 1024×1024 RGBA，与原文件一致，无需裁剪。
- `LaunchRenderer` 中 `image("operator", ...)` 在主菜单（980×980）与终端页面背景（790×790）两处渲染，均自动应用新立绘。

## 四、版本号升级

- `gradle.properties`：`mod_version` 由 `b2` 升至 `b3`。
- `processResources` 将 `dioxideliteng-version.properties` 中的 `${version}` 展开为 `b3`。
- `ClientBranding.DISPLAY_VERSION` 读取该值并显示在主菜单「选项」卡片，界面版本标识自动更新为 **B3**。
- `fabric.mod.json` 版本号同步为 `${version}`（构建时展开）。

## 五、源码补全说明

当前 `src/` 工作树在 B3 开发前仅含 131 个 Java 文件，缺少 `util`、`mainmenu`、`terminal`、`mixins`、`hud`、`loading` 等包及全部资源（`fabric.mod.json`、mixins、assets）。完整 B2 源码存在于 `dist/dioxideliteng-b2-sources.jar`（245 个文件）。

本次开发从 `dist/dioxideliteng-b2-sources.jar` 提取缺失的 114 个 Java 文件与 110 个资源文件补入 `src/`，使项目可独立编译。`src/` 为 B2 源码的严格超集，无 src 独有文件，因此补全过程不会覆盖已有修改。

## 六、构建环境与产物

| 项目 | 值 |
| --- | --- |
| Gradle Wrapper | 9.6.1 |
| JDK | Java 25（OpenJDK 25.0.2） |
| Loom | 1.15.5 |
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5 |

构建命令：

```shell
./gradlew build -x test
```

构建需通过代理下载 Minecraft / Fabric / JavaFX 等依赖（HTTP/HTTPS 代理 `127.0.0.1:18080`）。

产物（`build/libs/`）：
```
dioxideliteng-b3.jar          # 主产物（含 JavaFX WebView / LWJGL 3.4.3 等内嵌依赖）
dioxideliteng-b3-sources.jar  # 源码包
```

构建结果：**BUILD SUCCESSFUL**，主产物中已确认包含 `StrongholdProtocolWindow` 类、维什戴尔「绝对主角」立绘及 `version=b3`。

## 七、已知限制

- 卫戍协议采用**独立 JavaFX 窗口**而非绘制进 Minecraft 画面内。将 WebView 离屏渲染进 LWJGL 帧缓冲技术难度高且不稳定，故选稳定方案——仍属于客户端内嵌（Minecraft 进程内弹出）。
- 卫戍协议联网依赖官方部署 `weishuxieyi.icu`；若需离线游玩，需将卫戍协议源码打包进 mod 并本地起 Node 服务，属后续可选改造。
