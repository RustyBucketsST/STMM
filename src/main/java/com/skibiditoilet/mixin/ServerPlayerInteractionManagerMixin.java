package com.skibiditoilet.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.skibiditoilet.power.FormHolder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** A player in a big form can break blocks as far away as they can reach. */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow
    @Final
    protected ServerPlayerEntity player;

    @ModifyExpressionValue(method = "processBlockBreakingAction",
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;MAX_BREAK_SQUARED_DISTANCE:D"))
    private double skibidi$formReach(double squaredDistance) {
        float scale = FormHolder.scale(this.player);
        return squaredDistance * scale * scale;
    }
}
