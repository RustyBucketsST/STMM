package com.skibiditoilet.client.character;

import com.skibiditoilet.character.CharacterData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/** The look being edited in the creator, shown on the local player while a creator screen is open. */
public final class CharacterPreview {
    @Nullable
    private static CharacterData preview;

    private CharacterPreview() {
    }

    @Nullable
    public static CharacterData get() {
        return preview;
    }

    public static void set(@Nullable CharacterData data) {
        preview = data;
    }

    /** Draws the entity standing upright, turned by {@code yaw} degrees (0 faces the viewer). */
    public static void drawSpinning(DrawContext context, int x, int y, int size, float yaw, LivingEntity entity) {
        float bodyYaw = entity.bodyYaw;
        float entityYaw = entity.getYaw();
        float pitch = entity.getPitch();
        float headYaw = entity.headYaw;
        float prevHeadYaw = entity.prevHeadYaw;
        entity.bodyYaw = 180.0f;
        entity.setYaw(180.0f);
        entity.setPitch(0.0f);
        entity.headYaw = 180.0f;
        entity.prevHeadYaw = 180.0f;

        Quaternionf rotation = new Quaternionf().rotateZ(MathHelper.PI).rotateY(yaw * MathHelper.RADIANS_PER_DEGREE);
        InventoryScreen.drawEntity(context, x, y, size, rotation, null, entity);

        entity.bodyYaw = bodyYaw;
        entity.setYaw(entityYaw);
        entity.setPitch(pitch);
        entity.headYaw = headYaw;
        entity.prevHeadYaw = prevHeadYaw;
    }
}
