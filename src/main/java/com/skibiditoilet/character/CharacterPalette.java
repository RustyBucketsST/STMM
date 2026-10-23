package com.skibiditoilet.character;

import java.util.List;

/** The ready-made color squares in the creator. Any 0xRRGGBB color is allowed through the custom picker. */
public final class CharacterPalette {
    public static final int WHITE = 0xF2F2F2;
    public static final int LIGHT_GRAY = 0xA4A9AE;
    public static final int GRAY = 0x5C6066;
    public static final int BLACK = 0x26262B;
    public static final int RED = 0xC0392B;
    public static final int ORANGE = 0xE67E22;
    public static final int YELLOW = 0xF1C40F;
    public static final int LIME = 0x7ED957;
    public static final int GREEN = 0x2E8B57;
    public static final int CYAN = 0x1ABC9C;
    public static final int LIGHT_BLUE = 0x5DADE2;
    public static final int BLUE = 0x2E5CB8;
    public static final int NAVY = 0x1F2A44;
    public static final int PURPLE = 0x8E44AD;
    public static final int PINK = 0xF06292;
    public static final int BROWN = 0x8B5A2B;

    public static final List<Integer> COLORS = List.of(
            WHITE, LIGHT_GRAY, GRAY, BLACK, RED, ORANGE, YELLOW, LIME,
            GREEN, CYAN, LIGHT_BLUE, BLUE, NAVY, PURPLE, PINK, BROWN);

    private CharacterPalette() {
    }
}
