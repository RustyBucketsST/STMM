package com.skibiditoilet;

import com.skibiditoilet.character.CharacterSync;
import com.skibiditoilet.character.CreatorRules;
import com.skibiditoilet.command.PartyCommand;
import com.skibiditoilet.command.SkibidiCommand;
import com.skibiditoilet.power.PowerManager;
import com.skibiditoilet.registry.ModAttachments;
import com.skibiditoilet.registry.ModBlocks;
import com.skibiditoilet.registry.ModEffects;
import com.skibiditoilet.registry.ModEntities;
import com.skibiditoilet.registry.ModItems;
import com.skibiditoilet.registry.ModParticles;
import com.skibiditoilet.registry.ModSounds;
import com.skibiditoilet.story.MissionManager;
import com.skibiditoilet.story.StorySync;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class SkibidiToiletMod implements ModInitializer {
    public static final String MOD_ID = "skibidi";

    @Override
    public void onInitialize() {
        ModSounds.init();
        ModParticles.init();
        ModEffects.init();
        ModEntities.init();
        ModBlocks.init();
        ModItems.init();
        ModAttachments.init();
        CharacterSync.init();
        CreatorRules.init();
        SkibidiCommand.init();
        StorySync.init();
        MissionManager.init();
        PartyCommand.init();
        PowerManager.init();
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
