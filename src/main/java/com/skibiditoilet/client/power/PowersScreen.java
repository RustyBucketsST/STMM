package com.skibiditoilet.client.power;

import com.skibiditoilet.character.CharacterData;
import com.skibiditoilet.client.character.AllianceButton;
import com.skibiditoilet.client.character.AllianceUi;
import com.skibiditoilet.client.character.ClientCharacterState;
import com.skibiditoilet.power.Power;
import com.skibiditoilet.power.PowerData;
import com.skibiditoilet.power.Tech;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * The Powers menu: your points, your type's powers (unlock them with points) and the tech your type
 * builds (spend points to get one, or a replacement for half price).
 */
public class PowersScreen extends Screen {
    private static final int WIDTH = 330;
    private static final int ROW = 32;
    private static final int SECTION = 14;
    private static final int BUTTON_WIDTH = 84;

    private final List<Power> powers;
    private final List<Tech> techs;
    private final List<Row> rows = new ArrayList<>();
    private int left;
    private int top;
    private int panelHeight;

    private record Row(int y, Text name, Text details, Text description, Power power, Tech tech) {
    }

    public PowersScreen() {
        super(Text.translatable("screen.skibidi.powers"));
        CharacterData character = ClientCharacterState.self();
        this.powers = Power.of(character.type());
        this.techs = Tech.of(character.type());
    }

    @Override
    protected void init() {
        this.rows.clear();
        this.panelHeight = 13 + 16 + SECTION + this.powers.size() * ROW + (this.techs.isEmpty() ? 0 : SECTION + this.techs.size() * ROW) + 6;
        this.left = (this.width - WIDTH) / 2;
        this.top = Math.max(4, (this.height - this.panelHeight) / 2);
        int y = this.top + 13 + 16 + SECTION;
        for (int slot = 0; slot < this.powers.size(); slot++) {
            Power power = this.powers.get(slot);
            Text key = ClientPowerState.POWER_KEYS[slot].getBoundKeyLocalizedText();
            Text details = Text.translatable("screen.skibidi.powers.details", key, power.energyCost(),
                    String.format("%.0f", power.cooldown() / 20.0f));
            this.rows.add(new Row(y, Text.translatable(power.translationKey()), details,
                    Text.translatable(power.descriptionKey()), power, null));
            this.addDrawableChild(new AllianceButton(this.left + WIDTH - BUTTON_WIDTH - 6, y + 8, BUTTON_WIDTH, 16,
                    Text.empty(), () -> ClientPowerState.unlock(power)) {
                @Override
                public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
                    PowerData data = ClientPowerState.data();
                    boolean has = data.has(power);
                    this.active = !has && data.points() >= power.pointCost();
                    this.setMessage(power.isFree() ? Text.translatable("screen.skibidi.powers.free")
                            : has ? Text.translatable("screen.skibidi.powers.unlocked")
                            : Text.translatable("screen.skibidi.powers.unlock", power.pointCost()));
                    super.renderButton(context, mouseX, mouseY, delta);
                }
            });
            y += ROW;
        }
        if (!this.techs.isEmpty()) {
            y += SECTION;
            for (Tech tech : this.techs) {
                this.rows.add(new Row(y, tech.item().getName(), Text.empty(), Text.translatable(tech.descriptionKey()), null, tech));
                this.addDrawableChild(new AllianceButton(this.left + WIDTH - BUTTON_WIDTH - 6, y + 8, BUTTON_WIDTH, 16,
                        Text.empty(), () -> ClientPowerState.build(tech)) {
                    @Override
                    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
                        PowerData data = ClientPowerState.data();
                        int cost = tech.cost(data.hasBuilt(tech));
                        this.active = data.points() >= cost;
                        this.setMessage(Text.translatable(data.hasBuilt(tech) ? "screen.skibidi.powers.rebuild"
                                : "screen.skibidi.powers.build", cost));
                        super.renderButton(context, mouseX, mouseY, delta);
                    }
                });
                y += ROW;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        CharacterData character = ClientCharacterState.self();
        Text title = Text.translatable("screen.skibidi.powers.title", Text.translatable(character.type().translationKey()));
        AllianceUi.panel(context, this.textRenderer, title, this.left, this.top, WIDTH, this.panelHeight);

        PowerData data = ClientPowerState.data();
        int y = this.top + 17;
        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.skibidi.powers.points", data.points()),
                this.left + 6, y, 0xFFD34D);
        Text energy = Text.translatable("screen.skibidi.powers.energy", (int) ClientPowerState.maxEnergy());
        context.drawTextWithShadow(this.textRenderer, energy, this.left + WIDTH - 6 - this.textRenderer.getWidth(energy), y,
                AllianceUi.TEXT_DIM);

        section(context, Text.translatable("screen.skibidi.powers.section.powers"), this.top + 13 + 16);
        if (!this.techs.isEmpty()) {
            section(context, Text.translatable("screen.skibidi.powers.section.tech"),
                    this.top + 13 + 16 + SECTION + this.powers.size() * ROW);
        }
        for (Row row : this.rows) {
            row(context, row, data);
        }
        super.render(context, mouseX, mouseY, delta);
        // The whole description, in case it didn't fit.
        for (Row row : this.rows) {
            if (mouseX >= this.left + 6 && mouseX < this.left + WIDTH - BUTTON_WIDTH - 8 && mouseY >= row.y && mouseY < row.y + ROW - 2) {
                context.drawOrderedTooltip(this.textRenderer, this.textRenderer.wrapLines(row.description, 200), mouseX, mouseY);
            }
        }
    }

    private void section(DrawContext context, Text label, int y) {
        context.drawTextWithShadow(this.textRenderer, label, this.left + 6, y + 3, AllianceUi.TITLE);
        int lineX = this.left + 10 + this.textRenderer.getWidth(label);
        context.fill(lineX, y + 7, this.left + WIDTH - 6, y + 8, AllianceUi.BORDER_DIM);
    }

    private void row(DrawContext context, Row row, PowerData data) {
        int x = this.left + 6;
        boolean owned = row.power != null ? data.has(row.power) : data.hasBuilt(row.tech);
        context.fill(x, row.y + 2, x + 20, row.y + 22, 0xFF0A121B);
        context.drawBorder(x, row.y + 2, 20, 20, owned ? AllianceUi.BORDER : AllianceUi.BORDER_DIM);
        if (row.power != null) {
            context.drawTexture(PowerHud.icon(row.power), x + 2, row.y + 4, 0, 0, 16, 16, 16, 16);
            if (!owned) {
                context.fill(x + 2, row.y + 4, x + 18, row.y + 20, 0x90000000);
            }
        } else {
            context.drawItem(row.tech.item().getDefaultStack(), x + 2, row.y + 4);
        }
        int textX = x + 26;
        int textWidth = WIDTH - 6 - BUTTON_WIDTH - 8 - 26 - 6;
        Text name = row.name.copy().formatted(owned ? Formatting.WHITE : Formatting.GRAY);
        context.drawTextWithShadow(this.textRenderer, name, textX, row.y + 2, 0xFFFFFF);
        if (!row.details.getString().isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, row.details, textX + this.textRenderer.getWidth(name) + 5, row.y + 2,
                    AllianceUi.TEXT_DIM);
        }
        List<OrderedText> lines = this.textRenderer.wrapLines(row.description, textWidth);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            context.drawTextWithShadow(this.textRenderer, lines.get(i), textX, row.y + 12 + i * 9, AllianceUi.TEXT_DIM);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientPowerState.isMenuKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
