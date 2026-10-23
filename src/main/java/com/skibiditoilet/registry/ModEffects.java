package com.skibiditoilet.registry;

import com.skibiditoilet.SkibidiToiletMod;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Effects the Alliance's powers put on toilets. A stunned toilet freezes where it is (see
 * SkibidiToiletEntity#isAiDisabled); a hypnotized one turns on other toilets instead of you.
 */
public final class ModEffects {
    public static final StatusEffect STUNNED = register("stunned",
            new ModEffect(StatusEffectCategory.HARMFUL, 0xFFF27A)
                    .addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, "3a7f1c42-5b6d-4e8f-9a0b-1c2d3e4f5a61",
                            -1.0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
    public static final StatusEffect HYPNOTIZED = register("hypnotized",
            new ModEffect(StatusEffectCategory.HARMFUL, 0xA040FF));

    private ModEffects() {
    }

    public static void init() {
    }

    private static StatusEffect register(String name, StatusEffect effect) {
        return Registry.register(Registries.STATUS_EFFECT, SkibidiToiletMod.id(name), effect);
    }

    /** StatusEffect's constructor is protected. */
    private static final class ModEffect extends StatusEffect {
        ModEffect(StatusEffectCategory category, int color) {
            super(category, color);
        }
    }
}
