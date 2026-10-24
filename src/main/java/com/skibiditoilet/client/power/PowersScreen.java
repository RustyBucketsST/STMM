package com.skibiditoilet.client.power;

import com.skibiditoilet.character.CharacterData;
import com.skibiditoilet.client.character.AllianceButton;
import com.skibiditoilet.client.character.AllianceUi;
import com.skibiditoilet.client.character.ClientCharacterState;
import com.skibiditoilet.power.Form;
import com.skibiditoilet.power.Gear;
import com.skibiditoilet.power.Power;
import com.skibiditoilet.power.PowerData;
import com.skibiditoilet.power.Tech;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The Powers menu: your points and four tabs. Powers: your type's powers (unlock them with points) and the tech your
 * type builds (spend points to get one, or a replacement for half price). Forms: the bigger forms, bought in order.
 * Titan: the Titan moves that come with the Titan form, and which keys they take. Gear: gear to buy, which shows on
 * you.
 */
public class PowersScreen extends Screen {
    private static final int WIDTH = 330;
    private static final int ROW = 32;
    private static final int SECTION = 14;
    private static final int BUTTON_WIDTH = 84;
    private static final int TAB_WIDTH = 70;
    /** The title, the points line, then the row of tabs. */
    private static final int TOP = 13 + 16 + 16;

    private enum Tab {
        POWERS, FORMS, TITAN, GEAR
    }

    /** The tab the menu opens on: the one it was left on. */
    private static Tab tab = Tab.POWERS;

    private final List<Power> powers;
    private final List<Power> titanKeys;
    private final List<Tech> techs;
    private final List<Row> rows = new ArrayList<>();
    private final List<Section> sections = new ArrayList<>();
    private int left;
    private int top;
    private int panelHeight;

    private record Row(int y, Text name, Text details, Text description, Power power, Tech tech, Form form, Gear gear) {
    }

    private record Section(int y, Text label) {
    }

    public PowersScreen() {
        super(Text.translatable("screen.skibidi.powers"));
        CharacterData character = ClientCharacterState.self();
        this.powers = Power.of(character.type());
        this.titanKeys = Power.onKeys(character.type(), Form.TITAN);
        this.techs = Tech.of(character.type());
    }

    @Override
    protected void init() {
        this.rows.clear();
        this.sections.clear();
        int content = switch (tab) {
            case POWERS -> SECTION + this.powers.size() * ROW + (this.techs.isEmpty() ? 0 : SECTION + this.techs.size() * ROW);
            case FORMS -> SECTION + Form.unlockable().length * ROW;
            case TITAN -> SECTION + this.titanKeys.size() * ROW;
            case GEAR -> SECTION + Gear.values().length * ROW;
        };
        this.panelHeight = TOP + content + 6;
        this.left = (this.width - WIDTH) / 2;
        this.top = Math.max(4, (this.height - this.panelHeight) / 2);
        int tabX = this.left + (WIDTH - Tab.values().length * (TAB_WIDTH + 4) + 4) / 2;
        for (Tab each : Tab.values()) {
            this.addDrawableChild(new AllianceButton(tabX + each.ordinal() * (TAB_WIDTH + 4), this.top + 28, TAB_WIDTH, 13,
                    Text.translatable("screen.skibidi.powers.section." + each.name().toLowerCase()), () -> tab == each,
                    () -> {
                        tab = each;
                        this.clearAndInit();
                    }));
        }
        int y = this.top + TOP;
        if (tab == Tab.POWERS) {
            y = this.addPowers(y);
            if (!this.techs.isEmpty()) {
                this.addTechs(y);
            }
        } else if (tab == Tab.FORMS) {
            this.addForms(y);
        } else if (tab == Tab.TITAN) {
            this.addTitanKeys(y);
        } else {
            this.addGear(y);
        }
    }

    /** Gear to buy, which shows on you in every form. Shoulder gear is your type's own. */
    private void addGear(int y) {
        this.sections.add(new Section(y, Text.translatable("screen.skibidi.powers.section.gear")));
        y += SECTION;
        String type = ClientCharacterState.self().type().asString();
        for (Gear gear : Gear.values()) {
            String key = gear == Gear.SHOULDERS ? gear.translationKey() + "." + type : gear.translationKey();
            this.rows.add(new Row(y, Text.translatable(key), Text.empty(), Text.translatable(key + ".description"),
                    null, null, null, gear));
            this.addDrawableChild(new AllianceButton(this.left + WIDTH - BUTTON_WIDTH - 6, y + 8, BUTTON_WIDTH, 16,
                    Text.empty(), () -> ClientPowerState.unlock(gear)) {
                @Override
                public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
                    PowerData data = ClientPowerState.data();
                    boolean has = data.has(gear);
                    this.active = !has && data.points() >= gear.pointCost();
                    this.setMessage(has ? Text.translatable("screen.skibidi.powers.owned")
                            : Text.translatable("screen.skibidi.powers.buy", gear.pointCost()));
                    super.renderButton(context, mouseX, mouseY, delta);
                }
            });
            y += ROW;
        }
    }

    /** The keys in a Titan form: the Titan moves (which come with the form), then the type's own powers kept. */
    private void addTitanKeys(int y) {
        this.sections.add(new Section(y, Text.translatable("screen.skibidi.powers.section.titan_keys")));
        y += SECTION;
        for (int slot = 0; slot < this.titanKeys.size(); slot++) {
            Power power = this.titanKeys.get(slot);
            Text key = ClientPowerState.POWER_KEYS[slot].getBoundKeyLocalizedText();
            Text details = Text.translatable("screen.skibidi.powers.details", key, power.energyCost(),
                    String.format("%.0f", power.cooldown() / 20.0f));
            this.rows.add(new Row(y, Text.translatable(power.translationKey()), details,
                    Text.translatable(power.descriptionKey()), power, null, null, null));
            this.addDrawableChild(new AllianceButton(this.left + WIDTH - BUTTON_WIDTH - 6, y + 8, BUTTON_WIDTH, 16,
                    Text.empty(), () -> {
            }) {
                @Override
                public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
                    PowerData data = ClientPowerState.data();
                    this.active = false;
                    this.setMessage(!power.isTitanMove()
                            ? Text.translatable(data.has(power) ? "screen.skibidi.powers.kept" : "screen.skibidi.powers.locked")
                            : Text.translatable(data.has(Form.TITAN) ? "screen.skibidi.powers.titan_ready"
                            : "screen.skibidi.powers.titan_needed"));
                    super.renderButton(context, mouseX, mouseY, delta);
                }
            });
            y += ROW;
        }
    }

    private int addPowers(int y) {
        this.sections.add(new Section(y, Text.translatable("screen.skibidi.powers.section.powers")));
        y += SECTION;
        for (int slot = 0; slot < this.powers.size(); slot++) {
            Power power = this.powers.get(slot);
            Text key = ClientPowerState.POWER_KEYS[slot].getBoundKeyLocalizedText();
            Text details = Text.translatable("screen.skibidi.powers.details", key, power.energyCost(),
                    String.format("%.0f", power.cooldown() / 20.0f));
            this.rows.add(new Row(y, Text.translatable(power.translationKey()), details,
                    Text.translatable(power.descriptionKey()), power, null, null, null));
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
        return y;
    }

    private void addTechs(int y) {
        this.sections.add(new Section(y, Text.translatable("screen.skibidi.powers.section.tech")));
        y += SECTION;
        for (Tech tech : this.techs) {
            this.rows.add(new Row(y, tech.item().getName(), Text.empty(), Text.translatable(tech.descriptionKey()),
                    null, tech, null, null));
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

    private void addForms(int y) {
        this.sections.add(new Section(y, Text.translatable("screen.skibidi.powers.section.forms")));
        y += SECTION;
        Text key = ClientPowerState.transformKey().getBoundKeyLocalizedText();
        for (Form form : Form.unlockable()) {
            Text details = Text.translatable("screen.skibidi.powers.form_details", key, form.transformCost(),
                    String.format("%.1f", form.drain()));
            Text description = Text.translatable(form.descriptionKey());
            Form previous = form.previous();
            if (previous != null && previous != Form.NORMAL) {
                description = description.copy().append(" ").append(Text.translatable("screen.skibidi.powers.form_needs",
                        Text.translatable(previous.translationKey())));
            }
            this.rows.add(new Row(y, Text.translatable(form.translationKey()), details, description, null, null, form, null));
            this.addDrawableChild(new AllianceButton(this.left + WIDTH - BUTTON_WIDTH - 6, y + 8, BUTTON_WIDTH, 16,
                    Text.empty(), () -> ClientPowerState.unlock(form)) {
                @Override
                public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
                    PowerData data = ClientPowerState.data();
                    boolean has = data.has(form);
                    this.active = !has && (previous == null || data.has(previous)) && data.points() >= form.pointCost();
                    this.setMessage(has ? Text.translatable("screen.skibidi.powers.unlocked")
                            : Text.translatable("screen.skibidi.powers.unlock", form.pointCost()));
                    super.renderButton(context, mouseX, mouseY, delta);
                }
            });
            y += ROW;
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

        for (Section section : this.sections) {
            section(context, section.label, section.y);
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
        boolean owned = row.power != null ? (row.power.isTitanMove() ? data.has(Form.TITAN) : data.has(row.power))
                : row.form != null ? data.has(row.form) : row.gear != null ? data.has(row.gear) : data.hasBuilt(row.tech);
        context.fill(x, row.y + 2, x + 20, row.y + 22, 0xFF0A121B);
        context.drawBorder(x, row.y + 2, 20, 20, owned ? AllianceUi.BORDER : AllianceUi.BORDER_DIM);
        if (row.power != null || row.form != null || row.gear != null) {
            Identifier icon = row.power != null ? PowerHud.icon(row.power) : row.form != null ? PowerHud.icon(row.form)
                    : PowerHud.icon(row.gear);
            context.drawTexture(icon, x + 2, row.y + 4, 0, 0, 16, 16, 16, 16);
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
