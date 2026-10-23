package com.skibiditoilet.story;

import net.minecraft.network.PacketByteBuf;

/**
 * The running mission as its players see it. The rest (stage kind, boss, text) comes from the mission itself.
 *
 * @param progress  enemies defeated so far in a DEFEAT stage
 * @param goal      enemies to defeat, or 1 when the stage is won by beating its boss
 * @param ticksLeft time left in a SURVIVE stage
 */
public record MissionStatus(String missionId, int stage, int progress, int goal, int ticksLeft) {
    public void write(PacketByteBuf buf) {
        buf.writeString(this.missionId);
        buf.writeVarInt(this.stage);
        buf.writeVarInt(this.progress);
        buf.writeVarInt(this.goal);
        buf.writeVarInt(this.ticksLeft);
    }

    public static MissionStatus read(PacketByteBuf buf) {
        return new MissionStatus(buf.readString(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }
}
