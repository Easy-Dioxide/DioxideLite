package com.dioxideliteng.util.krs;

/**
 * Port of Krs GuiEntityRenderGuard
 * (com.instrumentalist.krs.utils.render.GuiEntityRenderGuard) - verbatim.
 *
 * <p>Depth counter that marks GUI picture-in-picture entity rendering (the
 * inventory paper-doll). The rotation render hooks must not apply the spoofed
 * rotation while an entity is rendered into a GUI texture.
 */
public final class GuiEntityRenderGuard {
   private static int depth = 0;

   private GuiEntityRenderGuard() {
   }

   public static void begin() {
      depth++;
   }

   public static void end() {
      if (depth > 0)
         depth--;
   }

   public static boolean isActive() {
      return depth > 0;
   }

   public static void run(Runnable action) {
      begin();
      try {
         action.run();
      } finally {
         end();
      }
   }
}
