package com.skibiditoilet.entity;

import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * The Hat Skibidi Toilet. Leaps high at its target and comes down with a slam that hurts and throws back
 * everyone around where it lands. Fights normally up close.
 */
public class HatToiletEntity extends SkibidiToiletEntity {
    private static final int LEAP_COOLDOWN_TICKS = 80;
    private static final double SLAM_RANGE = 3.0;
    private static final float SLAM_DAMAGE = 6.0f;

    private int leapCooldown = LEAP_COOLDOWN_TICKS / 2;

    public HatToiletEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void addAttackGoals() {
        this.goalSelector.add(1, new LeapSlamGoal(this));
        super.addAttackGoals();
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.leapCooldown > 0) {
            this.leapCooldown--;
        }
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    private void slam() {
        ServerWorld world = (ServerWorld) this.getWorld();
        for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(SLAM_RANGE),
                SkibidiToiletEntity::isToiletEnemy)) {
            if (victim.damage(this.getDamageSources().mobAttack(this), SLAM_DAMAGE)) {
                Vec3d away = victim.getPos().subtract(this.getPos()).normalize();
                victim.takeKnockback(1.2, -away.x, -away.z);
                victim.addVelocity(0.0, 0.35, 0.0);
            }
        }
        ModFx.shockwave(world, this.getPos(), (float) SLAM_RANGE, 0.55f);
        world.playSound(null, this.getBlockPos(), ModSounds.HAT_TOILET_SLAM, SoundCategory.HOSTILE, 1.5f, 1.0f);
    }

    /** Jumps at a target a few blocks away and slams down on landing. */
    static class LeapSlamGoal extends Goal {
        private final HatToiletEntity toilet;
        private int ticks;

        LeapSlamGoal(HatToiletEntity toilet) {
            this.toilet = toilet;
            this.setControls(EnumSet.of(Control.MOVE, Control.JUMP));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = this.toilet.getTarget();
            if (target == null || !target.isAlive() || this.toilet.leapCooldown > 0 || !this.toilet.isOnGround()) {
                return false;
            }
            double distanceSquared = this.toilet.squaredDistanceTo(target);
            return distanceSquared > 4.0 * 4.0 && distanceSquared < 14.0 * 14.0 && this.toilet.getVisibilityCache().canSee(target);
        }

        @Override
        public void start() {
            LivingEntity target = this.toilet.getTarget();
            Vec3d toward = target.getPos().subtract(this.toilet.getPos());
            Vec3d horizontal = new Vec3d(toward.x, 0.0, toward.z);
            // Roughly enough forward speed to land on the target with this much lift.
            Vec3d push = horizontal.normalize().multiply(Math.min(1.4, horizontal.length() * 0.12));
            this.toilet.setVelocity(push.x, 0.85, push.z);
            this.toilet.velocityDirty = true;
            this.toilet.getNavigation().stop();
            this.toilet.playSound(SoundEvents.ENTITY_RAVAGER_STEP, 1.0f, 1.4f);
            this.toilet.leapCooldown = LEAP_COOLDOWN_TICKS;
            this.ticks = 0;
        }

        @Override
        public boolean shouldContinue() {
            return this.ticks < 60;
        }

        @Override
        public void tick() {
            LivingEntity target = this.toilet.getTarget();
            if (target != null) {
                this.toilet.getLookControl().lookAt(target, 30.0f, 30.0f);
            }
            // A few ticks in, touching the ground again means it has landed.
            if (++this.ticks > 4 && this.toilet.isOnGround()) {
                this.toilet.slam();
                this.ticks = 60;
            }
        }
    }
}
