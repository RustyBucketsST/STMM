package com.skibiditoilet.client.character;

import com.skibiditoilet.character.CharacterData;
import com.skibiditoilet.character.CharacterSync;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * The client's copy of the characters the server sent, and the rule that the creator keeps
 * opening until the player has made their character.
 *
 * Characters are kept by entity id rather than on the entities themselves: the server sends a
 * player's character just before that player's spawn packet, so the entity may not exist yet.
 */
public final class ClientCharacterState {
    // Ticks to wait before reopening the creator, so closing the pause menu doesn't flicker.
    private static final int REOPEN_DELAY_TICKS = 8;

    private static final Int2ObjectMap<CharacterData> CHARACTERS = new Int2ObjectOpenHashMap<>();
    private static int reopenDelay;
    // Choices made so far in the creator, kept while the pause menu opened from it is shown.
    @Nullable
    private static CharacterData draft;

    private ClientCharacterState() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(CharacterSync.SYNC, (client, handler, buf, sender) -> {
            int entityId = buf.readVarInt();
            CharacterData data = CharacterSync.read(buf);
            if (data != null) {
                client.execute(() -> CHARACTERS.put(entityId, data));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(CharacterSync.OPEN_CUSTOMIZER, (client, handler, buf, sender) ->
                client.execute(() -> client.setScreen(CustomizeScreen.recustomize(self()))));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientTickEvents.END_CLIENT_TICK.register(ClientCharacterState::openCreatorIfNeeded);
    }

    /** The character the server last sent for this entity, or null if none has arrived. */
    @Nullable
    public static CharacterData get(int entityId) {
        return CHARACTERS.get(entityId);
    }

    /** The local player's character (the default one until the server has sent it). */
    public static CharacterData self() {
        CharacterData data = selfOrNull();
        return data != null ? data : CharacterData.DEFAULT;
    }

    @Nullable
    private static CharacterData selfOrNull() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null ? CHARACTERS.get(client.player.getId()) : null;
    }

    /** Sends the finished creator to the server and assumes it worked, so the creator doesn't reopen meanwhile. */
    public static void submitCreated(CharacterData data) {
        send(CharacterSync.CREATE, data);
        draft = null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            CHARACTERS.put(client.player.getId(), data.withCreated(true));
        }
    }

    public static void submitLook(CharacterData data) {
        send(CharacterSync.UPDATE, data);
    }

    /** Remembers the creator's choices and delays reopening it, while the pause menu opened from it is shown. */
    public static void pauseCreator(CharacterData choices) {
        saveDraft(choices);
        reopenDelay = REOPEN_DELAY_TICKS;
    }

    /** Remembers unfinished creator choices so the creator reopens with them. Ignored once the character exists. */
    public static void saveDraft(CharacterData choices) {
        if (!self().created()) {
            draft = choices;
        }
    }

    private static void send(Identifier channel, CharacterData data) {
        PacketByteBuf buf = PacketByteBufs.create();
        CharacterSync.write(buf, data);
        ClientPlayNetworking.send(channel, buf);
    }

    private static void openCreatorIfNeeded(MinecraftClient client) {
        if (reopenDelay > 0) {
            reopenDelay--;
            return;
        }
        CharacterData self = selfOrNull();
        if (self != null && !self.created() && client.currentScreen == null) {
            client.setScreen(new TypeSelectScreen(draft != null ? draft : self));
        }
    }

    private static void reset() {
        CHARACTERS.clear();
        reopenDelay = 0;
        draft = null;
        CharacterPreview.set(null);
    }
}
