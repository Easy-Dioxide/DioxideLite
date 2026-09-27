package com.dioxidelite.module.modules.render;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.function.Function;

/**
 * Blue Archive halo rendered above the player's head.
 * Ported from https://github.com/opai-client/blue-archive-halo (MC 1.8.9).
 */
public final class Halo extends Module {

    public static final Halo INSTANCE = new Halo();

    public enum HaloStyle {
        SHIROKO("砂狼白子", 0.08,
                "halo/shiroko/layer0.png", "halo/shiroko/layer1.png"),
        SERIKA("黑见芹香", 0.08,
                "halo/serika/layer0.png", "halo/serika/layer1.png"),
        HOSHINO("小鸟游星野", 0.06,
                "halo/hoshino/layer0.png", "halo/hoshino/layer1.png", "halo/hoshino/layer2.png"),
        LOGO("Opai Logo", 0.0, "halo/logo.png");

        final String label;
        final double layerSpacing;
        final String[] layers;

        HaloStyle(String label, double layerSpacing, String... layers) {
            this.label = label;
            this.layerSpacing = layerSpacing;
            this.layers = layers;
        }

        @Override public String toString() { return label; }
    }

    public final EnumSetting<HaloStyle> style = add(new EnumSetting<>("Style", HaloStyle.SHIROKO));
    public final DoubleSetting size = add(new DoubleSetting("Size", 1.5, 0.1, 3.0, 0.1));
    public final DoubleSetting spacing = add(new DoubleSetting("Spacing", 0.65, 0.5, 2.0, 0.05));
    public final DoubleSetting xRot = add(new DoubleSetting("X Rot", 0.0, -90.0, 90.0, 1.0));
    public final DoubleSetting yRot = add(new DoubleSetting("Y Rot", 0.0, -90.0, 90.0, 1.0));
    public final BooleanSetting followPitch = add(new BooleanSetting("Follow Pitch", false));
    public final BooleanSetting renderInFirstPerson = add(new BooleanSetting("Render In First Person", false));
    public final BooleanSetting floating = add(new BooleanSetting("Floating Animation", true));

    private Halo() {
        super("Halo", Category.RENDER);
    }

    private static final RenderPipeline HALO_PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation("pipeline/dioxide_lite_halo")
            .withShaderDefine("PER_FACE_LIGHTING", 0)
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();

    private static final Function<Identifier, RenderType> HALO_TYPE = Util.memoize(
            texture -> RenderType.create("dioxide_lite_halo", RenderSetup.builder(HALO_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .sortOnUpload()
                    .createRenderSetup()));

    @Listen
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;

        if (mc.options.getCameraType().isFirstPerson() && !renderInFirstPerson.get()) return;

        HaloStyle hs = style.get();
        float s = size.get().floatValue();
        float imgSize = s;

        Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
        PoseStack stack = event.getPoseStack();

        double px = mc.player.getX() - cam.x;
        double py = mc.player.getY() + 1.4 - cam.y;
        double pz = mc.player.getZ() - cam.z;

        stack.pushPose();
        stack.translate(px, py, pz);

        // Face player yaw
        float yaw = mc.player.getYRot();
        stack.mulPose(Axis.YP.rotationDegrees(-yaw + 180));

        if (followPitch.get()) {
            float pitch = mc.player.getXRot();
            stack.mulPose(Axis.XP.rotationDegrees(-pitch));
        }

        stack.mulPose(Axis.ZP.rotationDegrees(yRot.get().floatValue()));
        stack.mulPose(Axis.XP.rotationDegrees(xRot.get().floatValue()));

        stack.translate(0, spacing.get(), 0);

        // Rotate plane to face up
        stack.mulPose(Axis.XP.rotationDegrees(90));

        float floatOffset = 0;
        if (floating.get()) {
            floatOffset = (float) (Math.sin(System.currentTimeMillis() * 0.001) * 0.05 + 0.05);
        }

        Matrix4f matrix = stack.last().pose();

        for (int i = 0; i < hs.layers.length; i++) {
            String layer = hs.layers[i];
            Identifier tex = Identifier.fromNamespaceAndPath("dioxidelite", layer);

            float half = imgSize / 2.0f;
            float layerY = (float) (i * hs.layerSpacing * s / 1.5) + floatOffset;

            RenderType type = HALO_TYPE.apply(tex);
            BufferBuilder buffer = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            int color = 0xFFFFFFFF;

            buffer.addVertex(matrix, -half, layerY, -half).setUv(0f, 1f).setColor(color);
            buffer.addVertex(matrix, half, layerY, -half).setUv(1f, 1f).setColor(color);
            buffer.addVertex(matrix, half, layerY, half).setUv(1f, 0f).setColor(color);
            buffer.addVertex(matrix, -half, layerY, half).setUv(0f, 0f).setColor(color);

            type.draw(buffer.buildOrThrow());
        }

        stack.popPose();
    }
}
