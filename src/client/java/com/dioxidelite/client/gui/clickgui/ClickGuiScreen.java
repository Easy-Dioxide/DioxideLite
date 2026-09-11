package com.dioxidelite.client.gui.clickgui;

/**
 * Marker interface for every DioxideLite ClickGUI screen.
 *
 * HUD renderers and mixins use this instead of the removed SkiaScreen type to
 * detect that a ClickGUI is open (and hide the in-game HUD while it is shown).
 */
public interface ClickGuiScreen {
}
