/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.spells.event;

import com.wynntils.models.spells.type.PartialSpellSource;
import com.wynntils.models.spells.type.SpellDirection;
import com.wynntils.models.spells.type.SpellFailureReason;
import com.wynntils.models.spells.type.SpellType;
import net.minecraftforge.eventbus.api.Event;

public abstract class SpellEvent extends Event {
    public abstract static class Casting extends SpellEvent {
        private final SpellDirection[] spellDirectionArray;
        private final PartialSpellSource partialSpellSource;

        protected Casting(SpellDirection[] spellDirectionArray, PartialSpellSource partialSpellSource) {
            this.spellDirectionArray = spellDirectionArray;
            this.partialSpellSource = partialSpellSource;
        }

        public SpellDirection[] getSpellDirectionArray() {
            return spellDirectionArray.clone();
        }

        public PartialSpellSource getSource() {
            return partialSpellSource;
        }
    }

    /**
     * Fired upon user inputting the next click in a sequence to cast a spell.
     */
    public static final class Partial extends Casting {
        public Partial(SpellDirection[] spellDirectionArray, PartialSpellSource partialSpellSource) {
            super(spellDirectionArray, partialSpellSource);
        }
    }

    /**
     * Fired upon successful, completed spell cast.
     * This event fires upon every three-click sequence, no matter if the spell was actually casted or not.
     */
    public static final class Completed extends Casting {
        private final SpellType spell;

        public Completed(SpellDirection[] spellDirectionArray, PartialSpellSource partialSpellSource, SpellType spell) {
            super(spellDirectionArray, partialSpellSource);
            this.spell = spell;
        }

        public SpellType getSpell() {
            return spell;
        }
    }

    public static final class Cast extends SpellEvent {
        private final SpellType spellType;
        private final int manaCost;

        public Cast(SpellType spellType, int manaCost) {
            this.spellType = spellType;
            this.manaCost = manaCost;
        }

        public SpellType getSpellType() {
            return spellType;
        }

        public int getManaCost() {
            return manaCost;
        }
    }

    public static final class Failed extends SpellEvent {
        private final SpellFailureReason failureReason;

        public Failed(SpellFailureReason failureReason) {
            this.failureReason = failureReason;
        }

        public SpellFailureReason getFailureReason() {
            return failureReason;
        }
    }
}
