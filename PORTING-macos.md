# DioxideLite 2.2.1 → macOS 适配记录

**适配对象：** `DioxideLite-2.2.1.jar`（Fabric mod，Minecraft 26.1.2，Skija 自绘 UI）
**目标平台：** macOS 26.x / Apple Silicon（arm64）与 Intel（x86_64）
**构建环境：** JDK 25.0.4.1 (Temurin) + Gradle 9.2.1 + Fabric Loom 1.15.5
**产物：** `dist/DioxideLite-2.2.1-macos.jar`

---

## 1. 结论

原 jar 在 macOS（以及 Windows）上都跑不起来，问题不止"平台原生库不对"，还有一处**发布包漏打包**：

| # | 根因 | 现象 | 本次修复 |
|---|------|------|----------|
| 1 | 发布包里**没有 Skija 的 Java API**（`skija-shared`、`humbleui types`），只打进了平台原生库 | 一进主菜单 UI 就 `NoClassDefFoundError: io.github.humbleui.skija.Canvas`，崩溃 | `include("io.github.humbleui:skija-shared")` + `include("io.github.humbleui:types")` |
| 2 | 只打包了 **Windows / Linux 的 Skija、WebRTC 原生库** | macOS 上 `UnsatisfiedLinkError`（找不到 `libskija.dylib`） | 按平台打包 `skija-macos-arm64` / `skija-macos-x64`、`webrtc-java-*-macos-aarch64` |
| 3 | `webrtc-java` 只打进了平台原生库，**Java 类同样缺失** | 基岩版 NetherNet / 语音通道 `NoClassDefFoundError` | `include("dev.kastle.webrtc:webrtc-java:1.0.3")` |
| 4 | 字体选择器写死 `powershell.exe` + WinForms，回退分支用 `java.awt.FileDialog` | macOS 上 AWT 需要 AppKit 主线程（已被 GLFW 占用），点"Import Font"会卡死/异常 | 改用 LWJGL `tinyfd` 原生对话框（macOS 走 AppleScript/Cocoa） |
| 5 | 打开目录用 `explorer.exe` / `java.awt.Desktop` | macOS 无效 | 新增 `PlatformSupport`，macOS 走 `/usr/bin/open`，Windows 走 explorer，Linux 走 `xdg-open` |
| 6 | 设备指纹用 JNA `Advapi32Util` 读 Windows 注册表 | macOS 上直接抛错（且发布包里本来就没有 JNA） | 新增 `PlatformSupport.cpuName()`：macOS `sysctl`、Linux `/proc/cpuinfo`、Windows `reg.exe`；彻底去掉 JNA 依赖 |
| 7 | 字体回退链只有 `Microsoft YaHei / Segoe UI` | macOS 上全部 miss，缺字回退失效 | 按系统选择回退字体（macOS：PingFang SC → Hiragino Sans GB → Heiti SC → STHeiti → Songti SC → Apple SD Gothic Neo → Helvetica Neue → Arial），最后再回落到系统默认字体 |
| 8 | `DirectContext.makeGL()` 失败时每帧重试 | 日志刷屏、掉帧 | 增加一次性降级：自绘 UI 停用、原版 HUD 继续工作 |

### 1.1 产物对照（两个包，用途不同）

| 产物 | 原生库 | 能用在哪 | 大小 |
|------|--------|----------|------|
| `dist/DioxideLite-2.2.1-macos.jar` | Skija: macos-arm64 + macos-x64；WebRTC: macos-aarch64 | **只有 macOS**（Apple Silicon + Intel） | 61.6 MB |
| `dist/DioxideLite-2.2.1-universal.jar` | Skija: windows-x64 + linux-x64 + macos-arm64 + macos-x64；WebRTC: windows-x86_64 + linux-x86_64 + macos-aarch64 + macos-x86_64 | **Windows x64 / Linux x64 / macOS（arm64 + Intel）三平台通用** | 93.4 MB |

> 只玩一个平台时，用源码包按平台构建更省空间（约 60 MB）；需要"一个 jar 到处跑"就用 universal 包。
> 两边的 Java 代码完全一样，区别只是打进包里的原生库集合。

---

## 2. 改动清单

```
build.gradle.kts                                     原生库按平台打包 + 补回 Java API + nested 库改为可选
gradle.properties                                    平台参数说明（默认按构建机探测）
src/main/java/com/dioxidelite/util/client/PlatformSupport.java        （新增）平台差异集中层
src/main/java/com/dioxidelite/module/modules/FontModule.java         字体选择/打开目录
src/main/java/com/dioxidelite/render/SkijaUi.java                    字体回退链
src/main/java/com/dioxidelite/render/SkijaRenderer.java              GL 上下文不可用时的降级
src/main/java/com/dioxidelite/util/alt/MicrosoftAuthService.java     打开登录页
src/main/java/com/dioxidelite/script/LuaScriptManager.java           打开脚本目录
src/main/java/com/dioxidelite/command/commands/BuiltInCommands.java  /config browse 打开配置目录
src/main/java/tritium/ncm/DeviceIdGenerator.java                     设备指纹去掉 JNA
scripts/build-macos.sh                               macOS 构建脚本（新增）
```

完整 unified diff：`macos-port.patch`（10 个文件，827 行）。

### 打包规则（build.gradle.kts）

```kotlin
// Skija：Java API 与平台原生库是两个 artifact，两个都要 include
implementation("io.github.humbleui:skija-shared:0.143.17");  include("...")
implementation("io.github.humbleui:types:0.2.0");            include("...")
skijaPlatforms.forEach { include("io.github.humbleui:$it:0.143.17") }

// webrtc-java 同理
include("dev.kastle.webrtc:webrtc-java:1.0.3")
include("dev.kastle.webrtc:webrtc-java:1.0.3:$webRtcPlatform")
```

* `skija_platforms` / `webrtc_platform` 未指定时**按构建机自动探测**，交叉打包时用 `-P` 覆盖。
* `libs/nested`（上游 dev 用的 67 个重复库）默认**关闭**：它和上面的 Maven `include` 完全重复，
  还会把另一份 netty 塞进运行时 classpath。需要复现上游 dev 包时用 `-Puse_nested_libs=true`。

---

## 3. 构建

```bash
cd /Users/zy123/Documents/科技改变生活/dioxide-mac
export JAVA_HOME=$HOME/jdks/jdk-25.0.4.1+1/Contents/Home

# 通用 macOS 包（Apple Silicon + Intel）
./gradlew clean build -Pskija_platforms=skija-macos-arm64,skija-macos-x64

# 只要 Apple Silicon
./gradlew clean build -Pskija_platforms=skija-macos-arm64

# 或者直接用脚本
./scripts/build-macos.sh --arm64-only
```

### 3.1 三平台通用包（Windows x64 + Linux x64 + macOS）

原生库平台是构建参数，一次可以把多个平台一起打进同一个 jar：

```bash
./gradlew clean build \
  -Pskija_platforms=skija-windows-x64,skija-linux-x64,skija-macos-arm64,skija-macos-x64 \
  -Pwebrtc_platforms=windows-x86_64,linux-x86_64,macos-aarch64,macos-x86_64
```

* `skija_platforms` / `webrtc_platforms` 都接受逗号分隔的多平台列表；不写则按构建机自动探测。
* 需要 Windows ARM64 / Linux ARM64 时再各加一项：`skija-windows-arm64`、`skija-linux-arm64`、
  `webrtc_platforms` 加 `windows-aarch64`、`linux-aarch64`（会让包再大 ~25 MB）。
* 每个平台的原生库都是独立的嵌套 jar（Fabric nested mod），运行时由 Skija / webrtc-java
  自己按 `os.name` + `os.arch` 选择，互不冲突；嵌套 mod id 已确认无重复。

同一条命令在 Windows 上会打出 Windows 包（自动探测），无需改配置。

---

## 4. 验证记录

### 4.1 构建

```
BUILD SUCCESSFUL in 21s        # ./gradlew build -Pskija_platforms=skija-macos-arm64,skija-macos-x64
```
产物 61,562,064 字节，`META-INF/jars/` 共 29 个嵌套库，其中：

```
META-INF/jars/skija-shared-0.143.17.jar          (Java API)
META-INF/jars/types-0.2.0.jar
META-INF/jars/skija-macos-arm64-0.143.17.jar     (libskija.dylib, 26 MB)
META-INF/jars/skija-macos-x64-0.143.17.jar
META-INF/jars/webrtc-java-1.0.3.jar              (Java 类)
META-INF/jars/webrtc-java-1.0.3-macos-aarch64.jar (libwebrtc-java-macos-aarch64.dylib)
```

### 4.2 生产环境等价的类链接检查

用真实 Minecraft 26.1.2 merged jar + fabric-loader/fabric-api + MC 自带库作为 classpath，
把 Skija / WebRTC / JNA 从 classpath 里**剔除**（只留 jar 内嵌套库，等价于 Fabric 线上加载方式），
然后对 jar 内每个类强制解析字段/方法/参数类型：

| jar | 完全解析通过 | 失败 |
|-----|-------------|------|
| 原始 `DioxideLite-2.2.1.jar` | 1511 / 1561 | **50 个**：`io.github.humbleui.skija.Canvas/Image/Font`、`io.github.humbleui.types.Rect` … |
| 本次 `DioxideLite-2.2.1-macos.jar` | 1560 / 1563 | 3 个，且都是**可选**集成（Sodium ×2、ModMenu ×1，源码里就是 `compileOnly`） |

结论：原包缺失的 Skija 类在新包里全部由嵌套 jar 提供。

### 4.3 原生库与文字渲染冒烟（macOS arm64 实机）

直接加载**新 jar 内部**的嵌套 Skija jar 跑渲染测试：

```
$ java -cp nested/skija-macos-arm64-0.143.17.jar:nested/skija-shared-0.143.17.jar:nested/types-0.2.0.jar ... SkijaSmoke
fontmgr = FontMgr(_ptr=...)
  family[167] = PingFang SC
  match(PingFang SC)  = Typeface(_ptr=...)
  match(Hiragino Sans GB) = Typeface(_ptr=...)
  match(null)         = Typeface(_ptr=...)
png bytes = 3621
SKIJA SMOKE OK
```

`libskija.dylib` 正常解压加载、字体枚举/匹配正常、中英文混排绘制成功。

### 4.4 真实客户端启动（Fabric Loom dev 客户端，macOS 实机）

```
$ ./gradlew runClient -Pskija_platforms=skija-macos-arm64,skija-macos-x64
[15:58:23] [main/INFO]  (FabricLoader) Loading 1 mods ...
[15:58:28] [Render thread/INFO] (DioxideLite) Initializing DioxideLite 2.2.1...
[15:58:29] [Render thread/INFO] (DioxideLite) DioxideLite visual runtime loaded.
[15:58:31] [Render thread/INFO] (DioxideLite) Loaded 97 modules.
[15:58:31] [Render thread/INFO] (DioxideLite) Registered 21 client commands with prefix .
```

* 客户端进程内 `vmmap` 确认原生库已加载：
  `__TEXT ... /private/var/folders/.../T/skija_0.143.17_arm64/libskija.dylib`（21.1 MB 代码段）
  —— Skija 的 macOS arm64 原生库在游戏进程里正常 `dlopen`。
* 渲染注入生效：`dioxide-lite.mixins.json:MinecraftMixin -> @Inject::DioxideLite$renderSkija` 注入成功。
* 窗口标题 `DioxideLite 2.2.1`，窗口截图（仅截取游戏窗口）非空：160×96 采样有 11,012 种颜色、
  亮度范围 9–765，自绘 UI 实际出图。
* 连续运行 8 分钟无 `Skija`/渲染异常日志（若 Skija 初始化失败会打印
  `Skija could not create an OpenGL context ...` 或每帧 `Recovering Skija renderer ...`）。
* 顺带确认：Loom 在 macOS 自动追加了 `-XstartOnFirstThread`（见进程命令行），
  自建启动脚本时必须自己加。

### 4.5 macOS 音频（网易云/QQ 音乐播放链路）

JavaSound 在 macOS 上是可用的，不需要额外原生库（`AUDIO PROBE OK`）：

```
os = Mac OS X / aarch64
mixers=7 output=4 input=4
opened output line: PCM_SIGNED 44100.0 Hz, 16 bit, stereo, 4 bytes/frame, little-endian
```

### 4.6 三平台通用包验证（本机能做的都做了）

```
$ ./gradlew clean build -Pskija_platforms=skija-windows-x64,skija-linux-x64,skija-macos-arm64,skija-macos-x64 \
      -Pwebrtc_platforms=windows-x86_64,linux-x86_64,macos-aarch64,macos-x86_64
BUILD SUCCESSFUL in 32s        → dist/DioxideLite-2.2.1-universal.jar (93,378,056 字节)
```

* 包内 34 个嵌套 jar，四个平台原生库路径齐全：
  `skija/windows/x64/skija.dll`、`skija/linux/x64/libskija.so`、
  `skija/macos/arm64/libskija.dylib`、`skija/macos/x64/libskija.dylib`；
  WebRTC 四个平台的 `libwebrtc-java-*` 同样齐备。
* 嵌套 mod id 无重复（多平台共存不会打架）。
* 生产等价类链接检查：**1560 / 1563**（仍是那 3 个可选集成）。
* 本机（macOS arm64）用通用包里的嵌套 jar 跑渲染冒烟 → `SKIJA SMOKE OK`
  ——即 Skija 在四个平台目录并存时能正确挑到本机那份。
* 抽出 `libwebrtc-java-macos-aarch64.dylib`，`System.load()` 成功（JNI_OnLoad 正常回调 Java 类）。

### 4.7 未能覆盖的部分

* Intel Mac（x86_64）只做了打包与类链接校验，没有 x86_64 机器实机跑图。
* 基岩版（NetherNet / WebRTC）联机只保证类与原生库齐备，未做端到端联机。
* 真机启动验证用的是 Loom dev 客户端（同一份源码、同一套 JDK 25 / MC 26.1.2），
  没有用启动器加载打包后的 jar 再跑一遍。
* **Windows / Linux 没有实机跑过**：只做了包结构与类链接校验。这两个平台的代码路径
  （`PlatformSupport` 的 explorer.exe / reg.exe、xdg-open、Linux 字体回退、tinyfd）
  都按平台分支写好并由链接检查覆盖，但真机验证需要在对应系统上跑一次。

## 5. macOS 运行须知

1. **必须用 JDK 25 启动**（类文件版本 69）。`java -version` 低于 25 会直接报
   `UnsupportedClassVersionError`。
2. 启动器需要加 **`-XstartOnFirstThread`**（GLFW 在 macOS 要求 JVM 从主线程启动）。
   官方启动器、Prism/PolyMC、HMCL 会自动加；自建启动脚本必须自己加，否则报
   `GLFW: Cocoa: Failed to create window` 之类错误。
3. 把 jar 放进 `.minecraft/mods/`（Fabric Loader 0.19.x + Fabric API 0.150.0+26.1.2 + MC 26.1.2）。
4. 首次启动时 Skija 会把 `libskija.dylib` 解压到 `java.io.tmpdir`（`/var/folders/.../T/skija_*`）
   再 `dlopen`，属正常行为；如需固定目录可加 `-Djava.io.tmpdir=<可写目录>`。
5. 若用带 **Hardened Runtime + Library Validation** 的第三方启动器（部分商业客户端），
   `dlopen` 未签名 dylib 会被拒；官方启动器/开源启动器无此限制。
6. 自绘 UI 依赖 OpenGL。macOS 上如果 Skija 拿不到可用的 GL 上下文，日志会出现一次
   `Skija could not create an OpenGL context on macos/arm64 (...)`，此时客户端自动停用自绘 UI，
   原版 HUD 与游戏功能不受影响（不会崩溃）。

---

## 6. 回滚记录

原始文件**未被修改**，原件哈希：

| 文件 | SHA-256 |
|------|---------|
| `~/Desktop/Dioxide client/DioxideLite-2.2.1.jar` | `972ee5ad8ebc55551cd7f7830f0e030470c0d0e622edd8d8c0e6c878c2315fab` |
| `~/Desktop/Dioxide client/DioxideLite-2.2.1-src.zip` | `c91cbce7daceea9e3e461a9a4a70da16e2324eaa761134413707064907ff1f0a` |
| 新产物 `dist/DioxideLite-2.2.1-macos.jar` | 见 `dist/SHA256SUMS.txt` |

回滚方式（任选其一）：

1. **删目录即可**：源码工程在 `~/Documents/科技改变生活/dioxide-mac`，删掉它不会有任何副作用。
2. **还原源码**：重新解压 `DioxideLite-2.2.1-src.zip`，或对工程反向应用补丁
   `patch -p1 -R < macos-port.patch`。
3. **还原 jar**：把原始 `DioxideLite-2.2.1.jar`（哈希见上表）拷回 `mods/` 即可，
   本次没有改动任何游戏目录、配置或注册表。
