package com.skibiditoilet.story;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skibiditoilet.registry.ModAttachments;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * A player's story progress: the missions they finished and which of those they claimed the reward for.
 * {@code points} is only read from old saves: points live in PowerData now, and PowerManager moves them there.
 */
@SuppressWarnings("UnstableApiUsage")
public record StoryData(Set<String> completed, Set<String> claimed, int points) {
    private static final Codec<Set<String>> ID_SET = Codec.STRING.listOf().xmap(Set::copyOf, ArrayList::new);

    public static final Codec<StoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ID_SET.optionalFieldOf("completed", Set.of()).forGetter(StoryData::completed),
            ID_SET.optionalFieldOf("claimed", Set.of()).forGetter(StoryData::claimed),
            Codec.INT.optionalFieldOf("points", 0).forGetter(StoryData::points)
    ).apply(instance, StoryData::new));

    public static final StoryData DEFAULT = new StoryData(Set.of(), Set.of(), 0);

    public static StoryData of(PlayerEntity player) {
        return player.getAttachedOrCreate(ModAttachments.STORY);
    }

    public boolean isCompleted(String mission) {
        return this.completed.contains(mission);
    }

    /** Finished, with the reward still waiting to be claimed. */
    public boolean isClaimable(String mission) {
        return this.completed.contains(mission) && !this.claimed.contains(mission);
    }

    public StoryData withCompleted(String mission) {
        return new StoryData(add(this.completed, mission), this.claimed, this.points);
    }

    public StoryData withClaimed(String mission) {
        return new StoryData(this.completed, add(this.claimed, mission), this.points);
    }

    private static Set<String> add(Set<String> set, String id) {
        Set<String> copy = new HashSet<>(set);
        copy.add(id);
        return Set.copyOf(copy);
    }
}
