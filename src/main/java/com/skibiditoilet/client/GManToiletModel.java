package com.skibiditoilet.client;

import com.skibiditoilet.client.bbmodel.GManToiletBbModel;
import com.skibiditoilet.entity.SkibidiToiletEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;

/**
 * G-Toilet as in the series (blockbench/gman_toilet.bbmodel): G-Man's head in a tall, plain toilet with no seat
 * or lid and a tall cistern. Modelled at twice the detail of a normal toilet and drawn at half the G-Man scale,
 * so the head's bob and flush, which are measured in normal pixels, are doubled to match.
 */
public class GManToiletModel extends SkibidiToiletModel {
    /** Model units per pixel. */
    public static final float DETAIL = 2.0f;

    public GManToiletModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        GManToiletBbModel.addToilet(modelData.getRoot());
        GManToiletBbModel.addHead(modelData.getRoot());
        return TexturedModelData.of(modelData, GManToiletBbModel.TEXTURE_WIDTH, GManToiletBbModel.TEXTURE_HEIGHT);
    }

    @Override
    public void setAngles(SkibidiToiletEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        this.head.pivotY = this.headPivotY + (this.head.pivotY - this.headPivotY) * DETAIL;
        this.head.pivotZ = this.headPivotZ + (this.head.pivotZ - this.headPivotZ) * DETAIL;
    }
}
