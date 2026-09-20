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

package com.viaversion.setsunavia.injection.mixin.core.access;

import com.viaversion.setsunavia.injection.access.core.ILocalSampleLogger;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.util.debugchart.LocalSampleLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LocalSampleLogger.class)
public abstract class MixinLocalSampleLogger implements ILocalSampleLogger {

    @Unique
    private ProtocolVersion DioxideLiteVia$forcedVersion;

    @Override
    public ProtocolVersion DioxideLiteVia$getForcedVersion() {
        return this.DioxideLiteVia$forcedVersion;
    }

    @Override
    public void DioxideLiteVia$setForcedVersion(ProtocolVersion version) {
        this.DioxideLiteVia$forcedVersion = version;
    }

}
