package com.skibiditoilet.client.character;

import com.skibiditoilet.character.CharacterData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;

/** Adds the character's head and suit to the player renderer. */
public class AllianceAppearanceFeature
        extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    public AllianceAppearanceFeature(
            FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        CharacterData data = PlayerAppearance.appearanceOf(player);
        if (data != null) {
            PlayerAppearance.render(matrices, vertexConsumers, light, player, this.getContextModel(), data);
        }
    }
}
