/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.character.type;

import net.minecraft.world.item.ItemStack;

public record ClassInfo(
        String name,
        ItemStack itemStack,
        int slot,
        ClassType classType,
        int level,
        int xp,
        int soulPoints,
        int completedQuests) {}
