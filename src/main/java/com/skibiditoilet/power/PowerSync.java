package com.skibiditoilet.power;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.registry.ModAttachments;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/** Power packets: points, unlocks, energy and cooldowns go to the client; power use, unlocks and builds come back. */
public final class PowerSync {
    /** S2C: the player's PowerData. */
    public static final Identifier DATA = SkibidiToiletMod.id("power_data");
    /** S2C: energy, max energy, then the ticks left on each of the player's power cooldowns, in key order. */
    public static final Identifier STATE = SkibidiToiletMod.id("power_state");
    /** S2C: entity id, Visible ordinal, ticks. A power others can see on a player: a shield up, a dance. */
    public static final Identifier VISIBLE = SkibidiToiletMod.id("power_visible");
    /** C2S: slot. Use the power on that key. */
    public static final Identifier USE = SkibidiToiletMod.id("power_use");
    /** C2S: power id. */
    public static final Identifier UNLOCK = SkibidiToiletMod.id("power_unlock");
    /** C2S: tech id. */
    public static final Identifier BUILD = SkibidiToiletMod.id("tech_build");

    /** Powers that show on the player who used them, for everyone around. */
    public enum Visible {
        SHIELD, DANCE
    }

    private PowerSync() {
    }

    public static void init() {
        // Packet handlers run on the network thread; read the buffer there, act on the server thread.
        ServerPlayNetworking.registerGlobalReceiver(USE, (server, player, handler, buf, sender) -> {
            int slot = buf.readVarInt();
            server.execute(() -> PowerManager.use(player, slot));
        });
        ServerPlayNetworking.registerGlobalReceiver(UNLOCK, (server, player, handler, buf, sender) -> {
            String id = buf.readString();
            server.execute(() -> PowerManager.unlock(player, Power.byId(id)));
        });
        ServerPlayNetworking.registerGlobalReceiver(BUILD, (server, player, handler, buf, sender) -> {
            String id = buf.readString();
            server.execute(() -> PowerManager.build(player, Tech.byId(id)));
        });
    }

    public static void set(ServerPlayerEntity player, PowerData data) {
        player.setAttached(ModAttachments.POWERS, data);
        sendData(player);
    }

    public static void sendData(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeNbt((NbtCompound) PowerData.CODEC.encodeStart(NbtOps.INSTANCE, PowerData.of(player)).result().orElseThrow());
        ServerPlayNetworking.send(player, DATA, buf);
    }

    @Nullable
    public static PowerData readData(PacketByteBuf buf) {
        NbtCompound nbt = buf.readNbt();
        return nbt != null ? PowerData.CODEC.parse(NbtOps.INSTANCE, nbt).result().orElse(null) : null;
    }

    public static void sendState(ServerPlayerEntity player, float energy, float maxEnergy, int[] cooldowns) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(energy);
        buf.writeFloat(maxEnergy);
        buf.writeIntArray(cooldowns);
        ServerPlayNetworking.send(player, STATE, buf);
    }

    /** Tells the player and everyone who can see them that a visible power is on for {@code ticks}. */
    public static void sendVisible(ServerPlayerEntity player, Visible visible, int ticks) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(player.getId());
        buf.writeEnumConstant(visible);
        buf.writeVarInt(ticks);
        ServerPlayNetworking.send(player, VISIBLE, PacketByteBufs.copy(buf));
        for (ServerPlayerEntity viewer : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(viewer, VISIBLE, PacketByteBufs.copy(buf));
        }
    }
}
