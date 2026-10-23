package com.skibiditoilet.story;

import com.skibiditoilet.character.CharacterData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.Difficulty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runs the missions in progress. Missions live only while their players are online: logging out or a
 * server restart ends them, and the mobs they spawned are removed instead of being saved.
 */
public final class MissionManager {
    /** Command tag on every mob a mission spawns, so leftovers are found and removed if the server crashed. */
    public static final String TAG = "skibidi_mission";
    /** Party members this close to the player who starts a mission join in. */
    private static final double JOIN_RANGE = 64.0;

    private static final List<MissionRun> RUNS = new ArrayList<>();
    private static final Map<UUID, MissionRun> BY_PLAYER = new HashMap<>();
    private static final Map<UUID, MissionRun> BY_MOB = new HashMap<>();
    // Tagged mobs loaded from disk that no mission owns; removed on the next tick, outside the load callback.
    private static final List<Entity> STRAYS = new ArrayList<>();

    private MissionManager() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(MissionManager::tick);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity) {
                MissionRun run = BY_PLAYER.get(entity.getUuid());
                if (run != null) {
                    run.onParticipantDied(entity.getUuid());
                }
                return;
            }
            MissionRun run = BY_MOB.get(entity.getUuid());
            if (run != null) {
                run.onMobGone(entity.getUuid());
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            MissionRun run = BY_PLAYER.get(newPlayer.getUuid());
            if (run != null) {
                run.onParticipantRespawned(newPlayer.getUuid());
            }
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity.getCommandTags().contains(TAG) && !BY_MOB.containsKey(entity.getUuid())) {
                STRAYS.add(entity);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> List.copyOf(RUNS).forEach(run -> run.end()));
    }

    /** Starts a mission for this player and their party members nearby. Returns why it can't start, or null. */
    @Nullable
    public static Text start(ServerPlayerEntity player, Mission mission) {
        StoryData data = StoryData.of(player);
        if (!CharacterData.of(player).created() || player.isSpectator() || !player.isAlive()) {
            return Text.translatable("story.skibidi.error.cannot_start");
        }
        if (BY_PLAYER.containsKey(player.getUuid())) {
            return Text.translatable("story.skibidi.error.already_running");
        }
        if (data.isCompleted(mission.id())) {
            return Text.translatable("story.skibidi.error.completed");
        }
        if (!StoryMissions.isUnlocked(data, mission)) {
            return Text.translatable("story.skibidi.error.locked");
        }
        if (player.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return Text.translatable("story.skibidi.error.peaceful");
        }

        List<ServerPlayerEntity> participants = new ArrayList<>();
        participants.add(player);
        for (ServerPlayerEntity member : PartyManager.get(player.getServer()).onlineMembers(player)) {
            if (member.getWorld() == player.getWorld() && member.isInRange(player, JOIN_RANGE) && member.isAlive()
                    && !member.isSpectator() && !BY_PLAYER.containsKey(member.getUuid())
                    && CharacterData.of(member).created()) {
                participants.add(member);
            }
        }
        MissionRun run = new MissionRun(mission, player.getServerWorld(), participants);
        RUNS.add(run);
        for (ServerPlayerEntity participant : participants) {
            BY_PLAYER.put(participant.getUuid(), run);
            StorySync.sendEvent(participant, StoryEvent.STARTED, mission);
        }
        run.begin();
        return null;
    }

    public static void abandon(ServerPlayerEntity player) {
        MissionRun run = BY_PLAYER.get(player.getUuid());
        if (run != null) {
            run.fail(StoryEvent.ABANDONED);
        }
    }

    public static boolean isInMission(ServerPlayerEntity player) {
        return BY_PLAYER.containsKey(player.getUuid());
    }

    /** Keeps a mission enemy counted when it turns into another mob (a Scientist Toilet's upgrade). */
    public static void onConverted(MobEntity from, MobEntity to) {
        MissionRun run = BY_MOB.get(from.getUuid());
        if (run != null) {
            run.replaceMob(from.getUuid(), to);
        }
    }

    /** Makes a mob that a mission enemy brought in (a Giant Flying Toilet's Flying Toilets) part of the mission too. */
    public static void onSummoned(MobEntity summoner, MobEntity summoned) {
        MissionRun run = BY_MOB.get(summoner.getUuid());
        if (run != null) {
            run.addSummoned(summoned);
        }
    }

    static void trackMob(UUID mob, MissionRun run) {
        BY_MOB.put(mob, run);
    }

    static void untrackMob(UUID mob) {
        BY_MOB.remove(mob);
    }

    static void untrackPlayer(UUID player) {
        BY_PLAYER.remove(player);
    }

    static void removeRun(MissionRun run) {
        RUNS.remove(run);
    }

    private static void tick(MinecraftServer server) {
        for (Entity stray : STRAYS) {
            if (!stray.isRemoved() && !BY_MOB.containsKey(stray.getUuid())) {
                stray.discard();
            }
        }
        STRAYS.clear();
        for (MissionRun run : List.copyOf(RUNS)) {
            run.tick(server);
        }
    }
}
