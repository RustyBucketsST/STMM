package com.skibiditoilet.fx;

import com.skibiditoilet.SkibidiToiletMod;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Visual effects, triggered on the server and drawn on the clients nearby. One small packet per effect;
 * the client builds the beams, particles and screen shake from it (see ClientFx).
 */
public final class ModFx {
    /** S2C: Kind ordinal, two points, a color, and two numbers whose meaning depends on the kind. */
    public static final Identifier FX = SkibidiToiletMod.id("fx");
    private static final double RANGE = 96.0;

    public enum Kind {
        /** A glowing beam from a to b. f1 = width. */
        BEAM,
        /** A spray of water from a to b (the Urinal's spit). */
        SPRAY,
        /** A ground shockwave at a. f1 = radius, f2 = screen shake strength. */
        SHOCKWAVE,
        /** Screen shake only, around a. f1 = strength, f2 = radius. */
        SHAKE,
        /** A camera flash at a. f1 = size. */
        CAMERA_FLASH,
        /** Sound-wave rings from a toward b. f1 = size. */
        SPEAKER_WAVE,
        /** TV static around a. f1 = size. */
        TV_STATIC,
        /** A burst of water at a. f1 = size. */
        SPLASH,
        /** A colored ring racing out flat from a, with nothing thrown up. f1 = radius. */
        RING,
        /**
         * A puff of dark smoke and static at a, where a TV Man vanished. f1 = size; f2 = 1 where they appeared,
         * a lighter puff around the feet that doesn't block their view.
         */
        TELEPORT,
        /** A cone of light and fire from a toward b. f1 = length. */
        CONE,
        /** Sparks off a shield at a, flying toward b. */
        SPARKS,
        /** Music notes rising around a. f1 = radius. */
        NOTES
    }

    private ModFx() {
    }

    public static void beam(ServerWorld world, Vec3d from, Vec3d to, int color, float width) {
        send(world, Kind.BEAM, from, to, color, width, 0.0f);
    }

    public static void spray(ServerWorld world, Vec3d from, Vec3d to) {
        send(world, Kind.SPRAY, from, to, 0xBFE6FF, 0.0f, 0.0f);
    }

    public static void shockwave(ServerWorld world, Vec3d pos, float radius, float shake) {
        send(world, Kind.SHOCKWAVE, pos, pos, 0xFFFFFF, radius, shake);
    }

    public static void shake(ServerWorld world, Vec3d pos, float strength, float radius) {
        send(world, Kind.SHAKE, pos, pos, 0, strength, radius);
    }

    public static void cameraFlash(ServerWorld world, Vec3d pos, float size) {
        send(world, Kind.CAMERA_FLASH, pos, pos, 0xFFFFFF, size, 0.0f);
    }

    public static void speakerWave(ServerWorld world, Vec3d from, Vec3d toward, float size) {
        speakerWave(world, from, toward, size, 0x9AD8FF);
    }

    public static void speakerWave(ServerWorld world, Vec3d from, Vec3d toward, float size, int color) {
        send(world, Kind.SPEAKER_WAVE, from, toward, color, size, 0.0f);
    }

    public static void tvStatic(ServerWorld world, Vec3d pos, float size) {
        send(world, Kind.TV_STATIC, pos, pos, 0xFFFFFF, size, 0.0f);
    }

    public static void splash(ServerWorld world, Vec3d pos, float size) {
        send(world, Kind.SPLASH, pos, pos, 0xBFE6FF, size, 0.0f);
    }

    public static void ring(ServerWorld world, Vec3d pos, float radius, int color) {
        send(world, Kind.RING, pos, pos, color, radius, 0.0f);
    }

    public static void teleport(ServerWorld world, Vec3d pos, float size, boolean arrival) {
        send(world, Kind.TELEPORT, pos, pos, 0x202020, size, arrival ? 1.0f : 0.0f);
    }

    public static void cone(ServerWorld world, Vec3d from, Vec3d toward, float length, int color) {
        send(world, Kind.CONE, from, toward, color, length, 0.0f);
    }

    public static void sparks(ServerWorld world, Vec3d pos, Vec3d toward, int color) {
        send(world, Kind.SPARKS, pos, toward, color, 0.0f, 0.0f);
    }

    public static void notes(ServerWorld world, Vec3d pos, float radius) {
        send(world, Kind.NOTES, pos, pos, 0, radius, 0.0f);
    }

    private static void send(ServerWorld world, Kind kind, Vec3d a, Vec3d b, int color, float f1, float f2) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(kind);
        writeVec(buf, a);
        writeVec(buf, b);
        buf.writeInt(color);
        buf.writeFloat(f1);
        buf.writeFloat(f2);
        for (ServerPlayerEntity player : PlayerLookup.around(world, a, RANGE)) {
            ServerPlayNetworking.send(player, FX, PacketByteBufs.copy(buf));
        }
    }

    private static void writeVec(PacketByteBuf buf, Vec3d v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    public static Vec3d readVec(PacketByteBuf buf) {
        return new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
