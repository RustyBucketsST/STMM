package com.skibiditoilet.client.story;

import com.skibiditoilet.client.character.AllianceUi;
import com.skibiditoilet.story.Mission;
import com.skibiditoilet.story.StoryEvent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;

/** A mission started, moved on, finished or failed: an Alliance-style card in the corner. */
public class MissionToast implements Toast {
    private static final long DURATION_MS = 5000L;
    private static final int WIDTH = 170;
    private static final int GOOD = 0xFF4CD787;
    private static final int BAD = 0xFFE5534B;

    private final StoryEvent event;
    private final Mission mission;

    public MissionToast(StoryEvent event, Mission mission) {
        this.event = event;
        this.mission = mission;
    }

    @Override
    public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        TextRenderer textRenderer = manager.getClient().textRenderer;
        int accent = this.event.isGood() ? GOOD : BAD;
        AllianceUi.panel(context, 0, 0, this.getWidth(), this.getHeight());
        context.fill(1, 1, 4, this.getHeight() - 1, accent);
        Text title = Text.translatable(this.event.translationKey());
        context.drawTextWithShadow(textRenderer, MissionText.fit(textRenderer, title, this.getWidth() - 14), 9, 7,
                accent & 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, MissionText.fit(textRenderer, MissionText.title(this.mission),
                this.getWidth() - 14), 9, 18, AllianceUi.TEXT);
        return startTime >= DURATION_MS ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
