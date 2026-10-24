package com.skibiditoilet.mixin.client;

import com.skibiditoilet.character.CharacterData;
import com.skibiditoilet.client.character.ClientCharacterState;
import com.skibiditoilet.client.character.PlayerAppearance;
import com.skibiditoilet.client.power.PlayerFormRenderer;
import com.skibiditoilet.power.Form;
import com.skibiditoilet.power.FormHolder;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the parts of the player that a character covers, and draws the suit sleeve in first person. A player in a
 * form is drawn by its PlayerFormRenderer instead, with a shadow to match their size, and has no first-person arm.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin
        extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private PlayerEntityRendererMixin(EntityRendererFactory.Context context,
                                      PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void skibidi$renderForm(AbstractClientPlayerEntity player, float yaw, float tickDelta, MatrixStack matrices,
                                    VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        // The shadow is drawn after this, with this renderer's radius.
        this.shadowRadius = 0.5f * FormHolder.scale(player);
        PlayerFormRenderer form = skibidi$formRenderer(player);
        if (form != null) {
            form.render(player, yaw, tickDelta, matrices, vertexConsumers, light);
            ci.cancel();
        }
    }

    @Inject(method = "setModelPose", at = @At("TAIL"))
    private void skibidi$hideCoveredParts(AbstractClientPlayerEntity player, CallbackInfo ci) {
        PlayerAppearance.hideCoveredParts(player, this.getModel());
    }

    @Inject(method = "renderArm", at = @At("HEAD"), cancellable = true)
    private void skibidi$noFormArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                   AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        if (skibidi$formRenderer(player) != null) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArm", at = @At("TAIL"))
    private void skibidi$renderSuitSleeve(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                          AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve,
                                          CallbackInfo ci) {
        boolean rightArm = arm == this.getModel().rightArm;
        PlayerAppearance.renderFirstPersonSuitArm(matrices, vertexConsumers, light, player, arm, rightArm);
    }

    @Unique
    @Nullable
    private static PlayerFormRenderer skibidi$formRenderer(AbstractClientPlayerEntity player) {
        Form form = FormHolder.form(player);
        if (form == Form.NORMAL) {
            return null;
        }
        CharacterData character = ClientCharacterState.get(player.getId());
        return PlayerFormRenderer.get(form, character != null && character.created() ? character.type() : null);
    }
}
