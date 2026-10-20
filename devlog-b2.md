# DioxideLite — Devlog / 版本开发日志（B2）

> 版本：**B2** ｜ Minecraft **26.3** ｜ Fabric **0.19.5** ｜ Java **25**（Temurin 25.0.4.1 LTS）
> 状态：构建通过 · 246 项单元测试全绿 · 运行时启动验证通过

---

## 一、版本定位

B2 是 DioxideLite 面向 Minecraft 26.3（Fabric / Java 25）新一代重构客户端的**视觉改版**。在 B1 整体重构（55 个功能模块、Mixin 运行时注入、统一品牌与启动体验）的基础上，B2 集中打磨视觉呈现：重绘启动动画与主菜单，调整 ClickGUI / HUD 的视觉细节，并完成全平台构建与运行时验证。

## 二、视觉改动

### 启动动画
- 自定义 **LaunchIntro** 四阶段启动动画（boot → login → connection → emblem），深色赛博朋克基调、黄色几何菱形边框与刻度装饰。
- 中央「轮回」黑底黄边方形标识 + `DioxideLite` 字样，左侧 `TERMINAL SERVICE` 登录窗，约 13 秒。

### 主菜单（干员主题）
- 「风暴瞭望 STORMWATCH」活动入口、干员立绘、终端数值面板（理智 / 等级 / 关卡进度 / 日期横幅）。
- 功能入口：单人游戏 / 账号设置 / 采购中心 / 多人游戏 / 选项 / 模组，以及好友、档案。

### ClickGUI
- Combat / Movement / Player / Visual / Misc 分类布局，顶部显示玩家信息与 Configurations，右下角铅笔按钮进入 HUD 编辑器。
- 左键开关模块、右键展开设置，GUI Scale 可调。

### HUD
- 顶部胶囊信息面板（DynamicIsland），实时显示客户端版本 / FPS / 延迟。
- 玩家信息 / 击杀 / 胜利统计 / Inventory 提示等 HUD 元素，支持 ClickGUI 右下角铅笔按钮进入编辑器自由拖拽排版。

## 三、构建环境与产物

| 项目 | 值 |
| --- | --- |
| Gradle Wrapper | 9.6.1 |
| JDK | Temurin 25.0.4.1 LTS |
| Loom | 1.15.5 |
| 依赖仓库 | Fabric / Maven Central / CCBlueX snapshots / lenni0451 releases |

构建命令：

```shell
./gradlew clean build
```

产物（`build/libs/`）：

- `dioxideliteng-b2.jar`（约 71 MB，含内嵌依赖与各平台 JavaFX / LWJGL native）
- `dioxideliteng-b2-sources.jar`（约 12.9 MB）

预构建产物已随仓库提供于 `dist/`。

测试：**246 项**（JUnit 5 / Jupiter），skipped 0，failures 0，errors 0。

## 四、运行验证

- 环境：Xvfb（DISPLAY=:99）+ llvmpipe 软渲染（Mesa 23.2.1，OpenGL 4.5 Core）。
- 结果：客户端成功加载 5 个 mod（dioxideliteng b2 / fabricloader 0.19.5 / minecraft 26.3 / mixinextras 0.5.5），进入主菜单并正常渲染启动动画与干员主题界面（截图见 `docs/screenshots/`）。
- 进入超平坦创造世界，开启 HUD 与 ClickGUI，截图验证 HUD 面板（玩家信息 / FPS / 击杀 / 胜利）与 ClickGUI 分类布局正常渲染。
- 已知：软渲染下帧率受限（约 4~7 FPS），属 llvmpipe 环境正常现象；无声卡设备存在 ALSA 音频告警，不影响功能。

## 五、使用方式

1. 按 `./gradlew build` 构建得到 `build/libs/dioxideliteng-b2.jar`。
2. 为 Minecraft 26.3 安装 Fabric Loader 0.19.5+。
3. 将 jar 放入 `mods` 目录，用 Java 25 启动 Fabric 档位。
4. **Right Shift** 打开 ClickGUI；聊天输入 `.help` 查看指令；`.config save/load <name>` 管理预设。
5. 自动状态存于 `dioxideliteng/state.json`，预设存于 `dioxideliteng/configs/`（游戏目录下）。

## 六、许可

GNU General Public License v3.0（详见 LICENSE）。第三方依赖与素材保留各自许可证。
