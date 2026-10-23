package com.skibiditoilet.client;

import com.skibiditoilet.entity.CameraStriderEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * The Camera Strider: a camera body with a big lens, up on three long legs (two in front, one behind).
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
        ModelPartData body = root.addChild("body", ModelPartBuilder.create()
                .uv(0, 0).cuboid(-6, -10, -8, 12, 10, 16),              // camera body
                ModelTransform.pivot(0, HIP_Y, 0));
        body.addChild("lens", ModelPartBuilder.create().uv(0, 26).cuboid(-3, -8, -14, 6, 6, 6), ModelTransform.NONE);
        body.addChild("light", ModelPartBuilder.create().uv(32, 26).cuboid(3, -11, -6, 2, 1, 2), ModelTransform.NONE);
        ModelPartBuilder leg = ModelPartBuilder.create().uv(24, 26).cuboid(-1, 0, -1, 2, 30, 2);
        root.addChild("left_leg", leg, ModelTransform.pivot(5, HIP_Y, -5));
        root.addChild("right_leg", leg, ModelTransform.pivot(-5, HIP_Y, -5));
        root.addChild("back_leg", leg, ModelTransform.pivot(0, HIP_Y, 6));
        return TexturedModelData.of(modelData, 64, 64);
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
