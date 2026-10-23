package com.skibiditoilet.entity;

import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The Fast Armored Skibidi Toilet: a toilet sealed in a huge black metal box. Its armor shrugs off most
 * hits. It revs up, then charges in a straight line at terrible speed, flattening everything in its path.
 * After a charge it needs a moment to recover, and its armor is open while it does: that's when to hit it.
 */
public class FastArmoredToiletEntity extends SkibidiToiletEntity {
    public static final float SCALE = 2.5f;
    private static final TrackedData<Boolean> CHARGING = DataTracker.registerData(FastArmoredToiletEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);
    private static final UUID OPEN_ARMOR = UUID.fromString("9d4f2b6e-1c3a-4e8b-a7d5-6f0e2c1b3a44");
    private static final int WINDUP_TICKS = 20;
    private static final int CHARGE_TICKS = 30;
    private static final int RECOVER_TICKS = 60;
    private static final int COOLDOWN_TICKS = 60;
    private static final double CHARGE_SPEED = 0.9;
    private static final float CHARGE_DAMAGE = 12.0f;

    public FastArmoredToiletEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.setStepHeight(1.0f);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 90.0)
                .add(EntityAttributes.GENERIC_ARMOR, 16.0)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 6.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.18)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CHARGING, false);
    }

    /** Revving up or charging; the renderer shakes it. */
    public boolean isCharging() {
        return this.dataTracker.get(CHARGING);
    }

    @Override
    protected void addAttackGoals() {
        this.goalSelector.add(1, new ChargeGoal(this));
        super.addAttackGoals();
    }

    @Override
    public boolean flushesOnDeath() {
        // A metal box: it falls over and bursts open in smoke instead.
        return false;
    }

    @Override
    public void handleStatus(byte status) {
        if (status == 60) {
            for (int i = 0; i < 6; i++) {
                this.getWorld().addParticle(ParticleTypes.EXPLOSION, this.getParticleX(0.8), this.getRandomBodyY(),
                        this.getParticleZ(0.8), 0.0, 0.0, 0.0);
            }
        }
        super.handleStatus(status);
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    private void setArmorOpen(boolean open) {
        EntityAttributeInstance armor = this.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
        if (armor == null) {
            return;
        }
        // tryRemoveModifier only removes saved modifiers; this one is temporary.
        armor.removeModifier(OPEN_ARMOR);
        if (open) {
            armor.addTemporaryModifier(new EntityAttributeModifier(OPEN_ARMOR, "Recovering", -1.0,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    /** Rev up, charge in a straight line, then sit still with the armor open for a few seconds. */
    static class ChargeGoal extends Goal {
        private enum Phase {
            WINDUP, CHARGE, RECOVER
        }

        private final FastArmoredToiletEntity toilet;
        private final Set<LivingEntity> alreadyHit = new HashSet<>();
        private Phase phase;
        private int ticks;
        private int cooldown = COOLDOWN_TICKS;
        private Vec3d direction = Vec3d.ZERO;

        ChargeGoal(FastArmoredToiletEntity toilet) {
            this.toilet = toilet;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
        }

        @Override
        public boolean canStart() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            LivingEntity target = this.toilet.getTarget();
            if (target == null || !target.isAlive() || !this.toilet.isOnGround()) {
                return false;
            }
            double distanceSquared = this.toilet.squaredDistanceTo(target);
            return distanceSquared > 4.0 * 4.0 && distanceSquared < 24.0 * 24.0 && this.toilet.getVisibilityCache().canSee(target);
        }

        @Override
        public void start() {
            this.phase = Phase.WINDUP;
            this.ticks = 0;
            this.alreadyHit.clear();
            this.toilet.getNavigation().stop();
            this.toilet.dataTracker.set(CHARGING, true);
            this.toilet.playSound(ModSounds.FAST_ARMORED_TOILET_REV, 2.0f, 1.0f);
        }

        @Override
        public boolean shouldContinue() {
            return this.phase != null;
        }

        @Override
        public void stop() {
            this.phase = null;
            this.cooldown = COOLDOWN_TICKS;
            this.toilet.dataTracker.set(CHARGING, false);
            this.toilet.setArmorOpen(false);
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            this.ticks++;
            switch (this.phase) {
                case WINDUP -> this.windUp();
                case CHARGE -> this.charge();
                case RECOVER -> {
                    if (this.ticks >= RECOVER_TICKS) {
                        this.phase = null;
                    }
                }
            }
        }

        private void windUp() {
            LivingEntity target = this.toilet.getTarget();
            if (target != null) {
                this.toilet.getLookControl().lookAt(target, 30.0f, 30.0f);
                Vec3d toward = target.getPos().subtract(this.toilet.getPos());
                this.direction = new Vec3d(toward.x, 0.0, toward.z).normalize();
                float yaw = (float) (MathHelper.atan2(this.direction.z, this.direction.x) * MathHelper.DEGREES_PER_RADIAN) - 90.0f;
                this.toilet.setYaw(yaw);
                this.toilet.bodyYaw = yaw;
            }
            ((ServerWorld) this.toilet.getWorld()).spawnParticles(ParticleTypes.LARGE_SMOKE, this.toilet.getX(),
                    this.toilet.getY() + 0.3, this.toilet.getZ(), 2, 0.8, 0.1, 0.8, 0.01);
            if (this.ticks >= WINDUP_TICKS) {
                this.phase = Phase.CHARGE;
                this.ticks = 0;
            }
        }

        private void charge() {
            this.toilet.setVelocity(this.direction.x * CHARGE_SPEED, this.toilet.getVelocity().y, this.direction.z * CHARGE_SPEED);
            this.toilet.velocityDirty = true;
            ServerWorld world = (ServerWorld) this.toilet.getWorld();
            // The ground shakes as it thunders past, and it kicks up dust behind it.
            if (this.ticks % 4 == 1) {
                ModFx.shake(world, this.toilet.getPos(), 0.18f, 16.0f);
            }
            world.spawnParticles(ParticleTypes.CLOUD, this.toilet.getX() - this.direction.x * 1.2, this.toilet.getY() + 0.1,
                    this.toilet.getZ() - this.direction.z * 1.2, 3, 0.6, 0.05, 0.6, 0.02);
            for (LivingEntity victim : this.toilet.getWorld().getEntitiesByClass(LivingEntity.class,
                    this.toilet.getBoundingBox().expand(0.5), victim -> isToiletEnemy(victim) && !this.alreadyHit.contains(victim))) {
                this.alreadyHit.add(victim);
                if (victim.damage(this.toilet.getDamageSources().mobAttack(this.toilet), CHARGE_DAMAGE)) {
                    victim.takeKnockback(2.0, -this.direction.x, -this.direction.z);
                    victim.addVelocity(0.0, 0.4, 0.0);
                    this.toilet.playSound(ModSounds.FAST_ARMORED_TOILET_CRASH, 1.0f, 1.3f);
                    ModFx.shake(world, victim.getPos(), 0.5f, 6.0f);
                }
            }
            if (this.toilet.horizontalCollision || this.ticks >= CHARGE_TICKS) {
                if (this.toilet.horizontalCollision) {
                    // Slammed into something solid.
                    world.spawnParticles(ParticleTypes.EXPLOSION, this.toilet.getX() + this.direction.x * 1.2,
                            this.toilet.getBodyY(0.5), this.toilet.getZ() + this.direction.z * 1.2, 2, 0.6, 0.4, 0.6, 0.0);
                    ModFx.shockwave(world, this.toilet.getPos(), 3.0f, 0.9f);
                    this.toilet.playSound(ModSounds.FAST_ARMORED_TOILET_CRASH, 2.5f, 1.0f);
                }
                this.phase = Phase.RECOVER;
                this.ticks = 0;
                this.toilet.setVelocity(Vec3d.ZERO);
                this.toilet.dataTracker.set(CHARGING, false);
                this.toilet.setArmorOpen(true);
            }
        }
    }
}
