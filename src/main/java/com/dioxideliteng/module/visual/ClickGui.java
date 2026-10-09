package com.dioxideliteng.module.visual;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.ui.clickgui.ClickGuiScreen;
import com.dioxideliteng.ui.clickgui.neverlose.NeverloseClickGuiScreen;
import com.dioxideliteng.ui.clickgui.opai.OpaiClickGuiScreen;
import com.dioxideliteng.ui.clickgui.opai.OpaiStyle;
import com.dioxideliteng.ui.clickgui.RockstarClickGuiScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public class ClickGui extends Feature {
   private static final String CLICK_GUI_LABEL = "ClickGUI";

   public final ModeSetting style = new ModeSetting("Style", this, "Opai", new String[]{"Modern", "Opai", "Neverlose"});
   public final ModeSetting renderMode = new ModeSetting("Interface", this, "LiquidGlass", new String[]{"LiquidGlass", "Normal"});
   public final ModeSetting opaiColor = new ModeSetting("Opai Color", this, "Lavender", new String[]{"Lavender", "Light Pink"});

   private Screen activeScreen;
   private OpaiClickGuiScreen opaiScreen;
   private NeverloseClickGuiScreen neverloseScreen;

   /** HUDs read the saved ClickGUI choice every frame, even while the GUI is closed. */
   public static OpaiStyle.Palette currentOpaiPalette() {
      ClickGui gui = FeatureManager.clickGui;
      return gui == null ? OpaiStyle.LAVENDER : OpaiStyle.palette(gui.opaiColor.getValue());
   }

   @Override
   public void onEnable() {
      if (this.style.is("Opai")) {
         if (this.opaiScreen == null) this.opaiScreen = new OpaiClickGuiScreen(this.opaiColor);
         this.activeScreen = this.opaiScreen;
      } else if (this.style.is("Neverlose")) {
         if (this.neverloseScreen == null) this.neverloseScreen = new NeverloseClickGuiScreen();
         this.activeScreen = this.neverloseScreen;
      } else {
         ClickGuiScreen modern = new ClickGuiScreen(RockstarClickGuiScreen.Style.MODERN);
         modern.setRenderMode(this.renderMode);
         this.activeScreen = modern;
      }

      final Screen screen = this.activeScreen;
      mc.gui.setScreen(screen);
      // Re-assert the screen on the next client tick. Opening from the chat command
      // closes the chat screen right after, which would otherwise override this screen.
      net.minecraft.client.Minecraft mcInst = mc;
      mcInst.execute(() -> {
         if (mcInst.gui.screen() == null) {
            mcInst.gui.setScreen(screen);
         }
      });
      this.setEnabled(false);
   }

   public ClickGui() {
      super(CLICK_GUI_LABEL, GLFW.GLFW_KEY_RIGHT_SHIFT, Category.VISUAL);
      this.renderMode.setVisible(() -> this.style.is("Modern"));
      this.opaiColor.setVisible(() -> this.style.is("Opai"));
   }
}
