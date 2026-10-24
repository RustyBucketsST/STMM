package com.skibiditoilet.client.power;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.character.CharacterData;
import com.skibiditoilet.client.character.AllianceUi;
import com.skibiditoilet.client.character.ClientCharacterState;
import com.skibiditoilet.power.Form;
import com.skibiditoilet.power.FormHolder;
import com.skibiditoilet.power.Power;
import com.skibiditoilet.power.PowerSync;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * The bottom-left corner: the energy bar and a slot for each power, with its key, its cooldown counting
 * down, the form the player is in, and a "+N points" popup when points come in. (The middle is the hotbar's,
 * the top-left the mission's.)
 */
final class PowerHud {
    static final int SLOT = 18;
    private static final int GAP = 2;
    private static final int ENERGY = 0xFF46C8FF;
    private static final int ENERGY_LOW = 0xFF2A6E8C;
    private static final int GAIN = 0xFFD34D;

    private PowerHud() {
    }

    static Identifier icon(Power power) {
        return SkibidiToiletMod.id("textures/gui/power/" + power.id() + ".png");
    }

    static Identifier icon(Form form) {
        return SkibidiToiletMod.id("textures/gui/power/" + form.id() + ".png");
    }

    static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        CharacterData character = ClientCharacterState.self();
        if (client.options.hudHidden || client.player == null || client.player.isSpectator() || !character.created()
                || ClientPowerState.maxEnergy() <= 0.0f) {
            return;
        }
        TextRenderer textRenderer = client.textRenderer;
        List<Power> powers = Power.of(character.type());
        int width = Math.max(4 * (SLOT + GAP) - GAP, powers.size() * (SLOT + GAP) - GAP) + 8;
        int height = SLOT + 18;
        int x = 2;
        int y = context.getScaledWindowHeight() - height - 2;
        AllianceUi.panel(context, x, y, width, height);

        // Energy: a bar and the number.
        float energy = ClientPowerState.energy();
        float max = ClientPowerState.maxEnergy();
        String amount = String.valueOf((int) energy);
        int barX = x + 4;
        int barY = y + 5;
        int barWidth = width - 10 - textRenderer.getWidth(amount);
        context.fill(barX, barY, barX + barWidth, barY + 4, AllianceUi.BORDER_DIM);
        int filled = Math.round(barWidth * MathHelper.clamp(energy / max, 0.0f, 1.0f));
        context.fill(barX, barY, barX + filled, barY + 4, energy < max * 0.25f ? ENERGY_LOW : ENERGY);
        context.fill(barX, barY, barX + filled, barY + 1, 0x60FFFFFF);
        context.drawTextWithShadow(textRenderer, amount, x + width - 4 - textRenderer.getWidth(amount), y + 3, AllianceUi.TEXT);

        for (int slot = 0; slot < powers.size(); slot++) {
            slot(context, textRenderer, powers.get(slot), slot, x + 4 + slot * (SLOT + GAP), y + 13, energy, tickDelta);
        }

        // The form, above the panel's right end, while it drains the bar.
        Form form = FormHolder.form(client.player);
        if (form != Form.NORMAL) {
            Text name = Text.translatable(form.translationKey());
            context.drawTextWithShadow(textRenderer, name, x + width - textRenderer.getWidth(name), y - 10, AllianceUi.HIGHLIGHT);
        }

        // "+N points", rising and fading above the panel.
        long since = ClientPowerState.ticks() - ClientPowerState.gainedTime();
        if (since < ClientPowerState.GAIN_TICKS && ClientPowerState.gained() > 0) {
            float t = (since + tickDelta) / ClientPowerState.GAIN_TICKS;
            int alpha = (int) (255 * MathHelper.clamp((1.0f - t) * 3.0f, 0.0f, 1.0f));
            if (alpha > 8) {
                Text gain = Text.translatable("hud.skibidi.points_gained", ClientPowerState.gained());
                context.drawTextWithShadow(textRenderer, gain, x + 2, y - 11 - (int) (t * 6), GAIN | alpha << 24);
            }
        }
    }

    private static void slot(DrawContext context, TextRenderer textRenderer, Power power, int slot, int x, int y,
                             float energy, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean unlocked = ClientPowerState.data().has(power);
        float cooldown = ClientPowerState.cooldownLeft(slot, tickDelta);
        float[] active = activeSpan(client, power, tickDelta);

        context.fill(x, y, x + SLOT, y + SLOT, 0xFF0A121B);
        context.drawTexture(icon(power), x + 1, y + 1, 0, 0, 16, 16, 16, 16);
        if (!unlocked) {
            context.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xC0000000);
        } else if (cooldown > 0.0f) {
            // Darkened from the top down by how much cooldown is left, with the seconds on top.
            int shade = Math.round((SLOT - 2) * MathHelper.clamp(cooldown / power.cooldown(), 0.0f, 1.0f));
            context.fill(x + 1, y + 1, x + SLOT - 1, y + 1 + shade, 0xB0000000);
            String seconds = String.valueOf((int) Math.ceil(cooldown / 20.0f));
            context.drawTextWithShadow(textRenderer, seconds, x + (SLOT - textRenderer.getWidth(seconds)) / 2 + 1, y + 5, 0xFFFFFF);
        } else if (energy < power.energyCost() && !client.player.isCreative()) {
            context.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0x902A0A30);
        }
        int frame = active != null ? AllianceUi.HIGHLIGHT : unlocked && cooldown <= 0.0f ? AllianceUi.BORDER : AllianceUi.BORDER_DIM;
        context.drawBorder(x, y, SLOT, SLOT, frame);
        if (active != null) {
            // A shrinking line under the slot while the power lasts.
            float left = active[1] / (active[0] + active[1]);
            context.fill(x, y + SLOT, x + Math.round(SLOT * left), y + SLOT + 1, AllianceUi.HIGHLIGHT);
        }

        // The key, small, in the bottom-right corner.
        String key = ClientPowerState.POWER_KEYS[slot].getBoundKeyLocalizedText().getString();
        if (key.length() > 2) {
            key = key.substring(0, 2);
        }
        context.getMatrices().push();
        context.getMatrices().translate(x + SLOT - 1 - textRenderer.getWidth(key) * 0.6f, y + SLOT - 6, 200);
        context.getMatrices().scale(0.6f, 0.6f, 1.0f);
        context.drawTextWithShadow(textRenderer, key, 0, 0, unlocked ? 0xFFFFFF : 0x808080);
        context.getMatrices().pop();
    }

    /** {elapsed, remaining} for a power that lasts a while and is on now, else null. */
    private static float[] activeSpan(MinecraftClient client, Power power, float tickDelta) {
        PowerSync.Visible visible = switch (power) {
            case HOLOSHIELD -> PowerSync.Visible.SHIELD;
            case ANTHEM_DANCE -> PowerSync.Visible.DANCE;
            default -> null;
        };
        return visible != null ? ClientPowerState.visible(client.player.getId(), visible, tickDelta) : null;
    }
}
