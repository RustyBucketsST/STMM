package com.skibiditoilet.entity;

import com.skibiditoilet.character.AllianceType;
import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModDamageTypes;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A giant Alliance member: the same brain as the regular ones, much bigger and tougher, with the 1.0 Titans' gear
 * (see AllianceAbilities). The Titan Cameraman's riot shield halves the damage of lasers and projectiles; Titans with
 * a jetpack land from a jetpack flight unhurt.
 */
public class TitanEntity extends AllianceEntity {
    // Heights as in the series: Titan TV Man the tallest (about 12 blocks), Titan Cameraman about 10 blocks,
    // Titan Speakerman the shortest (about 8.5 blocks).
    public static final float CAMERAMAN_SCALE = 5.0f;
    public static final float SPEAKERMAN_SCALE = 4.25f;
    public static final float TV_MAN_SCALE = 6.0f;
    /** A Titan takes many small steps for its size; only every so often is one a real stomp. */
    private static final int STOMP_TICKS = 12;

    /** How long after a jetpack launch it still burns and the landing doesn't hurt. */
    private static final int JETPACK_TICKS = 40;
    private static final int SHIELD_COLOR = 0x5FE3FF;

    private int lastStomp;
    private int jetpackUntil;

    public TitanEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setStepHeight(2.5f);
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
            ModFx.shockwave(world, this.getPos(), this.getWidth() * 1.1f, 0.22f);
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && this.getWorld() instanceof ServerWorld world) {
            ModFx.shockwave(world, target.getPos(), this.getWidth() * 1.7f, 0.45f);
        }
        return hit;
    }

    /** Lights the jetpack (the launch itself is AllianceAbilities' jetpack ability). */
    public void fireJetpack() {
        this.jetpackUntil = this.age + JETPACK_TICKS;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age < this.jetpackUntil && this.getWorld() instanceof ServerWorld world) {
            // Flames out of the jetpack on his back.
            float yaw = this.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
            Vec3d back = new Vec3d(MathHelper.sin(yaw), 0.0, -MathHelper.cos(yaw)).multiply(this.getWidth() * 0.45);
            Vec3d nozzle = this.getPos().add(back).add(0.0, this.getHeight() * 0.5, 0.0);
            double spread = this.getWidth() * 0.12;
            world.spawnParticles(ParticleTypes.FLAME, nozzle.x, nozzle.y, nozzle.z, 6, spread, spread, spread, 0.02);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, nozzle.x, nozzle.y - spread, nozzle.z, 2, spread, spread, spread, 0.01);
        }
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (this.age < this.jetpackUntil + 60) {
            return false;
        }
        return super.handleFallDamage(fallDistance, damageMultiplier, damageSource);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.kind() == AllianceType.CAMERAMAN && !this.getWorld().isClient && isRanged(source)) {
            amount *= 0.5f;
            Vec3d from = source.getPosition();
            if (from != null && this.getWorld() instanceof ServerWorld world) {
                Vec3d chest = this.getPos().add(0.0, this.getHeight() * 0.6, 0.0);
                ModFx.sparks(world, chest.add(from.subtract(chest).normalize().multiply(this.getWidth() * 0.6)), from, SHIELD_COLOR);
            }
        }
        return super.damage(source, amount);
    }

    private static boolean isRanged(DamageSource source) {
        return source.isIn(DamageTypeTags.IS_PROJECTILE) || source.isOf(ModDamageTypes.LASER) || source.isOf(ModDamageTypes.SPIT);
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }
}
