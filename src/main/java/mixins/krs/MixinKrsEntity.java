package mixins.krs;

import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.combat.TargetStrafe;
import com.dioxideliteng.krs.hacks.features.level.AlwaysRiptide;
import com.dioxideliteng.krs.hacks.features.level.NoBlockEffects;
import com.dioxideliteng.krs.hacks.features.movement.EntityControl;
import com.dioxideliteng.krs.hacks.features.movement.NoPush;
import com.dioxideliteng.krs.hacks.features.movement.Step;
import com.dioxideliteng.krs.hacks.features.player.Phase;
import com.dioxideliteng.krs.utils.IMinecraft;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * KRS entity hooks. KRS's {@code moveRelative} rewrite is intentionally absent: DioxideLiteNG's MixinEntity owns that
 * method and calls {@code KrsHooks.moveRelativeYaw}, so there is a single patch of it.
 */
@Mixin(Entity.class)
public abstract class MixinKrsEntity implements IMinecraft {

    @Shadow
    public abstract float getYRot();

    @Shadow
    public abstract void setDeltaMovement(Vec3 velocity);

    @Shadow
    public abstract Vec3 getDeltaMovement();

    @Shadow
    public abstract boolean isSprinting();

    @Shadow
    public abstract void addDeltaMovement(Vec3 velocity);

    @Shadow
    public boolean needsSync;

    @Shadow
    public abstract void setDeltaMovement(double x, double y, double z);

    @ModifyReturnValue(method = "getBlockSpeedFactor", at = @At("RETURN"))
    private float krs$noBlockEffectsSpeedFactor(float original) {
        return NoBlockEffects.shouldIgnore(this) ? 1.0f : original;
    }

    @ModifyReturnValue(method = "getBlockJumpFactor", at = @At("RETURN"))
    private float krs$noBlockEffectsJumpFactor(float original) {
        return NoBlockEffects.shouldIgnore(this) ? 1.0f : original;
    }

    @ModifyReturnValue(method = "getBlockBounciness", at = @At("RETURN"))
    private double krs$noBlockEffectsBounciness(double original) {
        return NoBlockEffects.shouldIgnore(this) ? 0.0 : original;
    }

    @ModifyReturnValue(method = "isSwimming", at = @At("RETURN"))
    public boolean krs$phaseSwimming(boolean original) {
        if ((Object) this instanceof LocalPlayer && ModuleManager.getModuleState(Phase.class))
            return false;

        return original;
    }

    @ModifyReturnValue(method = "getEyeHeight()F", at = @At("RETURN"))
    public float krs$phaseEyeHeight(float original) {
        if ((Object) this instanceof LocalPlayer) {
            if (!mc.player.isShiftKeyDown() && ModuleManager.getModuleState(Phase.class))
                return 1.62f;
        }

        return original;
    }

    @ModifyReturnValue(method = "getDimensions", at = @At("RETURN"))
    private EntityDimensions krs$riptideDimensions(EntityDimensions original, Pose pose) {
        return AlwaysRiptide.hookDimensions(original, (Entity) (Object) this, pose);
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 krs$targetStrafeCollisionGuard(Vec3 movement) {
        if ((Object) this instanceof LocalPlayer && TargetStrafe.targetStrafeHook())
            return TargetStrafe.preventHorizontalCollision(movement);

        return movement;
    }

    @ModifyReturnValue(method = "collide", at = @At("RETURN"))
    private Vec3 krs$stepping(Vec3 original) {
        if ((Object) this instanceof LocalPlayer) {
            if (ModuleManager.getModuleState(Step.class) && original.y > 0.6)
                Step.steppingFunctions();
        }

        return original;
    }

    @ModifyReturnValue(method = "getControllingPassenger", at = @At("RETURN"))
    private LivingEntity krs$entityControlPassenger(LivingEntity original) {
        return EntityControl.hookControllingPassenger(original, (Entity) (Object) this);
    }

    @ModifyArgs(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"))
    private void krs$noPush(Args args, Entity entity) {
        if ((Object) this instanceof LocalPlayer) {
            if (ModuleManager.getModuleState(NoPush.class) || ModuleManager.getModuleState(Phase.class)) {
                args.set(0, 0.0);
                args.set(2, 0.0);
            }
        }
    }
}
