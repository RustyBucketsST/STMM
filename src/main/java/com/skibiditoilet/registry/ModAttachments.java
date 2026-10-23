package com.skibiditoilet.registry;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.character.CharacterData;
import com.skibiditoilet.power.PowerData;
import com.skibiditoilet.story.StoryData;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Data stored on entities. Saved with the player and kept through death; synced to clients by CharacterSync, StorySync and PowerSync. */
@SuppressWarnings("UnstableApiUsage")
public final class ModAttachments {
    public static final AttachmentType<CharacterData> CHARACTER = AttachmentRegistry.<CharacterData>builder()
            .persistent(CharacterData.CODEC)
            .copyOnDeath()
            .initializer(() -> CharacterData.DEFAULT)
            .buildAndRegister(SkibidiToiletMod.id("character"));
    public static final AttachmentType<StoryData> STORY = AttachmentRegistry.<StoryData>builder()
            .persistent(StoryData.CODEC)
            .copyOnDeath()
            .initializer(() -> StoryData.DEFAULT)
            .buildAndRegister(SkibidiToiletMod.id("story"));
    public static final AttachmentType<PowerData> POWERS = AttachmentRegistry.<PowerData>builder()
            .persistent(PowerData.CODEC)
            .copyOnDeath()
            .initializer(() -> PowerData.DEFAULT)
            .buildAndRegister(SkibidiToiletMod.id("powers"));

    private ModAttachments() {
    }

    public static void init() {
    }
}
