package com.skibiditoilet.client.character;

import com.skibiditoilet.character.AllianceType;
import com.skibiditoilet.character.CharacterData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

/** First creator screen: pick Cameraman, Speakerman or TV Man, shown on yourself. */
public class TypeSelectScreen extends Screen {
    private static final int STEPS = 5;

    private CharacterData data;
    private float previewYaw;

    private int previewLeft;
    private int previewTop;
    private int previewWidth;
    private int previewHeight;

    public TypeSelectScreen(CharacterData data) {
        super(Text.translatable("screen.skibidi.creator.choose_type"));
        this.data = data;
    }

    @Override
    protected void init() {
        CharacterPreview.set(this.data);
        int centerX = this.width / 2;
        this.previewWidth = 140;
        this.previewLeft = centerX - this.previewWidth / 2;
        this.previewTop = 28;
        this.previewHeight = this.height - this.previewTop - 80;
        int arrowY = this.previewTop + this.previewHeight / 2 - 10;
        this.addDrawableChild(new AllianceButton(this.previewLeft - 26, arrowY, 20, 20, Text.literal("<"), () -> this.cycleType(-1)));
        this.addDrawableChild(new AllianceButton(this.previewLeft + this.previewWidth + 6, arrowY, 20, 20, Text.literal(">"),
                () -> this.cycleType(1)));
        this.addDrawableChild(new AllianceButton(centerX - 50, this.height - 26, 100, 20, Text.translatable("screen.skibidi.creator.next"),
                () -> this.client.setScreen(CustomizeScreen.create(this.data))));
    }

    private void cycleType(int step) {
        AllianceType[] types = AllianceType.values();
        AllianceType next = types[Math.floorMod(this.data.type().ordinal() + step, types.length)];
        // A new type starts from its own default look, keeping the body style and class already picked.
        this.data = CharacterData.forType(next).withBody(this.data.body()).withClass(this.data.playerClass())
                .withSuitColor(this.data.suitColor()).withTieColor(this.data.tieColor());
        CharacterPreview.set(this.data);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 6, AllianceUi.TITLE);
        AllianceUi.progress(context, centerX - 60, 18, 120, 0, STEPS);

        AllianceUi.panel(context, this.previewLeft, this.previewTop, this.previewWidth, this.previewHeight);
        int size = (int) Math.min((this.previewHeight - 12) / 2.3f, 60.0f);
        context.enableScissor(this.previewLeft + 1, this.previewTop + 1,
                this.previewLeft + this.previewWidth - 1, this.previewTop + this.previewHeight - 1);
        CharacterPreview.drawSpinning(context, centerX, this.previewTop + this.previewHeight - 8, size, this.previewYaw, this.client.player);
        context.disableScissor();
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.skibidi.creator.drag_hint"),
                centerX, this.previewTop + 4, AllianceUi.TEXT_DIM);

        int infoWidth = Math.min(300, this.width - 12);
        int infoTop = this.height - 74;
        AllianceUi.panel(context, this.textRenderer, Text.translatable(this.data.type().translationKey()),
                centerX - infoWidth / 2, infoTop, infoWidth, 44);
        List<OrderedText> lines = this.textRenderer.wrapLines(Text.translatable(this.data.type().descriptionKey()), infoWidth - 12);
        for (int i = 0; i < Math.min(lines.size(), 3); i++) {
            context.drawCenteredTextWithShadow(this.textRenderer, lines.get(i), centerX, infoTop + 17 + i * 9, AllianceUi.TEXT);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        this.previewYaw += (float) deltaX * 2.0f;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void close() {
        // You can't skip making a character: Esc goes to the pause menu and the creator comes back after.
        ClientCharacterState.pauseCreator(this.data);
        this.client.setScreen(new GameMenuScreen(true));
    }

    @Override
    public void removed() {
        CharacterPreview.set(null);
        ClientCharacterState.saveDraft(this.data);
    }
}
