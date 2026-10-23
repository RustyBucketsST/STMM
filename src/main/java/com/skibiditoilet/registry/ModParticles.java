package com.skibiditoilet.registry;

import com.skibiditoilet.SkibidiToiletMod;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * The mod's own particles. They're spawned on the client by ClientFx (which also sets their color and
 * size) and by dying toilets, so they're never sent from the server.
 */
public final class ModParticles {
    /** A soft glowing dot: flashes, sparks, laser impacts. */
    public static final DefaultParticleType GLOW = register("glow");
    /** A ring facing the camera that grows and fades: speaker sound waves. */
    public static final DefaultParticleType RING = register("ring");
    /** A ring lying flat on the ground: shockwaves. */
    public static final DefaultParticleType RING_FLAT = register("ring_flat");
    /** A water drop circling down into a toilet bowl. Velocity carries radius, spin and sink speed. */
    public static final DefaultParticleType SWIRL = register("swirl");
    /** A flickering speck of TV static. */
    public static final DefaultParticleType STATIC = register("static");

    private ModParticles() {
    }

    public static void init() {
    }

    private static DefaultParticleType register(String name) {
        return Registry.register(Registries.PARTICLE_TYPE, SkibidiToiletMod.id(name), FabricParticleTypes.simple(true));
    }
}
