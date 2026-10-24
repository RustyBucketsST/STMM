package com.skibiditoilet.power;

import com.skibiditoilet.character.AllianceType;
import com.skibiditoilet.character.CharacterData;
import com.skibiditoilet.character.Stat;
import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModAttachments;
import com.skibiditoilet.registry.ModSounds;
import com.skibiditoilet.story.StoryData;
import com.skibiditoilet.story.StorySync;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Energy, cooldowns, forms and the powers that last a while (the Holoshield, the Anthem Dance). Kept per player
 * while they're online; energy starts full on every login, and players log in, respawn and come back from the End
 * in their normal form.
 */
public final class PowerManager {
    private static final float BASE_ENERGY = 60.0f;
    private static final float ENERGY_PER_STAT = 10.0f;
    private static final float REGEN_PER_TICK = 4.0f / 20.0f;
    private static final int SYNC_TICKS = 5;
    /** Ticks between transforming and transforming again (or turning back). */
    private static final int TRANSFORM_COOLDOWN = 20;
    private static final UUID FORM_HEALTH = UUID.fromString("4f0c2a6e-2b1d-4d7e-9a51-6d3c8e1f7a01");
    private static final UUID FORM_ATTACK = UUID.fromString("4f0c2a6e-2b1d-4d7e-9a51-6d3c8e1f7a02");
    private static final UUID FORM_ARMOR = UUID.fromString("4f0c2a6e-2b1d-4d7e-9a51-6d3c8e1f7a03");
    private static final UUID FORM_SPEED = UUID.fromString("4f0c2a6e-2b1d-4d7e-9a51-6d3c8e1f7a04");
    private static final UUID FORM_KNOCKBACK = UUID.fromString("4f0c2a6e-2b1d-4d7e-9a51-6d3c8e1f7a05");

    /** What's going on with one player's powers right now. */
    static final class Runtime {
        float energy = -1.0f;
        final Map<Power, Long> readyAt = new HashMap<>();
        long shieldUntil;
        long danceUntil;
        long noFallUntil;
        long lastBash;
        long transformReadyAt;
        boolean dirty = true;
    }

    private static final Map<UUID, Runtime> RUNTIMES = new HashMap<>();

    private PowerManager() {
    }

    public static void init() {
        PowerSync.init();
        ServerTickEvents.END_SERVER_TICK.register(PowerManager::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            moveStoryPoints(player);
            PowerSync.sendData(player);
            RUNTIMES.remove(player.getUuid());
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return true;
            }
            Runtime runtime = RUNTIMES.get(player.getUuid());
            if (runtime == null) {
                return true;
            }
            long now = player.getServer().getTicks();
            if (source.isOf(DamageTypes.FALL) && now < runtime.noFallUntil) {
                return false;
            }
            return now >= runtime.shieldUntil || !PowerEffects.shieldBlocks(player, source);
        });
        // The client makes a new player entity in the new world, which has to be told the form again.
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            if (FormHolder.form(player) != Form.NORMAL) {
                PowerSync.sendForm(player);
            }
        });
    }

    /** Points earned before powers existed were kept with the story; they move to the shared wallet once. */
    private static void moveStoryPoints(ServerPlayerEntity player) {
        StoryData story = StoryData.of(player);
        if (story.points() > 0) {
            PowerData data = PowerData.of(player);
            player.setAttached(ModAttachments.POWERS, data.withPoints(data.points() + story.points()));
            StorySync.set(player, new StoryData(story.completed(), story.claimed(), 0));
        }
    }

    public static void addPoints(ServerPlayerEntity player, int points) {
        PowerData data = PowerData.of(player);
        PowerSync.set(player, data.withPoints(data.points() + points));
    }

    public static float maxEnergy(ServerPlayerEntity player) {
        CharacterData character = CharacterData.of(player);
        return BASE_ENERGY + ENERGY_PER_STAT * character.playerClass().startingStat(Stat.ENERGY);
    }

    /** How hard powers hit: 10% more for each point of Power, and more again in a bigger form. */
    public static float powerScale(ServerPlayerEntity player) {
        float base = 1.0f + 0.1f * CharacterData.of(player).playerClass().startingStat(Stat.POWER);
        return base * (1.0f + FormHolder.form(player).powerBonus());
    }

    private static Runtime runtime(ServerPlayerEntity player) {
        Runtime runtime = RUNTIMES.computeIfAbsent(player.getUuid(), uuid -> new Runtime());
        if (runtime.energy < 0) {
            runtime.energy = maxEnergy(player);
        }
        return runtime;
    }

    static boolean isShielded(ServerPlayerEntity player) {
        Runtime runtime = RUNTIMES.get(player.getUuid());
        return runtime != null && player.getServer().getTicks() < runtime.shieldUntil;
    }

    /** Uses the power on key {@code slot}: it has to be unlocked, off cooldown, and affordable. */
    static void use(ServerPlayerEntity player, int slot) {
        CharacterData character = CharacterData.of(player);
        if (!character.created() || player.isSpectator() || !player.isAlive()) {
            return;
        }
        List<Power> powers = Power.of(character.type());
        if (slot < 0 || slot >= powers.size()) {
            return;
        }
        Power power = powers.get(slot);
        Runtime runtime = runtime(player);
        long now = player.getServer().getTicks();
        if (!PowerData.of(player).has(power)) {
            player.sendMessage(Text.translatable("power.skibidi.locked", Text.translatable(power.translationKey()))
                    .formatted(Formatting.GRAY), true);
            return;
        }
        if (now < runtime.readyAt.getOrDefault(power, 0L)) {
            return;
        }
        boolean free = player.isCreative();
        if (!free && runtime.energy < power.energyCost()) {
            player.playSound(ModSounds.POWER_NO_ENERGY, SoundCategory.PLAYERS, 1.0f, 1.0f);
            player.sendMessage(Text.translatable("power.skibidi.no_energy").formatted(Formatting.AQUA), true);
            return;
        }
        if (!PowerEffects.use(player, power, powerScale(player), runtime, now)) {
            return;
        }
        if (!free) {
            runtime.energy -= power.energyCost();
        }
        runtime.readyAt.put(power, now + power.cooldown());
        runtime.dirty = true;
    }

    static void unlock(ServerPlayerEntity player, @Nullable Power power) {
        PowerData data = PowerData.of(player);
        CharacterData character = CharacterData.of(player);
        if (power == null || !character.created() || power.type() != character.type() || data.has(power)
                || data.points() < power.pointCost()) {
            return;
        }
        PowerSync.set(player, data.withUnlocked(power));
        player.playSound(ModSounds.MISSION_REWARD, SoundCategory.PLAYERS, 1.0f, 1.2f);
    }

    /** Forms are bought in order: each needs the one before it. */
    static void unlock(ServerPlayerEntity player, Form form) {
        PowerData data = PowerData.of(player);
        Form previous = form.previous();
        if (!CharacterData.of(player).created() || data.has(form) || (previous != null && !data.has(previous))
                || data.points() < form.pointCost()) {
            return;
        }
        PowerSync.set(player, data.withUnlocked(form));
        player.playSound(ModSounds.MISSION_REWARD, SoundCategory.PLAYERS, 1.0f, 1.2f);
    }

    /**
     * The transform key: takes the next form up if it's unlocked, there's energy for it and room to grow; with
     * {@code back} (sneaking), turns back to normal.
     */
    static void transform(ServerPlayerEntity player, boolean back) {
        CharacterData character = CharacterData.of(player);
        if (!character.created() || player.isSpectator() || !player.isAlive()) {
            return;
        }
        Runtime runtime = runtime(player);
        long now = player.getServer().getTicks();
        if (now < runtime.transformReadyAt) {
            return;
        }
        Form current = FormHolder.form(player);
        if (back) {
            if (current != Form.NORMAL) {
                setForm(player, Form.NORMAL);
                runtime.transformReadyAt = now + TRANSFORM_COOLDOWN;
            }
            return;
        }
        Form next = current.next();
        if (next == null) {
            player.sendMessage(Text.translatable("form.skibidi.strongest").formatted(Formatting.GRAY), true);
            return;
        }
        if (!PowerData.of(player).has(next)) {
            player.sendMessage(Text.translatable("power.skibidi.locked", Text.translatable(next.translationKey()))
                    .formatted(Formatting.GRAY), true);
            return;
        }
        boolean free = player.isCreative();
        if (!free && runtime.energy < next.transformCost()) {
            player.playSound(ModSounds.POWER_NO_ENERGY, SoundCategory.PLAYERS, 1.0f, 1.0f);
            player.sendMessage(Text.translatable("power.skibidi.no_energy").formatted(Formatting.AQUA), true);
            return;
        }
        if (!hasRoom(player, next.scale(character.type()))) {
            player.sendMessage(Text.translatable("form.skibidi.no_room").formatted(Formatting.GRAY), true);
            return;
        }
        if (!free) {
            runtime.energy -= next.transformCost();
        }
        runtime.transformReadyAt = now + TRANSFORM_COOLDOWN;
        runtime.dirty = true;
        setForm(player, next);
    }

    /** Whether the player would fit where they stand at {@code scale} times normal size. */
    private static boolean hasRoom(ServerPlayerEntity player, float scale) {
        EntityDimensions dimensions = player.getDimensions(player.getPose()).scaled(scale / FormHolder.scale(player));
        Box box = dimensions.getBoxAt(player.getPos()).contract(1.0E-7);
        return player.getWorld().isSpaceEmpty(player, box);
    }

    /**
     * Puts the player in {@code form}: their size, their stats (keeping the same share of their health), and
     * everyone around is told. Growing bursts out with the type's color; turning back just vents.
     */
    static void setForm(ServerPlayerEntity player, Form form) {
        Form old = FormHolder.form(player);
        if (form == old) {
            return;
        }
        AllianceType type = CharacterData.of(player).type();
        float healthShare = player.getHealth() / player.getMaxHealth();
        FormHolder.set(player, form, form.scale(type));
        modifier(player, EntityAttributes.GENERIC_MAX_HEALTH, FORM_HEALTH, form.health(), EntityAttributeModifier.Operation.ADDITION);
        modifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, FORM_ATTACK, form.attack(), EntityAttributeModifier.Operation.ADDITION);
        modifier(player, EntityAttributes.GENERIC_ARMOR, FORM_ARMOR, form.armor(), EntityAttributeModifier.Operation.ADDITION);
        modifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, FORM_SPEED, form.speed(), EntityAttributeModifier.Operation.MULTIPLY_BASE);
        modifier(player, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, FORM_KNOCKBACK, form.knockbackResistance(),
                EntityAttributeModifier.Operation.ADDITION);
        if (player.isAlive()) {
            player.setHealth(Math.max(1.0f, healthShare * player.getMaxHealth()));
        }
        PowerSync.sendForm(player);

        ServerWorld world = player.getServerWorld();
        Vec3d feet = player.getPos();
        float size = player.getWidth();
        if (form.ordinal() > old.ordinal()) {
            ModFx.shockwave(world, feet, size * 2.0f, form == Form.LARGE ? 0.2f : 0.6f);
            ModFx.ring(world, feet.add(0.0, player.getHeight() * 0.5, 0.0), size * 1.5f, color(type));
            world.playSound(null, feet.x, feet.y, feet.z, ModSounds.POWER_TRANSFORM, SoundCategory.PLAYERS,
                    1.0f + 0.3f * form.ordinal(), 1.2f - 0.15f * form.ordinal());
        } else {
            world.playSound(null, feet.x, feet.y, feet.z, ModSounds.POWER_TRANSFORM_BACK, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
    }

    private static void modifier(ServerPlayerEntity player, EntityAttribute attribute, UUID id, double amount,
                                 EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTemporaryModifier(new EntityAttributeModifier(id, "Skibidi form", amount, operation));
        }
    }

    /** The color of each type's core: blue, red, purple. */
    private static int color(AllianceType type) {
        return switch (type) {
            case CAMERAMAN -> 0x46C8FF;
            case SPEAKERMAN -> 0xFF4646;
            case TV_MAN -> 0xB45AFF;
        };
    }

    static void build(ServerPlayerEntity player, @Nullable Tech tech) {
        PowerData data = PowerData.of(player);
        CharacterData character = CharacterData.of(player);
        if (tech == null || !character.created() || tech.type() != character.type()) {
            return;
        }
        int cost = tech.cost(data.hasBuilt(tech));
        if (data.points() < cost) {
            return;
        }
        PowerSync.set(player, data.withBuilt(tech, cost));
        player.getInventory().offerOrDrop(tech.item().getDefaultStack());
        player.playSound(ModSounds.MISSION_REWARD, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTicks();
        RUNTIMES.keySet().removeIf(uuid -> server.getPlayerManager().getPlayer(uuid) == null);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Form form = FormHolder.form(player);
            if (form != Form.NORMAL && (player.isSpectator() || !CharacterData.of(player).created())) {
                setForm(player, Form.NORMAL);
                form = Form.NORMAL;
            }
            if (!CharacterData.of(player).created()) {
                continue;
            }
            Runtime runtime = runtime(player);
            float max = maxEnergy(player);
            if (form != Form.NORMAL && player.isAlive()) {
                // A form drains energy instead of letting it come back, until it runs out.
                if (!player.isCreative()) {
                    runtime.energy = Math.max(0.0f, Math.min(max, runtime.energy) - form.drain() / 20.0f);
                    runtime.dirty = true;
                    if (runtime.energy <= 0.0f) {
                        setForm(player, Form.NORMAL);
                        player.sendMessage(Text.translatable("form.skibidi.out_of_energy").formatted(Formatting.AQUA), true);
                    }
                }
            } else if (runtime.energy != max) {
                // (Max energy can drop below what's stored if the player remakes their character with another class.)
                runtime.energy = Math.min(max, runtime.energy + REGEN_PER_TICK);
                runtime.dirty = true;
            }
            if (now < runtime.shieldUntil) {
                PowerEffects.shieldBash(player, runtime, now, powerScale(player));
            }
            if (now < runtime.danceUntil && (runtime.danceUntil - now) % 20 == 0) {
                PowerEffects.anthemPulse(player);
            }
            if (runtime.dirty && now % SYNC_TICKS == 0) {
                runtime.dirty = false;
                sync(player, runtime, now, max);
            }
        }
    }

    private static void sync(ServerPlayerEntity player, Runtime runtime, long now, float max) {
        List<Power> powers = Power.of(CharacterData.of(player).type());
        int[] cooldowns = powers.stream().mapToInt(power -> (int) Math.max(0, runtime.readyAt.getOrDefault(power, 0L) - now))
                .toArray();
        PowerSync.sendState(player, runtime.energy, max, cooldowns);
        // Keep sending while anything is still cooling down, so the HUD counts down.
        if (Arrays.stream(cooldowns).anyMatch(ticks -> ticks > 0)) {
            runtime.dirty = true;
        }
    }

    /** The point straight ahead of the player's eyes, for aiming area powers. */
    static Vec3d aim(ServerPlayerEntity player, double distance) {
        return player.getEyePos().add(player.getRotationVec(1.0f).multiply(distance));
    }
}
