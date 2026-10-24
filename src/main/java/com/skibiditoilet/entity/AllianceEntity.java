package com.skibiditoilet.entity;

import com.skibiditoilet.character.AllianceType;
import com.skibiditoilet.entity.ability.AllianceAbilities;
import com.skibiditoilet.entity.ability.MobAbilities;
import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModEffects;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A Cameraman, Speakerman or TV Man. Leaves players alone and hunts Skibidi Toilets, punching them and using its
 * abilities (see AllianceAbilities).
 */
public class AllianceEntity extends PathAwareEntity {
    @Nullable
    private MobAbilities<AllianceEntity> abilities;
    private boolean abilitiesMade;

    public AllianceEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes(double health, double speed, double damage) {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, health)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, speed)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, damage)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    public static boolean canSpawnInLight(EntityType<? extends AllianceEntity> type, ServerWorldAccess world,
                                          SpawnReason reason, BlockPos pos, Random random) {
        return world.getBaseLightLevel(pos, 0) > 8 && canMobSpawn(type, world, reason, pos, random);
    }

    /** Which kind of head this one has, read from its id (large and Titan versions included). */
    public AllianceType kind() {
        String id = Registries.ENTITY_TYPE.getId(this.getType()).getPath();
        return id.contains("speaker") ? AllianceType.SPEAKERMAN : id.contains("tv_man") ? AllianceType.TV_MAN : AllianceType.CAMERAMAN;
    }

    /** Every hit comes with its head's effect: a camera flash, a blast of sound, or a burst of TV static. */
    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && this.getWorld() instanceof ServerWorld world) {
            float size = this.getHeight() / 1.95f;
            Vec3d head = this.getEyePos();
            switch (this.kind()) {
                case CAMERAMAN -> {
                    ModFx.cameraFlash(world, head.add(this.getRotationVec(1.0f).multiply(0.4 * size)), size);
                    this.playSound(ModSounds.CAMERAMAN_FLASH, 1.0f, 1.0f / (0.6f + size * 0.4f));
                }
                case SPEAKERMAN -> {
                    ModFx.speakerWave(world, head, target.getBoundingBox().getCenter(), size);
                    this.playSound(ModSounds.SPEAKERMAN_BLAST, 1.0f, 1.0f / (0.6f + size * 0.4f));
                }
                case TV_MAN -> {
                    ModFx.tvStatic(world, head, size);
                    this.playSound(ModSounds.TV_MAN_STATIC, 1.0f, 1.0f / (0.6f + size * 0.4f));
                }
            }
        }
        return hit;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (!this.abilitiesMade) {
            this.abilities = AllianceAbilities.of(this);
            this.abilitiesMade = true;
        }
        if (this.abilities != null) {
            this.abilities.tick(this);
        }
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        // Stays in the world like an animal instead of despawning when players are far away.
        return false;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));

        // Never turns on another Alliance member, even if one hits it by accident.
        this.targetSelector.add(1, new RevengeGoal(this, AllianceEntity.class));
        // Hypnotized toilets are on our side for now.
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, SkibidiToiletEntity.class, 10, true, false,
                toilet -> !toilet.hasStatusEffect(ModEffects.HYPNOTIZED)));
    }
}
