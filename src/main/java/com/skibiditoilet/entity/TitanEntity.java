package com.skibiditoilet.entity;

import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** A giant Alliance member. Same brain as the regular ones, much bigger and tougher. */
public class TitanEntity extends AllianceEntity {
    public static final float SCALE = 2.5f;
    /** A Titan takes many small steps for its size; only every so often is one a real stomp. */
    private static final int STOMP_TICKS = 12;

    private int lastStomp;

    public TitanEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setStepHeight(1.0f);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return AllianceEntity.createAttributes(200.0, 0.3, 12.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (this.age - this.lastStomp < STOMP_TICKS) {
            return;
        }
        this.lastStomp = this.age;
        this.playSound(ModSounds.TITAN_STEP, 1.5f, 0.9f + this.random.nextFloat() * 0.2f);
        if (this.getWorld() instanceof ServerWorld world) {
            ModFx.shockwave(world, this.getPos(), 1.6f, 0.22f);
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && this.getWorld() instanceof ServerWorld world) {
            ModFx.shockwave(world, target.getPos(), 2.5f, 0.45f);
        }
        return hit;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }
}
