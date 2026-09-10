package com.dioxidelite;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DioxideLite implements ModInitializer {
	public static final String MOD_ID = "dioxide-lite";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info("Welcome to use DioxideLite!");
	}
}