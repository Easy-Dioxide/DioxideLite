# DioxideLite — Devlog / 版本开发日志（B7）

> 版本：**B7** ｜ Minecraft **26.3** ｜ Fabric **0.19.5** ｜ Java **25**（Temurin 25.0.4.1 LTS）
> 状态：构建通过 · `dioxideliteng-b7.jar`（69 MB）· Krs 全量非可视化模块移植

---

## 一、版本定位

B7 以 **B3 为基座**重建（旧 b4 基座移植版保留在 `b7-b4base-backup` 分支），
将 Krs 客户端的**全部非可视化、非开发类功能模块**完整移植到 DioxideLiteNG，
并补齐 b4 版缺失的事件分发钩子。移植范围覆盖 combat / exploit / level / movement /
player / nulling 六大类目，保持与 Krs 逐一对应的绕过逻辑，仅适配 API 层。

### 为什么回退 b4 基座

b4 基座的 Krs 移植存在**依赖断裂**：部分事件（如 `BLOCK_EDGE`）只完成了注册，
没有任何 Mixin 分发点，导致订阅方成为死代码。B7 回退至干净的 B3 基座，
先建事件层、再移模块，从结构上杜绝该类问题。

## 二、事件层重建（9 个新事件 + 全部分发点）

新增事件原型（`com.dioxideliteng.event.Events`）与实现类（`event/impl/`）：

| 事件 | 分发点（Mixin） | 说明 |
| --- | --- | --- |
| `UPDATE` | `MixinPlayer`（tick，含 fallTicks 追踪） | 对应 Krs `onUpdate` |
| `ATTACK` | `MixinMultiPlayerGameMode#attack` HEAD（可取消） | 对应 Krs `AttackEvent` |
| `BLOCK` | `MixinBlockCollisions`（`@ModifyExpressionValue` ×3） | 对应 Krs `BlockEvent` |
| `BLOCK_EDGE` | `MixinPlayer`（isStayingOnGroundSurface） | 对应 Krs `BlockEdgeEvent` |
| `CANCEL_BLOCK_BREAKING` | `MixinMultiPlayerGameMode#stopDestroyBlock` HEAD（可取消） | 挖掘中断取消 |
| `HANDLE_INPUT` | `MixinMinecraft` | 输入处理期 |
| `KEY` | `MixinKeyboardHandler`（新） | 键盘事件 |
| `WORLD` | `MixinMinecraft` | 世界加载/卸载 |
| `MODIFY_PACKET` | `MixinConnection`（收发双向） | 数据包改写 |

> 每个事件均逐一核对「注册 ⇄ 分发」双向存在，无死注册（b4 故障模式）。

## 三、模块移植清单（102 个功能模块）

| 类目 | 数量 | 代表模块 |
| --- | --- | --- |
| Combat | 15 | AutoPot、AutoTotem、CrossbowExploit、MaceExploit、Velocity2、Criticals2、WTap、Teams、TargetStrafe |
| Movement | 27 | Fly2（13 模式）、Speed2（12 模式）、Blink2、Phase、NoSlow2、Stasis2、Step、Jesus2、Spider2 |
| Player | 21 | Freecam、InvManager2、Scaffold2、ChestStealer2、NoFall2、AutoTool2、SpinBot2、HackerDetector2 |
| Misc / Exploit / Level | 39 | Disabler2（7 服务器模式）、Nuker、Xray、NoteBot、TPStealer、ServerCrasher、AutoBypass2、CivBreak |

- 与原生模块重名的移植版统一加 `2` 后缀（如 `KillAura2`、`Blink2`），两版并存注册。
- 全部模块经 `FeatureManager` 注册（当前注册总数 **124**），ClickGUI 立即可见。

## 四、Utils 与基础设施

- `util/krs/`：31 个工具类（RotationManager、RotationScraper、MovementUtil、
  PacketUtil、PlayerUtil、BehaviorUtils、MSTimer、TimerUtil、Slots、SlotGroup、
  ChunkUtil、BlinkUtil、ToolUtil、RandomUtil、Tuple、EntityExtension、InputUtil）。
- `util/pathfinder/`：4 个寻路类（MainPathFinder、LinearPathFinder、PathFinder、PathHub）。
- `setting/TextSetting`：文本设置项（Krs parity）。
- `MixinLocalPlayer` / `MixinLivingEntityRenderer` / `MixinClientCommonPacketListener`：
  旋转与渲染同步钩子。

## 五、MC 26.3 API 适配（编译期修复 64 处）

首轮编译暴露 64 个适配错误，全部修复：

- `InputConstants.isKeyDown` 签名收窄为单 `int` 参数（去掉 `Window` 实参，9 处）。
- `LivingEntity.swing` 需三参数 `(hand, SwingAnimation, boolean)`（2 处）。
- `ServerboundSwingPacket` 在 26.3 被移除，摆动通知改发 `ServerboundPunchPacket.INSTANCE`（2 处）。
- `RenderPipeline` 迁移至 `com.mojang.renderpearl.api.pipeline`（CaveFinder）。
- `ClientboundLevelChunkWithLightPacket` 改为 record 访问器 `x()/z()`；
  `ClientboundSectionBlocksUpdatePacket.sectionPos` 私有化，改经 `runUpdates` 取位（ChunkUtil）。
- NG 设置体系 `setVisible` 返回基类 `Setting`，链式赋值拆分为声明 + 构造器内调用（CivBreak）；
  `BooleanSetting/NumberSetting` 取值统一为 `getValue()`。
- 清理重复方法定义（`MovementUtil.strafe`）、补齐缺失 import（Category / Speed2）与
  无参构造（MinibloxSpeed）。

## 六、构建环境与产物

```shell
JAVA_HOME=<JDK 25> ./gradlew build
```

- `build/libs/dioxideliteng-b7.jar`（69 MB，含内嵌依赖与 JavaFX/LWJGL native）
- `build/libs/dioxideliteng-b7-sources.jar`（13 MB）

## 七、后续事项

- 运行时启动验证（进服点击 GUI 逐类目冒烟）尚未执行，需在真实客户端环境完成。
- CaveFinder 的 `shouldCullFront` 渲染钩子待接入地形渲染 Mixin（当前仅编译通过）。
