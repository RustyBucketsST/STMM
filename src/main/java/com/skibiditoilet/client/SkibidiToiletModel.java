package com.skibiditoilet.client;

import com.skibiditoilet.entity.SkibidiToiletEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * A toilet facing -Z (the mob's front) with a head popping out of the bowl.
 * Model space is in pixels with the ground at y = 24.
 */
public class SkibidiToiletModel extends SinglePartEntityModel<SkibidiToiletEntity> {
    private static final float HEAD_PIVOT_Y = 13.0f;
    private static final float HEAD_PIVOT_Z = -0.5f;

    private final ModelPart root;
    private final ModelPart head;

    public SkibidiToiletModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        root.addChild("pedestal", ModelPartBuilder.create().uv(0, 50).cuboid(-3, 18, -3, 6, 6, 7), ModelTransform.NONE);
        root.addChild("bowl", ModelPartBuilder.create().uv(0, 16).cuboid(-5, 12, -6, 10, 6, 11), ModelTransform.NONE);
        root.addChild("tank", ModelPartBuilder.create().uv(0, 33).cuboid(-6, 3, 5, 12, 13, 4), ModelTransform.NONE);
        root.addChild("lid", ModelPartBuilder.create().uv(32, 33).cuboid(-5, 3, 4, 10, 9, 1), ModelTransform.NONE);
        root.addChild("handle", ModelPartBuilder.create().uv(42, 16).cuboid(-7, 5, 6, 1, 1, 2), ModelTransform.NONE);
        root.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4, -8, -4, 8, 8, 8),
                ModelTransform.pivot(0, HEAD_PIVOT_Y, HEAD_PIVOT_Z));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    @Override
    public void setAngles(SkibidiToiletEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        // The signature bob: the head pops up and down out of the bowl and sways side to side.
        float bob = Math.abs(MathHelper.sin(animationProgress * 0.35f));
        float lunge = MathHelper.sin(this.handSwingProgress * MathHelper.PI);

        head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
        head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE + lunge * 0.8f;
        head.roll = MathHelper.sin(animationProgress * 0.2f) * 0.15f
                + MathHelper.cos(limbAngle * 0.9f) * 0.3f * limbDistance;
        head.pivotY = HEAD_PIVOT_Y - bob * (1.5f + limbDistance * 2.0f);
        head.pivotZ = HEAD_PIVOT_Z - lunge * 3.0f;
    }
}
