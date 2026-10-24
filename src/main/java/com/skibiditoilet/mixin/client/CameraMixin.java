package com.skibiditoilet.mixin.client;

import com.skibiditoilet.client.fx.ClientFx;
import com.skibiditoilet.power.FormHolder;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Screen shake: nudges where the camera looks (never where the player looks) after vanilla places it. Also keeps
 * the third-person camera as far from a player in a big form as from a normal one, for their size.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private Entity focusedEntity;

    @Shadow
    public abstract float getYaw();

    @Shadow
    public abstract float getPitch();

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void skibidi$shake(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
                               float tickDelta, CallbackInfo ci) {
        float[] offset = ClientFx.shakeOffset(tickDelta);
        if (offset != null) {
            this.setRotation(this.getYaw() + offset[0], this.getPitch() + offset[1]);
        }
    }

    @ModifyConstant(method = "update", constant = @Constant(doubleValue = 4.0))
    private double skibidi$formCameraDistance(double distance) {
        return this.focusedEntity instanceof PlayerEntity player ? distance * FormHolder.scale(player) : distance;
    }
}
