package com.skibiditoilet.registry;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.block.ToiletBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {
    public static final Block TOILET = register("toilet", new ToiletBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.WHITE)
            .strength(1.5f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()));

    private ModBlocks() {
    }

    public static void init() {
    }

    private static Block register(String name, Block block) {
        return Registry.register(Registries.BLOCK, SkibidiToiletMod.id(name), block);
    }
}
