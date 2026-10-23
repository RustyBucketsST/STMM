package com.skibiditoilet.registry;

import com.skibiditoilet.SkibidiToiletMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Every sound in the mod. The sound files are made by tools/gen_sounds.py; docs/custom-sounds.md explains how
 * to swap in your own with a resource pack.
 */
public final class ModSounds {
    public static final SoundEvent SKIBIDI_TOILET_AMBIENT = register("entity.skibidi_toilet.ambient");
    public static final SoundEvent SKIBIDI_TOILET_HURT = register("entity.skibidi_toilet.hurt");
    public static final SoundEvent SKIBIDI_TOILET_DEATH = register("entity.skibidi_toilet.death");
    public static final SoundEvent SKIBIDI_TOILET_FLUSH = register("entity.skibidi_toilet.flush");
    public static final SoundEvent SKIBIDI_TOILET_BITE = register("entity.skibidi_toilet.bite");
    public static final SoundEvent JETPACK_TOILET_SHOOT = register("entity.jetpack_toilet.shoot");
    public static final SoundEvent ALLIANCE_BLASTER_SHOOT = register("item.alliance_blaster.shoot");
    public static final SoundEvent CAMERA_STRIDER_SHOOT = register("entity.camera_strider.shoot");
    public static final SoundEvent GIANT_FLYING_TOILET_LASER = register("entity.giant_flying_toilet.laser");
    public static final SoundEvent GIANT_FLYING_TOILET_SWOOP = register("entity.giant_flying_toilet.swoop");
    public static final SoundEvent SKIBIDI_URINAL_SPIT = register("entity.skibidi_urinal.spit");
    public static final SoundEvent FAST_ARMORED_TOILET_REV = register("entity.fast_armored_toilet.rev");
    public static final SoundEvent FAST_ARMORED_TOILET_CRASH = register("entity.fast_armored_toilet.crash");
    public static final SoundEvent HAT_TOILET_SLAM = register("entity.hat_toilet.slam");
    public static final SoundEvent TITAN_STEP = register("entity.titan.step");
    public static final SoundEvent SAINT_TOILET_HYMN = register("entity.saint_toilet.hymn");
    public static final SoundEvent SAINT_TOILET_ROAR = register("entity.saint_toilet.roar");
    public static final SoundEvent DJ_KICK = register("entity.dj_toilet.kick");
    public static final SoundEvent DJ_SNARE = register("entity.dj_toilet.snare");
    public static final SoundEvent DJ_HAT = register("entity.dj_toilet.hat");
    public static final SoundEvent DJ_CLAP = register("entity.dj_toilet.clap");
    public static final SoundEvent CAMERAMAN_FLASH = register("entity.cameraman.flash");
    public static final SoundEvent SPEAKERMAN_BLAST = register("entity.speakerman.blast");
    public static final SoundEvent TV_MAN_STATIC = register("entity.tv_man.static");
    public static final SoundEvent MISSION_START = register("story.mission_start");
    public static final SoundEvent MISSION_OBJECTIVE = register("story.objective");
    public static final SoundEvent MISSION_COMPLETE = register("story.mission_complete");
    public static final SoundEvent MISSION_FAILED = register("story.mission_failed");
    public static final SoundEvent MISSION_REWARD = register("story.reward");
    public static final SoundEvent UI_CLICK = register("ui.click");
    public static final SoundEvent POWER_HOLOSHIELD = register("power.holoshield");
    public static final SoundEvent POWER_SHIELD_HIT = register("power.shield_hit");
    public static final SoundEvent POWER_RECORD_SCAN = register("power.record_scan");
    public static final SoundEvent POWER_SONIC_BOOTS = register("power.sonic_boots");
    public static final SoundEvent POWER_ANTHEM_DANCE = register("power.anthem_dance");
    public static final SoundEvent POWER_HYPNOSIS = register("power.hypnosis");
    public static final SoundEvent POWER_TELEPORT = register("power.teleport");
    public static final SoundEvent POWER_BURNING_LIGHT = register("power.burning_light");
    public static final SoundEvent POWER_STUN_FLASH = register("power.stun_flash");
    public static final SoundEvent POWER_NO_ENERGY = register("power.no_energy");
    public static final SoundEvent STUN_RIFLE_SHOOT = register("item.stun_rifle.shoot");
    public static final SoundEvent GRAVITY_PLUNGER_PULL = register("item.gravity_plunger.pull");
    public static final SoundEvent ENERGY_KNIFE_SWING = register("item.energy_knife.swing");

    private ModSounds() {
    }

    public static void init() {
    }

    private static SoundEvent register(String name) {
        Identifier id = SkibidiToiletMod.id(name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}
