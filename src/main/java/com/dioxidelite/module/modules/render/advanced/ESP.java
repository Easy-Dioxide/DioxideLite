package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.render.NameTagLogoRenderer;
import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.event.events.VanillaHudRenderEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ButtonSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.setting.settings.KeybindSetting;
import com.dioxidelite.util.player.HealthDetectionUtils;
import com.dioxidelite.util.player.TeamColorUtils;
import com.dioxidelite.util.render.ColorUtils;
import com.dioxidelite.util.render.WorldToScreen;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.joml.Vector4d;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家铭牌 + 装备 ESP（移植自 来源客户端 ESP：名字/血量/延迟铭牌 + 装备栏，跟着玩家框走）。
 * 投影和布局在 Render3D 层计算，背景与文字在 Render2D（Skija 画布）层绘制，
 * 物品图标通过 VanillaHudRenderEvent 走原版 HUD 渲染层。
 */
public final class ESP extends Module {

    public static final ESP INSTANCE = new ESP();

    /** 铭牌/装备相对玩家框的停靠边。来源客户端 的 0..3 zones。 */
    public enum Zone {
        TOP,
        LEFT,
        RIGHT,
        BOTTOM
    }

    private static final float FONT_SIZE = 9.0F;
    private static final float LINE_PAD = 4.0F;
    private static final float SLOT_SIZE = 22.0F;
    private static final float SLOT_GAP = 2.0F;
    private static final int ITEM_INSET = 3;
    private static final float PLATE_GAP = 10.0F;
    private static final float PLATE_PAD = 3.0F;
    /** 名牌图标相对字号的大小、以及与文字的间距。 */
    private static final float ICON_SCALE = 1.5F;
    private static final float ICON_GAP = 3.0F;
    private static final float PLATE_ROUND = 8.0F;
    private static final float STACK_GAP = 3.0F;

    // --- Names / name details ---
    public final BooleanSetting names = add(new BooleanSetting("Names", true));
    public final BooleanSetting namePrefix = add(new BooleanSetting("Prefix", true).visibleWhen(names::get));
    /** 名牌左侧的客户端 logo 图标（原版名牌旁边那个 name icon）。 */
    public final BooleanSetting nameIcon = add(new BooleanSetting("Icon", true).visibleWhen(names::get));
    public final BooleanSetting nameHealth = add(new BooleanSetting("Health", true).visibleWhen(names::get));
    public final BooleanSetting namePing = add(new BooleanSetting("Ping", true).visibleWhen(names::get));

    // --- Equipment / slots ---
    public final BooleanSetting equipment = add(new BooleanSetting("Equipment", true));
    public final BooleanSetting slotHelmet = add(new BooleanSetting("Helmet", true).visibleWhen(equipment::get));
    public final BooleanSetting slotChestplate = add(new BooleanSetting("Chestplate", true).visibleWhen(equipment::get));
    public final BooleanSetting slotLeggings = add(new BooleanSetting("Leggings", true).visibleWhen(equipment::get));
    public final BooleanSetting slotBoots = add(new BooleanSetting("Boots", true).visibleWhen(equipment::get));
    public final BooleanSetting slotMainHand = add(new BooleanSetting("Main Hand", true).visibleWhen(equipment::get));
    public final BooleanSetting slotOffHand = add(new BooleanSetting("Off Hand", true).visibleWhen(equipment::get));
    public final BooleanSetting enchantPeek = add(new BooleanSetting("Enchant Peek", true).visibleWhen(equipment::get));
    public final KeybindSetting peekKey = add(new KeybindSetting("Peek Key", GLFW.GLFW_KEY_LEFT_ALT)
            .visibleWhen(() -> equipment.get() && enchantPeek.get()));

    // --- Appearance ---
    public final DoubleSetting distance = add(new DoubleSetting("Distance", 256.0, 8.0, 256.0, 8.0));
    public final DoubleSetting scale = add(new DoubleSetting("Scale", 1.0, 0.5, 2.0, 0.05));
    public final ColorSetting textColor = add(new ColorSetting("Text Color", new Color(0xFFFFFFFF, true)));
    public final ColorSetting backgroundColor = add(new ColorSetting("Background", new Color(0xA0181A1F, true)));

    // --- Opal 风格 2D 框 + 血条 ---
    public final BooleanSetting box = add(new BooleanSetting("Box", false));
    public final BooleanSetting boxStroke = add(new BooleanSetting("Box Stroke", true).visibleWhen(box::get));
    public final ColorSetting boxColor = add(new ColorSetting("Box Color", new Color(0xFFFFFFFF, true))
            .visibleWhen(box::get));
    public final BooleanSetting healthBar = add(new BooleanSetting("Health Bar", false));
    public final BooleanSetting healthBarStroke = add(new BooleanSetting("Health Bar Stroke", true)
            .visibleWhen(healthBar::get));

    // --- Layout ---
    public final BooleanSetting freeLayout = add(new BooleanSetting("Free Layout", false));
    public final EnumSetting<Zone> nameZone = add(new EnumSetting<>("Name Zone", Zone.TOP)
            .visibleWhen(() -> !freeLayout.get()));
    public final EnumSetting<Zone> equipmentZone = add(new EnumSetting<>("Equipment Zone", Zone.BOTTOM)
            .visibleWhen(() -> !freeLayout.get()));
    public final BooleanSetting equipmentFirst = add(new BooleanSetting("Equipment First", false)
            .visibleWhen(() -> !freeLayout.get()));
    public final DoubleSetting nameFreeX = add(new DoubleSetting("Name Free X", 0.0, -2.0, 2.0, 0.0001)
            .visibleWhen(freeLayout::get));
    public final DoubleSetting nameFreeY = add(new DoubleSetting("Name Free Y", -0.72, -2.0, 2.0, 0.0001)
            .visibleWhen(freeLayout::get));
    public final DoubleSetting equipmentFreeX = add(new DoubleSetting("Equipment Free X", -0.6, -2.0, 2.0, 0.0001)
            .visibleWhen(freeLayout::get));
    public final DoubleSetting equipmentFreeY = add(new DoubleSetting("Equipment Free Y", 0.0, -2.0, 2.0, 0.0001)
            .visibleWhen(freeLayout::get));
    public final ButtonSetting resetLayout = add(new ButtonSetting("Reset Layout", this::resetLayout));

    private final List<Plate> plates = new ArrayList<>();

    private ESP() {
        super("ESP", Category.RENDER);
    }

    /** 恢复所有布局相关设置到默认值。 */
    public void resetLayout() {
        freeLayout.reset();
        nameZone.reset();
        equipmentZone.reset();
        equipmentFirst.reset();
        nameFreeX.reset();
        nameFreeY.reset();
        equipmentFreeX.reset();
        equipmentFreeY.reset();
    }

    @Override
    protected void onDisable() {
        plates.clear();
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        refreshPlates(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
    }

    @Listen
    private void onRender2D(Render2DEvent event) {
        // Skija pass 与 HUD pass 可能不在同一时刻取数据：这里重新投影一次，
        // 保证名牌文字用的是**本 pass 最新**的插值位置（否则移动时名牌会"漂"）。
        refreshPlates(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        if (plates.isEmpty()) {
            return;
        }
        Canvas canvas = event.canvas();
        boolean peek = isPeekActive();

        for (Plate plate : plates) {
            drawPlate(canvas, plate);
            if (peek && !plate.items().isEmpty()) {
                drawEnchantPeek(canvas, plate);
            }
        }
    }

    @Listen
    private void onVanillaHud(VanillaHudRenderEvent event) {
        // 装备图标走原版 HUD pass：同样重新投影，和上面的文字用同一帧的锚点。
        refreshPlates(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        if (plates.isEmpty()) {
            return;
        }
        GuiGraphicsExtractor graphics = event.graphics();

        for (Plate plate : plates) {
            if (plate.items().isEmpty()) {
                continue;
            }
            float renderScale = plate.scale();
            float startX = plate.equipCx() - plate.equipW() * 0.5F;

            graphics.pose().pushMatrix();
            graphics.pose().translate(startX, plate.equipCy() - plate.equipH() * 0.5F);
            graphics.pose().scale(renderScale, renderScale);
            float itemX = ITEM_INSET;
            for (ItemStack stack : plate.items()) {
                int x = Math.round(itemX);
                int y = Math.round(ITEM_INSET);
                graphics.item(stack, x, y);
                graphics.itemDecorations(mc.font, stack, x, y);
                itemX += SLOT_SIZE + SLOT_GAP;
            }
            graphics.pose().popMatrix();
        }
    }

    /** 重新投影所有目标的 2D 名牌框（每个 pass 调用一次，避免跨 pass 数据错帧）。 */
    private void refreshPlates(float partialTick) {
        plates.clear();
        if (noPlayer()) {
            return;
        }
        double maxDistanceSq = distance.get() * distance.get();
        for (Player target : mc.level.players()) {
            if (!isValidTarget(target, maxDistanceSq)) {
                continue;
            }
            Vector4d projected = WorldToScreen.getEntityPositionsOn2D(target, partialTick);
            if (projected == null || projected.z - projected.x < 2.0 || projected.w - projected.y < 4.0) {
                continue;
            }
            plates.add(buildPlate(target, projected));
        }
    }

    /**
     * 本模块的 Names 打开时，是否要隐藏原版（Minecraft 自带）的头顶名字。
     * 由 {@code EntityRendererMixin} / {@code LivingEntityRendererMixin} 调用。
     */
    public boolean hidesVanillaNameTag(LivingEntity entity) {
        if (!isEnabled() || !names.get() || !(entity instanceof Player player)) {
            return false;
        }
        return isValidTarget(player, distance.get() * distance.get());
    }

    // --- data / layout ---------------------------------------------------------

    private record Plate(Player player, float left, float top, float right, float bottom, float scale,
                         List<NameLine> lines, float nameCx, float nameCy, float nameW, float nameH,
                         float iconSize,
                         List<ItemStack> items, float equipCx, float equipCy, float equipW, float equipH) {
    }

    private record NameLine(String text, int color) {
    }

    private Plate buildPlate(Player target, Vector4d projected) {
        float left = (float) projected.x;
        float top = (float) projected.y;
        float right = (float) projected.z;
        float bottom = (float) projected.w;
        float width = Math.max(1.0F, right - left);
        float height = Math.max(1.0F, bottom - top);
        float renderScale = scale.get().floatValue() * Mth.clamp(height / 36.0F, 0.55F, 2.2F);

        List<NameLine> lines = buildNameLines(target);
        float nameW = 0.0F;
        for (NameLine line : lines) {
            nameW = Math.max(nameW, SkijaUi.textWidthWithFallback(line.text(), FONT_SIZE * renderScale));
        }
        float iconSize = (!lines.isEmpty() && nameIcon.get()) ? FONT_SIZE * renderScale * ICON_SCALE : 0.0F;
        if (!lines.isEmpty()) {
            nameW += PLATE_PAD * 2.0F * renderScale;
            if (iconSize > 0.0F) {
                nameW += iconSize + ICON_GAP * renderScale;
            }
        }
        float nameH = lines.isEmpty() ? 0.0F : lines.size() * (FONT_SIZE + LINE_PAD) * renderScale;

        List<ItemStack> items = equipment.get() ? buildItems(target) : List.of();
        float equipW = items.isEmpty() ? 0.0F
                : items.size() * SLOT_SIZE * renderScale + (items.size() - 1) * SLOT_GAP * renderScale;
        float equipH = items.isEmpty() ? 0.0F : SLOT_SIZE * renderScale;

        float[] nameAnchor = freeLayout.get()
                ? freeAnchor(left, top, right, bottom, nameFreeX.get(), nameFreeY.get())
                : zoneAnchor(left, top, right, bottom, renderScale, nameW, nameH, nameZone.get());
        float[] equipAnchor = freeLayout.get()
                ? freeAnchor(left, top, right, bottom, equipmentFreeX.get(), equipmentFreeY.get())
                : zoneAnchor(left, top, right, bottom, renderScale, equipW, equipH, equipmentZone.get());

        if (!freeLayout.get() && !lines.isEmpty() && !items.isEmpty()
                && nameZone.is(equipmentZone.get())) {
            stackAnchors(nameAnchor, nameW, nameH, equipAnchor, equipW, equipH,
                    nameZone.get(), equipmentFirst.get());
        }

        return new Plate(target, left, top, right, bottom, renderScale, lines,
                nameAnchor[0], nameAnchor[1], nameW, nameH, iconSize,
                items, equipAnchor[0], equipAnchor[1], equipW, equipH);
    }

    private List<NameLine> buildNameLines(Player target) {
        List<NameLine> lines = new ArrayList<>(3);
        if (!names.get()) {
            return lines;
        }

        int nameColor = TeamColorUtils.getNameColor(target, textColor.get()).getRGB();
        String prefix = teamPrefix(target);
        if (namePrefix.get() && !prefix.isEmpty()) {
            lines.add(new NameLine(prefix, nameColor));
        }
        lines.add(new NameLine(target.getName().getString(), nameColor));

        if (nameHealth.get()) {
            float health = HealthDetectionUtils.getHealth(target);
            float maximum = Math.max(1.0F, Math.max(target.getMaxHealth(), health));
            lines.add(new NameLine(Math.round(health) + "hp",
                    healthColor(Mth.clamp(health / maximum, 0.0F, 1.0F))));
        }
        if (namePing.get()) {
            int ping = ping(target);
            if (ping > 0) {
                lines.add(new NameLine(ping + "ms", textColor.argb()));
            }
        }
        return lines;
    }

    private List<ItemStack> buildItems(Player target) {
        List<ItemStack> items = new ArrayList<>(6);
        if (slotHelmet.get()) {
            appendItem(items, target.getItemBySlot(EquipmentSlot.HEAD));
        }
        if (slotChestplate.get()) {
            appendItem(items, target.getItemBySlot(EquipmentSlot.CHEST));
        }
        if (slotLeggings.get()) {
            appendItem(items, target.getItemBySlot(EquipmentSlot.LEGS));
        }
        if (slotBoots.get()) {
            appendItem(items, target.getItemBySlot(EquipmentSlot.FEET));
        }
        if (slotMainHand.get()) {
            appendItem(items, target.getMainHandItem());
        }
        if (slotOffHand.get()) {
            appendItem(items, target.getOffhandItem());
        }
        return items;
    }

    private static void appendItem(List<ItemStack> items, ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            items.add(stack.copy());
        }
    }

    private static float[] zoneAnchor(float left, float top, float right, float bottom, float scale,
                                      float plateW, float plateH, Zone zone) {
        float centerX = (left + right) * 0.5F;
        float centerY = (top + bottom) * 0.5F;
        float gap = PLATE_GAP * scale;
        return switch (zone) {
            case TOP -> new float[]{centerX, top - gap - plateH * 0.5F};
            case BOTTOM -> new float[]{centerX, bottom + gap + plateH * 0.5F};
            case LEFT -> new float[]{left - gap - plateW * 0.5F, centerY};
            case RIGHT -> new float[]{right + gap + plateW * 0.5F, centerY};
        };
    }

    private static float[] freeAnchor(float left, float top, float right, float bottom,
                                      double freeX, double freeY) {
        float centerX = (left + right) * 0.5F;
        float centerY = (top + bottom) * 0.5F;
        float width = Math.max(1.0F, right - left);
        float height = Math.max(1.0F, bottom - top);
        return new float[]{centerX + (float) freeX * width, centerY + (float) freeY * height};
    }

    /** 同名停靠边时，把第二块板沿该方向再往外推。 */
    private static void stackAnchors(float[] nameAnchor, float nameW, float nameH,
                                     float[] equipAnchor, float equipW, float equipH,
                                     Zone zone, boolean equipmentFirst) {
        float[] first = equipmentFirst ? equipAnchor : nameAnchor;
        float firstW = equipmentFirst ? equipW : nameW;
        float firstH = equipmentFirst ? equipH : nameH;
        float[] second = equipmentFirst ? nameAnchor : equipAnchor;
        float secondW = equipmentFirst ? nameW : equipW;
        float secondH = equipmentFirst ? nameH : equipH;
        float gap = STACK_GAP;

        switch (zone) {
            case TOP -> second[1] = first[1] - firstH * 0.5F - gap - secondH * 0.5F;
            case BOTTOM -> second[1] = first[1] + firstH * 0.5F + gap + secondH * 0.5F;
            case LEFT -> second[0] = first[0] - firstW * 0.5F - gap - secondW * 0.5F;
            case RIGHT -> second[0] = first[0] + firstW * 0.5F + gap + secondW * 0.5F;
        }
    }

    // --- drawing ---------------------------------------------------------------

    private void drawPlate(Canvas canvas, Plate plate) {
        // Opal 风格 2D 描边框
        if (box.get()) {
            float x = plate.left();
            float y = plate.top();
            float w = plate.right() - plate.left();
            float h = plate.bottom() - plate.top();
            int color = boxColor.get().getRGB();
            if (boxStroke.get()) {
                // 黑色外描边 + 彩色内框
                SkijaUi.outline(canvas, x - 1.0F, y - 1.0F, w + 2.0F, h + 2.0F, 0.0F, 2.0F, 0xFF000000);
                SkijaUi.outline(canvas, x, y, w, h, 0.0F, 1.0F, color);
            } else {
                SkijaUi.outline(canvas, x, y, w, h, 0.0F, 1.0F, color);
            }
        }

        // Opal 风格血条（左侧竖条）
        if (healthBar.get() && plate.player() != null) {
            float x = plate.left();
            float y = plate.top();
            float h = plate.bottom() - plate.top();
            float health = HealthDetectionUtils.getHealth(plate.player());
            float maximum = Math.max(1.0F, Math.max(plate.player().getMaxHealth(), health));
            float ratio = Mth.clamp(health / maximum, 0.0F, 1.0F);
            float barThickness = 2.0F;
            float barX = x - barThickness - 2.0F;
            float barY = y + h * (1.0F - ratio);
            float barH = h * ratio;
            int barColor = healthColor(ratio);
            if (healthBarStroke.get()) {
                SkijaUi.outline(canvas, barX - 1.0F, y - 1.0F, barThickness + 2.0F, h + 2.0F, 0.0F, 1.0F, 0xFF000000);
            }
            SkijaUi.fill(canvas, barX, barY, barThickness, barH, barColor);
        }

        if (!plate.lines().isEmpty()) {
            float x = plate.nameCx() - plate.nameW() * 0.5F;
            float y = plate.nameCy() - plate.nameH() * 0.5F;
            SkijaUi.rounded(canvas, x, y, plate.nameW(), plate.nameH(),
                    PLATE_ROUND * plate.scale(), backgroundColor.argb());

            float textSize = FONT_SIZE * plate.scale();
            float lineHeight = plate.nameH() / plate.lines().size();
            float textLeft = x;
            float textRight = x + plate.nameW();
            float iconSize = plate.iconSize();
            if (iconSize > 0.0F) {
                float iconX = x + PLATE_PAD * plate.scale();
                float iconY = y + (plate.nameH() - iconSize) * 0.5F;
                NameTagLogoRenderer.drawIcon(canvas, iconX, iconY, iconSize, 255);
                textLeft = iconX + iconSize + ICON_GAP * plate.scale();
            }
            float cursorY = y;
            for (NameLine line : plate.lines()) {
                float textWidth = SkijaUi.textWidthWithFallback(line.text(), textSize);
                SkijaUi.textWithFallback(canvas, line.text(),
                        textLeft + (textRight - textLeft - textWidth) * 0.5F, cursorY, lineHeight,
                        line.color(), textSize);
                cursorY += lineHeight;
            }
        }

        if (!plate.items().isEmpty()) {
            float x = plate.equipCx() - plate.equipW() * 0.5F;
            float y = plate.equipCy() - plate.equipH() * 0.5F;
            float slot = SLOT_SIZE * plate.scale();
            float gap = SLOT_GAP * plate.scale();
            for (int i = 0; i < plate.items().size(); i++) {
                SkijaUi.rounded(canvas, x + i * (slot + gap), y, slot, slot,
                        PLATE_ROUND * plate.scale(), backgroundColor.argb());
            }
        }
    }

    private void drawEnchantPeek(Canvas canvas, Plate plate) {
        List<String> lines = new ArrayList<>();
        for (ItemStack stack : plate.items()) {
            lines.addAll(enchantLines(stack));
        }
        if (lines.isEmpty()) {
            return;
        }

        float textSize = FONT_SIZE * plate.scale();
        float lineHeight = (FONT_SIZE + LINE_PAD) * plate.scale();
        float width = 0.0F;
        for (String line : lines) {
            width = Math.max(width, SkijaUi.textWidthWithFallback(line, textSize));
        }
        width += PLATE_PAD * 2.0F * plate.scale();
        float height = lines.size() * lineHeight + PLATE_PAD * 2.0F * plate.scale();
        float x = plate.equipCx() - width * 0.5F;
        float y = plate.equipCy() + plate.equipH() * 0.5F + STACK_GAP * plate.scale();

        SkijaUi.rounded(canvas, x, y, width, height, 4.0F * plate.scale(), backgroundColor.argb());
        float cursorY = y + PLATE_PAD * plate.scale();
        for (String line : lines) {
            SkijaUi.textWithFallback(canvas, line, x + PLATE_PAD * plate.scale(), cursorY,
                    lineHeight, textColor.argb(), textSize);
            cursorY += lineHeight;
        }
    }

    // --- helpers ---------------------------------------------------------------

    private boolean isValidTarget(Player target, double maxDistanceSq) {
        if (!target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (target == mc.player) {
            return !mc.options.getCameraType().isFirstPerson();
        }
        return mc.player.distanceToSqr(target) <= maxDistanceSq;
    }

    private int ping(Player target) {
        if (mc.getConnection() == null) {
            return 0;
        }
        PlayerInfo info = mc.getConnection().getPlayerInfo(target.getUUID());
        return info == null ? 0 : Math.max(0, info.getLatency());
    }

    private static String teamPrefix(Player target) {
        Team team = target.getTeam();
        if (!(team instanceof PlayerTeam playerTeam)) {
            return "";
        }
        Component prefix = playerTeam.getPlayerPrefix();
        return prefix == null ? "" : prefix.getString().trim();
    }

    private boolean isPeekActive() {
        if (!enchantPeek.get() || !peekKey.isBound()) {
            return false;
        }
        return GLFW.glfwGetKey(mc.getWindow().handle(), peekKey.get()) == GLFW.GLFW_PRESS;
    }

    private static List<String> enchantLines(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        ItemEnchantments enchantments = stack.getEnchantments();
        if (enchantments.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>(enchantments.size());
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            if (enchantment == null) {
                continue;
            }
            String name = Enchantment.getFullname(enchantment, 1).getString()
                    .replaceAll("\\s+[IVXLCDM]+$", "");
            lines.add(shorten(name) + entry.getIntValue());
        }
        lines.sort(String.CASE_INSENSITIVE_ORDER);
        return lines;
    }

    /** 来源客户端 的装备附魔缩写：单词取首字母，单单词取前 3 个字符。 */
    private static String shorten(String text) {
        String[] parts = text.split(" ");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(3, parts[0].length()));
        }
        StringBuilder builder = new StringBuilder(parts.length);
        for (String part : parts) {
            if (!part.isEmpty()) {
                builder.append(part.charAt(0));
            }
        }
        return builder.toString();
    }

    /** 来源客户端 的血量渐变色：红 -38302 → 橙 -15253 → 绿 -6560885。 */
    private static int healthColor(float ratio) {
        Color low = new Color(0xFFFF6A62, true);
        Color mid = new Color(0xFFFFC46B, true);
        Color high = new Color(0xFF9BE38B, true);
        Color color = ratio < 0.5F
                ? ColorUtils.interpolate(low, mid, ratio * 2.0F)
                : ColorUtils.interpolate(mid, high, (ratio - 0.5F) * 2.0F);
        return color.getRGB();
    }

    // PORT-NOTE: 需要拖拽式布局编辑 GUI（来源客户端 UiSupport_558 / "Edit layout"）；
    //            本端口只实现了 Zone 停靠 + Free Layout 偏移的静态布局。
}
