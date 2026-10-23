package com.skibiditoilet.registry;

import com.skibiditoilet.SkibidiToiletMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

/** Damage types are data-driven; the definitions live in data/skibidi/damage_type. */
public final class ModDamageTypes {
    public static final RegistryKey<DamageType> LASER = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, SkibidiToiletMod.id("laser"));
    public static final RegistryKey<DamageType> SPIT = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, SkibidiToiletMod.id("spit"));
    public static final RegistryKey<DamageType> POWER = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, SkibidiToiletMod.id("power"));

    private ModDamageTypes() {
    }

    public static DamageSource laser(World world, Entity attacker) {
        return new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(LASER), attacker);
    }

    /** A Skibidi Urinal's spray of water. */
    public static DamageSource spit(World world, Entity attacker) {
        return new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(SPIT), attacker);
    }

    /** A player's Alliance power: a sonic blast, burning light, a stun flash. */
    public static DamageSource power(World world, Entity attacker) {
        return new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(POWER), attacker);
    }
}
