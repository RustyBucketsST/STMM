package com.skibiditoilet.client.story;

import com.skibiditoilet.story.Mission;
import com.skibiditoilet.story.Spawn;
import com.skibiditoilet.story.Stage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

import java.util.Locale;

/** The words for missions shared by the story screen, the HUD and the toasts. */
final class MissionText {
    private MissionText() {
    }

    /** "Ep. 7: First Flush" */
    static Text title(Mission mission) {
        return Text.translatable("story.skibidi.episode_title", mission.episode(), Text.translatable(mission.translationKey()));
    }

    /** What to do in a stage: its own line if the mission has one, otherwise "Survive" / "Defeat <boss>" / "Defeat the toilets". */
    static MutableText objective(Mission mission, int stageIndex) {
        String custom = mission.stageKey(stageIndex);
        if (I18n.hasTranslation(custom)) {
            return Text.translatable(custom);
        }
        Stage stage = mission.stages().get(stageIndex);
        Spawn boss = stage.boss();
        if (stage.kind() == Stage.Kind.SURVIVE) {
            return Text.translatable("story.skibidi.objective.survive");
        }
        return boss != null
                ? Text.translatable("story.skibidi.objective.boss", boss.type().getName())
                : Text.translatable("story.skibidi.objective.defeat");
    }

    /** The objective with its size, for the mission details: "Survive (0:30)", "Defeat the toilets (x10)". */
    static Text objectiveWithSize(Mission mission, int stageIndex) {
        Stage stage = mission.stages().get(stageIndex);
        MutableText text = objective(mission, stageIndex);
        if (stage.kind() == Stage.Kind.SURVIVE) {
            return text.append(" (" + time(stage.seconds() * 20) + ")");
        }
        return stage.boss() == null ? text.append(" (x" + stage.enemyCount() + ")") : text;
    }

    /** The text as is if it fits, otherwise cut short with "...". */
    static OrderedText fit(TextRenderer textRenderer, Text text, int width) {
        if (textRenderer.getWidth(text) <= width) {
            return text.asOrderedText();
        }
        StringVisitable cut = textRenderer.trimToWidth(text, width - textRenderer.getWidth("..."));
        return Language.getInstance().reorder(StringVisitable.concat(cut, StringVisitable.plain("...")));
    }

    /** m:ss */
    static String time(int ticks) {
        int seconds = (ticks + 19) / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
