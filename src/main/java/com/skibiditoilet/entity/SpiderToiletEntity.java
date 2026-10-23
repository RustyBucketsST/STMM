package com.skibiditoilet.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.PounceAtTargetGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;

/** A Skibidi Toilet on spider legs: fast, fragile, and pounces at its target. */
public class SpiderToiletEntity extends SkibidiToiletEntity {
    public SpiderToiletEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.38)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void addAttackGoals() {
        this.goalSelector.add(1, new PounceAtTargetGoal(this, 0.4f));
        super.addAttackGoals();
    }
}
