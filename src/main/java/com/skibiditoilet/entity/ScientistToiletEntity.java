package com.skibiditoilet.entity;

import com.skibiditoilet.registry.ModEntities;
import com.skibiditoilet.story.MissionManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A support toilet. Keeps away from fights, keeps nearby toilets buffed, and every so often
 * upgrades a plain Skibidi Toilet into a Big, Spider or Jetpack Toilet.
 */
public class ScientistToiletEntity extends SkibidiToiletEntity {
    private static final double SUPPORT_RANGE = 10.0;
    private static final int BUFF_INTERVAL_TICKS = 60;
    private static final int BUFF_DURATION_TICKS = 100;
    private static final int UPGRADE_COOLDOWN_TICKS = 600;

    private int upgradeCooldown = UPGRADE_COOLDOWN_TICKS;

    public ScientistToiletEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void addAttackGoals() {
        // No attack of its own: it backs away from anyone who gets close.
        this.goalSelector.add(1, new FleeEntityGoal<>(this, PlayerEntity.class, 8.0f, 1.0, 1.2));
        this.goalSelector.add(1, new FleeEntityGoal<>(this, AllianceEntity.class, 8.0f, 1.0, 1.2));
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.age % BUFF_INTERVAL_TICKS == 0) {
            this.buffNearbyToilets();
        }
        if (this.upgradeCooldown > 0) {
            this.upgradeCooldown--;
        } else if (this.upgradeNearbyToilet()) {
            this.upgradeCooldown = UPGRADE_COOLDOWN_TICKS;
        }
    }

    private List<SkibidiToiletEntity> nearbyToilets() {
        return this.getWorld().getEntitiesByClass(SkibidiToiletEntity.class,
                this.getBoundingBox().expand(SUPPORT_RANGE), toilet -> toilet != this && toilet.isAlive());
    }

    private void buffNearbyToilets() {
        for (SkibidiToiletEntity toilet : this.nearbyToilets()) {
            toilet.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, BUFF_DURATION_TICKS), this);
            toilet.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, BUFF_DURATION_TICKS), this);
        }
    }

    /** Turns the closest plain Skibidi Toilet into a random stronger type. Returns false if there was none. */
    private boolean upgradeNearbyToilet() {
        SkibidiToiletEntity toilet = this.nearbyToilets().stream()
                .filter(candidate -> candidate.getType() == ModEntities.SKIBIDI_TOILET)
                .min(Comparator.comparingDouble(this::squaredDistanceTo))
                .orElse(null);
        if (toilet == null) {
            return false;
        }
        // Only upgrades that fit where the toilet stands, so a Big Toilet isn't wedged into a tunnel.
        List<EntityType<? extends SkibidiToiletEntity>> upgrades = Stream.of(
                        ModEntities.BIG_SKIBIDI_TOILET, ModEntities.SPIDER_TOILET, ModEntities.JETPACK_TOILET)
                .filter(type -> this.getWorld().isSpaceEmpty(
                        type.createSimpleBoundingBox(toilet.getX(), toilet.getY(), toilet.getZ())))
                .<EntityType<? extends SkibidiToiletEntity>>map(type -> type)
                .toList();
        if (upgrades.isEmpty()) {
            return false;
        }
        SkibidiToiletEntity upgraded = toilet.convertTo(upgrades.get(this.random.nextInt(upgrades.size())), true);
        if (upgraded == null) {
            return false;
        }
        MissionManager.onConverted(toilet, upgraded);
        upgraded.setTarget(toilet.getTarget());
        ((ServerWorld) this.getWorld()).spawnParticles(ParticleTypes.EXPLOSION,
                upgraded.getX(), upgraded.getBodyY(0.5), upgraded.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        this.getWorld().playSound(null, upgraded.getBlockPos(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED,
                SoundCategory.HOSTILE, 1.0f, 1.0f);
        return true;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("UpgradeCooldown", this.upgradeCooldown);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("UpgradeCooldown")) {
            this.upgradeCooldown = nbt.getInt("UpgradeCooldown");
        }
    }
}
