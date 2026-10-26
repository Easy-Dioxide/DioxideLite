package com.dioxideliteng.krs.imguimc.customwindow;

import com.dioxideliteng.krs.command.KrsCommands;
import com.dioxideliteng.krs.imguimc.interfaces.Renderable;
import com.dioxideliteng.krs.imguimc.interfaces.Theme;

/**
 * KRS 模块面板的精简替代。
 *
 * <p>原版是一个 1000 行的 ImGui 窗口，内含模块开关、参数编辑、配置读写与命令解析。
 * 按移植策略「只搬功能模块，不搬 KRS 界面」，这里不再绘制任何窗口：
 * 模块开关与参数由 DioxideLite NG 的 ClickGui 负责，命令逻辑迁到
 * {@link KrsCommands}。</p>
 *
 * <p>保留此类是为了让引用它的模块（Spammer、PluginsDetector 等）与
 * {@code ModuleManager} 保持原样调用。</p>
 */
public final class ModuleRenderable implements Renderable {

    public ModuleRenderable() {
    }

    /** 转发到 {@link KrsCommands}，保持原有调用签名。 */
    public static void executeCommand(String command, Boolean chatMode) {
        KrsCommands.executeCommand(command, chatMode);
    }

    /** 转发到 {@link KrsCommands}。 */
    public static void addCommandLog(String log) {
        KrsCommands.addCommandLog(log);
    }

    /** 转发到 {@link KrsCommands}。 */
    public static String keyName(int key) {
        return KrsCommands.keyName(key);
    }

    @Override
    public String getName() {
        return "KRS Modules";
    }

    @Override
    public Theme getTheme() {
        return null;
    }

    @Override
    public void render() {
        // KRS 界面不参与渲染，模块列表由 NG 的 ClickGui 呈现。
    }
}
