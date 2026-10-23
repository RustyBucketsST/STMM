package com.skibiditoilet.registry;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.item.AllianceBlasterItem;
import com.skibiditoilet.item.EnergyKnifeItem;
import com.skibiditoilet.item.GravityPlungerItem;
import com.skibiditoilet.item.StunRifleItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
    public static final Item SKIBIDI_TOILET_SPAWN_EGG = register("skibidi_toilet_spawn_egg",
            new SpawnEggItem(ModEntities.SKIBIDI_TOILET, 0xF2F2F2, 0xE0AC69, new Item.Settings()));
    public static final Item SPIDER_TOILET_SPAWN_EGG = register("spider_toilet_spawn_egg",
            new SpawnEggItem(ModEntities.SPIDER_TOILET, 0xF2F2F2, 0x2B2B2B, new Item.Settings()));
    public static final Item JETPACK_TOILET_SPAWN_EGG = register("jetpack_toilet_spawn_egg",
            new SpawnEggItem(ModEntities.JETPACK_TOILET, 0xF2F2F2, 0xD9502E, new Item.Settings()));
    public static final Item BIG_SKIBIDI_TOILET_SPAWN_EGG = register("big_skibidi_toilet_spawn_egg",
            new SpawnEggItem(ModEntities.BIG_SKIBIDI_TOILET, 0xF2F2F2, 0xB5703F, new Item.Settings()));
    public static final Item SCIENTIST_TOILET_SPAWN_EGG = register("scientist_toilet_spawn_egg",
            new SpawnEggItem(ModEntities.SCIENTIST_TOILET, 0xF2F2F2, 0x7FB8E0, new Item.Settings()));
    public static final Item SKIBIDI_URINAL_SPAWN_EGG = register("skibidi_urinal_spawn_egg",
            new SpawnEggItem(ModEntities.SKIBIDI_URINAL, 0xF2F2F2, 0xA9D6E5, new Item.Settings()));
    public static final Item MICRO_TOILET_SPAWN_EGG = register("micro_skibidi_toilet_spawn_egg",
            new SpawnEggItem(ModEntities.MICRO_TOILET, 0xF2F2F2, 0xF5C38B, new Item.Settings()));
    public static final Item FLYING_TOILET_SPAWN_EGG = register("flying_skibidi_toilet_spawn_egg",
            new SpawnEggItem(ModEntities.FLYING_TOILET, 0xF2F2F2, 0x87CEEB, new Item.Settings()));
    public static final Item DJ_TOILET_SPAWN_EGG = register("dj_skibidi_toilet_spawn_egg",
            new SpawnEggItem(ModEntities.DJ_TOILET, 0xF2F2F2, 0x8E44AD, new Item.Settings()));
    public static final Item GIANT_FLYING_TOILET_SPAWN_EGG = register("giant_flying_skibidi_toilet_spawn_egg",
            new SpawnEggItem(ModEntities.GIANT_FLYING_TOILET, 0xD9D9D9, 0x4682B4, new Item.Settings()));
    public static final Item SAINT_TOILET_SPAWN_EGG = register("saint_skibidi_toilet_spawn_egg",
            new SpawnEggItem(ModEntities.SAINT_TOILET, 0xF2F2F2, 0xD4AF37, new Item.Settings()));
    public static final Item TRIPLE_TOILET_SPAWN_EGG = register("triple_skibidi_toilet_spawn_egg",
            new SpawnEggItem(ModEntities.TRIPLE_TOILET, 0xF2F2F2, 0x7A4E2D, new Item.Settings()));
    public static final Item FAST_ARMORED_TOILET_SPAWN_EGG = register("fast_armored_skibidi_toilet_spawn_egg",
            new SpawnEggItem(ModEntities.FAST_ARMORED_TOILET, 0x1A1A1E, 0x5A5A62, new Item.Settings()));
    public static final Item HAT_TOILET_SPAWN_EGG = register("hat_skibidi_toilet_spawn_egg",
            new SpawnEggItem(ModEntities.HAT_TOILET, 0xF2F2F2, 0x2B2B2B, new Item.Settings()));
    public static final Item CAMERAMAN_SPAWN_EGG = register("cameraman_spawn_egg",
            new SpawnEggItem(ModEntities.CAMERAMAN, 0x1C1C1C, 0x9AA3AD, new Item.Settings()));
    public static final Item SPEAKERMAN_SPAWN_EGG = register("speakerman_spawn_egg",
            new SpawnEggItem(ModEntities.SPEAKERMAN, 0x1C1C1C, 0x6B6B6B, new Item.Settings()));
    public static final Item TV_MAN_SPAWN_EGG = register("tv_man_spawn_egg",
            new SpawnEggItem(ModEntities.TV_MAN, 0x1C1C1C, 0x4FC3F7, new Item.Settings()));
    public static final Item LARGE_CAMERAMAN_SPAWN_EGG = register("large_cameraman_spawn_egg",
            new SpawnEggItem(ModEntities.LARGE_CAMERAMAN, 0x283246, 0x9AA3AD, new Item.Settings()));
    public static final Item LARGE_SPEAKERMAN_SPAWN_EGG = register("large_speakerman_spawn_egg",
            new SpawnEggItem(ModEntities.LARGE_SPEAKERMAN, 0x4A3426, 0x6B6B6B, new Item.Settings()));
    public static final Item CAMERA_STRIDER_SPAWN_EGG = register("camera_strider_spawn_egg",
            new SpawnEggItem(ModEntities.CAMERA_STRIDER, 0x5C6066, 0x3FA9F5, new Item.Settings()));
    public static final Item COMMANDER_SPAWN_EGG = register("commander_spawn_egg",
            new SpawnEggItem(ModEntities.COMMANDER, 0x1F2A44, 0xD4AF37, new Item.Settings()));

    // Does double damage to toilets, see SkibidiToiletEntity#damage.
    public static final Item PLUNGER = register("plunger",
            new SwordItem(ToolMaterials.IRON, 1, -2.4f, new Item.Settings()));
    public static final Item ALLIANCE_BLASTER = register("alliance_blaster",
            new AllianceBlasterItem(new Item.Settings().maxDamage(250)));

    // Built in the Powers menu (see Tech); not craftable.
    public static final Item STUN_RIFLE = register("stun_rifle",
            new StunRifleItem(new Item.Settings().maxDamage(200)));
    // Also a plunger: double damage to toilets.
    public static final Item GRAVITY_PLUNGER = register("gravity_plunger",
            new GravityPlungerItem(ToolMaterials.IRON, 2, -2.4f, new Item.Settings().maxDamage(400)));
    public static final Item ENERGY_KNIFE = register("energy_knife",
            new EnergyKnifeItem(ToolMaterials.DIAMOND, 1, -1.4f, new Item.Settings()));

    public static final Item TOILET = register("toilet", new BlockItem(ModBlocks.TOILET, new Item.Settings()));

    private ModItems() {
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> {
            entries.add(SKIBIDI_TOILET_SPAWN_EGG);
            entries.add(SPIDER_TOILET_SPAWN_EGG);
            entries.add(JETPACK_TOILET_SPAWN_EGG);
            entries.add(BIG_SKIBIDI_TOILET_SPAWN_EGG);
            entries.add(SCIENTIST_TOILET_SPAWN_EGG);
            entries.add(SKIBIDI_URINAL_SPAWN_EGG);
            entries.add(MICRO_TOILET_SPAWN_EGG);
            entries.add(FLYING_TOILET_SPAWN_EGG);
            entries.add(DJ_TOILET_SPAWN_EGG);
            entries.add(GIANT_FLYING_TOILET_SPAWN_EGG);
            entries.add(SAINT_TOILET_SPAWN_EGG);
            entries.add(TRIPLE_TOILET_SPAWN_EGG);
            entries.add(FAST_ARMORED_TOILET_SPAWN_EGG);
            entries.add(HAT_TOILET_SPAWN_EGG);
            entries.add(CAMERAMAN_SPAWN_EGG);
            entries.add(SPEAKERMAN_SPAWN_EGG);
            entries.add(TV_MAN_SPAWN_EGG);
            entries.add(LARGE_CAMERAMAN_SPAWN_EGG);
            entries.add(LARGE_SPEAKERMAN_SPAWN_EGG);
            entries.add(CAMERA_STRIDER_SPAWN_EGG);
            entries.add(COMMANDER_SPAWN_EGG);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(PLUNGER);
            entries.add(ALLIANCE_BLASTER);
            entries.add(STUN_RIFLE);
            entries.add(GRAVITY_PLUNGER);
            entries.add(ENERGY_KNIFE);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(TOILET));
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, SkibidiToiletMod.id(name), item);
    }
}
