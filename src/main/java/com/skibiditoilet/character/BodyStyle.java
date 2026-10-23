package com.skibiditoilet.character;

import net.minecraft.util.StringIdentifiable;

/** Whether only the head changes, or the player also wears the Alliance suit over their skin. */
public enum BodyStyle implements StringIdentifiable {
    HEAD_ONLY("head_only"),
    SUIT("suit");

    public static final StringIdentifiable.Codec<BodyStyle> CODEC = StringIdentifiable.createCodec(BodyStyle::values);

    private final String id;

    BodyStyle(String id) {
        this.id = id;
    }

    @Override
    public String asString() {
        return this.id;
    }

    public String translationKey() {
        return "character.skibidi.body." + this.id;
    }

    public String descriptionKey() {
        return translationKey() + ".description";
    }
}
