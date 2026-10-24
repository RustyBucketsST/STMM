package com.skibiditoilet.client;

import com.skibiditoilet.client.bbmodel.TripleSkibidiToiletBbModel;
import com.skibiditoilet.entity.SkibidiToiletEntity;
import com.skibiditoilet.entity.TripleToiletEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.util.math.MathHelper;

/**
 * The Triple Skibidi Toilet as in the series (blockbench/triple_skibidi_toilet.bbmodel): two toilets joined
 * side by side on a platform, each with a head, and a third toilet behind them whose horned head ("head")
 * rises above the others. The front heads disappear as they get flushed: the left one first, then the right.
 */
public class TripleToiletModel extends SkibidiToiletModel {
    private final ModelPart left;
    private final ModelPart right;
    private final float sideY;

    public TripleToiletModel(ModelPart root) {
        super(root);
        this.left = root.getChild("head_left");
        this.right = root.getChild("head_right");
        this.sideY = this.left.pivotY;
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        TripleSkibidiToiletBbModel.addBody(root);
        TripleSkibidiToiletBbModel.addHead(root);
        TripleSkibidiToiletBbModel.addHeadLeft(root);
        TripleSkibidiToiletBbModel.addHeadRight(root);
        return TexturedModelData.of(modelData, TripleSkibidiToiletBbModel.TEXTURE_WIDTH,
                TripleSkibidiToiletBbModel.TEXTURE_HEIGHT);
    }

    @Override
    public void setAngles(SkibidiToiletEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        int heads = TripleToiletEntity.heads(entity);
        this.left.visible = heads >= 3;
        this.right.visible = heads >= 2;
        // Each front head bobs to its own beat.
        float flush = flushProgress(entity, animationProgress);
        animateSide(this.left, animationProgress + 7.0f, headYaw, headPitch, limbDistance, flush);
        animateSide(this.right, animationProgress + 13.0f, headYaw, headPitch, limbDistance, flush);
    }

    private void animateSide(ModelPart head, float progress, float headYaw, float headPitch,
                             float limbDistance, float flush) {
        float bob = Math.abs(MathHelper.sin(progress * 0.35f));
        head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE + flush * flush * 20.0f;
        head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
        head.roll = MathHelper.sin(progress * 0.2f) * 0.15f;
        head.pivotY = this.sideY - bob * (1.5f + limbDistance * 2.0f) + flush * 8.5f;
    }
}
