package com.dioxideliteng.krs.render;

import com.dioxideliteng.krs.imguimc.interfaces.Renderable;
import imgui.ImGui;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * KRS 的 ImGui 渲染队列。
 *
 * <p>原版实现了 {@code ClientModInitializer} 并在此启动 KRS 客户端。移植后不注册
 * 独立入口点（由 DioxideLite NG 的 {@code DioxideLiteNGClient} 统一初始化），
 * 只保留渲染队列与 dockspace ID。</p>
 */
public final class Initializer {

    public static final List<Renderable> renderstack = new CopyOnWriteArrayList<>();

    private Initializer() {
    }

    public static void pushRenderable(Renderable renderable) {
        renderstack.add(renderable);
    }

    public static void pullEveryRenderable() {
        renderstack.clear();
    }

    public static int getDockId() {
        return ImGui.getID("imgui-mc dockspace");
    }
}
