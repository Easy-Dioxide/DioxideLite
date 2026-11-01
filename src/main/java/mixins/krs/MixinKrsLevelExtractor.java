package mixins.krs;

import com.dioxideliteng.krs.KrsBridge;
import com.dioxideliteng.krs.events.features.Render3DEvent;
import com.dioxideliteng.krs.events.features.RenderEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * World overlays of ported KRS modules. {@link Render3DEvent} / {@link RenderEvent} are raised while a gizmo collection
 * is open, so listeners can draw with {@code Gizmos.*} exactly like DioxideLiteNG's BedAura target box does; the
 * collector owns depth, blending and camera transforms.
 */
@Mixin(LevelExtractor.class)
public abstract class MixinKrsLevelExtractor {

    @Inject(method = "extractGizmos", at = @At("HEAD"))
    private void krs$extractWorldOverlays(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean render3d = KrsBridge.has(Render3DEvent.class);
        boolean render = KrsBridge.has(RenderEvent.class);
        if (!render3d && !render) return;

        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        try (var collection = mc.levelExtractor.collectPerFrameMainThreadGizmos()) {
            PoseStack stack = new PoseStack();
            if (render3d) KrsBridge.call(new Render3DEvent(stack, partialTicks));
            if (render) KrsBridge.call(new RenderEvent(stack, partialTicks));
        }
    }
}
