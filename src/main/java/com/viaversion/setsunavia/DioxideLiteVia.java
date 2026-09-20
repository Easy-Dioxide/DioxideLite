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

package com.viaversion.setsunavia;

import com.viaversion.setsunavia.api.DioxideLiteViaBase;
import org.jetbrains.annotations.ApiStatus;

/**
 * Holder class for the {@link DioxideLiteViaBase} implementation
 */
public final class DioxideLiteVia {

    private static DioxideLiteViaBase impl;

    @ApiStatus.Internal
    public static void init(final DioxideLiteViaBase impl) {
        if (DioxideLiteVia.impl != null) {
            throw new IllegalStateException("DioxideLiteVia has already been initialized!");
        }
        DioxideLiteVia.impl = impl;
    }

    /**
     * @return the DioxideLiteViaBase implementation which is set by the internals
     */
    public static DioxideLiteViaBase getImpl() {
        if (impl == null) {
            throw new IllegalStateException("DioxideLiteVia has not been initialized yet!");
        }
        return impl;
    }

}
