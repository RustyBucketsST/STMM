package com.skibiditoilet.client;

import com.skibiditoilet.entity.SkibidiToiletEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.util.math.MathHelper;

/**
 * The toilet without its pedestal, standing on four spider legs per side, at the toilet's detail. Legs are listed
 * front (-Z) to back.
 */
public class SpiderToiletModel extends SkibidiToiletModel {
    private static final int LEGS_PER_SIDE = 4;
    /** The UV of the "spider_leg" reference cube in blockbench/skibidi_toilet.bbmodel, where the legs are drawn. */
    private static final int LEG_U = 88;
    private static final int LEG_V = 126;
    private static final float[] LEG_Z = {-9.0f, -3.0f, 3.0f, 9.0f};
    // Resting pose: legs fan out front-to-back and angle down to the ground.
    private static final float[] LEG_YAW = {-0.785f, -0.393f, 0.393f, 0.785f};
    private static final float[] LEG_ROLL = {0.785f, 0.58f, 0.58f, 0.785f};

    private final ModelPart[] rightLegs = new ModelPart[LEGS_PER_SIDE];
    private final ModelPart[] leftLegs = new ModelPart[LEGS_PER_SIDE];

    public SpiderToiletModel(ModelPart root) {
        super(root, DETAILED);
        for (int i = 0; i < LEGS_PER_SIDE; i++) {
            this.rightLegs[i] = root.getChild("right_leg_" + i);
            this.leftLegs[i] = root.getChild("left_leg_" + i);
        }
    }

    public static TexturedModelData getSpiderTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        addToilet(root, false);
        for (int i = 0; i < LEGS_PER_SIDE; i++) {
            root.addChild("right_leg_" + i, ModelPartBuilder.create().uv(LEG_U, LEG_V).cuboid(-30, -2, -2, 32, 4, 4),
                    ModelTransform.pivot(-8, 6, LEG_Z[i]));
            root.addChild("left_leg_" + i, ModelPartBuilder.create().uv(LEG_U, LEG_V).mirrored()
                    .cuboid(-2, -2, -2, 32, 4, 4), ModelTransform.pivot(8, 6, LEG_Z[i]));
        }
        return textured(modelData);
    }

    @Override
    public void setAngles(SkibidiToiletEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        // Walk cycle: each leg swings forward/back and lifts, alternating so neighbours are out of step.
        for (int i = 0; i < LEGS_PER_SIDE; i++) {
            float phase = i * MathHelper.HALF_PI;
            float swing = MathHelper.cos(limbAngle * 1.3324f + phase) * 0.4f * limbDistance;
            float lift = Math.abs(MathHelper.sin(limbAngle * 0.6662f + phase)) * 0.4f * limbDistance;
            this.rightLegs[i].yaw = LEG_YAW[i] + swing;
            this.rightLegs[i].roll = -LEG_ROLL[i] + lift;
            this.leftLegs[i].yaw = -LEG_YAW[i] - swing;
            this.leftLegs[i].roll = LEG_ROLL[i] - lift;
        }
    }
}
