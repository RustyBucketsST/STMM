package com.skibiditoilet.power;

import com.skibiditoilet.character.AllianceType;

import java.util.Arrays;
import java.util.List;

/**
 * The powers each Alliance type has, from the series' lore. Each type's powers sit in a fixed order on
 * the power keys (R, G, V, B by default). The first power of each type comes free; the rest are unlocked
 * with points in the Powers menu.
 *
 * @param pointCost   points to unlock it
 * @param energyCost  energy each use takes
 * @param cooldown    ticks before it can be used again
 */
public enum Power {
    HOLOSHIELD("holoshield", AllianceType.CAMERAMAN, 0, 30, 240),
    RECORD_SCAN("record_scan", AllianceType.CAMERAMAN, 60, 25, 400),
    SONIC_BLAST("sonic_blast", AllianceType.SPEAKERMAN, 0, 25, 80),
    SONIC_BOOTS("sonic_boots", AllianceType.SPEAKERMAN, 50, 15, 60),
    ANTHEM_DANCE("anthem_dance", AllianceType.SPEAKERMAN, 120, 40, 600),
    HYPNOSIS("hypnosis", AllianceType.TV_MAN, 0, 30, 200),
    TELEPORT("teleport", AllianceType.TV_MAN, 50, 20, 60),
    BURNING_LIGHT("burning_light", AllianceType.TV_MAN, 100, 25, 120),
    STUN_FLASH("stun_flash", AllianceType.TV_MAN, 160, 35, 300);

    private final String id;
    private final AllianceType type;
    private final int pointCost;
    private final int energyCost;
    private final int cooldown;

    Power(String id, AllianceType type, int pointCost, int energyCost, int cooldown) {
        this.id = id;
        this.type = type;
        this.pointCost = pointCost;
        this.energyCost = energyCost;
        this.cooldown = cooldown;
    }

    public String id() {
        return this.id;
    }

    public AllianceType type() {
        return this.type;
    }

    public int pointCost() {
        return this.pointCost;
    }

    public int energyCost() {
        return this.energyCost;
    }

    public int cooldown() {
        return this.cooldown;
    }

    /** Free powers are always unlocked. */
    public boolean isFree() {
        return this.pointCost == 0;
    }

    public String translationKey() {
        return "power.skibidi." + this.id;
    }

    public String descriptionKey() {
        return translationKey() + ".description";
    }

    /** A type's powers in key order: slot 0 is the first power key. */
    public static List<Power> of(AllianceType type) {
        return Arrays.stream(values()).filter(power -> power.type == type).toList();
    }

    public static Power byId(String id) {
        return Arrays.stream(values()).filter(power -> power.id.equals(id)).findFirst().orElse(null);
    }
}
