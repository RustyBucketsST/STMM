package com.skibiditoilet.entity;

import com.skibiditoilet.registry.ModEntities;
import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModSounds;
import com.skibiditoilet.story.MissionManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * The Giant Flying Skibidi Toilet. Circles high above its target firing lasers and drops Flying Toilets
 * to join the fight. Every so often it swoops down to bite, which is the moment to hit it up close.
 */
public class GiantFlyingToiletEntity extends SkibidiToiletEntity {
    public static final float SCALE = 3.0f;
    private static final double CIRCLE_RADIUS = 12.0;
    private static final double CIRCLE_HEIGHT = 9.0;
    private static final double LASER_RANGE = 32.0;
    private static final float LASER_DAMAGE = 5.0f;
    private static final int LASER_COOLDOWN_TICKS = 50;
    private static final int SWOOP_INTERVAL_TICKS = 240;
    private static final int SWOOP_MAX_TICKS = 100;
    private static final int DROP_INTERVAL_TICKS = 240;
    private static final int MAX_FLYERS = 4;
    private static final int BEAM_COLOR = 0xFF3A1A;

    public GiantFlyingToiletEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.moveControl = new GlideMoveControl(this);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 200.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void addAttackGoals() {
        this.goalSelector.add(2, new CircleAndSwoopGoal(this));
    }

    @Override
    protected void addWanderGoal() {
        this.goalSelector.add(5, new DriftGoal(this));
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        LivingEntity target = this.getTarget();
        if (target != null && this.age % DROP_INTERVAL_TICKS == 0) {
            this.dropFlyers(target);
        }
    }

    /** Lets two Flying Toilets out underneath, as long as there aren't already plenty around. */
    private void dropFlyers(LivingEntity target) {
        ServerWorld world = (ServerWorld) this.getWorld();
        int nearby = world.getEntitiesByClass(FlyingToiletEntity.class, this.getBoundingBox().expand(24.0), LivingEntity::isAlive).size();
        for (int i = 0; i < Math.min(2, MAX_FLYERS - nearby); i++) {
            FlyingToiletEntity flyer = ModEntities.FLYING_TOILET.create(world);
            if (flyer == null) {
                return;
            }
            flyer.refreshPositionAndAngles(this.getX() + (i * 2 - 1), this.getY() - 1.5, this.getZ(), this.getYaw(), 0.0f);
            flyer.initialize(world, world.getLocalDifficulty(flyer.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
            flyer.setTarget(target);
            world.spawnEntity(flyer);
            world.spawnParticles(ParticleTypes.POOF, flyer.getX(), flyer.getBodyY(0.5), flyer.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
            MissionManager.onSummoned(this, flyer);
        }
    }

    @Override
    public boolean hasNoGravity() {
        // Always airborne, even when loaded from a save or a /summon that says otherwise.
        return true;
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    /** Turns the body to face a point, since flying mobs don't turn by walking. */
    private void face(Vec3d point) {
        float yaw = (float) (MathHelper.atan2(point.z - this.getZ(), point.x - this.getX()) * MathHelper.DEGREES_PER_RADIAN) - 90.0f;
        this.setYaw(yaw);
        this.bodyYaw = yaw;
    }

    /** Flies straight at its destination, slowing as it arrives, like a Ghast. */
    static class GlideMoveControl extends MoveControl {
        GlideMoveControl(GiantFlyingToiletEntity toilet) {
            super(toilet);
        }

        @Override
        public void tick() {
            if (this.state != State.MOVE_TO) {
                this.entity.setVelocity(this.entity.getVelocity().multiply(0.9));
                return;
            }
            Vec3d delta = new Vec3d(this.targetX - this.entity.getX(), this.targetY - this.entity.getY(), this.targetZ - this.entity.getZ());
            double distance = delta.length();
            if (distance < 1.0) {
                this.state = State.WAIT;
                return;
            }
            double acceleration = this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 0.05;
            this.entity.setVelocity(this.entity.getVelocity().add(delta.multiply(acceleration / distance)));
        }
    }

    /** Floats to random spots around it when there's no one to fight. */
    static class DriftGoal extends Goal {
        private final GiantFlyingToiletEntity toilet;

        DriftGoal(GiantFlyingToiletEntity toilet) {
            this.toilet = toilet;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return this.toilet.getTarget() == null && !this.toilet.getMoveControl().isMoving()
                    && this.toilet.getRandom().nextInt(40) == 0;
        }

        @Override
        public boolean shouldContinue() {
            return false;
        }

        @Override
        public void start() {
            Vec3d spot = this.toilet.getPos().add(this.toilet.getRandom().nextDouble() * 24.0 - 12.0,
                    this.toilet.getRandom().nextDouble() * 6.0 - 3.0, this.toilet.getRandom().nextDouble() * 24.0 - 12.0);
            this.toilet.getMoveControl().moveTo(spot.x, spot.y, spot.z, 0.6);
            this.toilet.face(spot);
        }
    }

    /** Circles overhead firing lasers, then every so often swoops down to bite. */
    static class CircleAndSwoopGoal extends Goal {
        private final GiantFlyingToiletEntity toilet;
        private float angle;
        private int laserCooldown;
        private int swoopTimer;
        private boolean swooping;

        CircleAndSwoopGoal(GiantFlyingToiletEntity toilet) {
            this.toilet = toilet;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = this.toilet.getTarget();
            return target != null && target.isAlive() && this.toilet.canTarget(target);
        }

        @Override
        public void start() {
            this.angle = this.toilet.getRandom().nextFloat() * MathHelper.TAU;
            this.laserCooldown = LASER_COOLDOWN_TICKS;
            this.swoopTimer = SWOOP_INTERVAL_TICKS;
            this.swooping = false;
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.toilet.getTarget();
            if (target == null) {
                return;
            }
            this.toilet.getLookControl().lookAt(target, 30.0f, 30.0f);
            this.toilet.face(target.getPos());
            if (this.swooping) {
                this.swoop(target);
            } else {
                this.circle(target);
            }
        }

        private void circle(LivingEntity target) {
            this.angle += 0.025f;
            this.toilet.getMoveControl().moveTo(target.getX() + MathHelper.cos(this.angle) * CIRCLE_RADIUS,
                    target.getY() + CIRCLE_HEIGHT, target.getZ() + MathHelper.sin(this.angle) * CIRCLE_RADIUS, 1.0);
            if (--this.laserCooldown <= 0 && this.toilet.getVisibilityCache().canSee(target)
                    && this.toilet.squaredDistanceTo(target) < LASER_RANGE * LASER_RANGE) {
                Vec3d aim = target.getBoundingBox().getCenter().subtract(this.toilet.getEyePos());
                LaserBeam.fire((ServerWorld) this.toilet.getWorld(), this.toilet, aim, LASER_RANGE, LASER_DAMAGE, BEAM_COLOR, 0.45f);
                this.toilet.playSound(ModSounds.GIANT_FLYING_TOILET_LASER, 3.0f, 1.0f);
                this.laserCooldown = LASER_COOLDOWN_TICKS;
            }
            if (--this.swoopTimer <= 0) {
                this.swooping = true;
                this.swoopTimer = SWOOP_MAX_TICKS;
                this.toilet.playSound(ModSounds.GIANT_FLYING_TOILET_SWOOP, 3.0f, 1.0f);
            }
        }

        private void swoop(LivingEntity target) {
            // Dive at the target's head height, so its bowl comes down level with whoever it's after.
            this.toilet.getMoveControl().moveTo(target.getX(), target.getY() + 0.5, target.getZ(), 1.6);
            double reach = this.toilet.getWidth() / 2 + target.getWidth() / 2 + 1.5;
            boolean close = this.toilet.squaredDistanceTo(target.getX(), target.getY() + 1.0, target.getZ()) < reach * reach * 2;
            if (close) {
                this.toilet.swingHand(this.toilet.getActiveHand());
                this.toilet.tryAttack(target);
                ModFx.shockwave((ServerWorld) this.toilet.getWorld(), target.getPos(), 2.5f, 0.5f);
            }
            if (close || --this.swoopTimer <= 0) {
                this.swooping = false;
                this.swoopTimer = SWOOP_INTERVAL_TICKS;
                this.laserCooldown = LASER_COOLDOWN_TICKS;
            }
        }
    }
}
