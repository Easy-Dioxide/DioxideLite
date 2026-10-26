# Devlog B13 — KRS 全量内核移植

> 任务：以 b3 为基座，把 KRS 最新版（MC 26.3）的全部功能模块移植进 DioxideLiteNG，保留 NG 作为唯一界面。
> 与 B7 的区别：B7 是「按模块逐个重写成 NG 风格」，B13 是「把 KRS 运行时内核整体搬进来 + 加桥接层」。

## 一、结论（TL;DR）

1. **KRS 整个运行时内核搬入**：133 个模块类、86 个 Mixin、1 个 accesswidener，依赖全部内嵌并改包名。
2. **不做逐模块重写**，改为加一层 `KrsModuleAdapter` 桥接：KRS 的 `Module` 包装成 NG 的 `Feature`，设置项每 tick 双向同步。
3. **22 个重名模块已加 `2`**，判定规则按类名 + 显示名归一化比对。
4. **B7–B12 那套手写实现被整体替换**。两套实现不兼容，B10–B12 的修复未迁入 B13（详见第五节）。

## 二、为什么要换一套做法

B7 的做法是：读 KRS 源码，把每个模块用 NG 的事件体系重写一遍。

这条路走到 B12，暴露出的问题是**每次审计都能找出新的语义偏差**：

| 版本 | 修掉的问题 |
|---|---|
| B8 | MovementFix routine 未接线、WTap / MaceExploit 攻击钩子事件身份比较错误 |
| B9 | Scaffold2 数学放置评分 16 处偏差、RotationManager 摆动条件不精确 |
| B10 | KillAura2 "no order" 多发摆动包、hypixel ncp 门控查错模块、Blink2 缺 Freecam 门控 |
| B11 | moveRelative / jumpFromGround / preMotion 三处模拟链断链 → GrimAC simulation VL |
| B12 | Scaffold2 报 VL，最后定位是 MovementFix 未手动开启（非代码问题） |

根因是：**重写必然产生语义漂移**。KRS 的模块之间有大量隐式耦合（RotationManager 的三态、PacketUtil 的 silent 通道、movement 的一整套 spoofed 坐标系），逐个重写时任何一处理解偏差都会变成绕过失效。

B13 换成另一条路：**内核原样搬过来，不从中间翻译**。

## 三、移植范围

| 类别 | 数量 |
|---|---|
| KRS 模块类 | 133 |
| 界面注册模块 | 125（124 正式 + `FukumaiPlayerTracker`） |
| Mixin | 86 |
| Access Widener | 1（`krs.accesswidener`，126 行） |
| Java 源文件（含 NG 原有） | 648 |

### 3.1 包名重映射

```
com.instrumentalist.krs       -> com.dioxideliteng.krs
com.instrumentalist.mixin.*   -> com.dioxideliteng.mixin.krs
org.nvgu                      -> com.dioxideliteng.krs.nvgu
xyz.breadloaf.imguimc         -> com.dioxideliteng.krs.imguimc
```

`com.instrumentalist.Client` 同时重命名为 `KrsClient`，避免与 NG 侧混淆。

### 3.2 `org.json` 的特殊处理

KRS 大量使用 `org.json.JSONObject`。直接引依赖会踩到一个坑：

```
Mod dioxideliteng uses the version b3 which isn't compatible with
Loader's extended semantic version format
(Could not parse version number component '20240303'!)
```

`org.json` 的版本号是纯日期（`20240303`、`20250107`……），**全部版本都不满足 Fabric 的 semver 校验**，嵌套 jar 会被拒绝加载。

解法：用 Gson 实现一份 `org.json` 兼容层，**保留原包名**。KRS 业务代码零改动，也不用内嵌那个 jar。

实现覆盖 `get` / `opt` / `has` / `put` / `remove` / `length` / `keySet` / `toList` 等约 20 个方法。

### 3.3 Mixin 注册

新建 `dioxideliteng.krs.mixins.json`，`package` 指向 `com.dioxideliteng.mixin.krs`，注册 86 个 client mixin。

剔除了 3 个：

| 剔除项 | 原因 |
|---|---|
| `MinecraftClientMixin` | 与 NG 同目标同方法，重复注入 |
| `RenderTickCounterDynamicMixin` | 同上 |
| `TitleScreenMixin` | 会抢 NG 的主菜单渲染 |

`Initializer` 保留但不实现 `ClientModInitializer`，改由 NG 统一初始化。

## 四、桥接设计

两套框架的差异：

| | NG | KRS |
|---|---|---|
| 基类 | 抽象类 `Feature` | `Module implements EventListener` |
| 事件 | `EventBus` 按 priority 分发 | `EventManager` |
| 设置 | 手动 `new BooleanSetting(...)` | 反射读 `@Module.Setting` 注解字段 |
| 值体系 | 各 Setting 类自持 | `SettingValue<T>` 统一抽象 |

### 4.1 KrsModuleAdapter

```java
public class KrsModuleAdapter extends Feature {
    private final Module module;
    private final List<Binding> bindings = new ArrayList<>();

    @Override public void onEnable()  { module.setState(true);  }
    @Override public void onDisable() { module.setState(false); }

    @Override public void clientTick() {
        for (Binding b : bindings) b.pushToKrsIfChanged();  // NG -> KRS
        if (!isEnabled()) return;
        for (Binding b : bindings) b.pullFromKrs();          // KRS -> NG
    }
}
```

**双向同步的意义**：NG 界面改参数要写回 KRS 并触发它自己的 onChange 逻辑；KRS 内部（比如模块自己切模式）改值时界面也要跟着变。用「快照比对」判断哪一侧变了，开销只是几个字段比较。

类型映射：

| KRS | NG | 说明 |
|---|---|---|
| `BooleanValue` | `BooleanSetting` | |
| `FloatValue` | `NumberSetting` | step 按区间跨度取 0.1 或 0.01 |
| `IntValue` | `NumberSetting` | step = 1.0 |
| `ListValue` | `ModeSetting` | 当前值不在选项内时回落到第一项 |
| `KeyBindValue` / `TextValue` / `ColorValue` | 跳过 | NG 无对应编辑器，模块功能不受影响 |

### 4.2 分类映射

`ModuleCategory` 的枚举常量是 `Combat` / `Movement` / `Player` / `Level` / `Exploit` / `Render` / `Dev`（**首字母大写**）。

第一版写成了字符串比较：

```java
return switch (name) {
    case "COMBAT" -> Category.COMBAT;   // 永远匹配不上
    ...
    default -> Category.MISC;
};
```

结果**所有 KRS 模块都会静默落进 MISC**，而且不会报错。改成按枚举对象匹配：

```java
return switch (krsCategory) {
    case Combat   -> Category.COMBAT;
    case Movement -> Category.MOVEMENT;
    case Player   -> Category.PLAYER;
    case Render   -> Category.VISUAL;
    default       -> Category.MISC;   // Level / Exploit / Dev
};
```

`default` 是显式写的：穷举 switch 在枚举扩充后会抛 `MatchException`，用 default 兜住。

### 4.3 界面处理

**不搬 KRS 的 GUI**，只搬功能：

- `NanoVGClickGuiScreen` 改为空占位 `Screen`
- `ModuleRenderable`（原 1003 行 ImGui 面板）退化为转发壳
- `ModuleManager` 里「右 Shift 打开 KRS 界面」的整段逻辑删除——NG 的 ClickGui 也是右 Shift，两个会抢键；且 KRS 的 `onDisable()` 里有 `this.toggle()` 递归，留着有死循环风险

KRS 的聊天命令系统保留（`KrsCommands`，396 行）。

### 4.4 初始化时机

`KrsClient.init()` 在 `FeatureManager.registerModules()` 末尾触发，保证：

1. `eventManager` 先就绪（`Module.setState` 要向它注册监听）
2. NG 模块先注册完（桥接时要知道 NG 已占用哪些名字）

`RotationManager` 构造期会读 `mc.player`，加了空值保护：无游戏实例时跳过创建，避免整个初始化失败。

## 五、重名处理

判定规则：**类名 + 显示名都比对**，且做归一化：

```java
private static String normalize(String name) {
    return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
}
```

归一化是必须的：KRS 显示名带空格（`"Kill Aura"`），NG 不带（`"KillAura"`），直接字符串比较**永远不相等**。第一版没做归一化，只算出 9 个重名，实际是 22 个。

| KRS 原名 | KRS 类名 | NG 已有 | 最终名称 |
|---|---|---|---|
| AntiBot | `AntiBot` | AntiBot | **AntiBot2** |
| AutoTool | `AutoTool` | AutoTool | **AutoTool2** |
| AutoWalk | `AutoWalk` | AutoWalk | **AutoWalk2** |
| Backtrack | `Backtrack` | Backtrack | **Backtrack2** |
| Blink | `Blink` | Blink | **Blink2** |
| ChestStealer | `ChestStealer` | ChestStealer | **ChestStealer2** |
| ClickGui | `ClickGui` | ClickGui | **ClickGui2** |
| Criticals | `Criticals` | Criticals | **Criticals2** |
| Disabler | `DisablerModule` | Disabler | **Disabler2** |
| FullBright | `FullBright` | FullBright | **FullBright2** |
| InventoryMove | `InventoryMove` | InventoryMove | **InventoryMove2** |
| KillAura | `KillAura` | KillAura | **KillAura2** |
| LongJump | `LongJump` | LongJump | **LongJump2** |
| NameTags | `NameTags` | NameTags | **NameTags2** |
| NoFall | `NoFall` | NoFall | **NoFall2** |
| NoSlow | `NoSlow` | NoSlow | **NoSlow2** |
| Scaffold | `Scaffold` | Scaffold | **Scaffold2** |
| Speed | `SpeedModule` | Speed | **Speed2** |
| Sprint | `Sprint` | Sprint | **Sprint2** |
| Stasis | `Stasis` | Stasis | **Stasis2** |
| Timer | `Timer` | Timer | **Timer2** |
| Velocity | `Velocity` | Velocity | **Velocity2** |

> `NoHurtCam` / `InvManager` 与 NG 的 `NoHurtCamera` / `InventoryManager` 名称不同，**不算重名**，保持原名。

显示名同时抹掉空格，与 NG 的紧凑风格统一（`KillAura2` 而不是 `Kill Aura2`）。

## 六、B7–B12 的修复去哪了

这是本次替换最需要注意的地方。

B13 用的是 KRS 原版实现，B7–B12 是在「重写版」上做的修复。两套代码结构完全不同，**没法直接套用**。

| 版本 | 修复内容 | B13 状态 |
|---|---|---|
| B8 | MovementFix routine 接线 | KRS 原版本就完整，不存在此问题 |
| B9 | Scaffold2 放置评分 16 处还原 | B13 直接用 KRS `Scaffold` 原实现 |
| B10 | KillAura2 摆动包 / NoSlow 门控 / Blink Freecam 门控 | B13 用 KRS `KillAura` / `Blink` 原实现 |
| B11 | moveRelative / jumpFromGround / preMotion 模拟链 | B13 用 KRS 原生 Mixin，链路本就闭环 |
| B12 | Scaffold2 报 VL → MovementFix 未开 | **同样是手动开关**，使用方式一致 |

结论：B8–B11 修的都是「重写时引入的偏差」，B13 用原版实现从根上绕过了这些问题。但 **B12 的使用注意事项依然成立**：

> 开 `Scaffold2` 必须同时开 `Movement Fix`（Movement 分类）。KRS 源码里同样没有自动启用逻辑。

B12 的三个 devlog（`devlog-b10.md` ~ `devlog-b12.md`）已原样保留在仓库，作为「重写路线」的历史记录。

## 七、验证

| 项目 | 结果 |
|---|---|
| `./gradlew clean build` | ✅ BUILD SUCCESSFUL |
| Mixin 目标类存在性 | ✅ 79 个精确命中 MC 26.3；7 个指向 Sodium / Iris / Indigo（未装则运行时跳过） |
| 注入点方法名（158 个） | ✅ 全部在目标类方法表中命中，0 失败 |
| 真实启动 | ✅ `[KRS-Port] bridge ready, 124 modules registered` |
| Mixin 应用错误 | ✅ 0 条 `InvalidInjectionException` |
| 分类 switch 字节码 | ✅ 7 个枚举常量全部映射到正确分支 |

### 7.1 校验方法

方法名校验不是靠 grep 字符串，而是解析目标类的字节码方法表：

```python
# 读 constant pool -> 定位 methods_count -> 逐个读 name_index / descriptor_index
# 再与 mixin 源文件里 @Inject(method=...) 的名字比对
```

一开始用朴素字符串搜索，报了 9 个「缺失」，逐个核查发现全是误报：

- `Camera.getMaxZoom` 等——`Camera` 有同名类冲突，我的解析器指向了错误条目
- `Player.getLookAngle`——描述符在常量池里被非常规项隔开，`bytes.find` 匹配不到

改成规范 class 文件解析后，158 个注入点全部命中。

### 7.2 已知环境限制

沙箱无 GPU，游戏在 `Minecraft.<init>` 创建 GL 窗口时崩溃：

```
BackendCreationException: Failed to create window for OpenGL context:
Couldn't find matching GLX visual
    at net.minecraft.client.Minecraft.<init>(Minecraft.java:533)
```

试过 Mesa 软渲染（`LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe` + Xvfb），GLX visual 仍不匹配。这是沙箱环境限制，与移植代码无关。

日志里 6 条 Sodium / Iris 的 `ClassNotFoundException` 同理——这两个 mod 没装，对应 Mixin 自动跳过。

## 八、构建与产物

```bash
./gradlew clean build
```

- `build/libs/dioxideliteng-b13.jar`（83.8 MB）
- `build/libs/dioxideliteng-b13-sources.jar`（19.9 MB）

新增依赖：`fabric-api`、`mixinextras >= 0.4.1`、`imgui-java 1.86.11`（三平台 natives）、`httpclient`、`jlayer`、`imageio-webp`、`lwjgl-stb`。

## 九、后续

1. **B13 的模块分类需要人工过一遍**：`Exploit` / `Level` / `Dev` 三类目前全部归入 MISC（NG 只有 5 个分类），如需细分要扩 NG 的 `Category` 枚举。
2. **KRS 的 `Rotations` 渲染模块与 NG 是否冲突**需要实机验证（B10 曾在 NG 侧重写过一版）。
3. 三种设置类型（`KeyBindValue` / `TextValue` / `ColorValue`）目前界面不可编辑，如需支持要扩 NG 的 Setting 体系。
