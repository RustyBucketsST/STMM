package com.skibiditoilet.power;

import com.skibiditoilet.character.AllianceType;
import com.skibiditoilet.registry.ModItems;
import net.minecraft.item.Item;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Gear a type builds rather than a power it has: the Cameramen's inventions and the Speakermen's knife.
 * Built in the Powers menu with points; building it again (if it's lost) costs half.
 */
public enum Tech {
    STUN_RIFLE("stun_rifle", AllianceType.CAMERAMAN, 80, () -> ModItems.STUN_RIFLE),
    GRAVITY_PLUNGER("gravity_plunger", AllianceType.CAMERAMAN, 100, () -> ModItems.GRAVITY_PLUNGER),
    ENERGY_KNIFE("energy_knife", AllianceType.SPEAKERMAN, 60, () -> ModItems.ENERGY_KNIFE);

    private final String id;
    private final AllianceType type;
    private final int pointCost;
    private final Supplier<Item> item;

    Tech(String id, AllianceType type, int pointCost, Supplier<Item> item) {
        this.id = id;
        this.type = type;
        this.pointCost = pointCost;
        this.item = item;
    }

    public String id() {
        return this.id;
    }

    public AllianceType type() {
        return this.type;
    }

    public Item item() {
        return this.item.get();
    }

    /** First build costs the full price; rebuilding one you already built costs half. */
    public int cost(boolean builtBefore) {
        return builtBefore ? this.pointCost / 2 : this.pointCost;
    }

    public String descriptionKey() {
        return "tech.skibidi." + this.id + ".description";
    }

    public static List<Tech> of(AllianceType type) {
        return Arrays.stream(values()).filter(tech -> tech.type == type).toList();
    }

    public static Tech byId(String id) {
        return Arrays.stream(values()).filter(tech -> tech.id.equals(id)).findFirst().orElse(null);
    }
}
