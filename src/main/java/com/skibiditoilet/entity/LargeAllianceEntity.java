package com.skibiditoilet.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;

/** A Large Cameraman or Large Speakerman: twice the size of a regular one, slower and much tougher. */
public class LargeAllianceEntity extends AllianceEntity {
    public static final float SCALE = 1.6f;

    public LargeAllianceEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setStepHeight(1.0f);
    }

    /** {@code knockback} is how far its punches throw: the Large Speakerman hits with a sound blast. */
    public static DefaultAttributeContainer.Builder createAttributes(double health, double damage, double knockback) {
        return AllianceEntity.createAttributes(health, 0.27, damage)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, knockback);
    }
}
