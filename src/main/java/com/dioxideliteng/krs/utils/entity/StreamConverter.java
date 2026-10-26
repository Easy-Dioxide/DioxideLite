package com.dioxideliteng.krs.utils.entity;

import com.dioxideliteng.krs.utils.IMinecraft;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import javax.imageio.ImageIO;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class StreamConverter implements IMinecraft {
    private static final Map<String, String> FORMATTED_NAME_CACHE = new ConcurrentHashMap<>();

    public static void getPlayerFaceAsInputStream(Player player, Consumer<InputStream> callback) {
        if (player == null || callback == null)
            return;

        try {
            mc.execute(() -> {
                try {
                    GameProfile profile = player.getGameProfile();
                    mc.getSkinManager().get(profile).whenComplete((skin, failure) -> {
                        try {
                            mc.execute(() -> {
                                if (failure != null || skin == null) {
                                    acceptSafely(callback, null);
                                    return;
                                }

                                Identifier skinIdentifier = skin.map(playerSkin -> playerSkin.body().texturePath())
                                        .orElseGet(() -> player instanceof AbstractClientPlayer abstractClientPlayer
                                                ? abstractClientPlayer.getSkin().body().texturePath()
                                                : null);
                                loadPlayerFace(skinIdentifier, callback);
                            });
                        } catch (RuntimeException exception) {
                            acceptSafely(callback, null);
                        }
                    });
                } catch (RuntimeException exception) {
                    acceptSafely(callback, null);
                }
            });
        } catch (RuntimeException exception) {
            acceptSafely(callback, null);
        }
    }

    private static void loadPlayerFace(Identifier skinIdentifier, Consumer<InputStream> callback) {
        if (skinIdentifier == null) {
            acceptSafely(callback, null);
            return;
        }

        NativeImage nativeImage = null;
        TextureContents textureContents = null;
        try {
            TextureManager textureManager = mc.getTextureManager();
            AbstractTexture texture = textureManager.getTexture(skinIdentifier);
            if (texture instanceof DynamicTexture dynamicTexture)
                nativeImage = dynamicTexture.getPixels();

            if (nativeImage == null) {
                textureContents = TextureContents.load(mc.getResourceManager(), skinIdentifier);
                nativeImage = textureContents.image();
            }
            if (nativeImage == null || nativeImage.getWidth() < 48 || nativeImage.getHeight() < 16) {
                acceptSafely(callback, null);
                return;
            }

            BufferedImage faceBase = nativeImageRegion(nativeImage, 8, 8, 8, 8);
            BufferedImage faceOverlay = nativeImageRegion(nativeImage, 40, 8, 8, 8);
            if (!isOverlayEmpty(faceOverlay))
                faceBase = combineLayers(faceBase, faceOverlay);

            acceptSafely(callback, imageToInputStream(faceBase));
        } catch (IOException | RuntimeException exception) {
            acceptSafely(callback, null);
        } finally {
            if (textureContents != null)
                textureContents.close();
        }
    }

    private static void acceptSafely(Consumer<InputStream> callback, InputStream inputStream) {
        try {
            callback.accept(inputStream);
        } catch (RuntimeException exception) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static BufferedImage nativeImageRegion(NativeImage nativeImage, int sourceX, int sourceY, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++)
                image.setRGB(x, y, nativeImage.getPixel(sourceX + x, sourceY + y));
        }
        return image;
    }

    private static boolean isOverlayEmpty(BufferedImage overlay) {
        for (int y = 0; y < overlay.getHeight(); y++) {
            for (int x = 0; x < overlay.getWidth(); x++) {
                if ((overlay.getRGB(x, y) >> 24) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BufferedImage combineLayers(BufferedImage base, BufferedImage overlay) {
        BufferedImage combined = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();
        g.drawImage(base, 0, 0, null);
        g.drawImage(overlay, 0, 0, null);
        g.dispose();
        return combined;
    }

    private static InputStream imageToInputStream(BufferedImage image) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ByteArrayInputStream(os.toByteArray());
    }

    public static String formatNameByPath(String registriesName) {
        if (registriesName == null || registriesName.isEmpty())
            return "";

        return FORMATTED_NAME_CACHE.computeIfAbsent(registriesName, StreamConverter::formatNameByPathUncached);
    }

    private static String formatNameByPathUncached(String registriesName) {
        StringBuilder formatted = new StringBuilder();
        boolean capitalizeNext = true;

        for (int i = 0, n = registriesName.length(); i < n; i++) {
            char c = registriesName.charAt(i);
            if (c == '_') {
                if (!formatted.isEmpty() && formatted.charAt(formatted.length() - 1) != ' ')
                    formatted.append(' ');
                capitalizeNext = true;
                continue;
            }

            formatted.append(capitalizeNext ? Character.toUpperCase(c) : c);
            capitalizeNext = false;
        }

        return formatted.toString();
    }
}
