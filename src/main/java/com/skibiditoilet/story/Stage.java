package com.skibiditoilet.story;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One step of a mission. A DEFEAT stage is won when its boss falls, or when every enemy is gone if it has
 * no boss. A SURVIVE stage is won by staying alive until the timer runs out, with reinforcements arriving
 * on the way; driving off its boss ends it early.
 */
public record Stage(Kind kind, List<Spawn> enemies, List<Spawn> allies, int seconds, @Nullable Spawn reinforcement,
                    int reinforceSeconds, int reinforceCap) {
    public enum Kind {
        DEFEAT, SURVIVE
    }

    public static Builder defeat(Spawn... enemies) {
        return new Builder(Kind.DEFEAT, 0, enemies);
    }

    public static Builder survive(int seconds, Spawn... enemies) {
        return new Builder(Kind.SURVIVE, seconds, enemies);
    }

    @Nullable
    public Spawn boss() {
        return this.enemies.stream().filter(Spawn::boss).findFirst().orElse(null);
    }

    public int enemyCount() {
        return this.enemies.stream().mapToInt(Spawn::count).sum();
    }

    public static final class Builder {
        private final Kind kind;
        private final int seconds;
        private final List<Spawn> enemies;
        private List<Spawn> allies = List.of();
        @Nullable
        private Spawn reinforcement;
        private int reinforceSeconds;
        private int reinforceCap;

        private Builder(Kind kind, int seconds, Spawn[] enemies) {
            this.kind = kind;
            this.seconds = seconds;
            this.enemies = List.of(enemies);
        }

        public Builder allies(Spawn... allies) {
            this.allies = List.of(allies);
            return this;
        }

        /** Every few seconds, sends in more enemies while fewer than {@code cap} are left. */
        public Builder reinforce(Spawn reinforcement, int everySeconds, int cap) {
            this.reinforcement = reinforcement;
            this.reinforceSeconds = everySeconds;
            this.reinforceCap = cap;
            return this;
        }

        public Stage build() {
            return new Stage(this.kind, this.enemies, this.allies, this.seconds, this.reinforcement,
                    this.reinforceSeconds, this.reinforceCap);
        }
    }
}
