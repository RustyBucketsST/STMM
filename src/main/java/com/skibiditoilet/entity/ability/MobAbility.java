package com.skibiditoilet.entity.ability;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

/**
 * Something a mob does every so often on top of its normal attack: a shot, a blast, a leap. It's used when it's off
 * cooldown and its condition holds for the mob's target (which the mob has to see); some charge up first, facing the
 * target, and go off at the end. Each mob gets its own instances (see MobAbilities), since they keep its cooldowns.
 */
public final class MobAbility<T extends MobEntity> {
    @FunctionalInterface
    public interface Condition<T> {
        boolean test(T mob, LivingEntity target, double distance);
    }

    @FunctionalInterface
    public interface Action<T> {
        void run(T mob, LivingEntity target);
    }

    @FunctionalInterface
    public interface Charge<T> {
        /** One tick of charging; {@code left} counts down to 1. */
        void tick(T mob, LivingEntity target, int left);
    }

    private final int cooldown;
    private final Condition<T> condition;
    private final Action<T> action;
    private int windup;
    private Charge<T> charge = (mob, target, left) -> {
    };
    /** Ticks until it can be used: at first a short random wait, so mobs that meet a toilet together don't all go at once. */
    private int wait = -1;
    private int charging;

    private MobAbility(int cooldown, Condition<T> condition, Action<T> action) {
        this.cooldown = cooldown;
        this.condition = condition;
        this.action = action;
    }

    /** Used every {@code cooldown} ticks at most, when {@code condition} holds. */
    public static <T extends MobEntity> MobAbility<T> of(int cooldown, Condition<T> condition, Action<T> action) {
        return new MobAbility<>(cooldown, condition, action);
    }

    /** Charges for {@code ticks} first, calling {@code charge} each tick (for the glow, the hum). */
    public MobAbility<T> windup(int ticks, Charge<T> charge) {
        this.windup = ticks;
        this.charge = charge;
        return this;
    }

    boolean isCharging() {
        return this.charging > 0;
    }

    /** Counts down the cooldown. */
    void cool(T mob) {
        if (this.wait < 0) {
            this.wait = 10 + mob.getRandom().nextInt(this.cooldown / 3 + 1);
        } else if (this.wait > 0) {
            this.wait--;
        }
    }

    /** Starts it (or uses it, if it has no windup) if it's ready and the condition holds. Returns whether it did. */
    boolean tryStart(T mob, LivingEntity target) {
        if (this.wait != 0 || !mob.canSee(target) || !this.condition.test(mob, target, mob.distanceTo(target))) {
            return false;
        }
        if (this.windup > 0) {
            this.charging = this.windup;
            this.chargeTick(mob, target);
        } else {
            this.action.run(mob, target);
            this.wait = this.cooldown;
        }
        return true;
    }

    /** One tick of charging; goes off at the end. Stops if the target is gone. */
    void chargeTick(T mob, LivingEntity target) {
        if (target == null || !target.isAlive()) {
            this.charging = 0;
            this.wait = this.cooldown / 2;
            return;
        }
        mob.getLookControl().lookAt(target, 30.0f, 30.0f);
        this.charge.tick(mob, target, this.charging);
        if (--this.charging == 0) {
            this.action.run(mob, target);
            this.wait = this.cooldown;
        }
    }
}
