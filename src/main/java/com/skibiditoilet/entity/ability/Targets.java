package com.skibiditoilet.entity.ability;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.Predicate;

/** Who an area attack hits: the things its user can see near a point, all around or in a cone. */
public final class Targets {
    private Targets() {
    }

    /** Toilets and other monsters: what powers and the Alliance hit, never players or the Alliance. */
    public static boolean isMonster(LivingEntity entity) {
        return entity.isAlive() && entity instanceof Monster && entity.isPartOfGame();
    }

    /**
     * What {@code user} can see within {@code range} of {@code from} that matches {@code which}, inside a cone around
     * {@code look} (a unit vector): {@code minDot} is the cosine of the cone's half-angle.
     */
    public static List<LivingEntity> inCone(LivingEntity user, Vec3d from, Vec3d look, double range, double minDot,
                                            Predicate<LivingEntity> which) {
        return around(user, from, range, which.and(entity -> {
            Vec3d to = entity.getBoundingBox().getCenter().subtract(from);
            double distance = to.length();
            // Something right in your face counts as in front, whatever the angle.
            return distance < 1.5 || to.multiply(1.0 / distance).dotProduct(look) >= minDot;
        }));
    }

    public static List<LivingEntity> around(LivingEntity user, Vec3d from, double range, Predicate<LivingEntity> which) {
        return user.getWorld().getEntitiesByClass(LivingEntity.class, user.getBoundingBox().expand(range),
                entity -> entity != user && entity.getBoundingBox().getCenter().squaredDistanceTo(from) <= range * range
                        && which.test(entity) && user.canSee(entity));
    }
}
