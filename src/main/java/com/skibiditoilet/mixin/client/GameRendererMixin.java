package com.skibiditoilet.mixin.client;

import com.skibiditoilet.power.FormHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** A player in a big form can hit mobs as far away as their size (3 blocks, or 6 in creative, times it). */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    MinecraftClient client;

    @ModifyConstant(method = "updateTargetedEntity", constant = @Constant(doubleValue = 6.0))
    private double skibidi$formCreativeReach(double distance) {
        return distance * this.skibidi$scale();
    }

    @ModifyConstant(method = "updateTargetedEntity", constant = @Constant(doubleValue = 9.0))
    private double skibidi$formAttackReach(double squaredDistance) {
        float scale = this.skibidi$scale();
        return squaredDistance * scale * scale;
    }

    @Unique
    private float skibidi$scale() {
        return this.client.getCameraEntity() instanceof PlayerEntity player ? FormHolder.scale(player) : 1.0f;
    }
}
