package com.dioxidelite.util.rotation;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/util/rotation/Priority.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


/**
 * Relative importance of a rotation request. When several modules ask
 * {@link com.dioxidelite.manager.RotationManager} to aim in the same tick, the one with
 * the highest {@link #priority} wins; equal or higher priority replaces the current
 * request, lower priority is ignored.
 */
public enum Priority {

    Lowest(0),
    Low(1),
    Medium(2),
    High(3),
    Highest(4);

    public final int priority;

    Priority(int priority) {
        this.priority = priority;
    }
}
