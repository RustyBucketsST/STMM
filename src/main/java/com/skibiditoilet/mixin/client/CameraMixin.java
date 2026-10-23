package com.skibiditoilet.mixin.client;

import com.skibiditoilet.client.fx.ClientFx;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Screen shake: nudges where the camera looks (never where the player looks) after vanilla places it. */
@Mixin(Camera.class)
public abstract class CameraMixin {
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
}
