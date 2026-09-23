/*
 * This file is part of DioxideLiteVia - https://github.com/ViaVersion/DioxideLiteVia
 * Copyright (C) 2021-2026 the original authors
 *                         - Florian Reuth <git@florianreuth.de>
 *                         - RK_01/RaphiMC
 * Copyright (C) 2023-2026 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.viaversion.dioxidelitevia.settings.impl;

import com.viaversion.dioxidelitevia.api.settings.SettingGroup;
import com.viaversion.dioxidelitevia.api.settings.type.BooleanSetting;
import com.viaversion.dioxidelitevia.api.settings.type.ModeSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class GeneralSettings extends SettingGroup {

    public static final GeneralSettings INSTANCE = new GeneralSettings();

    private final MutableComponent[] ORIENTATION_OPTIONS = new MutableComponent[]{
        Component.translatable("base.dioxidelitevia.none"),
        Component.translatable("base.dioxidelitevia.left_top"),
        Component.translatable("base.dioxidelitevia.right_top"),
        Component.translatable("base.dioxidelitevia.left_bottom"),
        Component.translatable("base.dioxidelitevia.right_bottom")
    };

    public final ModeSetting multiplayerScreenButtonOrientation = new ModeSetting(this, Component.translatable("general_settings.dioxidelitevia.multiplayer_screen_button_orientation"), 2, ORIENTATION_OPTIONS);
    public final ModeSetting addServerScreenButtonOrientation = new ModeSetting(this, Component.translatable("general_settings.dioxidelitevia.add_server_screen_button_orientation"), 2, ORIENTATION_OPTIONS);
    public final ModeSetting directConnectScreenButtonOrientation = new ModeSetting(this, Component.translatable("general_settings.dioxidelitevia.direct_connect_screen_button_orientation"), 2, ORIENTATION_OPTIONS);
    public final ModeSetting removeNotAvailableItemsFromCreativeTab = new ModeSetting(this, Component.translatable("general_settings.dioxidelitevia.filter_creative_tabs"),
        Component.translatable("base.dioxidelitevia.vanilla_and_modded"),
        Component.translatable("base.dioxidelitevia.vanilla_only"),
        Component.translatable("base.dioxidelitevia.off")
    );
    public final BooleanSetting saveSelectedProtocolVersion = new BooleanSetting(this, Component.translatable("general_settings.dioxidelitevia.save_selected_protocol_version"), true);
    public final BooleanSetting showClassicLoadingProgressInConnectScreen = new BooleanSetting(this, Component.translatable("general_settings.dioxidelitevia.show_classic_loading_progress"), true);
    public final BooleanSetting showAdvertisedServerVersion = new BooleanSetting(this, Component.translatable("general_settings.dioxidelitevia.show_advertised_server_version"), true);
    public final ModeSetting ignorePacketTranslationErrors = new ModeSetting(this, Component.translatable("general_settings.dioxidelitevia.ignore_packet_translation_errors"),
        Component.translatable("base.dioxidelitevia.kick"),
        Component.translatable("base.dioxidelitevia.cancel_and_notify"),
        Component.translatable("base.dioxidelitevia.cancel")
    );
    public final BooleanSetting loadSkinsAndSkullsInLegacyVersions = new BooleanSetting(this, Component.translatable("general_settings.dioxidelitevia.load_skins_and_skulls_in_legacy_versions"), true);
    public final BooleanSetting emulateInventoryActionsInAlphaVersions = new BooleanSetting(this, Component.translatable("general_settings.dioxidelitevia.emulate_inventory_actions_in_alpha_versions"), true);
    public final BooleanSetting saveScrollPositionInSlotScreens = new BooleanSetting(this, Component.translatable("general_settings.dioxidelitevia.save_scroll_position_in_slot_screens"), true);
    public final BooleanSetting experimentalBlockConnections = new BooleanSetting(this, Component.translatable("general_settings.dioxidelitevia.experimental_block_connections"), false);

    public GeneralSettings() {
        super(Component.translatable("setting_group_name.dioxidelitevia.general"));
        emulateInventoryActionsInAlphaVersions.lockValue();
        experimentalBlockConnections.lockValue();
    }

    public static void setOrientation(final Position position, final int orientationIndex, final int width, final int height) {
        switch (orientationIndex) {
            case 1 -> position.setPosition(5, 5);
            case 2 -> position.setPosition(width - 98 - 5, 5);
            case 3 -> position.setPosition(5, height - 20 - 5);
            case 4 -> position.setPosition(width - 98 - 5, height - 20 - 5);
        }
    }

    @FunctionalInterface
    public interface Position {

        void setPosition(int x, int y);

    }

}
