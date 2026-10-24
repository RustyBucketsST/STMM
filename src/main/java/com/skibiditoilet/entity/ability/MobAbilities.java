package com.skibiditoilet.entity.ability;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

import java.util.List;

/**
 * A mob's abilities, ticked from its mobTick. One goes at a time: while one charges the others wait, and after one
 * goes off there's a short gap before the next.
 */
public final class MobAbilities<T extends MobEntity> {
    private static final int GAP = 20;

    private final List<MobAbility<T>> abilities;
    private int gap;

    @SafeVarargs
    public MobAbilities(MobAbility<T>... abilities) {
        this.abilities = List.of(abilities);
    }

    public void tick(T mob) {
        LivingEntity target = mob.getTarget();
        for (MobAbility<T> ability : this.abilities) {
            if (ability.isCharging()) {
                ability.chargeTick(mob, target);
                if (!ability.isCharging()) {
                    this.gap = GAP;
                }
                return;
            }
        }
        for (MobAbility<T> ability : this.abilities) {
            ability.cool(mob);
        }
        if (this.gap > 0) {
            this.gap--;
            return;
        }
        if (target == null || !target.isAlive() || mob.isAiDisabled()) {
            return;
        }
        for (MobAbility<T> ability : this.abilities) {
            if (ability.tryStart(mob, target)) {
                if (!ability.isCharging()) {
                    this.gap = GAP;
                }
                return;
            }
        }
    }
}
