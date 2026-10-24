package com.skibiditoilet.entity;

import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModDamageTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

/**
 * An instant shot from an entity's eyes (or another point on it): the lasers of the Alliance Blaster, the flying
 * toilets and G-Toilet's eyes, the Titans' core beam and plasma cannon, and the Skibidi Urinal's water spray.
 */
public final class LaserBeam {
    private LaserBeam() {
    }

    /**
     * Fires a laser along {@code direction}, damaging the first living thing hit, drawn as a glowing beam.
     * Returns what was hit, or null.
     */
    @Nullable
    public static Entity fire(ServerWorld world, LivingEntity shooter, Vec3d direction, double range, float damage,
                              int color, float width) {
        return fireFrom(world, shooter, shooter.getEyePos(), direction, range, damage, color, width).target;
    }

    /** Like fire, from {@code start} instead of the eyes. Returns the shot: where it ended and what it hit. */
    public static Shot fireFrom(ServerWorld world, LivingEntity shooter, Vec3d start, Vec3d direction, double range,
                                float damage, int color, float width) {
        Shot shot = trace(world, shooter, start, direction, range);
        shot.hit(ModDamageTypes.laser(world, shooter), damage);
        ModFx.beam(world, shot.start, shot.end, color, width);
        return shot;
    }

    /** The first living thing along {@code direction} within {@code range}, with nothing in the way, or null. */
    @Nullable
    public static Entity aim(ServerWorld world, LivingEntity shooter, Vec3d direction, double range) {
        return aimFrom(world, shooter, shooter.getEyePos(), direction, range);
    }

    /** Like aim, from {@code start} instead of the eyes. */
    @Nullable
    public static Entity aimFrom(ServerWorld world, LivingEntity shooter, Vec3d start, Vec3d direction, double range) {
        return trace(world, shooter, start, direction, range).target;
    }

    /** Sprays water along {@code direction} (the Urinal's spit). Returns what was hit, or null. */
    @Nullable
    public static Entity spray(ServerWorld world, LivingEntity shooter, Vec3d direction, double range, float damage,
                               DamageSource source) {
        Shot shot = trace(world, shooter, shooter.getEyePos(), direction, range);
        shot.hit(source, damage);
        ModFx.spray(world, shot.start, shot.end);
        return shot.target;
    }

    /** A shot from {@code start} that stopped at {@code end}: at a wall, at {@code target}, or out of range. */
    public record Shot(Vec3d start, Vec3d end, @Nullable Entity target) {
        void hit(DamageSource source, float damage) {
            if (this.target != null) {
                this.target.damage(source, damage);
            }
        }
    }

    /** Where a shot from {@code start} ends: at the first wall, or at the first mob before that wall. */
    private static Shot trace(ServerWorld world, LivingEntity shooter, Vec3d start, Vec3d direction, double range) {
        Vec3d end = start.add(direction.normalize().multiply(range));
        BlockHitResult blockHit = world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, shooter));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getPos();
        }
        EntityHitResult entityHit = ProjectileUtil.raycast(shooter, start, end,
                shooter.getBoundingBox().stretch(end.subtract(start)).expand(1.0),
                // The Ender Dragon is hit through its parts, which aren't living entities themselves.
                entity -> (entity instanceof LivingEntity || entity instanceof EnderDragonPart)
                        && entity.isAlive() && !entity.isSpectator() && entity.canHit(),
                start.squaredDistanceTo(end));
        return entityHit != null ? new Shot(start, entityHit.getPos(), entityHit.getEntity()) : new Shot(start, end, null);
    }
}
