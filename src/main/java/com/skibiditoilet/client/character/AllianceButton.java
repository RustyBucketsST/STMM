package com.skibiditoilet.client.character;

import com.skibiditoilet.registry.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;

/** A button in the creator's Alliance style. A selected button glows blue. */
public class AllianceButton extends ClickableWidget {
    private final BooleanSupplier selected;
    private final Runnable onPress;

    public AllianceButton(int x, int y, int width, int height, Text message, BooleanSupplier selected, Runnable onPress) {
        super(x, y, width, height, message);
        this.selected = selected;
        this.onPress = onPress;
    }

    public AllianceButton(int x, int y, int width, int height, Text message, Runnable onPress) {
        this(x, y, width, height, message, () -> false, onPress);
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean isSelected = this.selected.getAsBoolean();
        int fill = isSelected ? AllianceUi.SELECTED : this.isHovered() ? AllianceUi.BUTTON_HOVER : AllianceUi.BUTTON;
        int frame = isSelected ? AllianceUi.HIGHLIGHT : this.isHovered() ? AllianceUi.BORDER : AllianceUi.BORDER_DIM;
        int right = this.getX() + this.width;
        int bottom = this.getY() + this.height;
        context.fill(this.getX(), this.getY(), right, bottom, fill);
        context.drawBorder(this.getX(), this.getY(), this.width, this.height, frame);
        if (isSelected) {
            context.fill(this.getX() + 1, bottom - 2, right - 1, bottom - 1, AllianceUi.HIGHLIGHT);
        }
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int color = !this.active ? AllianceUi.TEXT_DIM : isSelected ? 0xFFFFFF : AllianceUi.TEXT;
        context.drawCenteredTextWithShadow(textRenderer, this.getMessage(), this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2, color);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.onPress.run();
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(PositionedSoundInstance.master(ModSounds.UI_CLICK, 1.0f));
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
