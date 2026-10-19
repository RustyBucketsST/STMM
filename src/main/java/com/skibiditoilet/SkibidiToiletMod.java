package com.skibiditoilet;

import com.skibiditoilet.entity.SkibidiToiletEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;

public class SkibidiToiletMod implements ModInitializer {
    public static final String MOD_ID = "skibidi";

    public static final EntityType<SkibidiToiletEntity> SKIBIDI_TOILET = Registry.register(
            Registries.ENTITY_TYPE,
            id("skibidi_toilet"),
            FabricEntityTypeBuilder.createMob()
                    .spawnGroup(SpawnGroup.MONSTER)
                    .entityFactory(SkibidiToiletEntity::new)
                    .dimensions(EntityDimensions.fixed(0.8f, 1.3f))
                    .spawnRestriction(SpawnRestriction.Location.ON_GROUND,
                            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                            HostileEntity::canSpawnInDark)
                    .build());

    public static final Item SKIBIDI_TOILET_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            id("skibidi_toilet_spawn_egg"),
            new SpawnEggItem(SKIBIDI_TOILET, 0xF2F2F2, 0xE0AC69, new Item.Settings()));

    public static final SoundEvent SKIBIDI_TOILET_AMBIENT = sound("entity.skibidi_toilet.ambient");
    public static final SoundEvent SKIBIDI_TOILET_HURT = sound("entity.skibidi_toilet.hurt");
    public static final SoundEvent SKIBIDI_TOILET_DEATH = sound("entity.skibidi_toilet.death");
    public static final SoundEvent SKIBIDI_TOILET_FLUSH = sound("entity.skibidi_toilet.flush");

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(SKIBIDI_TOILET, SkibidiToiletEntity.createAttributes());

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS)
                .register(entries -> entries.add(SKIBIDI_TOILET_SPAWN_EGG));

        // Spawns at night / in the dark across the Overworld, like zombies.
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
                SpawnGroup.MONSTER, SKIBIDI_TOILET, 40, 1, 3);
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    private static SoundEvent sound(String name) {
        Identifier id = id(name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}
