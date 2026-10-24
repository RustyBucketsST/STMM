package com.skibiditoilet.entity;

import com.skibiditoilet.entity.ability.MobAbilities;
import com.skibiditoilet.entity.ability.MobAbility;
import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The giant G-Man boss toilet, with a boss bar. Fights like a Skibidi Toilet, and every few seconds his eyes glow
 * yellow and fire twin lasers, as in episodes 12, 17 and 20.
 */
public class GManToiletEntity extends SkibidiToiletEntity {
    /** As tall as a Titan (about 10 blocks), whom he fights as an equal in the series. */
    public static final float SCALE = 7.5f;
    private static final int LASER_COLOR = 0xFFE14A;
    private static final double LASER_RANGE = 40.0;
    /** Blocks per unit of gman_toilet.bbmodel, which is built at twice the detail. */
    private static final double UNIT = SCALE / 2.0 / 16.0;

    private final MobAbilities<GManToiletEntity> abilities = new MobAbilities<>(
            MobAbility.<GManToiletEntity>of(90, (gman, target, distance) -> distance > 4.0 && distance < LASER_RANGE,
                            GManToiletEntity::fireEyeLasers)
                    .windup(18, (gman, target, left) -> {
                        if (left == 18) {
                            gman.playSound(ModSounds.TITAN_CHARGE, 3.0f, 1.3f);
                        }
                        if (left % 3 == 0) {
                            ServerWorld world = (ServerWorld) gman.getWorld();
                            for (boolean leftEye : new boolean[]{true, false}) {
                                Vec3d eye = gman.eye(leftEye);
                                ModFx.sparks(world, eye, eye.add(gman.getRotationVec(1.0f)), LASER_COLOR);
                            }
                        }
                    }));

    private final ServerBossBar bossBar =
            new ServerBossBar(this.getDisplayName(), BossBar.Color.RED, BossBar.Style.PROGRESS);

    public GManToiletEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.setStepHeight(2.5f);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        this.abilities.tick(this);
    }

    private void fireEyeLasers(LivingEntity target) {
        ServerWorld world = (ServerWorld) this.getWorld();
        Vec3d at = target.getBoundingBox().getCenter();
        for (boolean leftEye : new boolean[]{true, false}) {
            Vec3d eye = this.eye(leftEye);
            LaserBeam.fireFrom(world, this, eye, at.subtract(eye), LASER_RANGE, 7.0f, LASER_COLOR, 0.35f);
        }
        this.playSound(ModSounds.GIANT_FLYING_TOILET_LASER, 3.0f, 1.2f);
    }

    /** Where one of his eyes is: under the brow of the head in gman_toilet.bbmodel, turned with the head. */
    private Vec3d eye(boolean left) {
        float yaw = this.headYaw * MathHelper.RADIANS_PER_DEGREE;
        Vec3d forward = new Vec3d(-MathHelper.sin(yaw), 0.0, MathHelper.cos(yaw));
        Vec3d side = new Vec3d(MathHelper.cos(yaw), 0.0, MathHelper.sin(yaw));
        return this.getPos().add(0.0, 35.0 * UNIT, 0.0).add(forward.multiply(9.5 * UNIT))
                .add(side.multiply((left ? 3.0 : -3.0) * UNIT));
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Text name) {
        super.setCustomName(name);
        this.bossBar.setName(this.getDisplayName());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (this.hasCustomName()) {
            this.bossBar.setName(this.getDisplayName());
        }
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }
}
