package com.skibiditoilet.story;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.power.PowerManager;
import com.skibiditoilet.registry.ModAttachments;
import com.skibiditoilet.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/** Story packets: progress and the running mission go to the client; start, claim and abandon come back. */
public final class StorySync {
    /** S2C: the player's StoryData. */
    public static final Identifier DATA = SkibidiToiletMod.id("story_data");
    /** S2C: boolean, then a MissionStatus if true. The mission the player is in, or none. */
    public static final Identifier STATUS = SkibidiToiletMod.id("mission_status");
    /** S2C: StoryEvent ordinal + mission id. Shown as a toast. */
    public static final Identifier EVENT = SkibidiToiletMod.id("story_event");
    /** S2C: no payload. Opens the story screen (sent when talking to the Commander). */
    public static final Identifier OPEN = SkibidiToiletMod.id("open_story");
    /** C2S: mission id. */
    public static final Identifier START = SkibidiToiletMod.id("mission_start");
    /** C2S: mission id. */
    public static final Identifier CLAIM = SkibidiToiletMod.id("mission_claim");
    /** C2S: no payload. */
    public static final Identifier ABANDON = SkibidiToiletMod.id("mission_abandon");

    private StorySync() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendData(handler.player);
            sendStatus(handler.player, null);
        });

        // Packet handlers run on the network thread; read the buffer there, act on the server thread.
        ServerPlayNetworking.registerGlobalReceiver(START, (server, player, handler, buf, sender) -> {
            String id = buf.readString();
            server.execute(() -> start(player, id));
        });
        ServerPlayNetworking.registerGlobalReceiver(CLAIM, (server, player, handler, buf, sender) -> {
            String id = buf.readString();
            server.execute(() -> claim(player, id));
        });
        ServerPlayNetworking.registerGlobalReceiver(ABANDON, (server, player, handler, buf, sender) ->
                server.execute(() -> MissionManager.abandon(player)));
    }

    public static void set(ServerPlayerEntity player, StoryData data) {
        player.setAttached(ModAttachments.STORY, data);
        sendData(player);
    }

    public static void openStory(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, OPEN, PacketByteBufs.empty());
    }

    private static void start(ServerPlayerEntity player, String id) {
        Mission mission = StoryMissions.get(id);
        Text error = mission != null ? MissionManager.start(player, mission) : Text.translatable("story.skibidi.error.unknown");
        if (error != null) {
            player.sendMessage(error, false);
        }
    }

    /** Hands out a finished mission's reward, once. Items that don't fit are dropped at the player's feet. */
    private static void claim(ServerPlayerEntity player, String id) {
        Mission mission = StoryMissions.get(id);
        StoryData data = StoryData.of(player);
        if (mission == null || !data.isClaimable(id)) {
            return;
        }
        set(player, data.withClaimed(id));
        PowerManager.addPoints(player, mission.points());
        for (ItemStack reward : mission.rewards()) {
            player.getInventory().offerOrDrop(reward.copy());
        }
        player.playSound(ModSounds.MISSION_REWARD, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    public static void sendData(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeNbt((NbtCompound) StoryData.CODEC.encodeStart(NbtOps.INSTANCE, StoryData.of(player)).result().orElseThrow());
        ServerPlayNetworking.send(player, DATA, buf);
    }

    @Nullable
    public static StoryData readData(PacketByteBuf buf) {
        NbtCompound nbt = buf.readNbt();
        return nbt != null ? StoryData.CODEC.parse(NbtOps.INSTANCE, nbt).result().orElse(null) : null;
    }

    public static void sendStatus(ServerPlayerEntity player, @Nullable MissionStatus status) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(status != null);
        if (status != null) {
            status.write(buf);
        }
        ServerPlayNetworking.send(player, STATUS, buf);
    }

    public static void sendEvent(ServerPlayerEntity player, StoryEvent event, Mission mission) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(event);
        buf.writeString(mission.id());
        ServerPlayNetworking.send(player, EVENT, buf);
    }
}
