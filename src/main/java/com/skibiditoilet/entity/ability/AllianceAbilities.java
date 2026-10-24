package com.skibiditoilet.entity.ability;

import com.skibiditoilet.entity.AllianceEntity;
import com.skibiditoilet.entity.LaserBeam;
import com.skibiditoilet.entity.SkibidiToiletEntity;
import com.skibiditoilet.entity.TitanEntity;
import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModEffects;
import com.skibiditoilet.registry.ModEntities;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.world.ServerWorld;
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
                    TitanMoves.blast(mob, from, look, range, 0.7, damage, knockback);
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

    // ---------------------------------------------------------------- Titans (see TitanMoves)

    /** Titan Cameraman: his blue core charges, then fires a thick beam that bursts where it lands. */
    private static MobAbility<AllianceEntity> coreBeam() {
        double range = TitanMoves.CORE_BEAM_RANGE;
        return MobAbility.<AllianceEntity>of(140,
                (mob, target, distance) -> distance > 6.0 && distance < range && clearShot(mob, TitanMoves.core(mob), target, range),
                (mob, target) -> TitanMoves.coreBeam(mob, aim(TitanMoves.core(mob), target), 1.0f))
                .windup(20, (mob, target, left) -> {
                    if (left == 20) {
                        mob.playSound(ModSounds.TITAN_CHARGE, 2.0f, 1.0f);
                    }
                    if (left % 4 == 0) {
                        Vec3d core = TitanMoves.core(mob);
                        ModFx.sparks(world(mob), core, core.add(aim(core, target)), TitanMoves.CORE_BLUE);
                    }
                });
    }

    /** Titan Speakerman: every speaker on his head and shoulders blasts at once. */
    private static MobAbility<AllianceEntity> titanSoundwave() {
        return MobAbility.<AllianceEntity>of(140, (mob, target, distance) -> distance < TitanMoves.SOUNDWAVE_RANGE * 0.8,
                        (mob, target) -> TitanMoves.soundwaves(mob, aim(mob.getEyePos(), target), 1.0f))
                .windup(10, (mob, target, left) -> {
                    if (left == 10) {
                        ModFx.ring(world(mob), mob.getEyePos(), mob.getWidth(), TitanMoves.PLASMA_RED);
                    }
                });
    }

    /** Titan Speakerman: a bolt from the plasma cannon on his left arm. */
    private static MobAbility<AllianceEntity> plasmaCannon() {
        double range = TitanMoves.PLASMA_RANGE;
        return MobAbility.of(80,
                (mob, target, distance) -> distance > 5.0 && distance < range && clearShot(mob, TitanMoves.cannon(mob), target, range),
                (mob, target) -> TitanMoves.plasmaCannon(mob, aim(TitanMoves.cannon(mob), target), 1.0f));
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
                    TitanMoves.jetpack(mob, new Vec3d(speed.x, up, speed.z));
                    if (mob instanceof TitanEntity titan) {
                        titan.fireJetpack();
                    }
                });
    }

    /** Titan TV Man: his screen crackles, then flashes the death screen at every toilet in front of him. */
    private static MobAbility<AllianceEntity> deathScreen() {
        return MobAbility.<AllianceEntity>of(220, (mob, target, distance) -> distance < TitanMoves.DEATH_SCREEN_RANGE,
                        (mob, target) -> TitanMoves.deathScreen(mob, aim(mob.getEyePos(), target), 1.0f))
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
        return MobAbility.of(100, (mob, target, distance) -> distance < TitanMoves.clawReach(mob),
                (mob, target) -> TitanMoves.clawSweep(mob, aim(mob.getPos(), target), 1.0f));
    }

    /** Titan TV Man: the grappling hook on his elbow reels a far toilet in to him. */
    private static MobAbility<AllianceEntity> grapplingHook() {
        double range = TitanMoves.HOOK_RANGE;
        return MobAbility.of(160,
                (mob, target, distance) -> distance > 10.0 && distance < range && clearShot(mob, TitanMoves.hand(mob), target, range),
                (mob, target) -> TitanMoves.grapplingHook(mob, target, 1.0f));
    }

    // ---------------------------------------------------------------- helpers

    /** Whether a shot from {@code from} would hit a toilet first (and not a player or Alliance member in the way). */
    private static boolean clearShot(AllianceEntity mob, Vec3d from, LivingEntity target, double range) {
        return TitanMoves.firstMonster(mob, from, aim(from, target), range) != null;
    }

    /** The way from {@code from} to the middle of the target. */
    private static Vec3d aim(Vec3d from, LivingEntity target) {
        return target.getBoundingBox().getCenter().subtract(from).normalize();
    }

    private static float pitch(AllianceEntity mob) {
        return 1.0f / (0.6f + mob.getHeight() / 1.95f * 0.4f);
    }

    private static ServerWorld world(AllianceEntity mob) {
        return (ServerWorld) mob.getWorld();
    }
}
