package com.skibiditoilet.client.character;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;

/** A 0..1 slider drawn as a color gradient (for hue, saturation or brightness), dragged with the mouse. */
public class GradientSlider extends ClickableWidget {
    private final DoubleToIntFunction colorAt;
    private final DoubleSupplier value;
    private final DoubleConsumer onChange;

    public GradientSlider(int x, int y, int width, int height, Text name,
                          DoubleToIntFunction colorAt, DoubleSupplier value, DoubleConsumer onChange) {
        super(x, y, width, height, name);
        this.colorAt = colorAt;
        this.value = value;
        this.onChange = onChange;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = this.getX();
        int y = this.getY();
        for (int i = 0; i < this.width; i++) {
            int color = this.colorAt.applyAsInt(i / (double) (this.width - 1));
            context.fill(x + i, y, x + i + 1, y + this.height, 0xFF000000 | color);
        }
        context.drawBorder(x - 1, y - 1, this.width + 2, this.height + 2,
                this.isHovered() ? AllianceUi.BORDER : AllianceUi.BORDER_DIM);
        int knob = x + (int) Math.round(this.value.getAsDouble() * (this.width - 1));
        context.fill(knob - 2, y - 2, knob + 2, y + this.height + 2, 0xFF000000);
        context.fill(knob - 1, y - 1, knob + 1, y + this.height + 1, 0xFFFFFFFF);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.setFromMouse(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        this.setFromMouse(mouseX);
    }

    private void setFromMouse(double mouseX) {
        this.onChange.accept(MathHelper.clamp((mouseX - this.getX()) / (this.width - 1), 0.0, 1.0));
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
