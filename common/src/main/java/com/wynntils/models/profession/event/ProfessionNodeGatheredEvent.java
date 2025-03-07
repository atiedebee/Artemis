/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.profession.event;

import net.minecraftforge.eventbus.api.Event;

public abstract class ProfessionNodeGatheredEvent extends Event {
    public static class LabelShown extends ProfessionNodeGatheredEvent {
        private boolean addCooldownArmorstand = false;

        public boolean shouldAddCooldownArmorstand() {
            return addCooldownArmorstand;
        }

        public void setAddCooldownArmorstand(boolean addCooldownArmorstand) {
            this.addCooldownArmorstand = addCooldownArmorstand;
        }
    }
}
