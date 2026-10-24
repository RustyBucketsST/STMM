package com.skibiditoilet.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.skibiditoilet.power.FormHolder;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** A player in a big form reaches as far as their size: the server allows their clicks from that far away. */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @ModifyExpressionValue(method = {"onPlayerInteractBlock", "onPlayerInteractEntity"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;MAX_BREAK_SQUARED_DISTANCE:D"))
    private double skibidi$formReach(double squaredDistance) {
        float scale = FormHolder.scale(this.player);
        return squaredDistance * scale * scale;
    }

    /** How far from their feet a block can be placed. */
    @ModifyConstant(method = "onPlayerInteractBlock", constant = @Constant(doubleValue = 64.0))
    private double skibidi$formPlaceReach(double squaredDistance) {
        float scale = FormHolder.scale(this.player);
        return squaredDistance * scale * scale;
    }
}
