/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.combat;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;

@ConfigCategory(Category.COMBAT)
public class CustomLootrunBeaconsFeature extends Feature {
    @Persisted
    public final Config<Boolean> removeOriginalBeacons = new Config<>(true);
}
