package com.skibiditoilet.power;

import org.jetbrains.annotations.Nullable;

/**
 * Gear a player buys with points in the Powers menu. Each piece shows on their model in every form (gear.bbmodel)
 * and gives a bonus:
 * <ul>
 * <li>Jetpack: hold jump in mid-air to fly up for a few seconds (it refuels on the ground); no fall damage.</li>
 * <li>Shoulder gear: the type's own (CCTV cameras, speakers, or blade claws on the back); powers hit harder. On a
 * Titan it's the Titan's own shoulder cameras, speaker towers or claws.</li>
 * <li>Armor plates: steel plates on the chest and shoulders; more armor.</li>
 * </ul>
 */
public enum Gear {
    JETPACK("jetpack", 120),
    SHOULDERS("shoulders", 150),
    PLATES("plates", 100);

    /** Ticks of thrust a full jetpack has. */
    public static final int JETPACK_FUEL = 60;
    /** How much harder powers hit with shoulder gear. */
    public static final float SHOULDER_POWER = 1.15f;
    public static final double PLATE_ARMOR = 4.0;

    private final String id;
    private final int pointCost;

    Gear(String id, int pointCost) {
        this.id = id;
        this.pointCost = pointCost;
    }

    /** The id kept in PowerData#unlocked when it's bought. */
    public String id() {
        return "gear_" + this.id;
    }

    public int pointCost() {
        return this.pointCost;
    }

    public String translationKey() {
        return "gear.skibidi." + this.id;
    }

    public String descriptionKey() {
        return translationKey() + ".description";
    }

    /** This piece's bit in a mask of the gear a player has (sent to clients to draw it). */
    public int bit() {
        return 1 << this.ordinal();
    }

    public boolean in(int mask) {
        return (mask & this.bit()) != 0;
    }

    public static int mask(PowerData data) {
        int mask = 0;
        for (Gear gear : values()) {
            if (data.has(gear)) {
                mask |= gear.bit();
            }
        }
        return mask;
    }

    @Nullable
    public static Gear byId(String id) {
        for (Gear gear : values()) {
            if (gear.id().equals(id)) {
                return gear;
            }
        }
        return null;
    }
}
