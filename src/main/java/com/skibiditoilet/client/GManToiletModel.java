package com.skibiditoilet.client;

import com.skibiditoilet.client.bbmodel.GManToiletBbModel;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;

/**
 * G-Toilet as in the series (blockbench/gman_toilet.bbmodel): G-Man's head in a tall, plain toilet with no seat
 * or lid and a tall cistern.
 */
public class GManToiletModel extends SkibidiToiletModel {
    public GManToiletModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        GManToiletBbModel.addToilet(modelData.getRoot());
        GManToiletBbModel.addHead(modelData.getRoot());
        return TexturedModelData.of(modelData, GManToiletBbModel.TEXTURE_WIDTH, GManToiletBbModel.TEXTURE_HEIGHT);
    }
}
