package com.skibiditoilet.story;

import com.skibiditoilet.entity.GManToiletEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** One mission being played: its players, the mobs it spawned, and which stage it's on. */
final class MissionRun {
    private static final double ENEMY_DISTANCE = 10.0;
    private static final double ALLY_DISTANCE = 3.0;
    /** The mission fails if every player gets this far from where the current stage started. */
    private static final double LEASH_RANGE = 96.0;
    /** Each extra player in the party adds this much to the enemies' health. */
    private static final float HEALTH_PER_EXTRA_PLAYER = 0.5f;
    private static final int SPAWN_ANGLES = 16;

    private final Mission mission;
    private final ServerWorld world;
    private final Set<UUID> participants = new LinkedHashSet<>();
    // Players who get the mission marked done: it was their next mission (not already done, not ahead of them).
    private final Set<UUID> credited = new HashSet<>();
    private final float enemyHealthScale;
    private final Map<UUID, Tracked> mobs = new HashMap<>();
    // Players who died and haven't respawned yet. Tracked by event because with instant respawn a player
    // can die and be back on their feet between two mission ticks.
    private final Set<UUID> downed = new HashSet<>();
    private boolean wiped;

    private Stage stage;
    private int stageIndex;
    private Vec3d anchor;
    private int ticks;
    private int defeated;
    private int goal;
    @Nullable
    private UUID boss;
    private boolean bossDown;
    @Nullable
    private ServerBossBar bossBar;
    private boolean ended;

    private static final class Tracked {
        final boolean enemy;
        final boolean escapes;
        Vec3d lastPos;

        Tracked(boolean enemy, boolean escapes, Vec3d pos) {
            this.enemy = enemy;
            this.escapes = escapes;
            this.lastPos = pos;
        }
    }

    MissionRun(Mission mission, ServerWorld world, List<ServerPlayerEntity> players) {
        this.mission = mission;
        this.world = world;
        for (ServerPlayerEntity player : players) {
            this.participants.add(player.getUuid());
            StoryData data = StoryData.of(player);
            if (!data.isCompleted(mission.id()) && StoryMissions.isUnlocked(data, mission)) {
                this.credited.add(player.getUuid());
            }
        }
        this.enemyHealthScale = 1.0f + HEALTH_PER_EXTRA_PLAYER * (players.size() - 1);
        this.stage = mission.stages().get(0);
        this.anchor = players.get(0).getPos();
    }

    void begin() {
        this.startStage(0);
    }

    private void startStage(int index) {
        this.stageIndex = index;
        this.stage = this.mission.stages().get(index);
        this.ticks = 0;
        this.defeated = 0;
        this.boss = null;
        this.bossDown = false;
        List<ServerPlayerEntity> online = this.onlineParticipants();
        if (online.isEmpty()) {
            this.end();
            return;
        }
        // The fight comes to whoever is standing; a player waiting to respawn may be far away.
        ServerPlayerEntity focus = online.stream().filter(LivingEntity::isAlive).findFirst().orElse(online.get(0));
        this.anchor = focus.getPos();

        for (Spawn ally : this.stage.allies()) {
            this.spawn(ally, focus, ALLY_DISTANCE, false);
        }
        int enemies = 0;
        for (Spawn enemy : this.stage.enemies()) {
            enemies += this.spawn(enemy, focus, ENEMY_DISTANCE, true);
        }
        this.goal = this.boss != null ? 1 : enemies;
        this.syncStatus();
    }

    /** Spawns one Spawn's mobs around a player. Returns how many came in. */
    private int spawn(Spawn spawn, ServerPlayerEntity around, double distance, boolean enemy) {
        int spawned = 0;
        for (int i = 0; i < spawn.count(); i++) {
            MobEntity mob = spawn.type().create(this.world);
            if (mob == null) {
                continue;
            }
            Vec3d pos = this.findSpot(spawn.type(), around.getPos(), distance);
            if (mob.hasNoGravity()) {
                pos = this.lift(spawn.type(), pos);
            }
            float yaw = (float) (MathHelper.atan2(around.getZ() - pos.z, around.getX() - pos.x) * MathHelper.DEGREES_PER_RADIAN) - 90.0f;
            mob.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0f);
            mob.setHeadYaw(yaw);
            mob.initialize(this.world, this.world.getLocalDifficulty(mob.getBlockPos()), SpawnReason.EVENT, null, null);
            mob.setPersistent();
            mob.addCommandTag(MissionManager.TAG);
            float health = spawn.health() > 0.0f ? spawn.health() : mob.getMaxHealth();
            if (enemy) {
                health *= this.enemyHealthScale;
            }
            setMaxHealth(mob, health);
            EntityAttributeInstance followRange = mob.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
            if (enemy && followRange != null && followRange.getBaseValue() < 40.0) {
                // Mission enemies come for you even from the edge of the fight.
                followRange.setBaseValue(40.0);
            }
            if (spawn.heldInPlace()) {
                mob.setAiDisabled(true);
            }

            MissionManager.trackMob(mob.getUuid(), this);
            this.mobs.put(mob.getUuid(), new Tracked(enemy, spawn.leavesAtHalfHealth(), pos));
            this.world.spawnEntityAndPassengers(mob);
            this.world.spawnParticles(ParticleTypes.POOF, pos.x, pos.y + mob.getHeight() / 2, pos.z, 12,
                    mob.getWidth() / 2, mob.getHeight() / 2, mob.getWidth() / 2, 0.02);
            if (enemy) {
                if (spawn.boss()) {
                    this.boss = mob.getUuid();
                    this.showBossBar(mob);
                }
                if (!spawn.heldInPlace()) {
                    mob.setTarget(around);
                }
            }
            spawned++;
        }
        return spawned;
    }

    private static void setMaxHealth(MobEntity mob, float health) {
        EntityAttributeInstance maxHealth = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(health);
            mob.setHealth(health);
        }
    }

    /**
     * A spot on solid ground about {@code distance} blocks away where the mob fits, trying all around the
     * player and a little above and below. Falls back to closer in, and then to the player's own spot.
     */
    private Vec3d findSpot(EntityType<?> type, Vec3d center, double distance) {
        for (double range : new double[]{distance, distance / 2}) {
            float start = this.world.random.nextFloat() * MathHelper.TAU;
            for (int i = 0; i < SPAWN_ANGLES; i++) {
                float angle = start + i * MathHelper.TAU / SPAWN_ANGLES;
                double d = range + this.world.random.nextDouble() * 2.0 - 1.0;
                double x = center.x + MathHelper.cos(angle) * d;
                double z = center.z + MathHelper.sin(angle) * d;
                for (int dy = 4; dy >= -8; dy--) {
                    BlockPos pos = BlockPos.ofFloored(x, center.y + dy, z);
                    BlockPos below = pos.down();
                    if (this.world.getBlockState(below).isSideSolidFullSquare(this.world, below, Direction.UP)
                            && this.world.getFluidState(pos).isEmpty()
                            && this.world.isSpaceEmpty(type.createSimpleBoundingBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
                        return new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    }
                }
            }
        }
        return center;
    }

    /** Flyers arrive up in the air: a few blocks above the spot, as high as there's room for. */
    private Vec3d lift(EntityType<?> type, Vec3d pos) {
        for (int up = 6; up > 0; up--) {
            if (this.world.isSpaceEmpty(type.createSimpleBoundingBox(pos.x, pos.y + up, pos.z))) {
                return pos.add(0.0, up, 0.0);
            }
        }
        return pos;
    }

    private void showBossBar(MobEntity mob) {
        // G-Toilet already has his own boss bar.
        if (mob instanceof GManToiletEntity) {
            return;
        }
        this.bossBar = new ServerBossBar(mob.getDisplayName(), BossBar.Color.RED, BossBar.Style.NOTCHED_10);
    }

    private void hideBossBar() {
        if (this.bossBar != null) {
            this.bossBar.clearPlayers();
            this.bossBar = null;
        }
    }

    void tick(MinecraftServer server) {
        if (this.ended) {
            return;
        }
        List<ServerPlayerEntity> online = this.onlineParticipants();
        if (online.isEmpty()) {
            this.end();
            return;
        }
        if (this.world.getDifficulty() == Difficulty.PEACEFUL) {
            this.fail(StoryEvent.FAILED_PEACEFUL);
            return;
        }
        if (this.wiped || this.downed.containsAll(this.participants) || online.stream().noneMatch(LivingEntity::isAlive)) {
            this.fail(StoryEvent.FAILED_DIED);
            return;
        }
        this.ticks++;
        if (this.ticks % 20 == 0) {
            if (online.stream().noneMatch(player -> player.getWorld() == this.world
                    && player.getPos().isInRange(this.anchor, LEASH_RANGE))) {
                this.fail(StoryEvent.FAILED_LEFT_AREA);
                return;
            }
            this.checkMissingMobs();
        }
        this.updateMobs(online);
        if (this.ended) {
            return;
        }

        if (this.stage.kind() == Stage.Kind.SURVIVE) {
            this.reinforce(online);
            if (this.ticks >= this.stage.seconds() * 20 || this.bossDown) {
                this.completeStage();
            } else if (this.ticks % 20 == 0) {
                this.syncStatus();
            }
        } else if (this.boss != null ? this.bossDown : this.enemiesLeft() == 0) {
            this.completeStage();
        }
    }

    /** Lets escaping enemies leave at half health, and keeps the boss bar current. */
    private void updateMobs(List<ServerPlayerEntity> online) {
        for (Map.Entry<UUID, Tracked> entry : List.copyOf(this.mobs.entrySet())) {
            if (!entry.getValue().escapes || !(this.world.getEntity(entry.getKey()) instanceof MobEntity mob)) {
                continue;
            }
            if (mob.isAlive() && mob.getHealth() <= mob.getMaxHealth() / 2) {
                this.escape(mob, online);
            }
        }
        if (this.bossBar != null && this.boss != null && this.world.getEntity(this.boss) instanceof LivingEntity boss) {
            this.bossBar.setPercent(boss.getHealth() / boss.getMaxHealth());
            for (ServerPlayerEntity player : online) {
                this.bossBar.addPlayer(player);
            }
        }
    }

    private void escape(MobEntity mob, List<ServerPlayerEntity> online) {
        poof(this.world, mob);
        this.world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2.0f, 0.6f);
        for (ServerPlayerEntity player : online) {
            player.sendMessage(Text.translatable("story.skibidi.escaped", mob.getDisplayName()), true);
        }
        mob.discard();
        this.onMobGone(mob.getUuid());
    }

    private void reinforce(List<ServerPlayerEntity> online) {
        Spawn reinforcement = this.stage.reinforcement();
        if (reinforcement == null || this.ticks % (this.stage.reinforceSeconds() * 20) != 0
                || this.enemiesLeft() >= this.stage.reinforceCap()) {
            return;
        }
        // Reinforcements head for a random player who is still standing.
        List<ServerPlayerEntity> alive = online.stream()
                .filter(player -> player.isAlive() && player.getWorld() == this.world).toList();
        if (!alive.isEmpty()) {
            this.spawn(reinforcement, alive.get(this.world.random.nextInt(alive.size())), ENEMY_DISTANCE, true);
        }
    }

    /**
     * Mobs normally leave through death, which onMobGone hears about. This catches the rest (removed by a
     * command, pushed through a portal) by looking for them where they were last seen, as long as that
     * place is loaded, so a mob in an unloaded chunk isn't mistaken for a gone one.
     */
    private void checkMissingMobs() {
        for (Map.Entry<UUID, Tracked> entry : List.copyOf(this.mobs.entrySet())) {
            if (this.world.getEntity(entry.getKey()) instanceof LivingEntity mob && mob.isAlive()) {
                entry.getValue().lastPos = mob.getPos();
            } else if (this.world.shouldTickEntity(BlockPos.ofFloored(entry.getValue().lastPos))) {
                this.onMobGone(entry.getKey());
            }
        }
    }

    /** The mission fails once every player in it is down at the same time. */
    void onParticipantDied(UUID player) {
        this.downed.add(player);
        if (this.downed.containsAll(this.participants)) {
            this.wiped = true;
        }
    }

    void onParticipantRespawned(UUID player) {
        this.downed.remove(player);
    }

    /** A mission mob died, escaped or vanished. */
    void onMobGone(UUID uuid) {
        Tracked tracked = this.mobs.remove(uuid);
        MissionManager.untrackMob(uuid);
        if (tracked == null || !tracked.enemy) {
            return;
        }
        this.defeated++;
        if (uuid.equals(this.boss)) {
            this.bossDown = true;
            this.hideBossBar();
        }
        if (!this.ended && this.stage.kind() == Stage.Kind.DEFEAT) {
            this.syncStatus();
        }
    }

    void addSummoned(MobEntity mob) {
        if (this.ended) {
            return;
        }
        mob.addCommandTag(MissionManager.TAG);
        mob.setPersistent();
        this.mobs.put(mob.getUuid(), new Tracked(true, false, mob.getPos()));
        MissionManager.trackMob(mob.getUuid(), this);
        // With no boss, every enemy has to go, so the new ones count toward the goal.
        if (this.boss == null && this.stage.kind() == Stage.Kind.DEFEAT) {
            this.goal++;
            this.syncStatus();
        }
    }

    void replaceMob(UUID from, MobEntity to) {
        Tracked tracked = this.mobs.remove(from);
        MissionManager.untrackMob(from);
        if (tracked == null) {
            return;
        }
        to.addCommandTag(MissionManager.TAG);
        to.setPersistent();
        this.mobs.put(to.getUuid(), tracked);
        MissionManager.trackMob(to.getUuid(), this);
        if (from.equals(this.boss)) {
            this.boss = to.getUuid();
        }
    }

    private int enemiesLeft() {
        return (int) this.mobs.values().stream().filter(tracked -> tracked.enemy).count();
    }

    private void completeStage() {
        // Whatever is left of this stage's enemies backs off.
        this.removeMobs(true);
        this.hideBossBar();
        if (this.stageIndex + 1 < this.mission.stages().size()) {
            for (ServerPlayerEntity player : this.onlineParticipants()) {
                StorySync.sendEvent(player, StoryEvent.STAGE_COMPLETE, this.mission);
            }
            this.startStage(this.stageIndex + 1);
            return;
        }
        for (ServerPlayerEntity player : this.onlineParticipants()) {
            boolean credit = this.credited.contains(player.getUuid());
            if (credit) {
                StorySync.set(player, StoryData.of(player).withCompleted(this.mission.id()));
            }
            StorySync.sendEvent(player, credit ? StoryEvent.COMPLETED : StoryEvent.HELPED, this.mission);
        }
        this.end();
    }

    void fail(StoryEvent reason) {
        for (ServerPlayerEntity player : this.onlineParticipants()) {
            StorySync.sendEvent(player, reason, this.mission);
        }
        this.end();
    }

    /** Stops the mission: every mob it brought leaves, and its players are free to start another. */
    void end() {
        if (this.ended) {
            return;
        }
        this.ended = true;
        this.removeMobs(false);
        this.hideBossBar();
        for (ServerPlayerEntity player : this.onlineParticipants()) {
            StorySync.sendStatus(player, null);
        }
        for (UUID participant : this.participants) {
            MissionManager.untrackPlayer(participant);
        }
        MissionManager.removeRun(this);
    }

    private void removeMobs(boolean enemiesOnly) {
        for (Map.Entry<UUID, Tracked> entry : List.copyOf(this.mobs.entrySet())) {
            if (enemiesOnly && !entry.getValue().enemy) {
                continue;
            }
            if (this.world.getEntity(entry.getKey()) instanceof MobEntity mob) {
                poof(this.world, mob);
                mob.discard();
            }
            this.mobs.remove(entry.getKey());
            MissionManager.untrackMob(entry.getKey());
        }
    }

    private static void poof(ServerWorld world, MobEntity mob) {
        world.spawnParticles(ParticleTypes.POOF, mob.getX(), mob.getBodyY(0.5), mob.getZ(), 20,
                mob.getWidth() / 2, mob.getHeight() / 2, mob.getWidth() / 2, 0.02);
    }

    private void syncStatus() {
        int ticksLeft = this.stage.kind() == Stage.Kind.SURVIVE ? Math.max(0, this.stage.seconds() * 20 - this.ticks) : 0;
        int progress = this.boss != null ? (this.bossDown ? 1 : 0) : Math.min(this.defeated, this.goal);
        MissionStatus status = new MissionStatus(this.mission.id(), this.stageIndex, progress, this.goal, ticksLeft);
        for (ServerPlayerEntity player : this.onlineParticipants()) {
            StorySync.sendStatus(player, status);
        }
    }

    /** The participants still online. Anyone who logged out is dropped from the mission for good. */
    private List<ServerPlayerEntity> onlineParticipants() {
        List<ServerPlayerEntity> online = new ArrayList<>();
        for (UUID uuid : List.copyOf(this.participants)) {
            ServerPlayerEntity player = this.world.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null) {
                online.add(player);
            } else {
                this.participants.remove(uuid);
                MissionManager.untrackPlayer(uuid);
            }
        }
        return online;
    }
}
