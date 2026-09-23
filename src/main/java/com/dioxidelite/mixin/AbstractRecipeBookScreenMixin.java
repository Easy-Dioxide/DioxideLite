package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/mixin/AbstractRecipeBookScreenMixin.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.module.modules.player.FastCraftModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects FastCraft rendering and click handling into the inventory screen.
 * <p>
 * Only activates on {@link InventoryScreen} (player inventory), not on
 * crafting tables or other {@link AbstractRecipeBookScreen} subclasses.
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {

    @Unique
    private boolean dioxidelite$isInventoryScreen() {
        return (Object) this instanceof InventoryScreen;
    }

    @Unique
    private FastCraftModule dioxidelite$getFastCraft() {
        FastCraftModule module = FastCraftModule.INSTANCE;
        return module.isEnabled() ? module : null;
    }

    @Unique
    private int dioxidelite$getFastCraftLeftPos() {
        return ((AbstractContainerScreenAccessor) this).dioxidelite$getLeftPos();
    }

    @Unique
    private int dioxidelite$getFastCraftTopPos() {
        return ((AbstractContainerScreenAccessor) this).dioxidelite$getTopPos();
    }

    /**
     * Renders the FastCraft panel after all other screen content.
     */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void dioxidelite$renderFastCraftPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                            float partialTick, CallbackInfo ci) {
        if (!dioxidelite$isInventoryScreen()) return;
        FastCraftModule fastCraft = dioxidelite$getFastCraft();
        if (fastCraft == null) return;

        fastCraft.render(graphics, dioxidelite$getFastCraftLeftPos(), dioxidelite$getFastCraftTopPos(), mouseX, mouseY);
    }

    /**
     * Intercepts mouse clicks before the recipe book handles them.
     * If the click lands on the FastCraft panel, the event is consumed.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void dioxidelite$clickFastCraftPanel(MouseButtonEvent event, boolean doubleClick,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (!dioxidelite$isInventoryScreen()) return;
        if (event.button() != 0) return; // Only left-click

        FastCraftModule fastCraft = dioxidelite$getFastCraft();
        if (fastCraft == null) return;

        if (fastCraft.mouseClicked(
                event.x(),
                event.y(),
                dioxidelite$getFastCraftLeftPos(),
                dioxidelite$getFastCraftTopPos())) {
            cir.setReturnValue(true);
        }
    }
}
