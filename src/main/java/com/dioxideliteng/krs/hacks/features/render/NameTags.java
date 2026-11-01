package com.dioxideliteng.krs.hacks.features.render;

import com.dioxideliteng.krs.KrsBridge;
import com.dioxideliteng.krs.events.features.RenderHudEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.player.Freecam;
import com.dioxideliteng.krs.hacks.features.player.MurdererDetector;
import com.dioxideliteng.krs.utils.entity.StreamConverter;
import com.dioxideliteng.krs.utils.input.Keys;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import com.dioxideliteng.krs.utils.value.FloatValue;
import com.dioxideliteng.ui.hud.HudLayouts;
import com.dioxideliteng.util.MutableVector3d;
import com.dioxideliteng.util.WorldToScreenProjector;
import com.dioxideliteng.util.render.HudBackdrop;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * KRS name tags ("NameTags2" next to DioxideLiteNG's own NameTags).
 *
 * <p>Keeps KRS's selection logic (players or all entities, local player, murderer tag, nearest-256 cap, far-to-near
 * ordering) but draws like DioxideLiteNG's tags: a blurred glass capsule from {@link HudBackdrop} with vanilla font text,
 * in the 2D pass. The vanilla tag of every entity shown here is suppressed (see {@link #replacesVanilla}).
 * KRS's per-tag overdraw clipping existed only for its NanoVG batching; far-to-near drawing covers overlaps here.
 */
public class NameTags extends Module {
    private static final int MAX_RENDERED_NAME_TAGS = 256;
    private static final int NAME_COLOR = 0xE6EBF0F4;
    private static final int TAG_COLOR = 0xC8FF1E1E;
    private static final float PADDING_X = 5f;
    private static final float PADDING_Y = 3f;
    private static final float RADIUS = 4f;

    @Setting
    private static final BooleanValue onlyPlayers = new BooleanValue("Only Players", true);

    @Setting
    private static final BooleanValue local = new BooleanValue("Local", false);

    @Setting
    private static final FloatValue scale = new FloatValue("Scale", 0.8f, 0.4f, 2.0f);

    private final MutableVector3d projected = new MutableVector3d();

    public NameTags() {
        super("Name Tags", ModuleCategory.Render, Keys.KEY_UNKNOWN, false, true);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    /** True when this module draws {@code entity}'s tag, so the vanilla one must be hidden. */
    public static boolean replacesVanilla(Entity entity) {
        return ModuleManager.getModuleState(NameTags.class) && shouldRender(entity);
    }

    public static boolean shouldRender(Entity entity) {
        if (mc.player == null || mc.level == null || mc.gui.hud.isHidden()) return false;

        boolean murderer = ModuleManager.getModuleState(MurdererDetector.class) && MurdererDetector.isMurderer(entity);
        if (!murderer && (entity instanceof ArmorStand || onlyPlayers.get() && !(entity instanceof Player))) return false;
        return !(entity instanceof LocalPlayer) || local.get()
            && (!mc.options.getCameraType().isFirstPerson() || ModuleManager.getModuleState(Freecam.class));
    }

    @Override
    public void onRenderHud(RenderHudEvent event) {
        GuiGraphicsExtractor graphics = KrsBridge.graphics();
        if (graphics == null || mc.player == null || mc.level == null || mc.gui.hud.isHidden()) return;

        float partialTicks = event.tickDelta;
        Vec3 camera = mc.gameRenderer.mainCamera().position();
        Iterable<? extends Entity> source = onlyPlayers.get() ? mc.level.players() : mc.level.entitiesForRendering();

        List<Tag> tags = new ArrayList<>();
        for (Entity entity : source) {
            if (entity.isRemoved() || !shouldRender(entity)) continue;

            Vec3 position = entity.getPosition(partialTicks);
            double y = position.y + entity.getBbHeight() + 0.55;
            if (WorldToScreenProjector.project(position.x, y, position.z, projected) == null
                || !Double.isFinite(projected.x) || !Double.isFinite(projected.y) || !Double.isFinite(projected.z)
                || projected.z < -1 || projected.z > 1) continue;

            double dx = position.x - camera.x, dy = y - camera.y, dz = position.z - camera.z;
            tags.add(new Tag(entity, (float) projected.x, (float) projected.y, dx * dx + dy * dy + dz * dz));
        }
        if (tags.isEmpty()) return;

        // Nearest 256 only, drawn far to near so closer tags sit on top.
        tags.sort(Comparator.comparingDouble(Tag::depth));
        if (tags.size() > MAX_RENDERED_NAME_TAGS) tags.subList(MAX_RENDERED_NAME_TAGS, tags.size()).clear();

        float size = scale.get();
        boolean murdererDetector = ModuleManager.getModuleState(MurdererDetector.class);
        for (int i = tags.size() - 1; i >= 0; i--) {
            Tag tag = tags.get(i);
            Component name = displayName(tag.entity);
            String tagText = murdererDetector && MurdererDetector.isMurderer(tag.entity) ? "Murderer" : null;

            float nameWidth = mc.font.width(name);
            float tagWidth = tagText == null ? 0f : mc.font.width(tagText);
            float lineHeight = mc.font.lineHeight;
            float width = Math.max(nameWidth, tagWidth) + PADDING_X * 2f;
            float height = lineHeight + PADDING_Y * 2f + (tagText == null ? 0f : lineHeight);

            // Capsule is centred on the anchor horizontally; the name line is the bottom one.
            float left = -width / 2f;
            float top = -height + lineHeight / 2f + PADDING_Y;
            HudBackdrop.widget(new HudLayouts.Box(tag.x + left * size, tag.y + top * size, width * size, height * size),
                RADIUS * size);

            graphics.nextStratum();
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate(tag.x, tag.y);
                graphics.pose().scale(size, size);
                graphics.text(mc.font, name, Math.round(-nameWidth / 2f), Math.round(-lineHeight / 2f), NAME_COLOR, false);
                if (tagText != null) {
                    graphics.text(mc.font, tagText, Math.round(-tagWidth / 2f), Math.round(-lineHeight / 2f - lineHeight),
                        TAG_COLOR, false);
                }
            } finally {
                graphics.pose().popMatrix();
            }
        }
        graphics.nextStratum();
    }

    private static Component displayName(Entity entity) {
        if (entity instanceof Player || entity.hasCustomName()) return entity.getDisplayName();
        return Component.literal(StreamConverter.formatNameByPath(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath()));
    }

    private record Tag(Entity entity, float x, float y, double depth) {
    }
}
