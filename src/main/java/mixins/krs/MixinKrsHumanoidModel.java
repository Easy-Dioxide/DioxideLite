package mixins.krs;

import com.dioxideliteng.krs.hacks.features.render.OldHitting;
import com.dioxideliteng.krs.utils.render.IEntityRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Third-person sword-block arm pose of OldHitting, for the local player only.
 *
 * <p>KRS replaced the middle of {@code HumanoidModel.setupAnim} with a copy of vanilla's logic. This version leaves
 * vanilla's method untouched and only adjusts the main arm after it ran, which is far less likely to break arm
 * animation if vanilla changes.
 */
@Mixin(net.minecraft.client.model.HumanoidModel.class)
public abstract class MixinKrsHumanoidModel<T extends HumanoidRenderState> {

    @Shadow
    @Final
    public ModelPart rightArm;

    @Shadow
    @Final
    public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void krs$oldHittingBlockPose(T state, CallbackInfo ci) {
        if (!(((IEntityRenderState) state).client$getEntity() instanceof LocalPlayer) || !OldHitting.shouldBlock())
            return;

        OldHitting.applyThirdPersonBlockPose(state.mainArm == HumanoidArm.RIGHT ? this.rightArm : this.leftArm, state.mainArm);
    }
}
