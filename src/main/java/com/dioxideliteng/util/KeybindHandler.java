package com.dioxideliteng.util;

import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLScancode;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler implements Wrapper {
   private static final boolean[] pressedKeys = new boolean[GLFW.GLFW_KEY_LAST + 1];

   public static void updateKeybinds() {
      if (Wrapper.mc.gui.screen() == null) {
         // Pump SDL events so SDL_GetKeyboardState reflects real key presses.
         try {
            SDLEvents.SDL_PumpEvents();
         } catch (Throwable ignored) {
         }
         java.nio.ByteBuffer state = SDLKeyboard.SDL_GetKeyboardState();

         for (Feature feature : FeatureManager.getModules()) {
            int keyCode = feature.getKey();
            if (keyCode != -1 && keyCode != 0) {
               int scancode = keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT ? SDLScancode.SDL_SCANCODE_RSHIFT : keyCode;
               boolean pressed = scancode >= 0 && scancode < state.limit() && state.get(scancode) != 0;
               if (!pressed) {
                  pressedKeys[keyCode] = false;
               } else if (!pressedKeys[keyCode]) {
                  for (Feature boundFeature : FeatureManager.getModules()) {
                     if (boundFeature.getKey() == keyCode) {
                        boundFeature.toggle();
                     }
                  }

                  pressedKeys[keyCode] = true;
               }
            }
         }
      }
   }
}
