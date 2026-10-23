package com.skibiditoilet.mixin.client;

import com.skibiditoilet.client.power.ClientPowerState;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The Anthem Dance: arms up and waving to the beat, head bobbing, feet stepping. */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void skibidi$dance(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress,
                               float headYaw, float headPitch, CallbackInfo ci) {
        if (!ClientPowerState.isDancing(entity.getId())) {
            return;
        }
        PlayerEntityModel<?> model = (PlayerEntityModel<?>) (Object) this;
        // One sway a second: two beats of the 120 BPM anthem.
        float beat = animationProgress * MathHelper.TAU / 20.0f;
        float sway = MathHelper.sin(beat);
        // Arms up in a V (with the arm raised, a negative roll swings the right arm outward).
        model.rightArm.pitch = -2.8f + sway * 0.45f;
        model.rightArm.yaw = 0.0f;
        model.rightArm.roll = -0.5f + sway * 0.25f;
        model.leftArm.pitch = -2.8f - sway * 0.45f;
        model.leftArm.yaw = 0.0f;
        model.leftArm.roll = 0.5f + sway * 0.25f;
        model.head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE + Math.abs(MathHelper.sin(beat)) * 0.25f - 0.1f;
        model.head.roll = sway * 0.12f;
        model.rightLeg.pitch = Math.max(0.0f, sway) * -0.5f;
        model.leftLeg.pitch = Math.max(0.0f, -sway) * -0.5f;
        // The outer layers follow the parts they cover.
        model.hat.copyTransform(model.head);
        model.rightSleeve.copyTransform(model.rightArm);
        model.leftSleeve.copyTransform(model.leftArm);
        model.rightPants.copyTransform(model.rightLeg);
        model.leftPants.copyTransform(model.leftLeg);
    }
}
