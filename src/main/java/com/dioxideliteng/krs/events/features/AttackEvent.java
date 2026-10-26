package com.dioxideliteng.krs.events.features;

import com.dioxideliteng.krs.events.EventArgument;
import com.dioxideliteng.krs.events.EventListener;
import java.util.Objects;
import net.minecraft.world.entity.Entity;

public class AttackEvent extends EventArgument {
    public final Entity entity;

    public AttackEvent(Entity entity) {
        this.entity = entity;
    }

    @Override
    public void call(EventListener listener) {
        Objects.requireNonNull(listener).onAttack(this);
    }
}