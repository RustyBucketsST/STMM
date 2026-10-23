package com.skibiditoilet.client.character;

import com.skibiditoilet.character.AllianceType;
import com.skibiditoilet.character.BodyStyle;
import com.skibiditoilet.character.CharacterData;
import com.skibiditoilet.character.CharacterPalette;
import com.skibiditoilet.character.PlayerClass;
import com.skibiditoilet.character.Stat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The customizer: the player previewed on the left (zooming in on the head for colors and details)
 * and tabs of choices on the right. Used by the creator (ends with the class pick and creates the
 * character) and when sitting on a toilet (looks only).
 */
public class CustomizeScreen extends Screen {
    private enum Tab { BODY, COLORS, DETAILS, CLASS }

    private enum ColorSlot { CASING, ACCENT, SUIT, TIE }

    private static final int MARGIN = 6;
    private static final int SWATCH = 12;
    private static final int MAX_STAT = 10;

    private final boolean creating;
    private final List<Tab> tabs;
    private CharacterData data;
    private int tabIndex;
    private boolean submitted;

    private float previewYaw = -25.0f;
    // 0 shows the whole body, 1 is a close-up of the head; eased toward the current tab's choice.
    private float zoom;

    private ColorSlot colorSlot = ColorSlot.CASING;
    private float hue;
    private float saturation;
    private float brightness;
    @Nullable
    private TextFieldWidget hexField;
    private boolean syncingHex;

    // Built by init() for the current tab and drawn by render().
    private final List<Label> labels = new ArrayList<>();
    private final List<StatRow> statRows = new ArrayList<>();

    private int previewX;
    private int previewWidth;
    private int panelX;
    private int panelWidth;
    private int panelTop;
    private int panelHeight;

    private CustomizeScreen(CharacterData data, boolean creating) {
        super(Text.translatable("screen.skibidi.creator.customize"));
        this.data = data;
        this.creating = creating;
        this.tabs = creating ? List.of(Tab.values()) : List.of(Tab.BODY, Tab.COLORS, Tab.DETAILS);
    }

    public static CustomizeScreen create(CharacterData data) {
        return new CustomizeScreen(data, true);
    }

    public static CustomizeScreen recustomize(CharacterData data) {
        return new CustomizeScreen(data, false);
    }

    @Override
    protected void init() {
        CharacterPreview.set(this.data);
        this.labels.clear();
        this.statRows.clear();
        this.hexField = null;

        this.panelTop = 28;
        this.panelHeight = this.height - this.panelTop - 32;
        this.previewX = MARGIN;
        this.previewWidth = MathHelper.clamp(this.width * 2 / 5, 110, 180);
        this.panelX = this.previewX + this.previewWidth + MARGIN;
        this.panelWidth = this.width - this.panelX - MARGIN;

        int tabCount = this.tabs.size();
        int tabWidth = (this.panelWidth - 8 - (tabCount - 1) * 2) / tabCount;
        for (int i = 0; i < tabCount; i++) {
            int index = i;
            Text name = Text.translatable("screen.skibidi.creator.tab." + this.tabs.get(i).name().toLowerCase(Locale.ROOT));
            this.addDrawableChild(new AllianceButton(this.panelX + 4 + i * (tabWidth + 2), this.panelTop + 4, tabWidth, 14,
                    name, () -> this.tabIndex == index, () -> this.switchTab(index)));
        }

        int left = this.panelX + 6;
        int top = this.panelTop + 24;
        int width = this.panelWidth - 12;
        switch (this.tabs.get(this.tabIndex)) {
            case BODY -> this.buildBodyTab(left, top, width);
            case COLORS -> this.buildColorsTab(left, top, width);
            case DETAILS -> this.buildDetailsTab(left, top, width);
            case CLASS -> this.buildClassTab(left, top, width);
        }

        boolean first = this.tabIndex == 0;
        boolean last = this.tabIndex == tabCount - 1;
        Text backText = Text.translatable(first && !this.creating ? "screen.skibidi.creator.cancel" : "screen.skibidi.creator.back");
        this.addDrawableChild(new AllianceButton(MARGIN, this.height - 26, 70, 20, backText, this::back));
        Text nextText = Text.translatable(last ? "screen.skibidi.creator.done" : "screen.skibidi.creator.next");
        this.addDrawableChild(new AllianceButton(this.width - MARGIN - 70, this.height - 26, 70, 20, nextText, this::next));
    }

    // ---------------------------------------------------------------- tabs

    private void buildBodyTab(int left, int top, int width) {
        this.labels.add(new Label(Text.translatable("screen.skibidi.creator.look"), left, top, width, AllianceUi.TEXT));
        int cardWidth = (width - 4) / 2;
        BodyStyle[] styles = BodyStyle.values();
        for (int i = 0; i < styles.length; i++) {
            BodyStyle style = styles[i];
            int x = left + i * (cardWidth + 4);
            this.addDrawableChild(new AllianceButton(x, top + 12, cardWidth, 20, Text.translatable(style.translationKey()),
                    () -> this.data.body() == style, () -> this.setData(this.data.withBody(style))));
            this.labels.add(new Label(Text.translatable(style.descriptionKey()), x, top + 36, cardWidth, AllianceUi.TEXT_DIM));
        }
    }

    private void buildColorsTab(int left, int top, int width) {
        List<ColorSlot> slots = this.data.body() == BodyStyle.SUIT
                ? List.of(ColorSlot.values())
                : List.of(ColorSlot.CASING, ColorSlot.ACCENT);
        if (!slots.contains(this.colorSlot)) {
            this.colorSlot = ColorSlot.CASING;
        }
        int slotWidth = (width - (slots.size() - 1) * 3) / slots.size();
        for (int i = 0; i < slots.size(); i++) {
            ColorSlot slot = slots.get(i);
            this.addDrawableChild(new AllianceButton(left + i * (slotWidth + 3), top, slotWidth, 16, this.slotName(slot),
                    () -> this.colorSlot == slot, () -> this.selectSlot(slot)));
        }

        // Ready-made colors: two rows of eight.
        int paletteTop = top + 22;
        List<Integer> colors = CharacterPalette.COLORS;
        for (int i = 0; i < colors.size(); i++) {
            this.addDrawableChild(new ColorSwatchWidget(left + (i % 8) * (SWATCH + 2), paletteTop + (i / 8) * (SWATCH + 2),
                    SWATCH, colors.get(i), this::currentColor, this::pickColor));
        }

        // Any color: hue, saturation and brightness sliders plus a #RRGGBB box.
        int customTop = paletteTop + 2 * (SWATCH + 2) + 6;
        this.labels.add(new Label(Text.translatable("screen.skibidi.creator.custom_color"), left, customTop, width, AllianceUi.TEXT_DIM));
        int sliderX = left + 10;
        int sliderWidth = Math.min(width - 12, 130);
        String[] names = {"hue", "saturation", "brightness"};
        for (int i = 0; i < names.length; i++) {
            this.labels.add(new Label(Text.translatable("screen.skibidi.creator." + names[i]), left, customTop + 11 + i * 12, 10, AllianceUi.TEXT));
        }
        this.addDrawableChild(new GradientSlider(sliderX, customTop + 12, sliderWidth, 7, Text.translatable("screen.skibidi.creator.hue"),
                t -> MathHelper.hsvToRgb((float) t, 1.0f, 1.0f), () -> this.hue, v -> {
                    this.hue = (float) v;
                    this.applyHsv();
                }));
        this.addDrawableChild(new GradientSlider(sliderX, customTop + 24, sliderWidth, 7, Text.translatable("screen.skibidi.creator.saturation"),
                t -> MathHelper.hsvToRgb(this.hue, (float) t, this.brightness), () -> this.saturation, v -> {
                    this.saturation = (float) v;
                    this.applyHsv();
                }));
        this.addDrawableChild(new GradientSlider(sliderX, customTop + 36, sliderWidth, 7, Text.translatable("screen.skibidi.creator.brightness"),
                t -> MathHelper.hsvToRgb(this.hue, this.saturation, (float) t), () -> this.brightness, v -> {
                    this.brightness = (float) v;
                    this.applyHsv();
                }));

        this.hexField = new TextFieldWidget(this.textRenderer, sliderX, customTop + 50, 60, 14, Text.literal("#"));
        this.hexField.setMaxLength(7);
        this.hexField.setChangedListener(this::onHexTyped);
        this.addDrawableChild(this.hexField);
        this.loadHsv();
        this.syncHex();
    }

    private void buildDetailsTab(int left, int top, int width) {
        AllianceType type = this.data.type();
        int y = top;
        int perRow = 3;
        int buttonWidth = (width - (perRow - 1) * 3) / perRow;
        for (int slot = 0; slot < type.detailSlots().size(); slot++) {
            AllianceType.DetailSlot detail = type.detailSlots().get(slot);
            this.labels.add(new Label(Text.translatable(detail.translationKey(type)), left, y, width, AllianceUi.TEXT));
            for (int option = 0; option < detail.options().size(); option++) {
                int slotIndex = slot;
                int optionIndex = option;
                this.addDrawableChild(new AllianceButton(left + (option % perRow) * (buttonWidth + 3), y + 11 + (option / perRow) * 21,
                        buttonWidth, 18, Text.translatable(detail.optionKey(type, option)),
                        () -> this.data.detail(slotIndex) == optionIndex,
                        () -> this.setData(this.data.withDetail(slotIndex, optionIndex))));
            }
            int rows = (detail.options().size() + perRow - 1) / perRow;
            y += 11 + rows * 21 + 6;
        }
    }

    private void buildClassTab(int left, int top, int width) {
        this.labels.add(new Label(Text.translatable("screen.skibidi.creator.pick_class"), left, top, width, AllianceUi.TEXT));
        int buttonWidth = (width - 3) / 2;
        PlayerClass[] classes = PlayerClass.values();
        for (int i = 0; i < classes.length; i++) {
            PlayerClass playerClass = classes[i];
            this.addDrawableChild(new AllianceButton(left + (i % 2) * (buttonWidth + 3), top + 11 + (i / 2) * 21, buttonWidth, 18,
                    Text.translatable(playerClass.translationKey()), () -> this.data.playerClass() == playerClass, () -> {
                        this.setData(this.data.withClass(playerClass));
                        this.clearAndInit();
                    }));
        }
        int descriptionTop = top + 11 + 2 * 21 + 3;
        this.labels.add(new Label(Text.translatable(this.data.playerClass().descriptionKey()), left, descriptionTop, width, AllianceUi.TEXT_DIM));
        int statsTop = descriptionTop + 24;
        this.labels.add(new Label(Text.translatable("screen.skibidi.creator.starting_stats"), left, statsTop, width, AllianceUi.TITLE));
        Stat[] stats = Stat.values();
        for (int i = 0; i < stats.length; i++) {
            this.statRows.add(new StatRow(stats[i], left, statsTop + 12 + i * 11, width));
        }
    }

    // ---------------------------------------------------------------- colors

    private Text slotName(ColorSlot slot) {
        return Text.translatable(switch (slot) {
            case CASING -> "character.skibidi.color.short.casing";
            case ACCENT -> "character.skibidi.color.short.accent." + this.data.type().asString();
            case SUIT -> "character.skibidi.color.short.suit";
            case TIE -> "character.skibidi.color.short.tie";
        });
    }

    private int currentColor() {
        return switch (this.colorSlot) {
            case CASING -> this.data.casingColor();
            case ACCENT -> this.data.accentColor();
            case SUIT -> this.data.suitColor();
            case TIE -> this.data.tieColor();
        };
    }

    private void setColor(int color) {
        this.setData(switch (this.colorSlot) {
            case CASING -> this.data.withCasingColor(color);
            case ACCENT -> this.data.withAccentColor(color);
            case SUIT -> this.data.withSuitColor(color);
            case TIE -> this.data.withTieColor(color);
        });
    }

    private void selectSlot(ColorSlot slot) {
        this.colorSlot = slot;
        this.loadHsv();
        this.syncHex();
    }

    private void pickColor(int color) {
        this.setColor(color);
        this.loadHsv();
        this.syncHex();
    }

    private void applyHsv() {
        this.setColor(MathHelper.hsvToRgb(this.hue, this.saturation, this.brightness) & 0xFFFFFF);
        this.syncHex();
    }

    /** Sets the sliders from the current color. Greys have no hue, so the hue slider stays where it was. */
    private void loadHsv() {
        int color = this.currentColor();
        float red = (color >> 16 & 0xFF) / 255.0f;
        float green = (color >> 8 & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float range = max - min;
        this.brightness = max;
        this.saturation = max == 0.0f ? 0.0f : range / max;
        if (range > 0.0f) {
            float h;
            if (max == red) {
                h = (green - blue) / range;
            } else if (max == green) {
                h = 2.0f + (blue - red) / range;
            } else {
                h = 4.0f + (red - green) / range;
            }
            this.hue = (h / 6.0f + 1.0f) % 1.0f;
        }
    }

    private void syncHex() {
        if (this.hexField != null) {
            this.syncingHex = true;
            this.hexField.setText(String.format("#%06X", this.currentColor()));
            this.syncingHex = false;
        }
    }

    private void onHexTyped(String text) {
        if (this.syncingHex) {
            return;
        }
        String digits = text.startsWith("#") ? text.substring(1) : text;
        if (digits.length() == 6 && digits.chars().allMatch(c -> Character.digit(c, 16) >= 0)) {
            this.setColor(Integer.parseInt(digits, 16));
            this.loadHsv();
        }
    }

    // ---------------------------------------------------------------- navigation

    private void setData(CharacterData updated) {
        this.data = updated;
        CharacterPreview.set(updated);
    }

    private void switchTab(int index) {
        this.tabIndex = index;
        this.clearAndInit();
    }

    private void back() {
        if (this.tabIndex > 0) {
            this.switchTab(this.tabIndex - 1);
        } else if (this.creating) {
            this.client.setScreen(new TypeSelectScreen(this.data));
        } else {
            this.client.setScreen(null);
        }
    }

    private void next() {
        if (this.tabIndex < this.tabs.size() - 1) {
            this.switchTab(this.tabIndex + 1);
            return;
        }
        this.submitted = true;
        if (this.creating) {
            ClientCharacterState.submitCreated(this.data);
        } else {
            ClientCharacterState.submitLook(this.data);
        }
        this.client.setScreen(null);
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 6, AllianceUi.TITLE);
        int steps = this.creating ? this.tabs.size() + 1 : this.tabs.size();
        int step = this.creating ? this.tabIndex + 1 : this.tabIndex;
        AllianceUi.progress(context, this.width / 2 - 60, 18, 120, step, steps);

        AllianceUi.panel(context, this.textRenderer, Text.translatable(this.data.type().translationKey()),
                this.previewX, this.panelTop, this.previewWidth, this.panelHeight);
        this.drawPreview(context);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.skibidi.creator.drag_hint"),
                this.previewX + this.previewWidth / 2, this.panelTop + this.panelHeight - 10, AllianceUi.TEXT_DIM);

        AllianceUi.panel(context, this.panelX, this.panelTop, this.panelWidth, this.panelHeight);
        for (Label label : this.labels) {
            List<OrderedText> lines = this.textRenderer.wrapLines(label.text, label.width);
            for (int i = 0; i < lines.size(); i++) {
                context.drawTextWithShadow(this.textRenderer, lines.get(i), label.x, label.y + i * 10, label.color);
            }
        }
        for (StatRow row : this.statRows) {
            this.drawStat(context, row);
        }
        if (this.hexField != null) {
            int x = this.hexField.getX() + this.hexField.getWidth() + 4;
            int y = this.hexField.getY();
            context.fill(x, y, x + 14, y + 14, AllianceUi.BORDER);
            context.fill(x + 1, y + 1, x + 13, y + 13, 0xFF000000 | this.currentColor());
        }

        super.render(context, mouseX, mouseY, delta);

        for (StatRow row : this.statRows) {
            if (mouseX >= row.x && mouseX < row.x + row.width && mouseY >= row.y - 1 && mouseY < row.y + 10) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.translatable(row.stat.translationKey()).formatted(Formatting.AQUA),
                        Text.translatable(row.stat.descriptionKey())), mouseX, mouseY);
            }
        }
    }

    private void drawPreview(DrawContext context) {
        Tab tab = this.tabs.get(this.tabIndex);
        float target = tab == Tab.COLORS || tab == Tab.DETAILS ? 1.0f : 0.0f;
        this.zoom += (target - this.zoom) * Math.min(1.0f, this.client.getLastFrameDuration() * 0.3f);

        int innerTop = this.panelTop + 14;
        int innerBottom = this.panelTop + this.panelHeight - 13;
        float bodySize = Math.min((innerBottom - innerTop - 6) / 2.3f, this.previewWidth / 1.6f);
        float size = MathHelper.lerp(this.zoom, bodySize, bodySize * 2.3f);
        // Height above the feet (in blocks) kept in the middle: the chest for the whole body, the head when zoomed.
        float focus = MathHelper.lerp(this.zoom, 0.95f, headCenter(this.data.type()));
        int feetY = (int) ((innerTop + innerBottom) / 2.0f + focus * size);

        context.enableScissor(this.previewX + 1, innerTop, this.previewX + this.previewWidth - 1, innerBottom);
        CharacterPreview.drawSpinning(context, this.previewX + this.previewWidth / 2, feetY, (int) size,
                this.previewYaw, this.client.player);
        context.disableScissor();
    }

    /** How high the middle of each device head is, in blocks above the feet (players are drawn at 15/16 size). */
    private static float headCenter(AllianceType type) {
        float halfHeightPixels = switch (type) {
            case CAMERAMAN -> 3.0f;
            case SPEAKERMAN -> 5.0f;
            case TV_MAN -> 6.0f; // including the antennas
        };
        return (24.0f + halfHeightPixels) / 16.0f * 0.9375f;
    }

    private void drawStat(DrawContext context, StatRow row) {
        int value = this.data.playerClass().startingStat(row.stat);
        context.drawTextWithShadow(this.textRenderer, Text.translatable(row.stat.translationKey()), row.x, row.y, AllianceUi.TEXT);
        int barLeft = row.x + 52;
        int barRight = row.x + row.width - 14;
        context.fill(barLeft, row.y + 2, barRight, row.y + 7, 0xFF0A121A);
        context.fill(barLeft, row.y + 2, barLeft + (barRight - barLeft) * value / MAX_STAT, row.y + 7, AllianceUi.HIGHLIGHT);
        context.drawTextWithShadow(this.textRenderer, String.valueOf(value), barRight + 4, row.y, AllianceUi.TEXT);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (mouseX >= this.previewX && mouseX < this.previewX + this.previewWidth) {
            this.previewYaw += (float) deltaX * 2.0f;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void close() {
        if (this.creating) {
            // Same rule as the type screen: no skipping the creator.
            ClientCharacterState.pauseCreator(this.data);
            this.client.setScreen(new GameMenuScreen(true));
        } else {
            super.close();
        }
    }

    @Override
    public void removed() {
        CharacterPreview.set(null);
        // Keep unfinished choices if the creator is closed by something else (death screen, dimension change...).
        if (this.creating && !this.submitted) {
            ClientCharacterState.saveDraft(this.data);
        }
    }

    private record Label(Text text, int x, int y, int width, int color) {
    }

    private record StatRow(Stat stat, int x, int y, int width) {
    }
}
