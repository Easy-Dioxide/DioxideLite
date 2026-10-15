package com.dioxideliteng.module.player;

import com.dioxideliteng.module.player.ChestStealer.IslandGeometry;
import com.dioxideliteng.module.player.ChestStealer.IslandView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/**
 * Contract for chest stealers that can render their looting progress on the dynamic
 * island instead of the vanilla container screen. Implemented by the native
 * {@link ChestStealer} and by the Krs-ported {@link ChestStealer2}; the island data
 * types are shared so {@code DynamicIslandManager} can drive either module.
 */
public interface ChestIslandSource {

   /** True while this module owns {@code screen} and the island should replace the vanilla chest UI. */
   boolean replacesContainer(Screen screen);

   /** Snapshot of the chest contents plus transfer feedback for the island panel. */
   IslandView islandView(Screen screen, long now, boolean reducedMotion);

   /** Draws the island shell and the chest item grid between the vanilla dimmer and native items. */
   void extractIslandItems(GuiGraphicsExtractor graphics, IslandView view, IslandGeometry geometry);
}
