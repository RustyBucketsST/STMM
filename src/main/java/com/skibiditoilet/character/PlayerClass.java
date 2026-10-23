package com.skibiditoilet.character;

import net.minecraft.util.StringIdentifiable;

/**
 * The second pick in the creator. Each class splits 20 starting stat points differently
 * (in Stat order: strength, power, defense, vitality, energy).
 */
public enum PlayerClass implements StringIdentifiable {
    FIGHTER("fighter", 7, 3, 4, 4, 2),
    SHOOTER("shooter", 2, 7, 3, 3, 5),
    TANK("tank", 4, 2, 7, 6, 1),
    SUPPORT("support", 2, 5, 3, 4, 6);

    public static final StringIdentifiable.Codec<PlayerClass> CODEC = StringIdentifiable.createCodec(PlayerClass::values);

    private final String id;
    private final int[] startingStats;

    PlayerClass(String id, int... startingStats) {
        this.id = id;
        this.startingStats = startingStats;
    }

    @Override
    public String asString() {
        return this.id;
    }

    public int startingStat(Stat stat) {
        return this.startingStats[stat.ordinal()];
    }

    public String translationKey() {
        return "character.skibidi.class." + this.id;
    }

    public String descriptionKey() {
        return translationKey() + ".description";
    }
}
