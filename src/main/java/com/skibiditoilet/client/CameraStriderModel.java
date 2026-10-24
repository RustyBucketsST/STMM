package com.skibiditoilet.client;

import com.skibiditoilet.client.bbmodel.CameraStriderBbModel;
import com.skibiditoilet.entity.CameraStriderEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * The Camera Strider as in the series (blockbench/camera_strider.bbmodel): a movie camera with a big lens and
 * guns on top and underneath, up on three long legs that bend at the knee (two at the sides, one behind).
 * The body turns to aim at what it's looking at; the legs step as it walks. Ground at y = 24.
 */
public class CameraStriderModel extends SinglePartEntityModel<CameraStriderEntity> {
    private static final float HIP_Y = -6.0f;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart backLeg;

    public CameraStriderModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.backLeg = root.getChild("back_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        CameraStriderBbModel.addBody(root);
        CameraStriderBbModel.addLeftLeg(root);
        CameraStriderBbModel.addRightLeg(root);
        CameraStriderBbModel.addBackLeg(root);
        return TexturedModelData.of(modelData, CameraStriderBbModel.TEXTURE_WIDTH, CameraStriderBbModel.TEXTURE_HEIGHT);
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }

    @Override
    public void setAngles(CameraStriderEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        this.body.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
        this.body.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE * 0.5f;
        float swing = MathHelper.cos(limbAngle * 0.5f) * 0.5f * limbDistance;
        this.leftLeg.pitch = swing;
        this.rightLeg.pitch = -swing;
        this.backLeg.pitch = swing * 0.6f;
        // A slight bounce in the body with each step.
        this.body.pivotY = HIP_Y + Math.abs(MathHelper.sin(limbAngle * 0.5f)) * limbDistance * 1.5f;
    }
}
