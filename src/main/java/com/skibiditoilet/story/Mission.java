package com.skibiditoilet.story;

import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** One episode of the series, played as a mission: its stages in order, and what finishing it gives. */
public record Mission(String id, int episode, List<Stage> stages, int points, List<ItemStack> rewards) {
    public static Builder episode(int episode, String name) {
        return new Builder(episode, name);
    }

    public String translationKey() {
        return "story.skibidi.mission." + this.id;
    }

    public String descriptionKey() {
        return translationKey() + ".description";
    }

    /** Optional custom objective text for a stage; the screen falls back to a generic one without it. */
    public String stageKey(int stage) {
        return translationKey() + ".stage" + stage;
    }

    /** The mob shown spinning in the story screen: the last boss, or else the first enemy of the last stage. */
    public EntityType<?> previewType() {
        for (int i = this.stages.size() - 1; i >= 0; i--) {
            Spawn boss = this.stages.get(i).boss();
            if (boss != null) {
                return boss.type();
            }
        }
        return this.stages.get(this.stages.size() - 1).enemies().get(0).type();
    }

    public static final class Builder {
        private final int episode;
        private final String id;
        private final List<Stage> stages = new ArrayList<>();
        private final List<ItemStack> rewards = new ArrayList<>();
        private int points;

        private Builder(int episode, String name) {
            this.episode = episode;
            this.id = name;
        }

        public Builder stage(Stage.Builder stage) {
            this.stages.add(stage.build());
            return this;
        }

        public Builder points(int points) {
            this.points = points;
            return this;
        }

        public Builder reward(ItemConvertible item, int count) {
            this.rewards.add(new ItemStack(item, count));
            return this;
        }

        public Mission build() {
            return new Mission(this.id, this.episode, List.copyOf(this.stages), this.points, List.copyOf(this.rewards));
        }
    }
}
