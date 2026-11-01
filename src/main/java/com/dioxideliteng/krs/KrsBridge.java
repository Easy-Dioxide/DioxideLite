package com.dioxideliteng.krs;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventPostMotion;
import com.dioxideliteng.event.impl.EventPreMotion;
import com.dioxideliteng.event.impl.EventRender2D;
import com.dioxideliteng.krs.events.EventArgument;
import com.dioxideliteng.krs.events.EventManager;
import com.dioxideliteng.krs.events.features.MotionEvent;
import com.dioxideliteng.krs.events.features.RenderHudEvent;
import com.dioxideliteng.krs.events.features.SendPacketEvent;
import com.dioxideliteng.krs.hacks.KrsFeature;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.combat.KillAura;
import com.dioxideliteng.krs.hacks.features.player.Scaffold;
import com.dioxideliteng.krs.hacks.features.render.ESP;
import com.dioxideliteng.krs.utils.NotificationManager;
import com.dioxideliteng.krs.utils.packet.PacketUtil;
import com.dioxideliteng.krs.utils.rotation.RotationManager;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Connects DioxideLiteNG's event system to the KRS event system.
 *
 * <ul>
 *   <li>Events DioxideLiteNG already raises (motion, 2D render, outgoing packets) are translated in {@link #dispatch}.</li>
 *   <li>Events DioxideLiteNG has no equivalent for (update, tick, world, attack, keyboard, mouse, 3D render, received
 *       packets, block collision) are raised by the mixins in {@code mixins.krs} through {@link #call}.</li>
 * </ul>
 *
 * Enabled ported modules are registered with the KRS {@link EventManager}; it keeps KRS's per-event listener routing.
 */
public final class KrsBridge {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final List<KrsFeature> FEATURES = new ArrayList<>();
    private static final Set<Module> REGISTERED = Collections.newSetFromMap(new IdentityHashMap<>());

    private static boolean initialized;
    /** The pre-motion event of the packet currently being built, so the matching post event can reuse it. */
    private static MotionEvent currentMotion;
    private static GuiGraphicsExtractor currentGraphics;

    private KrsBridge() {
    }

    // ------------------------------------------------------------------ lifecycle

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        Client.eventManager = new EventManager();
        Client.rotationManager = new RotationManager();
        Client.notificationManager = new NotificationManager();
        Client.eventManager.register(KrsCore.INSTANCE);
        Client.loaded = true;
    }

    public static synchronized void add(KrsFeature feature) {
        FEATURES.add(feature);
    }

    public static synchronized List<KrsFeature> features() {
        return List.copyOf(FEATURES);
    }

    /** Registers newly enabled modules and unregisters disabled ones. Cheap enough to run every tick. */
    public static synchronized void refresh() {
        EventManager manager = Client.eventManager;
        if (manager == null) return;

        for (KrsFeature feature : FEATURES) {
            Module module = feature.module();
            boolean enabled = feature.isEnabled();
            boolean registered = REGISTERED.contains(module);
            if (enabled && !registered) {
                manager.register(module);
                REGISTERED.add(module);
            } else if (!enabled && registered) {
                manager.unregister(module);
                REGISTERED.remove(module);
            }
        }
    }

    public static boolean ready() {
        return initialized && Client.eventManager != null && Client.loaded;
    }

    // ------------------------------------------------------------------ shared visuals (HUD / Dynamic Island)

    /** True while the ported KRS Scaffold ("Scaffold2") is enabled; lets the Dynamic Island show its scaffold panel. */
    public static boolean scaffoldEnabled() {
        return initialized && ModuleManager.getModuleState(Scaffold.class);
    }

    public static boolean isScaffold(Feature feature) {
        return feature instanceof KrsFeature krs && krs.module() instanceof Scaffold;
    }

    /** The entity the ported KRS KillAura ("KillAura2") is currently attacking, for DioxideLiteNG's TargetHUD. */
    public static Entity auraTarget() {
        if (!initialized || !ModuleManager.getModuleState(KillAura.class)) return null;

        Entity target = KillAura.closestEntity;
        return target != null && target.isAlive() && !target.isRemoved() ? target : null;
    }

    // ------------------------------------------------------------------ KRS-side API used by mixins

    public static boolean has(Class<? extends EventArgument> type) {
        EventManager manager = Client.eventManager;
        return manager != null && Client.loaded && manager.hasListeners(type);
    }

    /** Dispatches {@code event} and returns whether a listener cancelled it. */
    public static boolean call(EventArgument event) {
        EventManager manager = Client.eventManager;
        if (manager == null || !Client.loaded || event == null) return false;
        manager.call(event);
        return event.isCancelled();
    }

    // ------------------------------------------------------------------ DioxideLiteNG events -> KRS events

    /** Called by DioxideLiteNG's EventBus after all native listeners ran. */
    public static void dispatch(Event event) {
        if (!ready()) return;

        if (event == Events.PRE_MOTION) {
            preMotion(Events.PRE_MOTION);
        } else if (event == Events.POST_MOTION) {
            postMotion();
        } else if (event == Events.RENDER_2D) {
            render2d(Events.RENDER_2D);
        } else if (event instanceof EventPacketSend send) {
            sendPacket(send);
        } else if (event == Events.ENTITY_OUTLINE) {
            // Per-entity hook of the glow outline; ESP is the only ported module that uses it.
            if (ModuleManager.getModuleState(ESP.class)) ESP.applyOutline(Events.ENTITY_OUTLINE);
        }
    }

    private static void preMotion(EventPreMotion pre) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            currentMotion = null;
            return;
        }

        MotionEvent event = new MotionEvent(pre.getX(), pre.getY(), pre.getZ(),
            Events.ROTATION.getYaw(), Events.ROTATION.getPitch(), pre.isOnGround(), false, false, false);
        float originalYaw = event.yaw;
        float originalPitch = event.pitch;

        // KRS exposes "would vanilla send a movement packet this tick" as isMoving.
        double dx = event.x - player.xLast;
        double dy = event.y - player.yLast;
        double dz = event.z - player.zLast;
        boolean positionChanged = dx * dx + dy * dy + dz * dz > 4.0E-8 || player.positionReminder >= 19;
        boolean rotationChanged = event.yaw != player.yRotLast || event.pitch != player.xRotLast;
        event.isMoving = positionChanged || rotationChanged;

        Client.eventManager.call(event);

        if (event.isCancelled()) {
            // KRS skipped the whole movement packet. Present vanilla with "nothing changed" so it sends none.
            pre.setX(player.xLast);
            pre.setY(player.yLast);
            pre.setZ(player.zLast);
            pre.setOnGround(player.lastOnGround);
            Events.ROTATION.setYaw(player.yRotLast);
            Events.ROTATION.setPitch(player.xRotLast);
            currentMotion = null;
            return;
        }

        boolean rotationEdited = event.yaw != originalYaw || event.pitch != originalPitch;
        RotationManager rotations = Client.rotationManager;
        if (rotations != null && (rotations.isRotating() || rotationEdited)) {
            float[] fixed = rotations.normalizeRotation(event.yaw, event.pitch, player.yRotLast, player.xRotLast);
            event.yaw = fixed[0];
            event.pitch = fixed[1];
        }

        pre.setX(event.x);
        pre.setY(event.y);
        pre.setZ(event.z);
        pre.setOnGround(event.onGround);
        Events.ROTATION.setYaw(event.yaw);
        Events.ROTATION.setPitch(event.pitch);
        currentMotion = event;
    }

    private static void postMotion() {
        MotionEvent pre = currentMotion;
        currentMotion = null;
        if (pre != null && pre.callInPost) {
            Client.eventManager.call(new MotionEvent(pre.x, pre.y, pre.z, pre.yaw, pre.pitch, pre.onGround, false, false, true));
        }
        if (Client.rotationManager != null) {
            Client.rotationManager.postMotionVisualTick();
        }
        // DioxideLiteNG's own Scaffold feeds the island's blocks-per-second readout from here; do the same for Scaffold2.
        if (scaffoldEnabled()) {
            DynamicIslandManager.sampleScaffoldMovement();
        }
    }

    private static void render2d(EventRender2D render) {
        if (!Client.eventManager.hasListeners(RenderHudEvent.class)) return;
        currentGraphics = render.getGraphics();
        try {
            Client.eventManager.call(new RenderHudEvent(render.getPartialTick()));
        } finally {
            currentGraphics = null;
        }
    }

    /**
     * The 2D graphics context of the frame being drawn. Only valid while a {@link RenderHudEvent} is being
     * dispatched (KRS drew through its own NanoVG queue; ported modules draw through DioxideLiteNG's graphics instead).
     */
    public static GuiGraphicsExtractor graphics() {
        return currentGraphics;
    }

    private static void sendPacket(EventPacketSend send) {
        Packet<?> packet = send.getPacket();
        if (packet == null || send.isCancelled()) return;
        // Packets KRS code sends on purpose must not be re-offered to KRS listeners.
        if (PacketUtil.consumeSilentPacket(packet)) return;
        if (!Client.eventManager.hasListeners(SendPacketEvent.class)) return;

        SendPacketEvent event = new SendPacketEvent(packet);
        Client.eventManager.call(event);
        if (event.isCancelled() || event.packet == null) {
            send.setCancelled(true);
        } else if (event.packet != packet) {
            send.replace(event.packet);
        }
    }
}
