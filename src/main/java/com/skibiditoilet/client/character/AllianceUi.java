package com.skibiditoilet.client.character;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * The creator's look: dark navy panels with blue "camera viewfinder" corners, like an Alliance monitor.
 * Everything is drawn with plain fills, so it stays sharp at every GUI scale.
 */
public final class AllianceUi {
    public static final int PANEL = 0xD80D1824;
    public static final int PANEL_HEADER = 0xF0153452;
    public static final int BORDER = 0xFF3FA9F5;
    public static final int BORDER_DIM = 0xFF23506F;
    public static final int BUTTON = 0xF00E1B28;
    public static final int BUTTON_HOVER = 0xF01A3A57;
    public static final int SELECTED = 0xFF1C6CA6;
    public static final int HIGHLIGHT = 0xFF66E3FF;
    public static final int TEXT = 0xE8F4FF;
    public static final int TEXT_DIM = 0x8FB3CF;
    public static final int TITLE = 0x66E3FF;

    private static final int CORNER = 5;

    private AllianceUi() {
    }

    /** A panel with a dim frame and bright viewfinder corners. */
    public static void panel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, PANEL);
        context.drawBorder(x, y, width, height, BORDER_DIM);
        corners(context, x, y, width, height, BORDER);
    }

    /** A panel with a title strip along its top edge. */
    public static void panel(DrawContext context, TextRenderer textRenderer, Text title, int x, int y, int width, int height) {
        panel(context, x, y, width, height);
        context.fill(x + 1, y + 1, x + width - 1, y + 12, PANEL_HEADER);
        context.fill(x + 1, y + 12, x + width - 1, y + 13, BORDER);
        context.drawCenteredTextWithShadow(textRenderer, title, x + width / 2, y + 3, TITLE);
    }

    public static void corners(DrawContext context, int x, int y, int width, int height, int color) {
        int right = x + width;
        int bottom = y + height;
        context.fill(x, y, x + CORNER, y + 1, color);
        context.fill(x, y, x + 1, y + CORNER, color);
        context.fill(right - CORNER, y, right, y + 1, color);
        context.fill(right - 1, y, right, y + CORNER, color);
        context.fill(x, bottom - 1, x + CORNER, bottom, color);
        context.fill(x, bottom - CORNER, x + 1, bottom, color);
        context.fill(right - CORNER, bottom - 1, right, bottom, color);
        context.fill(right - 1, bottom - CORNER, right, bottom, color);
    }

    /** A segmented bar: one segment per step, lit up to and including the current one. */
    public static void progress(DrawContext context, int x, int y, int width, int step, int steps) {
        int gap = 2;
        int segment = (width - gap * (steps - 1)) / steps;
        for (int i = 0; i < steps; i++) {
            int left = x + i * (segment + gap);
            int color = i < step ? SELECTED : i == step ? HIGHLIGHT : BORDER_DIM;
            context.fill(left, y, left + segment, y + 4, color);
        }
    }
}
