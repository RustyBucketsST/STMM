package com.skibiditoilet.mixin.client;

import com.skibiditoilet.client.character.PlayerAppearance;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the parts of the player that a character covers, and draws the suit sleeve in first person. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "setModelPose", at = @At("TAIL"))
    private void skibidi$hideCoveredParts(AbstractClientPlayerEntity player, CallbackInfo ci) {
        PlayerAppearance.hideCoveredParts(player, ((PlayerEntityRenderer) (Object) this).getModel());
    }

    @Inject(method = "renderArm", at = @At("TAIL"))
    private void skibidi$renderSuitSleeve(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                          AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve,
                                          CallbackInfo ci) {
        boolean rightArm = arm == ((PlayerEntityRenderer) (Object) this).getModel().rightArm;
        PlayerAppearance.renderFirstPersonSuitArm(matrices, vertexConsumers, light, player, arm, rightArm);
    }
}
