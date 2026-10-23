package com.skibiditoilet.entity;

import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * The Saint Skibidi Toilet. Sings a hymn that slowly heals every toilet around it, itself included.
 * Hurt it badly enough and it turns demonic: red eyes, no more healing, but much faster and stronger.
 */
public class SaintToiletEntity extends SkibidiToiletEntity {
    private static final TrackedData<Boolean> DEMONIC = DataTracker.registerData(SaintToiletEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);
    private static final UUID DEMONIC_SPEED = UUID.fromString("5b3c7c1e-6a0f-4d7e-9d0a-2f0d8b1f6a11");
    private static final UUID DEMONIC_DAMAGE = UUID.fromString("0e6f1c2a-3b1d-4c5e-8f9a-7b6c5d4e3f22");
    private static final double HYMN_RANGE = 12.0;
    private static final int HYMN_INTERVAL_TICKS = 60;
    private static final int HYMN_REGENERATION_TICKS = 80;

    public SaintToiletEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 100.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DEMONIC, false);
    }

    public boolean isDemonic() {
        return this.dataTracker.get(DEMONIC);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.isDemonic()) {
            return;
        }
        if (this.getHealth() <= this.getMaxHealth() / 2) {
            this.turnDemonic();
        } else if (this.age % HYMN_INTERVAL_TICKS == 0) {
            this.singHymn();
        }
    }

    private void singHymn() {
        ServerWorld world = (ServerWorld) this.getWorld();
        for (SkibidiToiletEntity toilet : world.getEntitiesByClass(SkibidiToiletEntity.class,
                this.getBoundingBox().expand(HYMN_RANGE), SkibidiToiletEntity::isAlive)) {
            toilet.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, HYMN_REGENERATION_TICKS), this);
        }
        world.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + this.getHeight() + 0.4, this.getZ(),
                6, 0.4, 0.2, 0.4, 0.02);
        // The choir lasts about as long as the gap between hymns, so it sings on and on.
        this.playSound(ModSounds.SAINT_TOILET_HYMN, 0.9f, 1.0f);
    }

    private void turnDemonic() {
        this.dataTracker.set(DEMONIC, true);
        this.removeStatusEffect(StatusEffects.REGENERATION);
        addModifier(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED), DEMONIC_SPEED, 0.5);
        addModifier(this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE), DEMONIC_DAMAGE, 0.6);
        ServerWorld world = (ServerWorld) this.getWorld();
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getBodyY(0.6), this.getZ(), 30, 0.5, 0.6, 0.5, 0.03);
        world.spawnParticles(ParticleTypes.FLAME, this.getX(), this.getBodyY(0.6), this.getZ(), 20, 0.5, 0.6, 0.5, 0.02);
        world.playSound(null, this.getBlockPos(), ModSounds.SAINT_TOILET_ROAR, SoundCategory.HOSTILE, 2.0f, 1.0f);
        ModFx.shockwave(world, this.getPos(), 2.5f, 0.4f);
    }

    private static void addModifier(EntityAttributeInstance attribute, UUID id, double multiplier) {
        if (attribute != null && attribute.getModifier(id) == null) {
            attribute.addPersistentModifier(new EntityAttributeModifier(id, "Demonic", multiplier,
                    EntityAttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * (this.isDemonic() ? 0.6f : 0.85f);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Demonic", this.isDemonic());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        // The demonic boosts are saved with the attributes; only the look needs restoring.
        this.dataTracker.set(DEMONIC, nbt.getBoolean("Demonic"));
    }
}
