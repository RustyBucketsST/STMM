package com.skibiditoilet.mixin;

import com.skibiditoilet.power.FormHolder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Players in a big form jump higher (though not as many times their height as a normal player). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getJumpVelocity", at = @At("RETURN"), cancellable = true)
    private void skibidi$formJump(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            float scale = FormHolder.scale(player);
            if (scale != 1.0f) {
                cir.setReturnValue(cir.getReturnValueF() * (float) Math.pow(scale, 0.35));
            }
        }
    }
}
