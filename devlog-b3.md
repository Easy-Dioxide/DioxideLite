# DioxideLite — Devlog / 版本开发日志（B3）

> 版本：**B3** ｜ Minecraft **26.3** ｜ Fabric **0.19.5** ｜ Java **25**
> 状态：构建通过 · 主菜单立绘更换 · 版本号升级

---

## 一、版本定位

B3 在 B2 基础上，将主菜单干员立绘更换为明日方舟「维什戴尔 · 绝对主角」皮肤，并同步版本号升级至 B3。

## 二、主菜单立绘更换

- 主菜单与终端页面背景共用的 `operator.png` 立绘更换为明日方舟干员**维什戴尔（Wiš'adel）「绝对主角」皮肤**立绘。
- 来源：PRTS Wiki `立绘_维什戴尔_skin2.png`（2026-04-01 上架，对应愚人节「绝对主角」系列皮肤）。
- 图片尺寸 1024×1024 RGBA，与原文件一致，无需裁剪。
- `LaunchRenderer` 中 `image("operator", ...)` 在主菜单（980×980）与终端页面背景（790×790）两处渲染，均自动应用新立绘。

## 三、版本号升级

- `gradle.properties`：`mod_version` 由 `b2` 升至 `b3`。
- `processResources` 将 `dioxideliteng-version.properties` 中的 `${version}` 展开为 `b3`。
- `ClientBranding.DISPLAY_VERSION` 读取该值并显示在主菜单「选项」卡片，界面版本标识自动更新为 **B3**。
- `fabric.mod.json` 版本号同步为 `${version}`（构建时展开）。

## 四、源码补全说明

`src/` 工作树在开发前仅含 131 个 Java 文件，缺少 `util`、`mainmenu`、`terminal`、`mixins`、`hud`、`loading` 等包及全部资源。从 B2 sources jar 提取缺失的 114 个 Java 文件与 110 个资源文件补入 `src/`，使项目可独立编译。

## 五、构建环境与产物

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

产物（`build/libs/`）：
```
dioxideliteng-b3.jar          # 主产物
dioxideliteng-b3-sources.jar  # 源码包
```

构建结果：**BUILD SUCCESSFUL**，主产物中已确认维什戴尔「绝对主角」立绘及 `version=b3`。
