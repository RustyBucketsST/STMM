package com.skibiditoilet.mixin.client;

import com.skibiditoilet.power.FormHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A player in a big form reaches blocks as far away as their size. */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "getReachDistance", at = @At("RETURN"), cancellable = true)
    private void skibidi$formReach(CallbackInfoReturnable<Float> cir) {
        if (this.client.player != null) {
            cir.setReturnValue(cir.getReturnValueF() * FormHolder.scale(this.client.player));
        }
    }
}
