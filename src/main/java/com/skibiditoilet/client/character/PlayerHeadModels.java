package com.skibiditoilet.client.character;

import com.skibiditoilet.SkibidiToiletMod;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;

/**
 * Models drawn on players: one device head per Alliance type (every head detail is its own part,
 * switched on or off) and the suit, which fits over the player like armor.
 * Head UVs match the textures/player/*.png files made by tools/gen_texture.py.
 */
public final class PlayerHeadModels {
    public static final EntityModelLayer CAMERA_HEAD = layer("player_camera_head");
    public static final EntityModelLayer SPEAKER_HEAD = layer("player_speaker_head");
    public static final EntityModelLayer TV_HEAD = layer("player_tv_head");
    public static final EntityModelLayer SUIT = layer("player_suit");

    private PlayerHeadModels() {
    }

    public static TexturedModelData camera() {
        ModelData modelData = new ModelData();
        ModelPartData head = modelData.getRoot().addChild("head", ModelPartBuilder.create(), ModelTransform.NONE);
        head.addChild("casing", ModelPartBuilder.create().uv(0, 0).cuboid(-3, -6, -5, 6, 6, 10), ModelTransform.NONE);
        head.addChild("lens_normal", ModelPartBuilder.create().uv(32, 0).cuboid(-2, -5, -7, 4, 4, 2), ModelTransform.NONE);
        head.addChild("lens_big", ModelPartBuilder.create().uv(32, 8).cuboid(-3, -6, -8, 6, 6, 3), ModelTransform.NONE);
        head.addChild("lens_double", ModelPartBuilder.create()
                .uv(44, 0).cuboid(-3, -5, -7, 2, 3, 2)
                .uv(44, 0).cuboid(1, -5, -7, 2, 3, 2), ModelTransform.NONE);
        head.addChild("light", ModelPartBuilder.create().uv(52, 0).cuboid(1, -8, -3, 2, 2, 2), ModelTransform.NONE);
        return TexturedModelData.of(modelData, 64, 64);
    }

    public static TexturedModelData speaker() {
        // Speaker count and grille style are drawn on the front face, so the shape never changes.
        ModelData modelData = new ModelData();
        ModelPartData head = modelData.getRoot().addChild("head", ModelPartBuilder.create(), ModelTransform.NONE);
        head.addChild("cabinet", ModelPartBuilder.create().uv(0, 0).cuboid(-5, -10, -4, 10, 10, 8), ModelTransform.NONE);
        return TexturedModelData.of(modelData, 64, 64);
    }

    public static TexturedModelData tv() {
        ModelData modelData = new ModelData();
        ModelPartData head = modelData.getRoot().addChild("head", ModelPartBuilder.create(), ModelTransform.NONE);
        head.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-6, -9, -3.5f, 12, 9, 7), ModelTransform.NONE);
        head.addChild("antenna_left", ModelPartBuilder.create().uv(40, 0).cuboid(-0.5f, -6, -0.5f, 1, 6, 1),
                ModelTransform.of(-2, -9, 0, 0, 0, -0.45f));
        head.addChild("antenna_right", ModelPartBuilder.create().uv(40, 0).cuboid(-0.5f, -6, -0.5f, 1, 6, 1),
                ModelTransform.of(2, -9, 0, 0, 0, 0.45f));
        head.addChild("antenna_single", ModelPartBuilder.create().uv(44, 0).cuboid(-0.5f, -5, -0.5f, 1, 5, 1),
                ModelTransform.pivot(0, -9, 0));
        return TexturedModelData.of(modelData, 64, 64);
    }

    /** A biped slightly bigger than the player, like armor, using the 64x32 armor texture layout. */
    public static TexturedModelData suit() {
        return TexturedModelData.of(BipedEntityModel.getModelData(new Dilation(0.3f), 0.0f), 64, 32);
    }

    private static EntityModelLayer layer(String name) {
        return new EntityModelLayer(SkibidiToiletMod.id(name), "main");
    }
}
