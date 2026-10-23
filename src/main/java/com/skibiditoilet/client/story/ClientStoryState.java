package com.skibiditoilet.client.story;

import com.skibiditoilet.client.character.ClientCharacterState;
import com.skibiditoilet.registry.ModSounds;
import com.skibiditoilet.story.Mission;
import com.skibiditoilet.story.MissionStatus;
import com.skibiditoilet.story.StoryData;
import com.skibiditoilet.story.StoryEvent;
import com.skibiditoilet.story.StoryMissions;
import com.skibiditoilet.story.StorySync;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/** The client's copy of the player's story progress and running mission, and the key that opens the story. */
public final class ClientStoryState {
    private static final KeyBinding OPEN_KEY = new KeyBinding("key.skibidi.story", InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_J, "key.categories.skibidi");

    private static StoryData data = StoryData.DEFAULT;
    @Nullable
    private static MissionStatus status;
    // When the status arrived, so a survive timer can count down smoothly between updates.
    private static long statusTime;
    private static long ticks;

    private ClientStoryState() {
    }

    public static void init() {
        KeyBindingHelper.registerKeyBinding(OPEN_KEY);

        ClientPlayNetworking.registerGlobalReceiver(StorySync.DATA, (client, handler, buf, sender) -> {
            StoryData received = StorySync.readData(buf);
            if (received != null) {
                client.execute(() -> data = received);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(StorySync.STATUS, (client, handler, buf, sender) -> {
            MissionStatus received = buf.readBoolean() ? MissionStatus.read(buf) : null;
            client.execute(() -> {
                status = received;
                statusTime = ticks;
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(StorySync.EVENT, (client, handler, buf, sender) -> {
            StoryEvent event = buf.readEnumConstant(StoryEvent.class);
            Mission mission = StoryMissions.get(buf.readString());
            if (mission != null) {
                client.execute(() -> {
                    client.getToastManager().add(new MissionToast(event, mission));
                    client.getSoundManager().play(PositionedSoundInstance.master(sound(event), 1.0f));
                });
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(StorySync.OPEN, (client, handler, buf, sender) ->
                client.execute(() -> client.setScreen(new StoryScreen())));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ticks++;
            while (OPEN_KEY.wasPressed()) {
                if (client.currentScreen == null && client.player != null && ClientCharacterState.self().created()) {
                    client.setScreen(new StoryScreen());
                }
            }
        });
        HudRenderCallback.EVENT.register(MissionHud::render);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    private static SoundEvent sound(StoryEvent event) {
        return switch (event) {
            case STARTED -> ModSounds.MISSION_START;
            case STAGE_COMPLETE -> ModSounds.MISSION_OBJECTIVE;
            case COMPLETED, HELPED -> ModSounds.MISSION_COMPLETE;
            default -> ModSounds.MISSION_FAILED;
        };
    }

    public static StoryData data() {
        return data;
    }

    @Nullable
    public static MissionStatus status() {
        return status;
    }

    /** Time left in the current SURVIVE stage, counted down since the server last said. */
    public static int ticksLeft(float tickDelta) {
        return status == null ? 0 : (int) Math.max(0, status.ticksLeft() - (ticks - statusTime + tickDelta));
    }

    public static void start(Mission mission) {
        send(StorySync.START, mission.id());
    }

    public static void claim(Mission mission) {
        send(StorySync.CLAIM, mission.id());
    }

    public static void abandon() {
        ClientPlayNetworking.send(StorySync.ABANDON, PacketByteBufs.empty());
    }

    private static void send(Identifier channel, String missionId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(missionId);
        ClientPlayNetworking.send(channel, buf);
    }

    private static void reset() {
        data = StoryData.DEFAULT;
        status = null;
    }

    static boolean isOpenKey(int keyCode, int scanCode) {
        return OPEN_KEY.matchesKey(keyCode, scanCode);
    }
}
