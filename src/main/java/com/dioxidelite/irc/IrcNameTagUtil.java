package com.dioxidelite.irc;

import com.dioxidelite.module.modules.player.IrcModule;
import com.dioxidelite.module.modules.render.LegendWatch;
import com.dioxidelite.util.legendwatch.LegendSuffixUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * Prepends the DioxideLite logo glyph to name tags of players who are
 * currently online on the IRC link. The glyph is a small bitmap font entry
 * ({@code dioxide-lite:nametag_logo}) so the vanilla name-tag pipeline can
 * render it without any custom GL code.
 */
public final class IrcNameTagUtil {

    public static final char LOGO_GLYPH = '\uE101';

    private static final FontDescription.Resource NAMETAG_LOGO_FONT =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("dioxide-lite", "nametag_logo"));

    private IrcNameTagUtil() {
    }

    /**
     * Returns the original component, optionally prefixed with the client logo
     * when {@code rawName} belongs to an IRC-online user (including the local
     * player, so their own name tag shows the logo in third person too).
     */
    public static Component appendLogoIfIrc(Component original, String rawName) {
        String clean = LegendSuffixUtil.cleanUsername(rawName);
        if (clean.isEmpty() || !IrcModule.isIrcUser(clean)) {
            return original;
        }
        // IRC logo lives on the client's name-tag module (LegendWatch → Render
        // category); toggleable there, on by default.
        if (!LegendWatch.INSTANCE.ircLogoEnabled()) {
            return original;
        }
        Component logo = Component.literal(String.valueOf(LOGO_GLYPH))
                .withStyle(style -> style.withFont(NAMETAG_LOGO_FONT).withColor(0xFFFFFFFF));
        return Component.empty()
                .append(logo)
                .append(Component.literal(" "))
                .append(original);
    }
}
