package com.skibiditoilet.power;

import com.skibiditoilet.character.AllianceType;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * The powers each Alliance type has, from the series' lore. Each type's powers sit in a fixed order on
 * the power keys (R, G, V, B by default). The first power of each type comes free; the rest are unlocked
 * with points in the Powers menu. In a Titan form the Titan moves take the first keys (see onKeys).
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
    STUN_FLASH("stun_flash", AllianceType.TV_MAN, 160, 35, 300),
    // Titan moves: the 1.0 Titans' gear, on the keys while in a Titan form (see onKeys). They come with the form.
    CORE_BEAM("core_beam", 25, 160),
    TITAN_JETPACK("titan_jetpack", 15, 80),
    TITAN_SOUNDWAVES("titan_soundwaves", 25, 140),
    PLASMA_CANNON("plasma_cannon", 20, 80),
    DEATH_SCREEN("death_screen", 30, 220),
    CLAW_SWEEP("claw_sweep", 15, 100),
    GRAPPLING_HOOK("grappling_hook", 15, 120);

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

    /** A Titan move: no type of its own (two types share the jetpack) and nothing to unlock. */
    Power(String id, int energyCost, int cooldown) {
        this(id, null, 0, energyCost, cooldown);
    }

    public String id() {
        return this.id;
    }

    /** The type whose power it is, or null for a Titan move. */
    @Nullable
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

    public boolean isTitanMove() {
        return this.type == null;
    }

    /** Free powers (and Titan moves) are always unlocked. */
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

    /**
     * What's on the power keys for a player of {@code type} in {@code form}. In a Titan form the type's Titan moves
     * come first, then the type's own powers that they don't replace.
     */
    public static List<Power> onKeys(AllianceType type, Form form) {
        if (!form.isTitan()) {
            return of(type);
        }
        return switch (type) {
            case CAMERAMAN -> List.of(CORE_BEAM, TITAN_JETPACK, HOLOSHIELD, RECORD_SCAN);
            case SPEAKERMAN -> List.of(TITAN_SOUNDWAVES, PLASMA_CANNON, TITAN_JETPACK, ANTHEM_DANCE);
            case TV_MAN -> List.of(DEATH_SCREEN, CLAW_SWEEP, GRAPPLING_HOOK, TELEPORT);
        };
    }

    public static Power byId(String id) {
        return Arrays.stream(values()).filter(power -> power.id.equals(id)).findFirst().orElse(null);
    }
}
