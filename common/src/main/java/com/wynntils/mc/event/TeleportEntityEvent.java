/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;

public class TeleportEntityEvent extends Event {
    private final Entity entity;
    private final Vec3 newPosition;

    public TeleportEntityEvent(Entity entity, Vec3 newPosition) {
        this.entity = entity;
        this.newPosition = newPosition;
    }

    public Entity getEntity() {
        return entity;
    }

    public Vec3 getNewPosition() {
        return newPosition;
    }
}
