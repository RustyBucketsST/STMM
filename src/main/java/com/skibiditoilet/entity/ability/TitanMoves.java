package com.skibiditoilet.entity.ability;

import com.skibiditoilet.entity.LaserBeam;
import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModDamageTypes;
import com.skibiditoilet.registry.ModEffects;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * The 1.0 Titans' moves, used by the Titan mobs (AllianceAbilities) and by players in a Titan form (their Titan
 * powers). A mob aims them at its target, a player where they look. {@code power} scales the damage. They only hit
 * toilets and other monsters. Where they come from (core, arm cannon, hand) is worked out from the user's size, so
 * they fit a Titan of any height.
 */
public final class TitanMoves {
    public static final int CORE_BLUE = 0x46C8FF;
    public static final int PLASMA_RED = 0xFF4A3A;
    public static final int TV_PURPLE = 0xB45AFF;
    private static final int HOOK_GREY = 0x9A9AA6;
    public static final double CORE_BEAM_RANGE = 40.0;
    public static final double SOUNDWAVE_RANGE = 16.0;
    public static final double PLASMA_RANGE = 36.0;
    public static final double DEATH_SCREEN_RANGE = 22.0;
    public static final double HOOK_RANGE = 30.0;

    private TitanMoves() {
    }

    /** Titan Cameraman: a thick beam from his blue core that bursts where it lands. */
    public static void coreBeam(LivingEntity user, Vec3d direction, float power) {
        Vec3d core = core(user);
        LaserBeam.Shot shot = LaserBeam.fireFrom(world(user), user, core, direction, CORE_BEAM_RANGE, 20.0f * power,
                CORE_BLUE, 0.9f);
        burst(user, shot.end(), 3.0, 8.0f * power, CORE_BLUE);
        sound(user, ModSounds.GIANT_FLYING_TOILET_LASER, 0.8f);
    }

    /** Titan Speakerman: every speaker on his head and shoulders blasts at once, a huge cone that shakes the ground. */
    public static void soundwaves(LivingEntity user, Vec3d direction, float power) {
        ServerWorld world = world(user);
        Vec3d head = user.getEyePos();
        blast(user, head, direction, SOUNDWAVE_RANGE, 0.6, 12.0f * power, 2.5);
        float size = user.getHeight() / 1.95f;
        ModFx.speakerWave(world, head, head.add(direction), size);
        Vec3d side = side(user).multiply(user.getWidth() * 0.9);
        Vec3d shoulders = user.getPos().add(0.0, user.getHeight() * 0.85, 0.0);
        ModFx.speakerWave(world, shoulders.add(side), shoulders.add(side).add(direction), size * 0.75f);
        ModFx.speakerWave(world, shoulders.subtract(side), shoulders.subtract(side).add(direction), size * 0.75f);
        ModFx.shake(world, user.getPos(), 0.5f, (float) SOUNDWAVE_RANGE + 8.0f);
        sound(user, ModSounds.SPEAKERMAN_BLAST, 0.55f);
    }

    /** Titan Speakerman: a bolt from the plasma cannon on his left arm that bursts where it lands. */
    public static void plasmaCannon(LivingEntity user, Vec3d direction, float power) {
        LaserBeam.Shot shot = LaserBeam.fireFrom(world(user), user, cannon(user), direction, PLASMA_RANGE, 14.0f * power,
                PLASMA_RED, 0.5f);
        burst(user, shot.end(), 2.5, 6.0f * power, PLASMA_RED);
        sound(user, ModSounds.TITAN_PLASMA, 1.0f);
    }

    /** The jetpack fires: launches the user at {@code velocity} with a blast under them (flames are the caller's). */
    public static void jetpack(LivingEntity user, Vec3d velocity) {
        user.setVelocity(velocity);
        user.velocityModified = true;
        user.fallDistance = 0.0f;
        ModFx.shockwave(world(user), user.getPos(), user.getWidth() * 1.5f, 0.3f);
        sound(user, ModSounds.TITAN_JETPACK, 0.8f);
    }

    /** Flames and smoke out of a jetpack on the user's back, for one tick of flight. */
    public static void jetpackFlames(LivingEntity user) {
        float yaw = user.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
        Vec3d back = new Vec3d(MathHelper.sin(yaw), 0.0, -MathHelper.cos(yaw)).multiply(user.getWidth() * 0.45);
        Vec3d nozzle = user.getPos().add(back).add(0.0, user.getHeight() * 0.5, 0.0);
        double spread = user.getWidth() * 0.12;
        ServerWorld world = world(user);
        world.spawnParticles(ParticleTypes.FLAME, nozzle.x, nozzle.y, nozzle.z, 6,
                spread, spread, spread, 0.02);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, nozzle.x, nozzle.y - spread, nozzle.z, 2,
                spread, spread, spread, 0.01);
    }

    /** Titan TV Man: the death screen flashes, stunning and hurting every toilet in front of him. */
    public static void deathScreen(LivingEntity user, Vec3d direction, float power) {
        ServerWorld world = world(user);
        Vec3d head = user.getEyePos();
        for (LivingEntity hit : Targets.inCone(user, head, direction, DEATH_SCREEN_RANGE, 0.65, Targets::isMonster)) {
            hit.addStatusEffect(new StatusEffectInstance(ModEffects.STUNNED, (int) (80 * Math.max(1.0f, power))), user);
            hit.damage(ModDamageTypes.power(world, user), 8.0f * power);
            ModFx.tvStatic(world, hit.getEyePos(), 0.8f);
        }
        ModFx.cone(world, head, head.add(direction), (float) DEATH_SCREEN_RANGE, TV_PURPLE);
        ModFx.cameraFlash(world, head.add(direction.multiply(2.0)), 4.0f);
        sound(user, ModSounds.POWER_STUN_FLASH, 0.7f);
    }

    /** Titan TV Man: one sweep of the triple blades on his shoulders through every toilet close in front. */
    public static void clawSweep(LivingEntity user, Vec3d direction, float power) {
        ServerWorld world = world(user);
        Vec3d from = user.getPos().add(0.0, user.getHeight() * 0.4, 0.0);
        Vec3d flat = direction.multiply(1, 0, 1).normalize();
        double reach = clawReach(user);
        for (LivingEntity hit : Targets.inCone(user, from, flat, reach, 0.0, Targets::isMonster)) {
            hit.damage(melee(user), 16.0f * power);
            hit.takeKnockback(1.5, -flat.x, -flat.z);
            ModFx.sparks(world, hit.getBoundingBox().getCenter(), from, TV_PURPLE);
        }
        user.swingHand(Hand.MAIN_HAND, true);
        ModFx.ring(world, user.getPos().add(0.0, 1.0, 0.0), (float) reach, TV_PURPLE);
        sound(user, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f);
        sound(user, ModSounds.ENERGY_KNIFE_SWING, 0.6f);
    }

    /** Titan TV Man: the grappling hook on his elbow shoots out and reels {@code target} in to him. */
    public static void grapplingHook(LivingEntity user, LivingEntity target, float power) {
        ModFx.beam(world(user), hand(user), target.getBoundingBox().getCenter(), HOOK_GREY, 0.15f);
        Vec3d pull = user.getPos().subtract(target.getPos());
        Vec3d speed = pull.multiply(1, 0, 1).normalize().multiply(Math.min(3.0, 0.9 + pull.length() * 0.08));
        target.setVelocity(speed.x, 0.5 + MathHelper.clamp(pull.y * 0.05, 0.0, 0.6), speed.z);
        target.velocityModified = true;
        target.damage(melee(user), 4.0f * power);
        sound(user, ModSounds.GRAVITY_PLUNGER_PULL, 0.6f);
    }

    /** The toilet a hook (or other shot) from {@code from} along {@code direction} would catch first, or null. */
    @Nullable
    public static LivingEntity firstMonster(LivingEntity user, Vec3d from, Vec3d direction, double range) {
        Entity first = LaserBeam.aimFrom(world(user), user, from, direction, range);
        return first instanceof LivingEntity living && Targets.isMonster(living) ? living : null;
    }

    // ---------------------------------------------------------------- where things are on a Titan

    /** The core, in the middle of the chest. */
    public static Vec3d core(LivingEntity user) {
        return user.getPos().add(0.0, user.getHeight() * 0.64, 0.0).add(forward(user).multiply(user.getWidth() * 0.5));
    }

    /** The muzzle of the plasma cannon on the left forearm. */
    public static Vec3d cannon(LivingEntity user) {
        return user.getPos().add(0.0, user.getHeight() * 0.38, 0.0).add(side(user).multiply(user.getWidth() * 0.85))
                .add(forward(user).multiply(user.getWidth() * 0.3));
    }

    /** The right hand, where the grappling hook shoots from. */
    public static Vec3d hand(LivingEntity user) {
        return user.getPos().add(0.0, user.getHeight() * 0.42, 0.0).subtract(side(user).multiply(user.getWidth() * 0.8));
    }

    /** How far the claws reach in front. */
    public static double clawReach(LivingEntity user) {
        return user.getWidth() * 0.5 + 6.0;
    }

    /** The user's left, flat: its body faces along {@code bodyYaw}. */
    private static Vec3d side(LivingEntity user) {
        float yaw = user.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(MathHelper.cos(yaw), 0.0, MathHelper.sin(yaw));
    }

    private static Vec3d forward(LivingEntity user) {
        float yaw = user.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(-MathHelper.sin(yaw), 0.0, MathHelper.cos(yaw));
    }

    // ---------------------------------------------------------------- helpers

    /** Hurts and pushes away every toilet in a cone. */
    static void blast(LivingEntity user, Vec3d from, Vec3d direction, double range, double minDot, float damage,
                      double knockback) {
        ServerWorld world = world(user);
        for (LivingEntity hit : Targets.inCone(user, from, direction, range, minDot, Targets::isMonster)) {
            hit.damage(ModDamageTypes.power(world, user), damage);
            Vec3d push = hit.getPos().subtract(user.getPos()).multiply(1, 0, 1).normalize().multiply(knockback);
            hit.addVelocity(push.x, 0.3 + knockback * 0.1, push.z);
            hit.velocityModified = true;
        }
    }

    /** Hurts every toilet within {@code radius} of where a big shot landed. */
    private static void burst(LivingEntity user, Vec3d at, double radius, float damage, int color) {
        ServerWorld world = world(user);
        for (LivingEntity hit : world.getEntitiesByClass(LivingEntity.class, new Box(at, at).expand(radius),
                entity -> Targets.isMonster(entity) && entity.getBoundingBox().getCenter().squaredDistanceTo(at) < radius * radius)) {
            hit.damage(ModDamageTypes.power(world, user), damage);
        }
        ModFx.shockwave(world, at, (float) radius, 0.25f);
        ModFx.ring(world, at, (float) radius, color);
    }

    private static DamageSource melee(LivingEntity user) {
        return user instanceof PlayerEntity player ? user.getDamageSources().playerAttack(player)
                : user.getDamageSources().mobAttack(user);
    }

    /** A sound from the user, louder and deeper the bigger they are. */
    private static void sound(LivingEntity user, SoundEvent sound, float pitch) {
        float size = user.getHeight() / 1.95f;
        user.getWorld().playSound(null, user.getX(), user.getY(), user.getZ(), sound, user.getSoundCategory(),
                Math.min(3.0f, 0.6f + size * 0.5f), pitch);
    }

    private static ServerWorld world(LivingEntity user) {
        return (ServerWorld) user.getWorld();
    }
}
