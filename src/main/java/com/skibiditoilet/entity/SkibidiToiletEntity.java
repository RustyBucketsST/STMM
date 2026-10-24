package com.skibiditoilet.entity;

import com.skibiditoilet.power.PowerManager;
import com.skibiditoilet.registry.ModEffects;
import com.skibiditoilet.registry.ModItems;
import com.skibiditoilet.registry.ModParticles;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

public class SkibidiToiletEntity extends HostileEntity {
    public SkibidiToiletEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.addAttackGoals();
        this.addWanderGoal();
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, AllianceEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, VillagerEntity.class, false));
    }

    /** Who a toilet's area attacks hurt: players in survival and members of the Alliance, not other toilets. */
    public static boolean isToiletEnemy(LivingEntity entity) {
        if (!entity.isAlive() || !entity.isPartOfGame()) {
            return false;
        }
        return entity instanceof PlayerEntity player ? !player.isCreative() : entity instanceof AllianceEntity;
    }

    /** How this kind of toilet roams when it has nothing to fight. */
    protected void addWanderGoal() {
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
    }

    /** How this kind of toilet fights. Other toilet types swap this out for their own attacks. */
    protected void addAttackGoals() {
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.2, false));
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit) {
            this.playSound(ModSounds.SKIBIDI_TOILET_BITE, 1.0f, 1.0f);
        }
        return hit;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Plungers (the Gravity Plunger too) hit toilets twice as hard.
        if ((source.isOf(DamageTypes.PLAYER_ATTACK) || source.isOf(DamageTypes.MOB_ATTACK))
                && source.getAttacker() instanceof LivingEntity attacker
                && (attacker.getMainHandStack().isOf(ModItems.PLUNGER) || attacker.getMainHandStack().isOf(ModItems.GRAVITY_PLUNGER))) {
            amount *= 2.0f;
        }
        return super.damage(source, amount);
    }

    /** A stunned toilet (a TV Man's Stun Flash, a Stun Rifle) is frozen where it stands. */
    @Override
    public boolean isAiDisabled() {
        return super.isAiDisabled() || this.hasStatusEffect(ModEffects.STUNNED);
    }

    /** Toilets this tough (G-Man, the Giant Flyer) shrug off hypnosis. */
    public boolean canBeHypnotized() {
        return this.getMaxHealth() < 150.0f;
    }

    /** A hypnotized toilet won't go after players or the Alliance; it picks on other toilets instead. */
    @Override
    public boolean canTarget(LivingEntity target) {
        if (this.hasStatusEffect(ModEffects.HYPNOTIZED) && (target instanceof PlayerEntity || target instanceof AllianceEntity)) {
            return false;
        }
        return super.canTarget(target);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.age % 10 == 0 && this.hasStatusEffect(ModEffects.HYPNOTIZED)
                && !(this.getTarget() instanceof SkibidiToiletEntity other && other.isAlive())) {
            this.setTarget(this.getWorld().getClosestEntity(SkibidiToiletEntity.class,
                    TargetPredicate.createAttackable().setBaseMaxDistance(16.0),
                    this, this.getX(), this.getEyeY(), this.getZ(), this.getBoundingBox().expand(16.0, 6.0, 16.0)));
        }
    }

    @Override
    protected void onStatusEffectRemoved(StatusEffectInstance effect) {
        super.onStatusEffectRemoved(effect);
        // Snapping out of hypnosis: stop fighting the other toilet and look for the real enemy again.
        if (effect.getEffectType() == ModEffects.HYPNOTIZED && !this.getWorld().isClient) {
            this.setTarget(null);
        }
    }

    /** Defeating a toilet earns its killer points for powers: one for every 10 health it had. */
    @Override
    public void onDeath(DamageSource source) {
        boolean alreadyDead = this.dead || this.isRemoved();
        super.onDeath(source);
        if (alreadyDead || this.getWorld().isClient) {
            return;
        }
        LivingEntity killer = source.getAttacker() instanceof ServerPlayerEntity player ? player : this.getPrimeAdversary();
        if (killer instanceof ServerPlayerEntity player) {
            PowerManager.addPoints(player, Math.max(1, Math.round(this.getMaxHealth() / 10.0f)));
        }
    }

    /**
     * Whether this toilet gets flushed when it dies: its head spins down into the bowl in a whirlpool
     * instead of the toilet falling over (see SkibidiToiletModel and SkibidiToiletRenderer).
     */
    public boolean flushesOnDeath() {
        return true;
    }

    @Override
    protected void updatePostDeath() {
        super.updatePostDeath();
        if (this.getWorld().isClient && this.flushesOnDeath()) {
            // Water circling down into the bowl, about where the head disappears.
            double radius = this.getWidth() * 0.45;
            for (int i = 0; i < 3; i++) {
                this.getWorld().addParticle(ModParticles.SWIRL, this.getX(), this.getY() + this.getHeight() * 0.62, this.getZ(),
                        radius, 0.35, 0.012 * this.getHeight());
            }
        }
    }

    @Override
    public void handleStatus(byte status) {
        // 60 is the "remove the body" puff of smoke; a flushed toilet splashes instead.
        if (status == 60 && this.flushesOnDeath()) {
            for (int i = 0; i < 20; i++) {
                this.getWorld().addParticle(ParticleTypes.SPLASH, this.getParticleX(0.6), this.getY() + this.getHeight() * 0.6,
                        this.getParticleZ(0.6), 0.0, 0.2, 0.0);
            }
            this.getWorld().addParticle(ParticleTypes.BUBBLE_POP, this.getX(), this.getY() + this.getHeight() * 0.6, this.getZ(),
                    0.0, 0.05, 0.0);
            return;
        }
        super.handleStatus(status);
    }

    @Override
    public int getMinAmbientSoundDelay() {
        // Sings more often than a normal mob (vanilla's minimum delay is 80 ticks).
        return 40;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SKIBIDI_TOILET_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SKIBIDI_TOILET_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SKIBIDI_TOILET_DEATH;
    }
}
