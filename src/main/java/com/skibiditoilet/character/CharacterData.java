package com.skibiditoilet.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skibiditoilet.registry.ModAttachments;
import net.minecraft.entity.Entity;

/**
 * A player's character: whether they finished the creator, what they picked, and how they look.
 * Immutable; change it with the with* methods and store the result back on the player.
 */
public record CharacterData(boolean created, AllianceType type, PlayerClass playerClass, BodyStyle body,
                            int casingColor, int accentColor, int suitColor, int tieColor,
                            int detail0, int detail1) {
    public static final Codec<CharacterData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("created").forGetter(CharacterData::created),
            AllianceType.CODEC.fieldOf("type").forGetter(CharacterData::type),
            PlayerClass.CODEC.fieldOf("class").forGetter(CharacterData::playerClass),
            BodyStyle.CODEC.fieldOf("body").forGetter(CharacterData::body),
            Codec.INT.fieldOf("casing_color").forGetter(CharacterData::casingColor),
            Codec.INT.fieldOf("accent_color").forGetter(CharacterData::accentColor),
            Codec.INT.fieldOf("suit_color").forGetter(CharacterData::suitColor),
            Codec.INT.fieldOf("tie_color").forGetter(CharacterData::tieColor),
            Codec.INT.fieldOf("detail0").forGetter(CharacterData::detail0),
            Codec.INT.fieldOf("detail1").forGetter(CharacterData::detail1)
    ).apply(instance, CharacterData::new));

    public static final CharacterData DEFAULT = forType(AllianceType.CAMERAMAN);

    /** A not-yet-created character of the given type with that type's default look. */
    public static CharacterData forType(AllianceType type) {
        return new CharacterData(false, type, PlayerClass.FIGHTER, BodyStyle.HEAD_ONLY,
                type.defaultCasingColor(), type.defaultAccentColor(), CharacterPalette.BLACK, CharacterPalette.BLACK,
                0, 0);
    }

    public static CharacterData of(Entity entity) {
        return entity.getAttachedOrElse(ModAttachments.CHARACTER, DEFAULT);
    }

    /** True if every choice is one the creator actually offers. The server checks this before accepting anything. */
    public boolean isValid() {
        return validColor(this.casingColor) && validColor(this.accentColor)
                && validColor(this.suitColor) && validColor(this.tieColor)
                && validDetail(0, this.detail0) && validDetail(1, this.detail1);
    }

    private static boolean validColor(int color) {
        return color >= 0 && color <= 0xFFFFFF;
    }

    private boolean validDetail(int slot, int option) {
        return option >= 0 && option < this.type.detailSlots().get(slot).options().size();
    }

    public int detail(int slot) {
        return slot == 0 ? this.detail0 : this.detail1;
    }

    public CharacterData withCreated(boolean created) {
        return new CharacterData(created, type, playerClass, body, casingColor, accentColor, suitColor, tieColor, detail0, detail1);
    }

    public CharacterData withClass(PlayerClass playerClass) {
        return new CharacterData(created, type, playerClass, body, casingColor, accentColor, suitColor, tieColor, detail0, detail1);
    }

    public CharacterData withBody(BodyStyle body) {
        return new CharacterData(created, type, playerClass, body, casingColor, accentColor, suitColor, tieColor, detail0, detail1);
    }

    public CharacterData withCasingColor(int color) {
        return new CharacterData(created, type, playerClass, body, color, accentColor, suitColor, tieColor, detail0, detail1);
    }

    public CharacterData withAccentColor(int color) {
        return new CharacterData(created, type, playerClass, body, casingColor, color, suitColor, tieColor, detail0, detail1);
    }

    public CharacterData withSuitColor(int color) {
        return new CharacterData(created, type, playerClass, body, casingColor, accentColor, color, tieColor, detail0, detail1);
    }

    public CharacterData withTieColor(int color) {
        return new CharacterData(created, type, playerClass, body, casingColor, accentColor, suitColor, color, detail0, detail1);
    }

    public CharacterData withDetail(int slot, int option) {
        return slot == 0
                ? new CharacterData(created, type, playerClass, body, casingColor, accentColor, suitColor, tieColor, option, detail1)
                : new CharacterData(created, type, playerClass, body, casingColor, accentColor, suitColor, tieColor, detail0, option);
    }

    /** Takes only the look (body, colors, details) from {@code other}, keeping this character's type, class and status. */
    public CharacterData withLookFrom(CharacterData other) {
        return new CharacterData(created, type, playerClass, other.body, other.casingColor, other.accentColor,
                other.suitColor, other.tieColor, other.detail0, other.detail1);
    }
}
