package com.dioxideliteng.krs.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * KRS 自带 NanoVG ClickGui 的占位实现。
 *
 * <p>移植策略：只搬功能模块，不搬 KRS 的界面。模块开关与参数统一由 DioxideLite NG 的
 * ClickGui 呈现，所以 KRS 的界面不会真的被打开。保留一个 {@link Screen} 子类，
 * 是为了让 KRS 模块和 Mixin 里的 {@code instanceof} 判断、按键拦截逻辑原样编译，
 * 行为等价于「KRS 界面从未打开」。</p>
 */
public class NanoVGClickGuiScreen extends Screen {

    private final Screen returnScreen;

    public NanoVGClickGuiScreen() {
        this(null);
    }

    public NanoVGClickGuiScreen(Screen returnScreen) {
        super(Component.literal("KRS Click GUI"));
        this.returnScreen = returnScreen instanceof NanoVGClickGuiScreen ? null : returnScreen;
    }

    /** KRS 原版在此渲染关闭动画的残留帧；现在没有 KRS 界面，恒为空操作。 */
    public static void renderDetachedIfNeeded() {
    }

    /** 文本输入聚焦、绑定按键等状态只存在于 KRS 界面中，这里恒为 false。 */
    public boolean shouldBlockGameMovement() {
        return false;
    }

    /** 同上：没有 KRS 界面时不需要同步游戏移动键。 */
    public boolean shouldSyncGameMovementKeys() {
        return false;
    }

    /** 关闭时回到打开前的界面，与 KRS 原版行为一致。 */
    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(returnScreen);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // 不绘制 KRS 界面：模块列表由 NG ClickGui 呈现。
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
