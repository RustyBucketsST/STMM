package com.skibiditoilet.power;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Every player keeps the form they're in (added by PlayerEntityMixin), on both sides: the server and each client
 * need it to give the player the form's size. The server decides it and tells clients with PowerSync#sendForm.
 */
public interface FormHolder {
    Form skibidi$getForm();

    /** How many times normal size the player is: 1 unless they're in a form. */
    float skibidi$getScale();

    void skibidi$setForm(Form form, float scale);

    static Form form(PlayerEntity player) {
        return ((FormHolder) player).skibidi$getForm();
    }

    static float scale(PlayerEntity player) {
        return ((FormHolder) player).skibidi$getScale();
    }

    /** Puts the player in {@code form} at {@code scale} times normal size, resizing them to match. */
    static void set(PlayerEntity player, Form form, float scale) {
        ((FormHolder) player).skibidi$setForm(form, scale);
        player.setStepHeight(Form.stepHeight(scale));
        player.calculateDimensions();
    }
}
