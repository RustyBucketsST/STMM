package com.skibiditoilet.power;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Every player keeps the form they're in (added by PlayerEntityMixin), on both sides: the server and each client
 * need it to give the player the form's size. The server decides it and tells clients with PowerSync#sendForm,
 * along with the gear the player has (which clients keep here to draw it).
 */
public interface FormHolder {
    Form skibidi$getForm();

    /** How many times normal size the player is: 1 unless they're in a form. */
    float skibidi$getScale();

    void skibidi$setForm(Form form, float scale);

    /** The gear the player has, as a Gear mask. Only kept on clients (the server reads PowerData). */
    int skibidi$getGear();

    void skibidi$setGear(int gear);

    static Form form(PlayerEntity player) {
        return ((FormHolder) player).skibidi$getForm();
    }

    static float scale(PlayerEntity player) {
        return ((FormHolder) player).skibidi$getScale();
    }

    static int gear(PlayerEntity player) {
        return ((FormHolder) player).skibidi$getGear();
    }

    /** Puts the player in {@code form} at {@code scale} times normal size, resizing them to match. */
    static void set(PlayerEntity player, Form form, float scale) {
        ((FormHolder) player).skibidi$setForm(form, scale);
        player.setStepHeight(Form.stepHeight(scale));
        player.calculateDimensions();
    }
}
