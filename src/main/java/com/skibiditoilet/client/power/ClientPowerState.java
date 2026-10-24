package com.skibiditoilet.client.power;

import com.skibiditoilet.client.character.ClientCharacterState;
import com.skibiditoilet.power.Form;
import com.skibiditoilet.power.FormHolder;
import com.skibiditoilet.power.Power;
import com.skibiditoilet.power.PowerData;
import com.skibiditoilet.power.PowerSync;
import com.skibiditoilet.power.Tech;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * The client's copy of the player's powers: points and unlocks, energy and cooldowns, and which players
 * nearby have a shield up or are dancing (the form each player is in is kept on the player, see FormHolder).
 * Also the power keys, the transform key and the key that opens the Powers menu.
 */
public final class ClientPowerState {
    private static final KeyBinding MENU_KEY = new KeyBinding("key.skibidi.powers", InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K, "key.categories.skibidi");
    /** One key per power slot, in the order Power#of lists a type's powers. */
    static final KeyBinding[] POWER_KEYS = {
            powerKey(1, GLFW.GLFW_KEY_R),
            powerKey(2, GLFW.GLFW_KEY_G),
            powerKey(3, GLFW.GLFW_KEY_V),
            powerKey(4, GLFW.GLFW_KEY_B),
    };
    /** Steps up a form; with sneak, turns back to normal. */
    private static final KeyBinding TRANSFORM_KEY = new KeyBinding("key.skibidi.transform", InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_Y, "key.categories.skibidi");
    /** How long the "+N points" popup stays up, in ticks. */
    static final int GAIN_TICKS = 50;

    private static PowerData data = PowerData.DEFAULT;
    private static boolean hasData;
    private static float energy;
    private static float maxEnergy;
    private static int[] cooldowns = new int[0];
    // When the cooldowns arrived, so they can count down smoothly between updates.
    private static long stateTime;
    private static long ticks;
    private static int gained;
    private static long gainedTime = -GAIN_TICKS;
    /** Entity id -> the client tick each of its visible powers runs until. */
    private static final Map<Integer, EnumMap<PowerSync.Visible, long[]>> VISIBLE = new HashMap<>();

    private ClientPowerState() {
    }

    private static KeyBinding powerKey(int slot, int key) {
        return new KeyBinding("key.skibidi.power_" + slot, InputUtil.Type.KEYSYM, key, "key.categories.skibidi");
    }

    public static void init() {
        KeyBindingHelper.registerKeyBinding(MENU_KEY);
        for (KeyBinding key : POWER_KEYS) {
            KeyBindingHelper.registerKeyBinding(key);
        }
        KeyBindingHelper.registerKeyBinding(TRANSFORM_KEY);

        ClientPlayNetworking.registerGlobalReceiver(PowerSync.DATA, (client, handler, buf, sender) -> {
            PowerData received = PowerSync.readData(buf);
            if (received != null) {
                client.execute(() -> receive(received));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(PowerSync.STATE, (client, handler, buf, sender) -> {
            float newEnergy = buf.readFloat();
            float newMax = buf.readFloat();
            int[] newCooldowns = buf.readIntArray();
            client.execute(() -> {
                energy = newEnergy;
                maxEnergy = newMax;
                cooldowns = newCooldowns;
                stateTime = ticks;
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(PowerSync.VISIBLE, (client, handler, buf, sender) -> {
            int entityId = buf.readVarInt();
            PowerSync.Visible visible = buf.readEnumConstant(PowerSync.Visible.class);
            int duration = buf.readVarInt();
            client.execute(() -> VISIBLE.computeIfAbsent(entityId, id -> new EnumMap<>(PowerSync.Visible.class))
                    .put(visible, new long[]{ticks, ticks + duration}));
        });

        ClientPlayNetworking.registerGlobalReceiver(PowerSync.FORM, (client, handler, buf, sender) -> {
            int entityId = buf.readVarInt();
            Form form = buf.readEnumConstant(Form.class);
            float scale = buf.readFloat();
            client.execute(() -> {
                if (client.world != null && client.world.getEntityById(entityId) instanceof PlayerEntity player) {
                    FormHolder.set(player, form, scale);
                }
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(ClientPowerState::tick);
        HudRenderCallback.EVENT.register(PowerHud::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ShieldRenderer::render);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    private static void receive(PowerData received) {
        int difference = received.points() - data.points();
        if (hasData && difference > 0) {
            // Kills close together add up into one popup.
            gained = ticks - gainedTime < GAIN_TICKS ? gained + difference : difference;
            gainedTime = ticks;
        }
        data = received;
        hasData = true;
    }

    private static void tick(MinecraftClient client) {
        ticks++;
        if (ticks % 20 == 0) {
            VISIBLE.values().removeIf(powers -> {
                powers.values().removeIf(span -> span[1] < ticks);
                return powers.isEmpty();
            });
        }
        boolean ready = client.player != null && client.currentScreen == null && ClientCharacterState.self().created();
        for (int slot = 0; slot < POWER_KEYS.length; slot++) {
            while (POWER_KEYS[slot].wasPressed()) {
                if (ready) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeVarInt(slot);
                    ClientPlayNetworking.send(PowerSync.USE, buf);
                }
            }
        }
        while (TRANSFORM_KEY.wasPressed()) {
            if (ready) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBoolean(client.player.isSneaking());
                ClientPlayNetworking.send(PowerSync.TRANSFORM, buf);
            }
        }
        while (MENU_KEY.wasPressed()) {
            if (ready) {
                client.setScreen(new PowersScreen());
            }
        }
    }

    private static void reset() {
        data = PowerData.DEFAULT;
        hasData = false;
        energy = 0.0f;
        maxEnergy = 0.0f;
        cooldowns = new int[0];
        gained = 0;
        gainedTime = -GAIN_TICKS;
        VISIBLE.clear();
    }

    public static PowerData data() {
        return data;
    }

    static float energy() {
        return energy;
    }

    static float maxEnergy() {
        return maxEnergy;
    }

    /** Ticks until the power on {@code slot} can be used again, counted down since the server last said. */
    static float cooldownLeft(int slot, float tickDelta) {
        return slot < cooldowns.length ? Math.max(0.0f, cooldowns[slot] - (ticks - stateTime + tickDelta)) : 0.0f;
    }

    static long ticks() {
        return ticks;
    }

    static int gained() {
        return gained;
    }

    static long gainedTime() {
        return gainedTime;
    }

    /**
     * How far into a visible power on this entity we are: {ticks since it started, ticks until it ends},
     * or null if it isn't on.
     */
    @Nullable
    static float[] visible(int entityId, PowerSync.Visible visible, float tickDelta) {
        EnumMap<PowerSync.Visible, long[]> powers = VISIBLE.get(entityId);
        long[] span = powers != null ? powers.get(visible) : null;
        if (span == null || ticks + tickDelta >= span[1]) {
            return null;
        }
        return new float[]{ticks - span[0] + tickDelta, span[1] - ticks - tickDelta};
    }

    /** Whether this player is doing the Anthem Dance right now. Read by PlayerEntityModelMixin. */
    public static boolean isDancing(int entityId) {
        return visible(entityId, PowerSync.Visible.DANCE, 0.0f) != null;
    }

    static void unlock(Power power) {
        send(PowerSync.UNLOCK, power.id());
    }

    static void unlock(Form form) {
        send(PowerSync.UNLOCK, form.id());
    }

    static KeyBinding transformKey() {
        return TRANSFORM_KEY;
    }

    static void build(Tech tech) {
        send(PowerSync.BUILD, tech.id());
    }

    private static void send(Identifier channel, String id) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(id);
        ClientPlayNetworking.send(channel, buf);
    }

    static boolean isMenuKey(int keyCode, int scanCode) {
        return MENU_KEY.matchesKey(keyCode, scanCode);
    }
}
