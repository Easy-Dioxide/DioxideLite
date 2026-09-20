package com.dioxidelite.script;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/script/LuaScriptError.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import java.nio.file.Path;
import java.time.Instant;

/** A load or callback failure retained for the {@code .lua errors} command. */
public record LuaScriptError(Path file, String phase, String message, Instant time) {
}
