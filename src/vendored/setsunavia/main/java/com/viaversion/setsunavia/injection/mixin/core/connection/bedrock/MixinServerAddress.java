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

package com.viaversion.setsunavia.injection.mixin.core.connection.bedrock;

import com.viaversion.setsunavia.injection.access.core.bedrock.IServerAddress;
import dev.kastle.netty.channel.nethernet.config.NetherNetAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerAddress.class)
public abstract class MixinServerAddress implements IServerAddress {

    @Unique
    private NetherNetAddress DioxideLiteVia$netherNetAddress;

    @Override
    public NetherNetAddress DioxideLiteVia$getNetherNetAddress() {
        return this.DioxideLiteVia$netherNetAddress;
    }

    @Override
    public void DioxideLiteVia$setNetherNetAddress(final NetherNetAddress address) {
        this.DioxideLiteVia$netherNetAddress = address;
    }

}
