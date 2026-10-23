package com.skibiditoilet.character;

import net.minecraft.util.StringIdentifiable;

import java.util.List;

/**
 * What kind of Alliance member a player is. Each type has two head-detail slots;
 * the option lists here are also what the server validates against.
 */
public enum AllianceType implements StringIdentifiable {
    CAMERAMAN("cameraman", CharacterPalette.LIGHT_GRAY, CharacterPalette.BLUE,
            new DetailSlot("lens", List.of("normal", "big", "double")),
            new DetailSlot("light", List.of("off", "on"))),
    SPEAKERMAN("speakerman", CharacterPalette.BLACK, CharacterPalette.GRAY,
            new DetailSlot("speakers", List.of("one", "two")),
            new DetailSlot("grille", List.of("cone", "mesh", "bars"))),
    TV_MAN("tv_man", CharacterPalette.GRAY, CharacterPalette.LIGHT_BLUE,
            new DetailSlot("antenna", List.of("rabbit_ears", "single", "none")),
            new DetailSlot("screen", List.of("blank", "smile", "angry", "eyes", "static")));

    public static final StringIdentifiable.Codec<AllianceType> CODEC = StringIdentifiable.createCodec(AllianceType::values);

    private final String id;
    private final int defaultCasingColor;
    private final int defaultAccentColor;
    private final List<DetailSlot> detailSlots;

    AllianceType(String id, int defaultCasingColor, int defaultAccentColor, DetailSlot... detailSlots) {
        this.id = id;
        this.defaultCasingColor = defaultCasingColor;
        this.defaultAccentColor = defaultAccentColor;
        this.detailSlots = List.of(detailSlots);
    }

    @Override
    public String asString() {
        return this.id;
    }

    public int defaultCasingColor() {
        return this.defaultCasingColor;
    }

    public int defaultAccentColor() {
        return this.defaultAccentColor;
    }

    public List<DetailSlot> detailSlots() {
        return this.detailSlots;
    }

    public String translationKey() {
        return "entity.skibidi." + this.id;
    }

    public String descriptionKey() {
        return "character.skibidi.type." + this.id + ".description";
    }

    /** The color slot that is a lens, a speaker cone or a TV screen, depending on the type. */
    public String accentColorKey() {
        return "character.skibidi.color.accent." + this.id;
    }

    /** One customizable head detail, e.g. the Cameraman's lens, with its options in order. */
    public record DetailSlot(String id, List<String> options) {
        public String translationKey(AllianceType type) {
            return "character.skibidi.detail." + type.id + "." + this.id;
        }

        public String optionKey(AllianceType type, int option) {
            return translationKey(type) + "." + this.options.get(option);
        }
    }
}
