package com.skibiditoilet.power;

import com.skibiditoilet.character.CharacterData;
import com.skibiditoilet.character.Stat;
import com.skibiditoilet.registry.ModAttachments;
import com.skibiditoilet.registry.ModSounds;
import com.skibiditoilet.story.StoryData;
import com.skibiditoilet.story.StorySync;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Energy, cooldowns and the powers that last a while (the Holoshield, the Anthem Dance). Kept per player
 * while they're online; energy starts full on every login.
 */
public final class PowerManager {
    private static final float BASE_ENERGY = 60.0f;
    private static final float ENERGY_PER_STAT = 10.0f;
    private static final float REGEN_PER_TICK = 4.0f / 20.0f;
    private static final int SYNC_TICKS = 5;

    /** What's going on with one player's powers right now. */
    static final class Runtime {
        float energy = -1.0f;
        final Map<Power, Long> readyAt = new HashMap<>();
        long shieldUntil;
        long danceUntil;
        long noFallUntil;
        long lastBash;
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

    /** How hard powers hit: 10% more for each point of Power. */
    public static float powerScale(ServerPlayerEntity player) {
        return 1.0f + 0.1f * CharacterData.of(player).playerClass().startingStat(Stat.POWER);
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
            if (!CharacterData.of(player).created()) {
                continue;
            }
            Runtime runtime = runtime(player);
            float max = maxEnergy(player);
            // (Max energy can drop below what's stored if the player remakes their character with another class.)
            if (runtime.energy != max) {
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
