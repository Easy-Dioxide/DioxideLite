package mixins;

import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import com.dioxideliteng.ui.NanoGui;
import com.dioxideliteng.ui.loading.DioxideLiteNGLoadingState;
import com.dioxideliteng.ui.mainmenu.launch.LaunchResources;
import com.dioxideliteng.ui.mainmenu.screen.RockstarTitleScreen;
import com.dioxideliteng.util.render.NVGRenderer;
import com.dioxideliteng.util.render.HudBackdrop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public class MixinGuiRenderer {

   private static boolean logged;
   @Unique private boolean dioxideliteng$islandBehindContainer;

   @Inject(method = "render", at = @At("HEAD"))
   private void dioxideliteng$prepareHudBackdrop(CallbackInfo callbackInfo) {
      this.dioxideliteng$islandBehindContainer = false;
      HudBackdrop.prepare();
      this.dioxideliteng$renderIslandBeforeContainer();
   }

   @Unique
   private void dioxideliteng$renderIslandBeforeContainer() {
      Screen current = Minecraft.getInstance().gui.screen();
      if (!(current instanceof AbstractContainerScreen<?>) || !NVGRenderer.isAvailable()
          || !DynamicIslandManager.shouldRender(current)) return;
      boolean started = NVGRenderer.beginFrame();
      try {
         if (started) {
            // Draw before prepare() renders the inventory avatar into its small PIP target.
            // The container gradient then covers the island without saving a PIP viewport.
            DynamicIslandManager.renderNano();
            this.dioxideliteng$islandBehindContainer = true;
         }
      } finally {
         if (started) {
            NVGRenderer.endFrame();
            NVGRenderer.clearScissors();
         }
      }
   }

   @Inject(method = "close", at = @At("HEAD"))
   private void dioxideliteng$closeHudBackdrop(CallbackInfo callbackInfo) {
      HudBackdrop.close();
   }

   @Inject(method = "endFrame", at = @At("TAIL"))
   private void dioxideliteng$renderNanoGui(CallbackInfo callbackInfo) {
      boolean hud = FeatureManager.hud != null && (FeatureManager.hud.isEnabled() || com.dioxideliteng.ui.hud.editor.HudEditorScreen.active());
      Minecraft mc = Minecraft.getInstance();
      Screen current = mc == null ? null : mc.gui.screen();
      boolean gui = current instanceof NanoGui && NVGRenderer.isAvailable();
      boolean title = current instanceof RockstarTitleScreen && NVGRenderer.isAvailable();
      boolean loading = mc != null && mc.gui.overlay() instanceof LoadingOverlay && NVGRenderer.isAvailable();
      boolean island = !this.dioxideliteng$islandBehindContainer && NVGRenderer.isAvailable() && DynamicIslandManager.shouldRender(current);
      boolean chestOverlay = NVGRenderer.isAvailable() && DynamicIslandManager.hasChestOverlay();
      if (!hud && !gui && !title && !loading && !island && !chestOverlay) {
         if (!logged) {
            logged = true;
            org.slf4j.LoggerFactory.getLogger("dioxideliteng-nvg").info(
               "[dioxideliteng] nano hud skipped (module {})",
               FeatureManager.hud == null ? "null" : "disabled");
         }
         return;
      }

      boolean started = false;
      try {
         started = NVGRenderer.beginFrame();
         if (started) {
            if (hud) {
               FeatureManager.hud.renderNano();
            }
            if (gui) {
               ((NanoGui) current).renderNano();
               if (mc.gui.screen() == current) {
                  if (current instanceof com.dioxideliteng.ui.clickgui.opai.OpaiClickGuiScreen opai) opai.drawHudEditButton();
                  else if (current instanceof com.dioxideliteng.ui.clickgui.RockstarClickGuiScreen modern) modern.drawHudEditButton();
               }
            }
            if (title) {
               ((RockstarTitleScreen) current).renderNano();
            }
            if (loading) {
               dioxideliteng$drawLoading(mc);
            }
            if (island) {
               DynamicIslandManager.renderNano();
            }
            if (chestOverlay) DynamicIslandManager.renderChestOverlay();
            if (current instanceof com.dioxideliteng.ui.hud.editor.HudEditorScreen editor) editor.renderOutline();
         }
         if (!logged) {
            logged = true;
            org.slf4j.LoggerFactory.getLogger("dioxideliteng-nvg").info(
               "[dioxideliteng] nano hud frame started={}, available={}",
               started, NVGRenderer.isAvailable());
         }
      } finally {
         if (started) {
            NVGRenderer.endFrame();
            NVGRenderer.clearScissors();
         }
      }
   }

   private static void dioxideliteng$drawLoading(Minecraft mc) {
      float width = mc.getWindow().getGuiScaledWidth();
      float height = mc.getWindow().getGuiScaledHeight();
      LaunchResources.renderer().loading(width, height,
         DioxideLiteNGLoadingState.done ? 1 : DioxideLiteNGLoadingState.progress, System.nanoTime() / 1_000_000_000.0);
   }
}
