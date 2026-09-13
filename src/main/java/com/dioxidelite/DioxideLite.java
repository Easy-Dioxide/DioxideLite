package com.dioxidelite;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DioxideLite {

    public static final String MOD_ID = "dioxide-lite";
    public static final String NAME = "DioxideLite";
    public static final String VERSION = "2.0.4";
    public static final String DEVELOPER = "DioxideLite";
    public static final String CREDITS =  DEVELOPER;
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private DioxideLite() {
    }


    public static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
