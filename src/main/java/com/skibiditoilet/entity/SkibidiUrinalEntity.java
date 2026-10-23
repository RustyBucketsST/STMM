package com.skibiditoilet.entity;

import com.skibiditoilet.registry.ModDamageTypes;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * A Skibidi Urinal. It can't move, so it works like a turret: it spits water at anyone in range, which
 * slows them down, and bites whoever gets right up close.
 */
public class SkibidiUrinalEntity extends SkibidiToiletEntity {
    private static final double SPIT_RANGE = 14.0;
    private static final float SPIT_DAMAGE = 2.0f;
    private static final int SPIT_COOLDOWN_TICKS = 30;
    private static final int SLOWNESS_TICKS = 40;
    private static final double BITE_RANGE = 2.5;

    public SkibidiUrinalEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 14.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void addAttackGoals() {
        this.goalSelector.add(2, new SpitGoal(this));
    }

    @Override
    protected void addWanderGoal() {
        // Fixed in place.
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushAway(Entity entity) {
    }

    /** Spits from range and bites up close, turning to face the target. */
    static class SpitGoal extends Goal {
        private final SkibidiUrinalEntity urinal;
        private int cooldown;

        SpitGoal(SkibidiUrinalEntity urinal) {
            this.urinal = urinal;
            this.setControls(EnumSet.of(Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = this.urinal.getTarget();
            return target != null && target.isAlive() && this.urinal.canTarget(target);
        }

        @Override
        public void start() {
            this.cooldown = SPIT_COOLDOWN_TICKS / 2;
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.urinal.getTarget();
            if (target == null) {
                return;
            }
            this.urinal.getLookControl().lookAt(target, 30.0f, 30.0f);
            this.urinal.setBodyYaw(this.urinal.getHeadYaw());
            if (--this.cooldown > 0 || !this.urinal.getVisibilityCache().canSee(target)) {
                return;
            }
            double distanceSquared = this.urinal.squaredDistanceTo(target);
            if (distanceSquared < BITE_RANGE * BITE_RANGE) {
                this.urinal.swingHand(this.urinal.getActiveHand());
                this.urinal.tryAttack(target);
                this.cooldown = 20;
            } else if (distanceSquared < SPIT_RANGE * SPIT_RANGE) {
                Vec3d aim = target.getBoundingBox().getCenter().subtract(this.urinal.getEyePos());
                Entity hit = LaserBeam.spray((ServerWorld) this.urinal.getWorld(), this.urinal, aim, SPIT_RANGE, SPIT_DAMAGE,
                        ModDamageTypes.spit(this.urinal.getWorld(), this.urinal));
                if (hit instanceof LivingEntity living) {
                    living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, SLOWNESS_TICKS), this.urinal);
                }
                this.urinal.playSound(ModSounds.SKIBIDI_URINAL_SPIT, 1.0f, 0.9f + this.urinal.getRandom().nextFloat() * 0.2f);
                this.cooldown = SPIT_COOLDOWN_TICKS;
            }
        }
    }
}
