package com.skibiditoilet.entity;

import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * The Triple Skibidi Toilet: one big toilet, three heads. Each bite can catch up to one victim per head.
 * As it gets hurt the heads get flushed one by one, and the fewer heads, the fewer it can bite at once.
 */
public class TripleToiletEntity extends SkibidiToiletEntity {
    public static final float SCALE = 1.8f;
    private static final double SPLASH_RANGE = 2.5;

    private int lastHeads = 3;

    public TripleToiletEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.setStepHeight(1.0f);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 120.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8);
    }

    /** Heads still on: one comes off for each third of its health lost. Also used by the renderer. */
    public static int heads(LivingEntity toilet) {
        return MathHelper.clamp(MathHelper.ceil(toilet.getHealth() / toilet.getMaxHealth() * 3.0f), 1, 3);
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit) {
            // The other heads snap at whoever else is standing close to the target.
            int extraBites = heads(this) - 1;
            float damage = (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            for (LivingEntity other : this.getWorld().getEntitiesByClass(LivingEntity.class,
                    target.getBoundingBox().expand(SPLASH_RANGE), other -> other != target && isToiletEnemy(other))) {
                if (extraBites-- <= 0) {
                    break;
                }
                other.damage(this.getDamageSources().mobAttack(this), damage);
            }
        }
        return hit;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        int heads = heads(this);
        if (heads < this.lastHeads) {
            // A head just got flushed.
            this.playSound(ModSounds.SKIBIDI_TOILET_FLUSH, 1.5f, 0.8f);
            this.playSound(ModSounds.SKIBIDI_TOILET_DEATH, 1.0f, 1.0f);
            ModFx.splash((ServerWorld) this.getWorld(), this.getPos().add(0.0, this.getHeight() * 0.6, 0.0), 1.4f);
        }
        this.lastHeads = heads;
    }
}
