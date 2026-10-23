package com.skibiditoilet.client.story;

import com.skibiditoilet.client.character.AllianceUi;
import com.skibiditoilet.story.Mission;
import com.skibiditoilet.story.MissionStatus;
import com.skibiditoilet.story.Stage;
import com.skibiditoilet.story.StoryMissions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * The running mission in the top-left corner: which episode, what to do, and how far along it is.
 * (The top-right corner belongs to toasts and status effects.)
 */
final class MissionHud {
    private static final int WIDTH = 150;

    private MissionHud() {
    }

    static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        MissionStatus status = ClientStoryState.status();
        Mission mission = status != null ? StoryMissions.get(status.missionId()) : null;
        if (mission == null || client.options.hudHidden || client.player == null) {
            return;
        }
        TextRenderer textRenderer = client.textRenderer;
        Stage stage = mission.stages().get(status.stage());
        int x = 4;
        int y = 4;
        int height = 36;
        AllianceUi.panel(context, x, y, WIDTH, height);

        Text title = MissionText.title(mission);
        context.drawTextWithShadow(textRenderer, MissionText.fit(textRenderer, title, WIDTH - 10), x + 5, y + 4,
                AllianceUi.TITLE);
        String stageCount = mission.stages().size() > 1 ? (status.stage() + 1) + "/" + mission.stages().size() + " " : "";
        Text objective = Text.literal(stageCount).append(MissionText.objective(mission, status.stage()));
        context.drawTextWithShadow(textRenderer, MissionText.fit(textRenderer, objective, WIDTH - 10), x + 5, y + 15,
                AllianceUi.TEXT);

        // Progress: time left to survive, or enemies defeated. A boss shows its health in its boss bar instead.
        int barY = y + height - 8;
        float fraction;
        String label;
        if (stage.kind() == Stage.Kind.SURVIVE) {
            int left = ClientStoryState.ticksLeft(tickDelta);
            fraction = 1.0f - left / (float) (stage.seconds() * 20);
            label = MissionText.time(left);
        } else if (stage.boss() == null) {
            fraction = status.goal() > 0 ? status.progress() / (float) status.goal() : 0.0f;
            label = status.progress() + "/" + status.goal();
        } else {
            return;
        }
        int labelWidth = textRenderer.getWidth(label);
        int barWidth = WIDTH - 14 - labelWidth;
        context.fill(x + 5, barY, x + 5 + barWidth, barY + 3, AllianceUi.BORDER_DIM);
        context.fill(x + 5, barY, x + 5 + Math.round(barWidth * Math.min(1.0f, fraction)), barY + 3, AllianceUi.HIGHLIGHT);
        context.drawTextWithShadow(textRenderer, label, x + WIDTH - 5 - labelWidth, barY - 3, AllianceUi.TEXT);
    }
}
