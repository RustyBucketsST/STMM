package com.skibiditoilet.client.character;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** A clickable color square. The one matching the current color gets a bright frame. */
public class ColorSwatchWidget extends ClickableWidget {
    private final int color;
    private final IntSupplier current;
    private final IntConsumer onPick;

    public ColorSwatchWidget(int x, int y, int size, int color, IntSupplier current, IntConsumer onPick) {
        super(x, y, size, size, Text.literal(String.format("#%06X", color)));
        this.color = color;
        this.current = current;
        this.onPick = onPick;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean selected = this.current.getAsInt() == this.color;
        int frame = selected ? AllianceUi.HIGHLIGHT : this.isHovered() ? AllianceUi.BORDER : 0xFF0A121A;
        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, frame);
        int inset = selected ? 2 : 1;
        context.fill(this.getX() + inset, this.getY() + inset, this.getX() + this.width - inset,
                this.getY() + this.height - inset, 0xFF000000 | this.color);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.onPick.accept(this.color);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
