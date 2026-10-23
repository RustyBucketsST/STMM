package com.skibiditoilet.entity;

import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

import java.util.List;

/**
 * The DJ Skibidi Toilet. Plays a beat, and every toilet that can hear it gets faster and tougher.
 * Fights like a normal toilet, but slowly: it would rather keep the music going.
 */
public class DjToiletEntity extends SkibidiToiletEntity {
    private static final double BOOST_RANGE = 16.0;
    private static final int BOOST_INTERVAL_TICKS = 40;
    private static final int BOOST_DURATION_TICKS = 60;
    private static final int BEAT_TICKS = 5;
    // An eight-step loop: kick, hat, snare, hat, kick, kick, clap, hat.
    private static final List<SoundEvent> BEAT = List.of(ModSounds.DJ_KICK, ModSounds.DJ_HAT, ModSounds.DJ_SNARE,
            ModSounds.DJ_HAT, ModSounds.DJ_KICK, ModSounds.DJ_KICK, ModSounds.DJ_CLAP, ModSounds.DJ_HAT);

    public DjToiletEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.age % BEAT_TICKS == 0) {
            this.playBeat();
        }
        if (this.age % BOOST_INTERVAL_TICKS == 0) {
            for (SkibidiToiletEntity toilet : this.getWorld().getEntitiesByClass(SkibidiToiletEntity.class,
                    this.getBoundingBox().expand(BOOST_RANGE), toilet -> toilet != this && toilet.isAlive())) {
                toilet.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, BOOST_DURATION_TICKS), this);
                toilet.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, BOOST_DURATION_TICKS), this);
            }
        }
    }

    /** One step of the drum loop. A note floats up from the DJ on every kick. */
    private void playBeat() {
        int step = (this.age / BEAT_TICKS) % BEAT.size();
        SoundEvent sound = BEAT.get(step);
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundCategory.HOSTILE, 0.8f, 1.0f);
        if (sound == ModSounds.DJ_KICK) {
            // Spawned with a count of 0, a note particle takes its color (0 to 1) from the x offset.
            ((ServerWorld) this.getWorld()).spawnParticles(ParticleTypes.NOTE, this.getX(), this.getY() + this.getHeight() + 0.3,
                    this.getZ(), 0, (this.age / BEAT_TICKS % 24) / 24.0, 0.0, 0.0, 1.0);
        }
    }
}
