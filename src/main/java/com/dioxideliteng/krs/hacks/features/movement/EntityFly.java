package com.dioxideliteng.krs.hacks.features.movement;



import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.events.features.MotionEvent;
import com.dioxideliteng.krs.events.features.TickEvent;
import com.dioxideliteng.krs.events.features.UpdateEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.utils.math.Interpolation;
import com.dioxideliteng.krs.utils.move.MovementUtil;
import com.dioxideliteng.krs.utils.packet.PacketUtil;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import com.dioxideliteng.krs.utils.value.FloatValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import com.dioxideliteng.krs.utils.input.Keys;

public class EntityFly extends Module {

    public EntityFly() {
        super("Entity Fly", ModuleCategory.Movement, Keys.KEY_UNKNOWN, false, true);
    }

    @Setting
    private static final BooleanValue stopBoatPaddles = new BooleanValue("Stop Boat Paddles", true);

    @Setting
    private static final BooleanValue twoBjpBoost = new BooleanValue("2b2tJP Boost", false);

    @Setting
    private static final FloatValue hSpeed = new FloatValue("Horizontal Speed", 2f, 0.1f, 4f, () -> !twoBjpBoost.get());

    @Setting
    private static final FloatValue vSpeed = new FloatValue("Vertical Speed", 1.2f, 0.1f, 4f, () -> !twoBjpBoost.get());

    public static boolean hookUpdatePaddles(AbstractBoat boatEntity, boolean original) {
        if (shouldIgnoreBoatSteering(boatEntity) && stopBoatPaddles.get())
            return false;

        return original;
    }

    public static boolean shouldIgnoreBoatSteering(AbstractBoat boatEntity) {
        var player = mc.player;
        if (ModuleManager.getModuleState(EntityFly.class) && player != null && player.isPassenger() && player.getVehicle() == boatEntity)
            return true;

        return false;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public String description() {
        return "Dismount with left alt";
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        var player = mc.player;
        if (player == null) return;

        if (player.isPassenger() && InputConstants.isKeyDown(Keys.KEY_LEFT_ALT)) {
            player.stopRiding();
            PacketUtil.sendPacket(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, true, false)));
            KrsClient.notificationManager.addNotification("Success", "Dismounted");
            return;
        }

        var vehicle = player.getVehicle();
        if (!player.isPassenger() || vehicle == null) return;

        float yMotion = 0f;
        boolean boostMode = twoBjpBoost.get();
        float booster = boostMode ? 3f : vSpeed.get();

        if (InputConstants.isKeyDown(InputConstants.getKey(mc.options.keyJump.saveString()).getValue()))
            yMotion += booster;

        if (InputConstants.isKeyDown(InputConstants.getKey(mc.options.keyShift.saveString()).getValue()))
            yMotion -= booster;

        double yaw = Math.toRadians(MovementUtil.getPlayerDirection());
        boolean moving = MovementUtil.isMoving();
        float horizontalSpeed = boostMode ? 17f : hSpeed.get();
        double xMotion = moving ? -Math.sin(yaw) * horizontalSpeed : 0.0;
        double zMotion = moving ? Math.cos(yaw) * horizontalSpeed : 0.0;

        if (stopBoatPaddles.get()) {
            float lerpedYaw = Interpolation.INSTANCE.lerp(vehicle.getYRot(), player.getYRot(), 0.35f);
            vehicle.setYRot(lerpedYaw);
        }

        vehicle.setNoGravity(true);
        vehicle.setDeltaMovement(xMotion, yMotion, zMotion);
    }

    @Override
    public void onTick(TickEvent event) {
        var player = mc.player;
        if (player == null || !player.isPassenger() || player.getVehicle() == null) return;

        mc.options.keyShift.setDown(false);
    }
}
