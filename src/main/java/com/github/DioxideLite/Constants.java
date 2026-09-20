package com.github.DioxideLite;

// ---------------------------------------------------------------------------
// [DioxideLite 修复] 机械改名残留修正。
// 本文件内容与原来的 com/github/setsuna/Constants.java 完全一致（包声明本来就
// 是 com.github.DioxideLite），此处只是把文件放到与包声明匹配的目录下。
//
// 原目录名 setsuna 与包声明不一致，属机械改名残留。javac 在收到显式文件列表时
// 能编译通过（Gradle 即如此），但按 sourcepath 查找时找不到该类 —— 这一点已在
// 本次编译校验中被实测证实：tritium/ncm/music/{CloudMusic,QqMusic}.java 报
// "package com.github.DioxideLite does not exist"，正是这个原因。
//
// 引用方 tritium/ncm/music/QqMusic.java 与 CloudMusic.java 里的
// `import com.github.DioxideLite.Constants;` 无需改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.DioxideLite;
import org.slf4j.Logger;

/**
 * Compatibility bridge for mechanically embedded legacy subsystems.
 */
public final class Constants {

    public static final Logger LOGGER = DioxideLite.LOGGER;

    private Constants() {
    }
}
