package com.skibiditoilet.client;

import com.skibiditoilet.entity.SkibidiToiletEntity;
import com.skibiditoilet.entity.TripleToiletEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.util.math.MathHelper;

/**
 * The Triple Skibidi Toilet: the toilet with two more heads on long necks to the left and right, each
 * with its own face (128-wide texture: middle face at u = 0, left at 64, right at 96, neck at u = 64, v = 16).
 * Side heads disappear as they get flushed: the left one first, then the right one.
 */
public class TripleToiletModel extends SkibidiToiletModel {
    private static final float SIDE_X = 8.5f;
    private static final float SIDE_Y = 10.0f;
    private static final float SIDE_ROLL = 0.3f;

    private final ModelPart left;
    private final ModelPart right;

    public TripleToiletModel(ModelPart root) {
        super(root);
        this.left = root.getChild("head_left");
        this.right = root.getChild("head_right");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        addToilet(root, true);
        addSideHead(root, "head_left", 64, -SIDE_X);
        addSideHead(root, "head_right", 96, SIDE_X);
        return TexturedModelData.of(modelData, 128, 64);
    }

    private static void addSideHead(ModelPartData root, String name, int faceU, float x) {
        ModelPartData head = root.addChild(name, ModelPartBuilder.create().uv(faceU, 0).cuboid(-4, -8, -4, 8, 8, 8),
                ModelTransform.pivot(x, SIDE_Y, -1.0f));
        // The neck hangs from the bottom of the head, angled back in toward the bowl.
        head.addChild("neck", ModelPartBuilder.create().uv(64, 16).cuboid(-1.5f, 0, -1.5f, 3, 8, 3),
                ModelTransform.of(0, 0, 0, 0, 0, x < 0 ? -0.6f : 0.6f));
    }

    @Override
    public void setAngles(SkibidiToiletEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        int heads = TripleToiletEntity.heads(entity);
        this.left.visible = heads >= 3;
        this.right.visible = heads >= 2;
        // Each side head bobs to its own beat, leaning outward.
        float flush = flushProgress(entity, animationProgress);
        animateSide(this.left, -1, animationProgress + 7.0f, headYaw, headPitch, limbDistance, flush);
        animateSide(this.right, 1, animationProgress + 13.0f, headYaw, headPitch, limbDistance, flush);
    }

    private static void animateSide(ModelPart head, int side, float progress, float headYaw, float headPitch,
                                    float limbDistance, float flush) {
        float bob = Math.abs(MathHelper.sin(progress * 0.35f));
        head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE + side * 0.25f + flush * flush * 20.0f * side;
        head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
        head.roll = side * SIDE_ROLL * (1.0f - flush) + MathHelper.sin(progress * 0.2f) * 0.15f;
        head.pivotY = SIDE_Y - bob * (1.5f + limbDistance * 2.0f) + flush * 10.0f;
    }
}
