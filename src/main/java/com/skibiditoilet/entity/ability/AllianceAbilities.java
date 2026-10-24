package com.skibiditoilet.entity.ability;

import com.skibiditoilet.entity.AllianceEntity;
import com.skibiditoilet.entity.LaserBeam;
import com.skibiditoilet.entity.SkibidiToiletEntity;
import com.skibiditoilet.entity.TitanEntity;
import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModDamageTypes;
import com.skibiditoilet.registry.ModEffects;
import com.skibiditoilet.registry.ModEntities;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * What the Alliance mobs do besides punching, from the series:
 * <ul>
 * <li>Cameramen shoot a paralyzer laser that stuns a toilet for a moment (the Large Cameraman's hits harder).</li>
 * <li>Speakermen blast soundwaves that knock toilets back (the Large Speakerman's are bigger).</li>
 * <li>TV Men hypnotize a toilet with their screen so it fights the others, and teleport to toilets far away.</li>
 * <li>Titan Cameraman 1.0 charges a beam from his blue core and flies at far targets with his jetpack (and his riot
 * shield halves ranged hits, see TitanEntity).</li>
 * <li>Titan Speakerman 1.0 blasts soundwaves from his head and shoulder speakers, fires the plasma cannon on his left
 * arm, and has a jetpack too.</li>
 * <li>Titan TV Man 1.0 shows his death screen (stunning everything in front of him), sweeps with his shoulder claws,
 * and reels far targets in with his grappling hook.</li>
 * </ul>
 * They only ever hit toilets and other monsters.
 */
public final class AllianceAbilities {
    private static final int PARALYZER_COLOR = 0x6FD8FF;
    private static final int HYPNOSIS_COLOR = 0xB45CFF;
    private static final int CORE_BLUE = 0x46C8FF;
    private static final int PLASMA_RED = 0xFF4A3A;
    private static final int TV_PURPLE = 0xB45AFF;
    private static final int HOOK_GREY = 0x9A9AA6;

    private AllianceAbilities() {
    }

    /** The abilities of this kind of Alliance mob, or null for those without (the story characters, the Strider). */
    @Nullable
    public static MobAbilities<AllianceEntity> of(AllianceEntity mob) {
        EntityType<?> type = mob.getType();
        if (type == ModEntities.CAMERAMAN) {
            return new MobAbilities<>(paralyzer(100, 3.0f, 30, 16.0));
        } else if (type == ModEntities.LARGE_CAMERAMAN) {
            return new MobAbilities<>(paralyzer(80, 6.0f, 50, 20.0));
        } else if (type == ModEntities.SPEAKERMAN) {
            return new MobAbilities<>(soundwave(120, 6.0, 4.0f, 1.2, 1.4f));
        } else if (type == ModEntities.LARGE_SPEAKERMAN) {
            return new MobAbilities<>(soundwave(100, 9.0, 7.0f, 1.8, 2.4f));
        } else if (type == ModEntities.TV_MAN) {
            return new MobAbilities<>(hypnosis(200, 10.0, 100), teleport(160));
        } else if (type == ModEntities.TITAN_CAMERAMAN) {
            return new MobAbilities<>(jetpack(), coreBeam());
        } else if (type == ModEntities.TITAN_SPEAKERMAN) {
            return new MobAbilities<>(jetpack(), titanSoundwave(), plasmaCannon());
        } else if (type == ModEntities.TITAN_TV_MAN) {
            return new MobAbilities<>(deathScreen(), clawSweep(), grapplingHook());
        }
        return null;
    }

    // ---------------------------------------------------------------- regular and Large

    /** A paralyzer laser rifle shot: a little damage, and the toilet hit is stunned. */
    private static MobAbility<AllianceEntity> paralyzer(int cooldown, float damage, int stunTicks, double range) {
        return MobAbility.of(cooldown,
                (mob, target, distance) -> distance > 3.0 && distance < range && clearShot(mob, mob.getEyePos(), target, range),
                (mob, target) -> {
                    Entity hit = LaserBeam.fire(world(mob), mob, aim(mob.getEyePos(), target), range, damage, PARALYZER_COLOR, 0.12f);
                    if (hit instanceof LivingEntity living && Targets.isMonster(living)) {
                        living.addStatusEffect(new StatusEffectInstance(ModEffects.STUNNED, stunTicks), mob);
                    }
                    mob.playSound(ModSounds.STUN_RIFLE_SHOOT, 1.0f, pitch(mob));
                });
    }

    /** A blast of sound in a cone toward the target: hurts and throws back every toilet in it. */
    private static MobAbility<AllianceEntity> soundwave(int cooldown, double range, float damage, double knockback,
                                                        float size) {
        return MobAbility.of(cooldown, (mob, target, distance) -> distance < range * 0.8,
                (mob, target) -> {
                    Vec3d from = mob.getEyePos();
                    Vec3d look = aim(from, target);
                    blast(mob, from, look, range, 0.7, damage, knockback);
                    ModFx.speakerWave(world(mob), from.subtract(0.0, 0.2, 0.0), from.add(look), size);
                    mob.playSound(ModSounds.SPEAKERMAN_BLAST, 1.0f, pitch(mob));
                });
    }

    /** The hypnosis screen: the toilet stops fighting the Alliance and goes after other toilets for a while. */
    private static MobAbility<AllianceEntity> hypnosis(int cooldown, double range, int ticks) {
        return MobAbility.of(cooldown,
                (mob, target, distance) -> distance < range && target instanceof SkibidiToiletEntity toilet
                        && toilet.canBeHypnotized() && !toilet.hasStatusEffect(ModEffects.HYPNOTIZED),
                (mob, target) -> {
                    ServerWorld world = world(mob);
                    target.addStatusEffect(new StatusEffectInstance(ModEffects.HYPNOTIZED, ticks), mob);
                    ((SkibidiToiletEntity) target).setTarget(null);
                    // Leave it be now it's on our side, and find another.
                    mob.setTarget(null);
                    ModFx.speakerWave(world, mob.getEyePos(), target.getEyePos(), 1.0f, HYPNOSIS_COLOR);
                    ModFx.tvStatic(world, target.getEyePos(), 0.8f);
                    mob.playSound(ModSounds.POWER_HYPNOSIS, 1.0f, 1.0f);
                });
    }

    /** Vanishes in static and reappears next to a toilet that's far away. */
    private static MobAbility<AllianceEntity> teleport(int cooldown) {
        return MobAbility.of(cooldown, (mob, target, distance) -> distance > 10.0 && distance < 32.0,
                (mob, target) -> {
                    Vec3d from = mob.getPos();
                    Vec3d away = from.subtract(target.getPos()).multiply(1, 0, 1).normalize();
                    Vec3d spot = target.getPos().add(away.multiply(target.getWidth() * 0.5 + mob.getWidth() * 0.5 + 1.0));
                    if (mob.teleport(spot.x, spot.y + 0.5, spot.z, false)) {
                        ServerWorld world = world(mob);
                        ModFx.teleport(world, from, 1.0f, false);
                        ModFx.teleport(world, mob.getPos(), 1.0f, true);
                        world.playSound(null, from.x, from.y, from.z, ModSounds.POWER_TELEPORT, mob.getSoundCategory(), 1.0f, 1.0f);
                        mob.playSound(ModSounds.POWER_TELEPORT, 1.0f, 1.2f);
                    }
                });
    }

    // ---------------------------------------------------------------- Titans

    /** Titan Cameraman: his blue core charges, then fires a thick beam that bursts where it lands. */
    private static MobAbility<AllianceEntity> coreBeam() {
        double range = 40.0;
        return MobAbility.<AllianceEntity>of(140,
                (mob, target, distance) -> distance > 6.0 && distance < range && clearShot(mob, core(mob), target, range),
                (mob, target) -> {
                    ServerWorld world = world(mob);
                    Vec3d core = core(mob);
                    LaserBeam.Shot shot = LaserBeam.fireFrom(world, mob, core, aim(core, target), range, 20.0f, CORE_BLUE, 0.9f);
                    burst(mob, shot.end(), 3.0, 8.0f, CORE_BLUE);
                    mob.playSound(ModSounds.GIANT_FLYING_TOILET_LASER, 3.0f, 0.8f);
                })
                .windup(20, (mob, target, left) -> {
                    if (left == 20) {
                        mob.playSound(ModSounds.TITAN_CHARGE, 2.0f, 1.0f);
                    }
                    if (left % 4 == 0) {
                        Vec3d core = core(mob);
                        ModFx.sparks(world(mob), core, core.add(aim(core, target)), CORE_BLUE);
                    }
                });
    }

    /** Titan Speakerman: every speaker on his head and shoulders blasts at once, a huge cone that shakes the ground. */
    private static MobAbility<AllianceEntity> titanSoundwave() {
        double range = 16.0;
        return MobAbility.<AllianceEntity>of(140, (mob, target, distance) -> distance < range * 0.8,
                (mob, target) -> {
                    ServerWorld world = world(mob);
                    Vec3d head = mob.getEyePos();
                    Vec3d look = aim(head, target);
                    blast(mob, head, look, range, 0.6, 12.0f, 2.5);
                    ModFx.speakerWave(world, head, head.add(look), 4.0f);
                    Vec3d side = side(mob).multiply(mob.getWidth() * 0.9);
                    Vec3d shoulders = mob.getPos().add(0.0, mob.getHeight() * 0.85, 0.0);
                    ModFx.speakerWave(world, shoulders.add(side), shoulders.add(side).add(look), 3.0f);
                    ModFx.speakerWave(world, shoulders.subtract(side), shoulders.subtract(side).add(look), 3.0f);
                    ModFx.shake(world, mob.getPos(), 0.5f, (float) range + 8.0f);
                    mob.playSound(ModSounds.SPEAKERMAN_BLAST, 3.0f, 0.55f);
                })
                .windup(10, (mob, target, left) -> {
                    if (left == 10) {
                        ModFx.ring(world(mob), mob.getEyePos(), mob.getWidth(), PLASMA_RED);
                    }
                });
    }

    /** Titan Speakerman: a bolt from the plasma cannon on his left arm that bursts where it lands. */
    private static MobAbility<AllianceEntity> plasmaCannon() {
        double range = 36.0;
        return MobAbility.of(80,
                (mob, target, distance) -> distance > 5.0 && distance < range && clearShot(mob, cannon(mob), target, range),
                (mob, target) -> {
                    Vec3d cannon = cannon(mob);
                    LaserBeam.Shot shot = LaserBeam.fireFrom(world(mob), mob, cannon, aim(cannon, target), range, 14.0f,
                            PLASMA_RED, 0.5f);
                    burst(mob, shot.end(), 2.5, 6.0f, PLASMA_RED);
                    mob.playSound(ModSounds.TITAN_PLASMA, 3.0f, 1.0f);
                });
    }

    /** Titan Cameraman and Speakerman: the jetpack fires and carries them at a toilet far away or up high. */
    private static MobAbility<AllianceEntity> jetpack() {
        return MobAbility.of(240,
                (mob, target, distance) -> mob.isOnGround()
                        && (distance > 18.0 && distance < 48.0 || target.getY() - mob.getY() > 4.0 && distance < 30.0),
                (mob, target) -> {
                    Vec3d flat = target.getPos().subtract(mob.getPos()).multiply(1, 0, 1);
                    // Air drag eats about a tenth of the speed each tick, so this lands near the target.
                    Vec3d speed = flat.normalize().multiply(Math.min(2.0, flat.length() / 11.0));
                    double up = 1.0 + MathHelper.clamp(target.getY() - mob.getY(), 0.0, 12.0) * 0.05;
                    mob.setVelocity(speed.x, up, speed.z);
                    mob.velocityModified = true;
                    if (mob instanceof TitanEntity titan) {
                        titan.fireJetpack();
                    }
                    ModFx.shockwave(world(mob), mob.getPos(), mob.getWidth() * 1.5f, 0.3f);
                    mob.playSound(ModSounds.TITAN_JETPACK, 3.0f, 0.8f);
                });
    }

    /** Titan TV Man: his screen flashes the death screen, stunning and hurting every toilet in front of him. */
    private static MobAbility<AllianceEntity> deathScreen() {
        double range = 22.0;
        return MobAbility.<AllianceEntity>of(220, (mob, target, distance) -> distance < range,
                (mob, target) -> {
                    ServerWorld world = world(mob);
                    Vec3d head = mob.getEyePos();
                    Vec3d look = aim(head, target);
                    for (LivingEntity hit : Targets.inCone(mob, head, look, range, 0.65, Targets::isMonster)) {
                        hit.addStatusEffect(new StatusEffectInstance(ModEffects.STUNNED, 80), mob);
                        hit.damage(ModDamageTypes.power(world, mob), 8.0f);
                        ModFx.tvStatic(world, hit.getEyePos(), 0.8f);
                    }
                    ModFx.cone(world, head, head.add(look), (float) range, TV_PURPLE);
                    ModFx.cameraFlash(world, head.add(look.multiply(2.0)), 4.0f);
                    mob.playSound(ModSounds.POWER_STUN_FLASH, 3.0f, 0.7f);
                })
                .windup(24, (mob, target, left) -> {
                    if (left == 24) {
                        mob.playSound(ModSounds.TV_MAN_STATIC, 3.0f, 0.6f);
                    }
                    if (left % 4 == 0) {
                        ModFx.tvStatic(world(mob), mob.getEyePos(), 2.5f);
                    }
                });
    }

    /** Titan TV Man: one sweep of the triple blades on his shoulders through every toilet close in front. */
    private static MobAbility<AllianceEntity> clawSweep() {
        return MobAbility.of(100, (mob, target, distance) -> distance < reach(mob),
                (mob, target) -> {
                    ServerWorld world = world(mob);
                    Vec3d from = mob.getPos().add(0.0, mob.getHeight() * 0.4, 0.0);
                    Vec3d look = aim(from, target).multiply(1, 0, 1).normalize();
                    for (LivingEntity hit : Targets.inCone(mob, from, look, reach(mob), 0.0, Targets::isMonster)) {
                        hit.damage(mob.getDamageSources().mobAttack(mob), 16.0f);
                        hit.takeKnockback(1.5, -look.x, -look.z);
                        ModFx.sparks(world, hit.getBoundingBox().getCenter(), from, TV_PURPLE);
                    }
                    mob.swingHand(Hand.MAIN_HAND);
                    ModFx.ring(world, mob.getPos().add(0.0, 1.0, 0.0), (float) reach(mob), TV_PURPLE);
                    mob.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 3.0f, 0.5f);
                    mob.playSound(ModSounds.ENERGY_KNIFE_SWING, 2.0f, 0.6f);
                });
    }

    /** Titan TV Man: the grappling hook on his elbow shoots out and reels a far toilet in to him. */
    private static MobAbility<AllianceEntity> grapplingHook() {
        double range = 30.0;
        return MobAbility.of(160,
                (mob, target, distance) -> distance > 10.0 && distance < range && clearShot(mob, hand(mob), target, range),
                (mob, target) -> {
                    ModFx.beam(world(mob), hand(mob), target.getBoundingBox().getCenter(), HOOK_GREY, 0.15f);
                    Vec3d pull = mob.getPos().subtract(target.getPos());
                    Vec3d speed = pull.multiply(1, 0, 1).normalize().multiply(Math.min(3.0, 0.9 + pull.length() * 0.08));
                    target.setVelocity(speed.x, 0.5 + MathHelper.clamp(pull.y * 0.05, 0.0, 0.6), speed.z);
                    target.velocityModified = true;
                    target.damage(mob.getDamageSources().mobAttack(mob), 4.0f);
                    mob.playSound(ModSounds.GRAVITY_PLUNGER_PULL, 3.0f, 0.6f);
                });
    }

    // ---------------------------------------------------------------- helpers

    /** Hurts and pushes away every toilet in a cone. */
    private static void blast(AllianceEntity mob, Vec3d from, Vec3d look, double range, double minDot, float damage,
                              double knockback) {
        ServerWorld world = world(mob);
        for (LivingEntity hit : Targets.inCone(mob, from, look, range, minDot, Targets::isMonster)) {
            hit.damage(ModDamageTypes.power(world, mob), damage);
            Vec3d push = hit.getPos().subtract(mob.getPos()).multiply(1, 0, 1).normalize().multiply(knockback);
            hit.addVelocity(push.x, 0.3 + knockback * 0.1, push.z);
            hit.velocityModified = true;
        }
    }

    /** Hurts every toilet within {@code radius} of where a big shot landed. */
    private static void burst(AllianceEntity mob, Vec3d at, double radius, float damage, int color) {
        ServerWorld world = world(mob);
        for (LivingEntity hit : world.getEntitiesByClass(LivingEntity.class, new Box(at, at).expand(radius),
                entity -> Targets.isMonster(entity) && entity.getBoundingBox().getCenter().squaredDistanceTo(at) < radius * radius)) {
            hit.damage(ModDamageTypes.power(world, mob), damage);
        }
        ModFx.shockwave(world, at, (float) radius, 0.25f);
        ModFx.ring(world, at, (float) radius, color);
    }

    /** Whether a shot from {@code from} would hit a toilet first (and not a player or Alliance member in the way). */
    private static boolean clearShot(AllianceEntity mob, Vec3d from, LivingEntity target, double range) {
        Entity first = LaserBeam.aimFrom(world(mob), mob, from, aim(from, target), range);
        return first instanceof LivingEntity living && Targets.isMonster(living);
    }

    /** The way from {@code from} to the middle of the target. */
    private static Vec3d aim(Vec3d from, LivingEntity target) {
        return target.getBoundingBox().getCenter().subtract(from).normalize();
    }

    /** The mob's left, flat: its body faces along {@code bodyYaw}. */
    private static Vec3d side(AllianceEntity mob) {
        float yaw = mob.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(MathHelper.cos(yaw), 0.0, MathHelper.sin(yaw));
    }

    private static Vec3d forward(AllianceEntity mob) {
        float yaw = mob.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(-MathHelper.sin(yaw), 0.0, MathHelper.cos(yaw));
    }

    /** The Titan's core, in the middle of his chest. */
    private static Vec3d core(AllianceEntity mob) {
        return mob.getPos().add(0.0, mob.getHeight() * 0.64, 0.0).add(forward(mob).multiply(mob.getWidth() * 0.5));
    }

    /** The muzzle of the plasma cannon on the Titan Speakerman's left forearm. */
    private static Vec3d cannon(AllianceEntity mob) {
        return mob.getPos().add(0.0, mob.getHeight() * 0.38, 0.0).add(side(mob).multiply(mob.getWidth() * 0.85))
                .add(forward(mob).multiply(mob.getWidth() * 0.3));
    }

    /** The Titan TV Man's right hand, where the hook shoots from. */
    private static Vec3d hand(AllianceEntity mob) {
        return mob.getPos().add(0.0, mob.getHeight() * 0.42, 0.0).subtract(side(mob).multiply(mob.getWidth() * 0.8));
    }

    /** How far the Titan TV Man's claws reach in front of him. */
    private static double reach(AllianceEntity mob) {
        return mob.getWidth() * 0.5 + 6.0;
    }

    private static float pitch(AllianceEntity mob) {
        return 1.0f / (0.6f + mob.getHeight() / 1.95f * 0.4f);
    }

    private static ServerWorld world(AllianceEntity mob) {
        return (ServerWorld) mob.getWorld();
    }
}
