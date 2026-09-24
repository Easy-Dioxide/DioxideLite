package com.dioxidelite.mixin;

import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import com.dioxidelite.DioxideLite;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Replaces the vanilla window icon with the DioxideLite client icon.
 * <p>
 * The icon PNGs ship under {@code assets/dioxide-lite/textures/icons/}; when either
 * resource is missing we fall back to the vanilla icons so the window still
 * gets a valid icon.
 */
@Mixin(Window.class)
public class WindowMixin {

    @Redirect(
            method = "setIcon",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/IconSet;getStandardIcons(Lnet/minecraft/server/packs/PackResources;)Ljava/util/List;"))
    private List<IoSupplier<InputStream>> DioxideLite$icons(IconSet iconSet, PackResources resources) throws IOException {
        IoSupplier<InputStream> icon16 = () -> DioxideLite.class.getResourceAsStream("/assets/dioxide-lite/textures/icons/icon_16x16.png");
        IoSupplier<InputStream> icon32 = () -> DioxideLite.class.getResourceAsStream("/assets/dioxide-lite/textures/icons/icon_32x32.png");
        IoSupplier<InputStream> icon48 = () -> DioxideLite.class.getResourceAsStream("/assets/dioxide-lite/textures/icons/icon_48x48.png");
        IoSupplier<InputStream> icon64 = () -> DioxideLite.class.getResourceAsStream("/assets/dioxide-lite/textures/icons/icon_64x64.png");
        IoSupplier<InputStream> icon128 = () -> DioxideLite.class.getResourceAsStream("/assets/dioxide-lite/textures/icons/icon_128x128.png");
        try (InputStream stream16 = icon16.get(); InputStream stream32 = icon32.get();
             InputStream stream48 = icon48.get(); InputStream stream64 = icon64.get(); InputStream stream128 = icon128.get()) {
            if (stream16 != null && stream32 != null && stream48 != null && stream64 != null && stream128 != null) {
                DioxideLite.LOGGER.info("Applying {} multi-resolution window/taskbar icon.", DioxideLite.NAME);
                return List.of(icon16, icon32, icon48, icon64, icon128);
            }
        }

        DioxideLite.LOGGER.warn("{} window icon resources are missing, falling back to vanilla icons.", DioxideLite.NAME);
        return iconSet.getStandardIcons(resources);
    }
}
