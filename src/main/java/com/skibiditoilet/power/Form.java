package com.skibiditoilet.power;

import com.skibiditoilet.character.AllianceType;
import com.skibiditoilet.entity.LargeAllianceEntity;
import com.skibiditoilet.entity.TitanEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The bigger forms a player can take, like the Alliance's own Large members and Titans. They're unlocked in
 * order with points in the Powers menu, then taken one step at a time with the transform key; each costs energy
 * to take and keeps draining it until the player turns back (or runs out). A form makes the player as big as the
 * mob it's modelled on and stronger in every way.
 *
 * @param pointCost     points to unlock it
 * @param transformCost energy it takes to transform
 * @param drain         energy drained per second while in the form
 */
public enum Form {
    NORMAL("normal", 0, 0, 0.0f, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0f),
    LARGE("large", 150, 10, 0.2f, 20.0, 3.0, 4.0, 0.15, 0.3, 0.2f),
    TITAN("titan", 400, 15, 0.4f, 80.0, 10.0, 10.0, 0.6, 1.0, 0.6f),
    TITAN_2("titan_2", 800, 20, 0.6f, 140.0, 16.0, 14.0, 0.75, 1.0, 1.0f);

    /** Titan 2.0s are their 1.0 selves rebuilt bigger. */
    private static final float TITAN_2_GROWTH = 1.1f;

    private final String id;
    private final int pointCost;
    private final int transformCost;
    private final float drain;
    private final double health;
    private final double attack;
    private final double armor;
    private final double speed;
    private final double knockbackResistance;
    private final float powerBonus;

    Form(String id, int pointCost, int transformCost, float drain, double health, double attack, double armor,
         double speed, double knockbackResistance, float powerBonus) {
        this.id = id;
        this.pointCost = pointCost;
        this.transformCost = transformCost;
        this.drain = drain;
        this.health = health;
        this.attack = attack;
        this.armor = armor;
        this.speed = speed;
        this.knockbackResistance = knockbackResistance;
        this.powerBonus = powerBonus;
    }

    /** How many times a normal player's size this form makes a player of {@code type}. */
    public float scale(AllianceType type) {
        return switch (this) {
            case NORMAL -> 1.0f;
            case LARGE -> LargeAllianceEntity.SCALE;
            case TITAN -> titanScale(type);
            case TITAN_2 -> titanScale(type) * TITAN_2_GROWTH;
        };
    }

    private static float titanScale(AllianceType type) {
        return switch (type) {
            case CAMERAMAN -> TitanEntity.CAMERAMAN_SCALE;
            case SPEAKERMAN -> TitanEntity.SPEAKERMAN_SCALE;
            case TV_MAN -> TitanEntity.TV_MAN_SCALE;
        };
    }

    /** How high a player this many times normal size can step: Titans step up 2.5 blocks like the mobs. */
    public static float stepHeight(float scale) {
        return Math.min(2.5f, 0.6f * scale);
    }

    /** Titan and Titan 2.0, which put the Titan moves on the power keys. */
    public boolean isTitan() {
        return this == TITAN || this == TITAN_2;
    }

    /** The id kept in PowerData#unlocked when it's bought. */
    public String id() {
        return "form_" + this.id;
    }

    public int pointCost() {
        return this.pointCost;
    }

    public int transformCost() {
        return this.transformCost;
    }

    public float drain() {
        return this.drain;
    }

    /** Extra max health, attack damage and armor, in points. */
    public double health() {
        return this.health;
    }

    public double attack() {
        return this.attack;
    }

    public double armor() {
        return this.armor;
    }

    /** Extra movement speed, as a fraction of the base speed. */
    public double speed() {
        return this.speed;
    }

    public double knockbackResistance() {
        return this.knockbackResistance;
    }

    /** How much harder powers hit in this form, as a fraction. */
    public float powerBonus() {
        return this.powerBonus;
    }

    /** The form the transform key goes to from this one, or null from the last. */
    @Nullable
    public Form next() {
        return this.ordinal() + 1 < values().length ? values()[this.ordinal() + 1] : null;
    }

    /** The form that has to be unlocked before this one, or null for the first. */
    @Nullable
    public Form previous() {
        return this.ordinal() > 0 ? values()[this.ordinal() - 1] : null;
    }

    public String translationKey() {
        return "form.skibidi." + this.id;
    }

    public String descriptionKey() {
        return translationKey() + ".description";
    }

    /** The forms that can be unlocked, in order. */
    public static Form[] unlockable() {
        return new Form[]{LARGE, TITAN, TITAN_2};
    }

    @Nullable
    public static Form byId(String id) {
        for (Form form : values()) {
            if (form.id().equals(id)) {
                return form;
            }
        }
        return null;
    }
}
