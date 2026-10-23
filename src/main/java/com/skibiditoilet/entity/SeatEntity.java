package com.skibiditoilet.entity;

import com.skibiditoilet.block.ToiletBlock;
import com.skibiditoilet.registry.ModBlocks;
import com.skibiditoilet.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Dismounting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Invisible entity a player rides while sitting on a toilet. Removes itself when they get up. */
public class SeatEntity extends Entity {
    // A seated player's feet end up at SEAT_HEIGHT - 0.35. Anything lower embeds them in the floor,
    // which they'd fall through if saved there (autosave, crash).
    private static final double SEAT_HEIGHT = 0.35;

    public SeatEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    public static boolean sitDown(World world, BlockPos pos, PlayerEntity player) {
        if (!world.getEntitiesByClass(SeatEntity.class, new Box(pos), seat -> true).isEmpty()) {
            return false;
        }
        SeatEntity seat = new SeatEntity(ModEntities.SEAT, world);
        seat.setPosition(pos.getX() + 0.5, pos.getY() + SEAT_HEIGHT, pos.getZ() + 0.5);
        world.spawnEntity(seat);
        return player.startRiding(seat);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient
                && (!this.hasPassengers() || !this.getWorld().getBlockState(this.getBlockPos()).isOf(ModBlocks.TOILET))) {
            this.discard();
        }
    }

    @Override
    public double getMountedHeightOffset() {
        return 0.0;
    }

    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        // Stand up in front of the toilet, or on top of it if that's blocked.
        BlockPos pos = this.getBlockPos();
        BlockState state = this.getWorld().getBlockState(pos);
        if (state.isOf(ModBlocks.TOILET)) {
            Vec3d front = Dismounting.findRespawnPos(passenger.getType(), this.getWorld(),
                    pos.offset(state.get(ToiletBlock.FACING)), true);
            if (front != null) {
                return front;
            }
        }
        return new Vec3d(this.getX(), pos.getY() + 1.0, this.getZ());
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}
