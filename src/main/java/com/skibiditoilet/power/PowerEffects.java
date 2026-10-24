package com.skibiditoilet.power;

import com.skibiditoilet.entity.AllianceEntity;
import com.skibiditoilet.entity.SkibidiToiletEntity;
import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModDamageTypes;
import com.skibiditoilet.registry.ModEffects;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * What each power does when it's used. Area powers hit what's in front of the player (a cone from their
 * eyes) or all around them, and only hit enemies: toilets and other monsters, never players or the Alliance.
 */
final class PowerEffects {
    static final int SHIELD_TICKS = 120;
    static final int DANCE_TICKS = 160;
    private static final int BASH_EVERY = 10;
    private static final int SHIELD_COLOR = 0x5FE3FF;
    private static final int SCAN_COLOR = 0x7CFF9B;
    private static final int HYPNOSIS_COLOR = 0xB45CFF;
    private static final int BURN_COLOR = 0xFF9A3C;
    /** Toilets this tough (G-Man, the Giant Flyer) shrug off hypnosis. */
    private static final float HYPNOSIS_MAX_HEALTH = 150.0f;

    private PowerEffects() {
    }

    /** Uses {@code power}. Returns false if it couldn't be used (nowhere to teleport), so it costs nothing. */
    static boolean use(ServerPlayerEntity player, Power power, float scale, PowerManager.Runtime runtime, long now) {
        ServerWorld world = player.getServerWorld();
        Vec3d eyes = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        switch (power) {
            case HOLOSHIELD -> {
                runtime.shieldUntil = now + SHIELD_TICKS;
                PowerSync.sendVisible(player, PowerSync.Visible.SHIELD, SHIELD_TICKS);
                ModFx.sparks(world, eyes.add(look.multiply(0.8)), eyes.add(look.multiply(2.0)), SHIELD_COLOR);
                sound(player, ModSounds.POWER_HOLOSHIELD, 1.0f);
            }
            case RECORD_SCAN -> {
                List<SkibidiToiletEntity> toilets = world.getEntitiesByClass(SkibidiToiletEntity.class,
                        player.getBoundingBox().expand(32.0), toilet -> toilet.isAlive() && toilet.squaredDistanceTo(player) < 32.0 * 32.0);
                for (SkibidiToiletEntity toilet : toilets) {
                    toilet.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, (int) (200 * scale)), player);
                }
                ModFx.cameraFlash(world, eyes.add(look.multiply(0.5)), 1.4f);
                ModFx.ring(world, player.getPos(), 32.0f, SCAN_COLOR);
                sound(player, ModSounds.POWER_RECORD_SCAN, 1.0f);
                player.sendMessage(Text.translatable("power.skibidi.record_scan.result", toilets.size())
                        .formatted(Formatting.GREEN), true);
            }
            case SONIC_BLAST -> {
                for (LivingEntity target : inCone(player, 7.0, 0.75)) {
                    target.damage(ModDamageTypes.power(world, player), 5.0f * scale);
                    Vec3d push = target.getPos().subtract(player.getPos()).multiply(1, 0, 1).normalize().multiply(1.6);
                    target.addVelocity(push.x, 0.35, push.z);
                    target.velocityModified = true;
                }
                ModFx.speakerWave(world, eyes.subtract(0.0, 0.2, 0.0), eyes.add(look), 1.8f);
                ModFx.shake(world, player.getPos(), 0.25f, 8.0f);
                sound(player, ModSounds.SPEAKERMAN_BLAST, 0.8f);
            }
            case SONIC_BOOTS -> {
                Vec3d flat = new Vec3d(look.x, 0.0, look.z);
                if (player.isSneaking() || flat.lengthSquared() < 1.0E-4) {
                    // Straight up.
                    player.setVelocity(player.getVelocity().x, 1.1, player.getVelocity().z);
                } else {
                    flat = flat.normalize().multiply(1.6);
                    player.setVelocity(flat.x, 0.35, flat.z);
                }
                player.velocityModified = true;
                player.fallDistance = 0.0f;
                runtime.noFallUntil = now + 60;
                ModFx.ring(world, player.getPos(), 2.5f, 0x9AD8FF);
                sound(player, ModSounds.POWER_SONIC_BOOTS, 1.0f);
            }
            case ANTHEM_DANCE -> {
                runtime.danceUntil = now + DANCE_TICKS;
                PowerSync.sendVisible(player, PowerSync.Visible.DANCE, DANCE_TICKS);
                world.playSoundFromEntity(null, player, ModSounds.POWER_ANTHEM_DANCE, SoundCategory.PLAYERS, 1.0f, 1.0f);
                anthemPulse(player);
            }
            case HYPNOSIS -> {
                int ticks = (int) (120 * scale);
                int hypnotized = 0;
                for (LivingEntity target : inCone(player, 10.0, 0.75)) {
                    if (target instanceof SkibidiToiletEntity toilet && toilet.getMaxHealth() < HYPNOSIS_MAX_HEALTH) {
                        toilet.addStatusEffect(new StatusEffectInstance(ModEffects.HYPNOTIZED, ticks), player);
                        toilet.setTarget(null);
                        ModFx.tvStatic(world, toilet.getEyePos(), 0.8f);
                        hypnotized++;
                    }
                }
                ModFx.speakerWave(world, eyes.subtract(0.0, 0.2, 0.0), eyes.add(look), 1.4f, HYPNOSIS_COLOR);
                sound(player, ModSounds.POWER_HYPNOSIS, 1.0f);
                if (hypnotized == 0) {
                    player.sendMessage(Text.translatable("power.skibidi.hypnosis.none").formatted(Formatting.GRAY), true);
                }
            }
            case TELEPORT -> {
                Vec3d spot = teleportSpot(player, 16.0);
                if (spot == null) {
                    player.sendMessage(Text.translatable("power.skibidi.teleport.blocked").formatted(Formatting.GRAY), true);
                    return false;
                }
                Vec3d from = player.getPos();
                player.stopRiding();
                player.requestTeleport(spot.x, spot.y, spot.z);
                player.fallDistance = 0.0f;
                runtime.noFallUntil = now + 40;
                ModFx.teleport(world, from, 1.0f, false);
                ModFx.teleport(world, spot, 1.0f, true);
                world.playSound(null, from.x, from.y, from.z, ModSounds.POWER_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                world.playSound(null, spot.x, spot.y, spot.z, ModSounds.POWER_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.2f);
            }
            case BURNING_LIGHT -> {
                for (LivingEntity target : inCone(player, 8.0, 0.8)) {
                    target.setOnFireFor(5);
                    target.damage(ModDamageTypes.power(world, player), 3.0f * scale);
                }
                ModFx.cone(world, eyes.subtract(0.0, 0.1, 0.0), eyes.add(look), 8.0f, BURN_COLOR);
                sound(player, ModSounds.POWER_BURNING_LIGHT, 1.0f);
            }
            case STUN_FLASH -> {
                int ticks = (int) (60 * scale);
                for (LivingEntity target : around(player, 7.0)) {
                    target.addStatusEffect(new StatusEffectInstance(ModEffects.STUNNED, ticks), player);
                    target.damage(ModDamageTypes.power(world, player), 2.0f * scale);
                }
                ModFx.cameraFlash(world, eyes, 3.5f);
                ModFx.tvStatic(world, eyes, 1.5f);
                ModFx.ring(world, player.getPos(), 7.0f, 0xFFFFFF);
                ModFx.shake(world, player.getPos(), 0.3f, 10.0f);
                sound(player, ModSounds.POWER_STUN_FLASH, 1.0f);
            }
        }
        return true;
    }

    /**
     * Whether the Holoshield stops this hit: anything coming from in front of the player that has a
     * position (lasers, bites, arrows, blasts), but not falling, drowning, fire and the like.
     */
    static boolean shieldBlocks(ServerPlayerEntity player, DamageSource source) {
        Vec3d from = source.getPosition();
        if (from == null || source.isIn(DamageTypeTags.BYPASSES_SHIELD)) {
            return false;
        }
        Vec3d toSource = from.subtract(player.getPos()).multiply(1, 0, 1);
        Vec3d facing = player.getRotationVec(1.0f).multiply(1, 0, 1);
        if (toSource.lengthSquared() < 1.0E-4 || facing.lengthSquared() < 1.0E-4
                || toSource.normalize().dotProduct(facing.normalize()) < 0.3) {
            return false;
        }
        float size = FormHolder.scale(player);
        Vec3d front = player.getEyePos().subtract(0.0, 0.4 * size, 0.0).add(facing.normalize().multiply(0.8 * size));
        ModFx.sparks(player.getServerWorld(), front, from, SHIELD_COLOR);
        sound(player, ModSounds.POWER_SHIELD_HIT, 1.0f + player.getRandom().nextFloat() * 0.2f);
        return true;
    }

    /** Running with the Holoshield up rams toilets out of the way. */
    static void shieldBash(ServerPlayerEntity player, PowerManager.Runtime runtime, long now, float scale) {
        if (!player.isSprinting() || now - runtime.lastBash < BASH_EVERY) {
            return;
        }
        Vec3d facing = player.getRotationVec(1.0f).multiply(1, 0, 1).normalize();
        // In front of the shield, which is as big as the player.
        float size = FormHolder.scale(player);
        Vec3d front = player.getPos().add(facing.multiply(size));
        Box box = new Box(front, front).expand(0.9 * size, 0.0, 0.9 * size).stretch(0.0, player.getHeight(), 0.0);
        List<LivingEntity> hit = player.getServerWorld().getEntitiesByClass(LivingEntity.class, box, PowerEffects::isEnemy);
        if (hit.isEmpty()) {
            return;
        }
        runtime.lastBash = now;
        for (LivingEntity target : hit) {
            target.damage(ModDamageTypes.power(player.getServerWorld(), player), 4.0f * scale);
            target.takeKnockback(1.2, -facing.x, -facing.z);
            ModFx.sparks(player.getServerWorld(), target.getBoundingBox().getCenter(), player.getEyePos(), SHIELD_COLOR);
        }
        sound(player, ModSounds.POWER_SHIELD_HIT, 0.7f);
    }

    /** One beat of the Anthem Dance: everyone on the player's side close by fights better for a while. */
    static void anthemPulse(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        List<LivingEntity> allies = world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(10.0),
                entity -> entity.isAlive() && (entity instanceof PlayerEntity || entity instanceof AllianceEntity)
                        && !entity.isSpectator() && entity.squaredDistanceTo(player) < 100.0);
        for (LivingEntity ally : allies) {
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, true, true), player);
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 0, true, true), player);
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 0, true, true), player);
        }
        ModFx.notes(world, player.getPos(), 2.5f);
        ModFx.ring(world, player.getPos(), 10.0f, 0x9AD8FF);
    }

    // ---------------------------------------------------------------- targeting

    /** Toilets and other monsters. Players and the Alliance are never hit by powers. */
    private static boolean isEnemy(LivingEntity entity) {
        return entity.isAlive() && entity instanceof Monster && entity.isPartOfGame();
    }

    /** Enemies within {@code range} of the player's eyes that the player can see, inside a cone around where they're looking. */
    private static List<LivingEntity> inCone(ServerPlayerEntity player, double range, double minDot) {
        Vec3d eyes = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        return enemies(player, range, entity -> {
            Vec3d to = entity.getBoundingBox().getCenter().subtract(eyes);
            double distance = to.length();
            // Something right in your face counts as in front, whatever the angle.
            return distance < 1.5 || to.multiply(1.0 / distance).dotProduct(look) >= minDot;
        });
    }

    private static List<LivingEntity> around(ServerPlayerEntity player, double range) {
        return enemies(player, range, entity -> true);
    }

    private static List<LivingEntity> enemies(ServerPlayerEntity player, double range, Predicate<LivingEntity> where) {
        Vec3d eyes = player.getEyePos();
        return player.getServerWorld().getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(range),
                entity -> isEnemy(entity) && entity.getBoundingBox().getCenter().squaredDistanceTo(eyes) <= range * range
                        && player.canSee(entity) && where.test(entity));
    }

    /**
     * Where a teleport along the player's view lands: as far as they can see, up to {@code range}, in the
     * first spot back along the way their body fits. Looking at the ground puts them standing on it.
     */
    @Nullable
    private static Vec3d teleportSpot(ServerPlayerEntity player, double range) {
        ServerWorld world = player.getServerWorld();
        Vec3d eyes = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        BlockHitResult hit = world.raycast(new RaycastContext(eyes, eyes.add(look.multiply(range)),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        double reach = hit.getType() == HitResult.Type.MISS ? range : hit.getPos().distanceTo(eyes) - 0.4;
        double eyeHeight = player.getStandingEyeHeight();
        // Try keeping the eyes where they'd be first, then lift the body up to stand on what was looked at.
        double[] lifts = {-eyeHeight, -eyeHeight + 0.5, -eyeHeight + 1.0, -0.1, 0.4};
        for (double d = reach; d >= 2.0; d -= 0.5) {
            Vec3d point = eyes.add(look.multiply(d));
            for (double lift : lifts) {
                Vec3d feet = point.add(0.0, lift, 0.0);
                if (world.isSpaceEmpty(player, player.getDimensions(player.getPose()).getBoxAt(feet))) {
                    return feet;
                }
            }
        }
        return null;
    }

    private static void sound(Entity at, SoundEvent sound, float pitch) {
        at.getWorld().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundCategory.PLAYERS, 1.0f, pitch);
    }
}
