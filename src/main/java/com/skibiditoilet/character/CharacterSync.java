package com.skibiditoilet.character;

import com.mojang.serialization.DataResult;
import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.entity.SeatEntity;
import com.skibiditoilet.registry.ModAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps every client up to date with the characters of the players it can see, and handles the
 * creator's "create" and the toilet re-customizer's "update" requests.
 */
public final class CharacterSync {
    /** S2C: entity id + character. Sent for yourself and for every player you can see. */
    public static final Identifier SYNC = SkibidiToiletMod.id("character_sync");
    /** S2C: no payload. Opens the re-customize screen (sent when a created player sits on a toilet). */
    public static final Identifier OPEN_CUSTOMIZER = SkibidiToiletMod.id("open_customizer");
    /** C2S: character. The finished creator. */
    public static final Identifier CREATE = SkibidiToiletMod.id("character_create");
    /** C2S: character. A new look from the re-customizer; only the look is used. */
    public static final Identifier UPDATE = SkibidiToiletMod.id("character_update");

    private CharacterSync() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendTo(handler.player, handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> broadcast(newPlayer));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> broadcast(player));
        EntityTrackingEvents.START_TRACKING.register((tracked, player) -> {
            if (tracked instanceof ServerPlayerEntity trackedPlayer) {
                sendTo(player, trackedPlayer);
            }
        });

        // Packet handlers run on the network thread; read the buffer there, act on the server thread.
        ServerPlayNetworking.registerGlobalReceiver(CREATE, (server, player, handler, buf, sender) -> {
            CharacterData requested = read(buf);
            server.execute(() -> create(player, requested));
        });
        ServerPlayNetworking.registerGlobalReceiver(UPDATE, (server, player, handler, buf, sender) -> {
            CharacterData requested = read(buf);
            server.execute(() -> update(player, requested));
        });
    }

    public static void set(ServerPlayerEntity player, CharacterData data) {
        player.setAttached(ModAttachments.CHARACTER, data);
        broadcast(player);
    }

    /** Tells a created player's client to open the re-customizer. */
    public static void openCustomizer(ServerPlayerEntity player) {
        if (CharacterData.of(player).created()) {
            ServerPlayNetworking.send(player, OPEN_CUSTOMIZER, PacketByteBufs.empty());
        }
    }

    private static void create(ServerPlayerEntity player, @Nullable CharacterData requested) {
        if (requested == null || !requested.isValid() || CharacterData.of(player).created()) {
            // The client already assumed success; tell it what is really saved.
            sendTo(player, player);
            return;
        }
        set(player, requested.withCreated(true));
    }

    private static void update(ServerPlayerEntity player, @Nullable CharacterData requested) {
        CharacterData current = CharacterData.of(player);
        // Looks can only be changed while sitting on a toilet, and type and class never change here.
        if (requested == null || !current.created() || !(player.getVehicle() instanceof SeatEntity)) {
            return;
        }
        CharacterData updated = current.withLookFrom(requested);
        if (updated.isValid()) {
            set(player, updated);
        }
    }

    private static void broadcast(ServerPlayerEntity subject) {
        sendTo(subject, subject);
        for (ServerPlayerEntity viewer : PlayerLookup.tracking(subject)) {
            sendTo(viewer, subject);
        }
    }

    private static void sendTo(ServerPlayerEntity receiver, ServerPlayerEntity subject) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(subject.getId());
        write(buf, CharacterData.of(subject));
        ServerPlayNetworking.send(receiver, SYNC, buf);
    }

    public static void write(PacketByteBuf buf, CharacterData data) {
        NbtElement nbt = CharacterData.CODEC.encodeStart(NbtOps.INSTANCE, data).result().orElseThrow();
        buf.writeNbt((NbtCompound) nbt);
    }

    @Nullable
    public static CharacterData read(PacketByteBuf buf) {
        NbtCompound nbt = buf.readNbt();
        if (nbt == null) {
            return null;
        }
        DataResult<CharacterData> result = CharacterData.CODEC.parse(NbtOps.INSTANCE, nbt);
        return result.result().orElse(null);
    }
}
