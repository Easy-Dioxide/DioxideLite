package com.dioxidelite.module.modules.movement.invmove;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（来源开源版）com/dioxidelite/module/modules/movement/invmove/InvMoveContext.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


public interface InvMoveContext {

    /** 玩家与世界是否就绪。 */
    boolean isAvailable();

    /** 当前打开的界面归类。 */
    InvMoveScreen screen();

    /** 该移动键在物理上是否真的按着（绕过 KeyMapping 的逻辑状态）。 */
    boolean isPhysicalKeyPressed(InvMoveKey key);

    /** 改写某个移动键的 KeyMapping 逻辑状态，让原版输入系统读到它。 */
    void setLogicalKeyPressed(InvMoveKey key, boolean pressed);

    /** 静默给服务端补发一个"关闭玩家背包"的包。 */
    void sendCloseInventoryPacket();

    // --- 设置项（2026-10-05 追加：把引擎里写死的策略改成可配置）---------------

    /** 是否允许在箱子 / 工作台等容器界面里移动。 */
    boolean allowContainers();

    /** 背包界面里是否也允许潜行。 */
    boolean allowSneak();

    /** 移动时是否自动冲刺。 */
    boolean autoSprint();
}
