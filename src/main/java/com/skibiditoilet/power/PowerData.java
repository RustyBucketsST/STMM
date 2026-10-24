package com.skibiditoilet.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skibiditoilet.registry.ModAttachments;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * A player's points and what they've bought with them: unlocked powers, forms and gear (by id) and tech they've built.
 * Points come from defeating toilets and from mission rewards.
 */
@SuppressWarnings("UnstableApiUsage")
public record PowerData(int points, Set<String> unlocked, Set<String> built) {
    private static final Codec<Set<String>> ID_SET = Codec.STRING.listOf().xmap(Set::copyOf, ArrayList::new);

    public static final Codec<PowerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("points", 0).forGetter(PowerData::points),
            ID_SET.optionalFieldOf("unlocked", Set.of()).forGetter(PowerData::unlocked),
            ID_SET.optionalFieldOf("built", Set.of()).forGetter(PowerData::built)
    ).apply(instance, PowerData::new));

    public static final PowerData DEFAULT = new PowerData(0, Set.of(), Set.of());

    public static PowerData of(PlayerEntity player) {
        return player.getAttachedOrCreate(ModAttachments.POWERS);
    }

    public boolean has(Power power) {
        return power.isFree() || this.unlocked.contains(power.id());
    }

    /** Normal is everyone's; the bigger forms are bought. */
    public boolean has(Form form) {
        return form == Form.NORMAL || this.unlocked.contains(form.id());
    }

    public boolean has(Gear gear) {
        return this.unlocked.contains(gear.id());
    }

    public boolean hasBuilt(Tech tech) {
        return this.built.contains(tech.id());
    }

    public PowerData withPoints(int points) {
        return new PowerData(points, this.unlocked, this.built);
    }

    public PowerData withUnlocked(Power power) {
        return new PowerData(this.points - power.pointCost(), add(this.unlocked, power.id()), this.built);
    }

    public PowerData withUnlocked(Form form) {
        return new PowerData(this.points - form.pointCost(), add(this.unlocked, form.id()), this.built);
    }

    public PowerData withUnlocked(Gear gear) {
        return new PowerData(this.points - gear.pointCost(), add(this.unlocked, gear.id()), this.built);
    }

    public PowerData withBuilt(Tech tech, int cost) {
        return new PowerData(this.points - cost, this.unlocked, add(this.built, tech.id()));
    }

    private static Set<String> add(Set<String> set, String id) {
        Set<String> copy = new HashSet<>(set);
        copy.add(id);
        return Set.copyOf(copy);
    }
}
