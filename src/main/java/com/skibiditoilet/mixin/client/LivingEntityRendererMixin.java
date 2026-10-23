package com.skibiditoilet.mixin.client;

import com.skibiditoilet.entity.SkibidiToiletEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mobs turn red while dying; a toilet being flushed shouldn't, it's just going down the drain. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "getOverlay", at = @At("HEAD"), cancellable = true)
    private static void skibidi$noRedWhileFlushed(LivingEntity entity, float whiteOverlayProgress,
                                                  CallbackInfoReturnable<Integer> cir) {
        if (entity instanceof SkibidiToiletEntity toilet && toilet.deathTime > 0 && toilet.flushesOnDeath()) {
            cir.setReturnValue(OverlayTexture.packUv(OverlayTexture.getU(whiteOverlayProgress), OverlayTexture.getV(false)));
        }
    }
}
