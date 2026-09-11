package com.dioxidelite.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static KeyMapping openSettings;

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("dioxide_lite", "dioxide_lite")
        );

        openSettings = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.dioxide_lite.open_settings",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));
    }
}