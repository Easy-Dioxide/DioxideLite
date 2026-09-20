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

package com.viaversion.setsunavia.protocoltranslator.impl.viaversion;

import com.viaversion.setsunavia.DioxideLiteViaImpl;
import com.viaversion.setsunavia.api.events.LoadingCycleCallback;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.viabedrock.DioxideLiteViaNettyPipelineProvider;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.vialegacy.DioxideLiteViaAlphaInventoryProvider;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.vialegacy.DioxideLiteViaClassicMPPassProvider;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.vialegacy.DioxideLiteViaClassicWorldHeightProvider;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.vialegacy.DioxideLiteViaEncryptionProvider;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.vialegacy.DioxideLiteViaGameProfileFetcher;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.vialegacy.DioxideLiteViaOldAuthProvider;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.viaversion.DioxideLiteViaAckSequenceProvider;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.viaversion.DioxideLiteViaBaseVersionProvider;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.viaversion.DioxideLiteViaCommandArgumentsProvider;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.viaversion.DioxideLiteViaCompressionProvider;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.viaversion.DioxideLiteViaHandItemProvider;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.viaversion.DioxideLiteViaPickItemProvider;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.viaversion.DioxideLiteViaPlayerAbilitiesProvider;
import com.viaversion.setsunavia.protocoltranslator.impl.provider.viaversion.DioxideLiteViaPlayerLookTargetProvider;
import com.viaversion.setsunavia.settings.impl.GeneralSettings;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.minecraft.signature.SignableCommandArgumentsProvider;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.api.platform.providers.ViaProviders;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.provider.PlayerLookTargetProvider;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.provider.PlayerAbilitiesProvider;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.provider.AckSequenceProvider;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.provider.PickItemProvider;
import com.viaversion.viaversion.protocols.v1_8to1_9.provider.CompressionProvider;
import com.viaversion.viaversion.protocols.v1_8to1_9.provider.HandItemProvider;
import net.raphimc.viabedrock.protocol.provider.NettyPipelineProvider;
import net.raphimc.vialegacy.protocol.alpha.a1_2_3_5_1_2_6tob1_0_1_1_1.provider.AlphaInventoryProvider;
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.provider.ClassicMPPassProvider;
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.provider.ClassicWorldHeightProvider;
import net.raphimc.vialegacy.protocol.release.r1_2_4_5tor1_3_1_2.provider.OldAuthProvider;
import net.raphimc.vialegacy.protocol.release.r1_6_4tor1_7_2_5.provider.EncryptionProvider;
import net.raphimc.vialegacy.protocol.release.r1_7_6_10tor1_8.provider.GameProfileFetcher;

public final class DioxideLiteViaPlatformLoader implements ViaPlatformLoader {

    @Override
    public void load() {
        final ViaProviders providers = Via.getManager().getProviders();

        providers.use(VersionProvider.class, new DioxideLiteViaBaseVersionProvider());
        providers.use(HandItemProvider.class, new DioxideLiteViaHandItemProvider());
        providers.use(PlayerLookTargetProvider.class, new DioxideLiteViaPlayerLookTargetProvider());
        providers.use(PlayerAbilitiesProvider.class, new DioxideLiteViaPlayerAbilitiesProvider());
        providers.use(SignableCommandArgumentsProvider.class, new DioxideLiteViaCommandArgumentsProvider());
        providers.use(AckSequenceProvider.class, new DioxideLiteViaAckSequenceProvider());
        providers.use(PickItemProvider.class, new DioxideLiteViaPickItemProvider());
        providers.use(CompressionProvider.class, new DioxideLiteViaCompressionProvider());

        providers.use(OldAuthProvider.class, new DioxideLiteViaOldAuthProvider());
        providers.use(ClassicWorldHeightProvider.class, new DioxideLiteViaClassicWorldHeightProvider());
        providers.use(EncryptionProvider.class, new DioxideLiteViaEncryptionProvider());
        providers.use(GameProfileFetcher.class, new DioxideLiteViaGameProfileFetcher());
        providers.use(ClassicMPPassProvider.class, new DioxideLiteViaClassicMPPassProvider());
        if (GeneralSettings.INSTANCE.emulateInventoryActionsInAlphaVersions.getValue()) {
            providers.use(AlphaInventoryProvider.class, new DioxideLiteViaAlphaInventoryProvider());
        }

        providers.use(NettyPipelineProvider.class, new DioxideLiteViaNettyPipelineProvider());

        DioxideLiteViaImpl.LOADING_CYCLE.invoker().onLoadCycle(LoadingCycleCallback.LoadingCycle.POST_VIAVERSION_LOAD);
    }

    @Override
    public void unload() {
    }

}
