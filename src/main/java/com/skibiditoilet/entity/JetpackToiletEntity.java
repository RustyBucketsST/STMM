package com.skibiditoilet.entity;

import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/** A Skibidi Toilet with a jetpack. Hovers off the ground and shoots lasers from a distance. */
public class JetpackToiletEntity extends SkibidiToiletEntity {
    private static final int HOVER_HEIGHT = 3;
    private static final double LASER_RANGE = 16.0;
    private static final float LASER_DAMAGE = 3.0f;
    private static final int LASER_COOLDOWN_TICKS = 40;
    private static final int BEAM_COLOR = 0xFF4433;

    public JetpackToiletEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.moveControl = new HoverMoveControl(this);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 18.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void addAttackGoals() {
        this.goalSelector.add(2, new HoverAndShootGoal(this));
    }

    @Override
    protected void addWanderGoal() {
        this.goalSelector.add(5, new HoverWanderGoal(this));
    }

    @Override
    public void tickMovement() {
        // The jetpack slows every fall, like a Blaze.
        if (!this.isOnGround() && this.getVelocity().y < 0.0) {
            this.setVelocity(this.getVelocity().multiply(1.0, 0.6, 1.0));
        }
        if (this.getWorld().isClient && !this.isOnGround()) {
            this.spawnJetpackFlames();
        }
        super.tickMovement();
    }

    @Override
    protected void mobTick() {
        // Thrust upward to hover a few blocks off the ground, and higher if the target is above us.
        LivingEntity target = this.getTarget();
        if (this.isNearGround() || (target != null && target.getEyeY() > this.getEyeY() + 1.0)) {
            Vec3d velocity = this.getVelocity();
            this.setVelocity(velocity.add(0.0, (0.3 - velocity.y) * 0.3, 0.0));
            this.velocityDirty = true;
        }
        super.mobTick();
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    /** True if there is ground, water or lava within HOVER_HEIGHT blocks below. */
    private boolean isNearGround() {
        BlockPos.Mutable pos = this.getBlockPos().mutableCopy();
        for (int i = 0; i < HOVER_HEIGHT; i++) {
            pos.move(0, -1, 0);
            BlockState state = this.getWorld().getBlockState(pos);
            if (!state.getCollisionShape(this.getWorld(), pos).isEmpty() || !state.getFluidState().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void spawnJetpackFlames() {
        // The two nozzles sit behind the tank, about 2/3 of a block back and half a block up.
        float yaw = this.getBodyYaw() * MathHelper.RADIANS_PER_DEGREE;
        double backX = MathHelper.sin(yaw) * 0.66;
        double backZ = -MathHelper.cos(yaw) * 0.66;
        double sideX = MathHelper.cos(yaw) * 0.19;
        double sideZ = MathHelper.sin(yaw) * 0.19;
        for (int side = -1; side <= 1; side += 2) {
            this.getWorld().addParticle(ParticleTypes.FLAME,
                    this.getX() + backX + sideX * side, this.getY() + 0.45, this.getZ() + backZ + sideZ * side,
                    0.0, -0.08, 0.0);
        }
    }

    /**
     * Vanilla movement switches to a "jumping" state that only ends on landing, which a hovering toilet
     * never does, so it would stop steering. Drop back out of it whenever we're in the air.
     */
    static class HoverMoveControl extends MoveControl {
        HoverMoveControl(JetpackToiletEntity toilet) {
            super(toilet);
        }

        @Override
        public void tick() {
            super.tick();
            if (this.state == MoveControl.State.JUMPING && !this.entity.isOnGround()) {
                this.state = MoveControl.State.WAIT;
            }
        }
    }

    /** Drifts to a random open spot nearby now and then when there's nothing to shoot. */
    static class HoverWanderGoal extends Goal {
        private static final int MAX_TICKS = 100;

        private final JetpackToiletEntity toilet;
        private Vec3d destination;
        private int ticks;

        HoverWanderGoal(JetpackToiletEntity toilet) {
            this.toilet = toilet;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (this.toilet.getTarget() != null || this.toilet.getRandom().nextInt(60) != 0) {
                return false;
            }
            // Ground pathfinding can't plan routes in the air, so just pick open air at the same height.
            Vec3d spot = this.toilet.getPos().add(
                    this.toilet.getRandom().nextDouble() * 20.0 - 10.0, 0.0, this.toilet.getRandom().nextDouble() * 20.0 - 10.0);
            if (!this.toilet.getWorld().isSpaceEmpty(this.toilet, this.toilet.getBoundingBox().offset(spot.subtract(this.toilet.getPos())))) {
                return false;
            }
            this.destination = spot;
            return true;
        }

        @Override
        public void start() {
            this.ticks = 0;
        }

        @Override
        public boolean shouldContinue() {
            return this.toilet.getTarget() == null && ++this.ticks < MAX_TICKS
                    && this.toilet.squaredDistanceTo(this.destination.x, this.toilet.getY(), this.destination.z) > 1.0;
        }

        @Override
        public void tick() {
            this.toilet.getMoveControl().moveTo(this.destination.x, this.destination.y, this.destination.z, 0.8);
        }
    }

    /** Closes in until it has a clear shot from a bit over half its range, then hovers there and fires. */
    static class HoverAndShootGoal extends Goal {
        private final JetpackToiletEntity toilet;
        private int cooldown;

        HoverAndShootGoal(JetpackToiletEntity toilet) {
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
            this.cooldown = LASER_COOLDOWN_TICKS / 2;
        }

        @Override
        public void stop() {
            this.toilet.getNavigation().stop();
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
            boolean canSee = this.toilet.getVisibilityCache().canSee(target);
            double distanceSquared = this.toilet.squaredDistanceTo(target);
            this.toilet.getLookControl().lookAt(target, 30.0f, 30.0f);

            // Steer straight at the target like a Blaze: ground pathfinding doesn't work while hovering.
            double holdDistance = LASER_RANGE * 0.6;
            if (!canSee || distanceSquared > holdDistance * holdDistance) {
                this.toilet.getMoveControl().moveTo(target.getX(), target.getY(), target.getZ(), 1.0);
            }

            if (--this.cooldown <= 0 && canSee && distanceSquared < LASER_RANGE * LASER_RANGE) {
                Vec3d aim = target.getBoundingBox().getCenter().subtract(this.toilet.getEyePos());
                LaserBeam.fire((ServerWorld) this.toilet.getWorld(), this.toilet, aim, LASER_RANGE, LASER_DAMAGE, BEAM_COLOR, 0.12f);
                this.toilet.playSound(ModSounds.JETPACK_TOILET_SHOOT, 1.0f, 1.0f);
                this.cooldown = LASER_COOLDOWN_TICKS;
            }
        }
    }
}
