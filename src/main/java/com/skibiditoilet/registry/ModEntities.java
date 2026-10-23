package com.skibiditoilet.registry;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.entity.AllianceEntity;
import com.skibiditoilet.entity.BigSkibidiToiletEntity;
import com.skibiditoilet.entity.CameraStriderEntity;
import com.skibiditoilet.entity.CommanderEntity;
import com.skibiditoilet.entity.DjToiletEntity;
import com.skibiditoilet.entity.FastArmoredToiletEntity;
import com.skibiditoilet.entity.FlyingToiletEntity;
import com.skibiditoilet.entity.GManToiletEntity;
import com.skibiditoilet.entity.GiantFlyingToiletEntity;
import com.skibiditoilet.entity.HatToiletEntity;
import com.skibiditoilet.entity.JetpackToiletEntity;
import com.skibiditoilet.entity.LargeAllianceEntity;
import com.skibiditoilet.entity.MicroToiletEntity;
import com.skibiditoilet.entity.SaintToiletEntity;
import com.skibiditoilet.entity.ScientistToiletEntity;
import com.skibiditoilet.entity.SeatEntity;
import com.skibiditoilet.entity.SkibidiToiletEntity;
import com.skibiditoilet.entity.SkibidiUrinalEntity;
import com.skibiditoilet.entity.SpiderToiletEntity;
import com.skibiditoilet.entity.TitanEntity;
import com.skibiditoilet.entity.TripleToiletEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.Heightmap;

public final class ModEntities {
    public static final EntityType<SkibidiToiletEntity> SKIBIDI_TOILET = register("skibidi_toilet",
            toilet(SkibidiToiletEntity::new, 0.8f, 1.3f));
    public static final EntityType<SpiderToiletEntity> SPIDER_TOILET = register("spider_toilet",
            toilet(SpiderToiletEntity::new, 1.3f, 1.3f));
    public static final EntityType<JetpackToiletEntity> JETPACK_TOILET = register("jetpack_toilet",
            toilet(JetpackToiletEntity::new, 0.8f, 1.3f));

    // No natural spawn: Big and Scientist Toilets will come from the storyline.
    public static final EntityType<BigSkibidiToiletEntity> BIG_SKIBIDI_TOILET = register("big_skibidi_toilet",
            toilet(BigSkibidiToiletEntity::new, 0.8f * BigSkibidiToiletEntity.SCALE, 1.3f * BigSkibidiToiletEntity.SCALE));
    public static final EntityType<ScientistToiletEntity> SCIENTIST_TOILET = register("scientist_toilet",
            toilet(ScientistToiletEntity::new, 0.8f, 1.3f));

    // No natural spawn: these come from the story (Chapter 1), with spawn eggs for trying them out.
    public static final EntityType<SkibidiUrinalEntity> SKIBIDI_URINAL = register("skibidi_urinal",
            toilet(SkibidiUrinalEntity::new, 0.7f, 1.5f));
    public static final EntityType<MicroToiletEntity> MICRO_TOILET = register("micro_skibidi_toilet",
            toilet(MicroToiletEntity::new, 0.8f * MicroToiletEntity.SCALE, 1.3f * MicroToiletEntity.SCALE));
    public static final EntityType<FlyingToiletEntity> FLYING_TOILET = register("flying_skibidi_toilet",
            toilet(FlyingToiletEntity::new, 0.8f, 1.3f));
    public static final EntityType<DjToiletEntity> DJ_TOILET = register("dj_skibidi_toilet",
            toilet(DjToiletEntity::new, 0.8f, 1.3f));
    public static final EntityType<SaintToiletEntity> SAINT_TOILET = register("saint_skibidi_toilet",
            toilet(SaintToiletEntity::new, 0.8f, 1.3f));
    public static final EntityType<TripleToiletEntity> TRIPLE_TOILET = register("triple_skibidi_toilet",
            toilet(TripleToiletEntity::new, 2.2f, 1.3f * TripleToiletEntity.SCALE));
    public static final EntityType<FastArmoredToiletEntity> FAST_ARMORED_TOILET = register("fast_armored_skibidi_toilet",
            toilet(FastArmoredToiletEntity::new, 0.95f * FastArmoredToiletEntity.SCALE, FastArmoredToiletEntity.SCALE));
    public static final EntityType<HatToiletEntity> HAT_TOILET = register("hat_skibidi_toilet",
            toilet(HatToiletEntity::new, 0.8f, 1.3f));
    public static final EntityType<GiantFlyingToiletEntity> GIANT_FLYING_TOILET = register("giant_flying_skibidi_toilet",
            FabricEntityTypeBuilder.createMob()
                    .spawnGroup(SpawnGroup.MONSTER)
                    .entityFactory(GiantFlyingToiletEntity::new)
                    .dimensions(EntityDimensions.fixed(0.8f * GiantFlyingToiletEntity.SCALE, 1.3f * GiantFlyingToiletEntity.SCALE))
                    .build());

    // No natural spawn: the storyline will summon G-Man.
    public static final EntityType<GManToiletEntity> GMAN_TOILET = register("gman_toilet",
            FabricEntityTypeBuilder.createMob()
                    .spawnGroup(SpawnGroup.MONSTER)
                    .entityFactory(GManToiletEntity::new)
                    .dimensions(EntityDimensions.fixed(0.8f * GManToiletEntity.SCALE, 1.3f * GManToiletEntity.SCALE))
                    .build());

    public static final EntityType<AllianceEntity> CAMERAMAN = register("cameraman", allianceMember());
    public static final EntityType<AllianceEntity> SPEAKERMAN = register("speakerman", allianceMember());
    public static final EntityType<AllianceEntity> TV_MAN = register("tv_man", allianceMember());

    // No natural spawn: these join the Alliance's fights in the story, with spawn eggs for trying them out.
    public static final EntityType<LargeAllianceEntity> LARGE_CAMERAMAN = register("large_cameraman",
            largeAllianceMember());
    public static final EntityType<LargeAllianceEntity> LARGE_SPEAKERMAN = register("large_speakerman",
            largeAllianceMember());
    public static final EntityType<CameraStriderEntity> CAMERA_STRIDER = register("camera_strider",
            FabricEntityTypeBuilder.createMob()
                    .spawnGroup(SpawnGroup.MISC)
                    .entityFactory(CameraStriderEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f * CameraStriderEntity.SCALE, 2.6f * CameraStriderEntity.SCALE))
                    .build());

    // No natural spawn: Titans will come from a questline.
    public static final EntityType<TitanEntity> TITAN_CAMERAMAN = register("titan_cameraman", titan());
    public static final EntityType<TitanEntity> TITAN_SPEAKERMAN = register("titan_speakerman", titan());
    public static final EntityType<TitanEntity> TITAN_TV_MAN = register("titan_tv_man", titan());

    // Story NPC: placed with its spawn egg, never spawns on its own.
    public static final EntityType<CommanderEntity> COMMANDER = register("commander",
            FabricEntityTypeBuilder.createMob()
                    .spawnGroup(SpawnGroup.MISC)
                    .entityFactory(CommanderEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build());

    public static final EntityType<SeatEntity> SEAT = register("seat",
            FabricEntityTypeBuilder.<SeatEntity>create(SpawnGroup.MISC, SeatEntity::new)
                    .dimensions(EntityDimensions.fixed(0.0f, 0.0f))
                    .disableSaving()
                    .disableSummon()
                    .build());

    private ModEntities() {
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(SKIBIDI_TOILET, SkibidiToiletEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SPIDER_TOILET, SpiderToiletEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(JETPACK_TOILET, JetpackToiletEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BIG_SKIBIDI_TOILET, BigSkibidiToiletEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCIENTIST_TOILET, ScientistToiletEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SKIBIDI_URINAL, SkibidiUrinalEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MICRO_TOILET, MicroToiletEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(FLYING_TOILET, FlyingToiletEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(DJ_TOILET, DjToiletEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(GIANT_FLYING_TOILET, GiantFlyingToiletEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SAINT_TOILET, SaintToiletEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TRIPLE_TOILET, TripleToiletEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(FAST_ARMORED_TOILET, FastArmoredToiletEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(HAT_TOILET, HatToiletEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(LARGE_CAMERAMAN, LargeAllianceEntity.createAttributes(60.0, 8.0, 0.5));
        FabricDefaultAttributeRegistry.register(LARGE_SPEAKERMAN, LargeAllianceEntity.createAttributes(70.0, 8.0, 2.0));
        FabricDefaultAttributeRegistry.register(CAMERA_STRIDER, CameraStriderEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(GMAN_TOILET, GManToiletEntity.createAttributes());

        FabricDefaultAttributeRegistry.register(CAMERAMAN, AllianceEntity.createAttributes(20.0, 0.3, 4.0));
        FabricDefaultAttributeRegistry.register(SPEAKERMAN, AllianceEntity.createAttributes(24.0, 0.27, 5.0));
        FabricDefaultAttributeRegistry.register(TV_MAN, AllianceEntity.createAttributes(22.0, 0.3, 4.0));

        FabricDefaultAttributeRegistry.register(TITAN_CAMERAMAN, TitanEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TITAN_SPEAKERMAN, TitanEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TITAN_TV_MAN, TitanEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(COMMANDER, CommanderEntity.createAttributes());

        // Spawns at night / in the dark across the Overworld, like zombies.
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
                SpawnGroup.MONSTER, SKIBIDI_TOILET, 40, 1, 3);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
                SpawnGroup.MONSTER, SPIDER_TOILET, 20, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
                SpawnGroup.MONSTER, JETPACK_TOILET, 15, 1, 2);

        // Alliance members spawn in daylight across the Overworld, like animals.
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
                SpawnGroup.CREATURE, CAMERAMAN, 4, 1, 3);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
                SpawnGroup.CREATURE, SPEAKERMAN, 4, 1, 3);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
                SpawnGroup.CREATURE, TV_MAN, 4, 1, 3);
    }

    private static <T extends SkibidiToiletEntity> EntityType<T> toilet(EntityType.EntityFactory<T> factory,
                                                                      float width, float height) {
        return FabricEntityTypeBuilder.<T>createMob()
                .spawnGroup(SpawnGroup.MONSTER)
                .entityFactory(factory)
                .dimensions(EntityDimensions.fixed(width, height))
                .spawnRestriction(SpawnRestriction.Location.ON_GROUND,
                        Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                        HostileEntity::canSpawnInDark)
                .build();
    }

    private static EntityType<LargeAllianceEntity> largeAllianceMember() {
        return FabricEntityTypeBuilder.createMob()
                .spawnGroup(SpawnGroup.MISC)
                .entityFactory(LargeAllianceEntity::new)
                .dimensions(EntityDimensions.fixed(0.6f * LargeAllianceEntity.SCALE, 1.95f * LargeAllianceEntity.SCALE))
                .build();
    }

    private static EntityType<AllianceEntity> allianceMember() {
        return FabricEntityTypeBuilder.createMob()
                .spawnGroup(SpawnGroup.CREATURE)
                .entityFactory(AllianceEntity::new)
                .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                .spawnRestriction(SpawnRestriction.Location.ON_GROUND,
                        Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                        AllianceEntity::canSpawnInLight)
                .build();
    }

    private static EntityType<TitanEntity> titan() {
        return FabricEntityTypeBuilder.createMob()
                .spawnGroup(SpawnGroup.MISC)
                .entityFactory(TitanEntity::new)
                .dimensions(EntityDimensions.fixed(0.6f * TitanEntity.SCALE, 1.95f * TitanEntity.SCALE))
                .build();
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, SkibidiToiletMod.id(name), type);
    }
}
