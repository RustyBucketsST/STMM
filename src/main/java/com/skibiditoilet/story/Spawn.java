package com.skibiditoilet.story;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;

/**
 * One kind of mob a mission stage brings in, as enemies or as Alliance allies.
 *
 * @param health             max health for this spawn, or 0 for the mob's normal health
 * @param boss               the stage is won by defeating this one, and it gets a boss bar
 * @param leavesAtHalfHealth leaves the fight at half health instead of dying (G-Toilet before the story lets you beat him)
 * @param heldInPlace        no AI, like the chained toilet in the interrogation
 */
public record Spawn(EntityType<? extends MobEntity> type, int count, float health, boolean boss,
                    boolean leavesAtHalfHealth, boolean heldInPlace) {
    public static Spawn of(EntityType<? extends MobEntity> type, int count) {
        return new Spawn(type, count, 0.0f, false, false, false);
    }

    public static Spawn boss(EntityType<? extends MobEntity> type) {
        return new Spawn(type, 1, 0.0f, true, false, false);
    }

    public Spawn health(float health) {
        return new Spawn(this.type, this.count, health, this.boss, this.leavesAtHalfHealth, this.heldInPlace);
    }

    public Spawn escapes() {
        return new Spawn(this.type, this.count, this.health, this.boss, true, this.heldInPlace);
    }

    public Spawn chained() {
        return new Spawn(this.type, this.count, this.health, this.boss, this.leavesAtHalfHealth, true);
    }
}
