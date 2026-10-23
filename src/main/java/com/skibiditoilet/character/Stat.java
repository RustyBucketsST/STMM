package com.skibiditoilet.character;

/** The five character stats. Shown in the creator now; they start affecting gameplay with the stats system. */
public enum Stat {
    STRENGTH("strength"),
    POWER("power"),
    DEFENSE("defense"),
    VITALITY("vitality"),
    ENERGY("energy");

    private final String id;

    Stat(String id) {
        this.id = id;
    }

    public String translationKey() {
        return "character.skibidi.stat." + this.id;
    }

    public String descriptionKey() {
        return translationKey() + ".description";
    }
}
