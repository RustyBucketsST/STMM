package com.skibiditoilet.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.FlyGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** A Skibidi Toilet that flies. Dive-bombs its target, bites, and bounces back up into the air. */
public class FlyingToiletEntity extends SkibidiToiletEntity {
    public FlyingToiletEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.moveControl = new FlightMoveControl(this, 20, true);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation navigation = new BirdNavigation(this, world);
        navigation.setCanPathThroughDoors(false);
        navigation.setCanSwim(true);
        return navigation;
    }

    @Override
    protected void addAttackGoals() {
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.3, true));
    }

    @Override
    protected void addWanderGoal() {
        this.goalSelector.add(5, new FlyGoal(this, 1.0));
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit) {
            // Pull up after the bite, ready for the next dive.
            Vec3d away = this.getPos().subtract(target.getPos()).multiply(1.0, 0.0, 1.0);
            Vec3d push = away.lengthSquared() > 1.0E-4 ? away.normalize().multiply(0.5) : Vec3d.ZERO;
            this.setVelocity(this.getVelocity().add(push.x, 0.6, push.z));
            this.velocityDirty = true;
        }
        return hit;
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
}
