package com.skibiditoilet.mixin;

import com.skibiditoilet.power.Form;
import com.skibiditoilet.power.FormHolder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the player's form and makes them its size: hitbox, eyes, and how far they can fall unhurt. */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements FormHolder {
    @Unique
    private Form skibidi$form = Form.NORMAL;
    // 0 until set: the Entity constructor asks for the player's size before this class's fields are set up.
    @Unique
    private float skibidi$scale;

    @Override
    public Form skibidi$getForm() {
        return this.skibidi$form != null ? this.skibidi$form : Form.NORMAL;
    }

    @Override
    public float skibidi$getScale() {
        return this.skibidi$scale > 0.0f ? this.skibidi$scale : 1.0f;
    }

    @Override
    public void skibidi$setForm(Form form, float scale) {
        this.skibidi$form = form;
        this.skibidi$scale = scale;
    }

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void skibidi$scaleDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        float scale = this.skibidi$getScale();
        if (scale != 1.0f) {
            cir.setReturnValue(cir.getReturnValue().scaled(scale));
        }
    }

    @Inject(method = "getActiveEyeHeight", at = @At("RETURN"), cancellable = true)
    private void skibidi$scaleEyeHeight(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        float scale = this.skibidi$getScale();
        if (scale != 1.0f) {
            cir.setReturnValue(cir.getReturnValueF() * scale);
        }
    }

    @ModifyVariable(method = "handleFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float skibidi$scaleFall(float fallDistance) {
        return fallDistance / this.skibidi$getScale();
    }
}
