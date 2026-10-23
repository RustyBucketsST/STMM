package com.skibiditoilet.entity;

import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * The Camera Strider: a big camera on three long legs. Keeps its distance from toilets and shoots them
 * with blue energy bolts from its lens, flying ones included.
 */
public class CameraStriderEntity extends AllianceEntity {
    public static final float SCALE = 1.4f;
    private static final double BOLT_RANGE = 28.0;
    private static final float BOLT_DAMAGE = 6.0f;
    private static final int BOLT_COOLDOWN_TICKS = 40;
    private static final int BOLT_COLOR = 0x55CCFF;

    public CameraStriderEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setStepHeight(1.5f);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return AllianceEntity.createAttributes(80.0, 0.25, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new ShootGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this, AllianceEntity.class));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, SkibidiToiletEntity.class, true));
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    /** Walks into range of its target, then stands and fires. */
    static class ShootGoal extends Goal {
        private final CameraStriderEntity strider;
        private int cooldown;

        ShootGoal(CameraStriderEntity strider) {
            this.strider = strider;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = this.strider.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            this.cooldown = BOLT_COOLDOWN_TICKS / 2;
        }

        @Override
        public void stop() {
            this.strider.getNavigation().stop();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.strider.getTarget();
            if (target == null) {
                return;
            }
            this.strider.getLookControl().lookAt(target, 30.0f, 30.0f);
            boolean canSee = this.strider.getVisibilityCache().canSee(target);
            boolean inRange = this.strider.squaredDistanceTo(target) < BOLT_RANGE * 0.8 * BOLT_RANGE * 0.8;
            if (canSee && inRange) {
                this.strider.getNavigation().stop();
            } else if (this.strider.age % 10 == 0) {
                this.strider.getNavigation().startMovingTo(target, 1.0);
            }
            if (--this.cooldown <= 0 && canSee && this.strider.squaredDistanceTo(target) < BOLT_RANGE * BOLT_RANGE) {
                Vec3d aim = target.getBoundingBox().getCenter().subtract(this.strider.getEyePos());
                LaserBeam.fire((ServerWorld) this.strider.getWorld(), this.strider, aim, BOLT_RANGE, BOLT_DAMAGE, BOLT_COLOR, 0.28f);
                this.strider.playSound(ModSounds.CAMERA_STRIDER_SHOOT, 2.0f, 1.0f);
                this.cooldown = BOLT_COOLDOWN_TICKS;
            }
        }
    }
}
