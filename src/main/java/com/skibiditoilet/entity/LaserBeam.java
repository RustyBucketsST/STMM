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
 * An instant shot from an entity's eyes: the lasers of the Alliance Blaster and the flying toilets, and the
 * Skibidi Urinal's water spray.
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
        Shot shot = trace(world, shooter, direction, range);
        shot.hit(ModDamageTypes.laser(world, shooter), damage);
        ModFx.beam(world, shot.start, shot.end, color, width);
        return shot.target;
    }

    /** The first living thing along {@code direction} within {@code range}, with nothing in the way, or null. */
    @Nullable
    public static Entity aim(ServerWorld world, LivingEntity shooter, Vec3d direction, double range) {
        return trace(world, shooter, direction, range).target;
    }

    /** Sprays water along {@code direction} (the Urinal's spit). Returns what was hit, or null. */
    @Nullable
    public static Entity spray(ServerWorld world, LivingEntity shooter, Vec3d direction, double range, float damage,
                               DamageSource source) {
        Shot shot = trace(world, shooter, direction, range);
        shot.hit(source, damage);
        ModFx.spray(world, shot.start, shot.end);
        return shot.target;
    }

    private record Shot(Vec3d start, Vec3d end, @Nullable Entity target) {
        void hit(DamageSource source, float damage) {
            if (this.target != null) {
                this.target.damage(source, damage);
            }
        }
    }

    /** Where a shot from the shooter's eyes ends: at the first wall, or at the first mob before that wall. */
    private static Shot trace(ServerWorld world, LivingEntity shooter, Vec3d direction, double range) {
        Vec3d start = shooter.getEyePos();
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
