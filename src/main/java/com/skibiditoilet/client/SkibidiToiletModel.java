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
    // Where the head rests, which differs between the toilet and the urinal.
    private final float headPivotY;
    private final float headPivotZ;

    public SkibidiToiletModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.headPivotY = this.head.pivotY;
        this.headPivotZ = this.head.pivotZ;
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        addToilet(modelData.getRoot(), true);
        return TexturedModelData.of(modelData, 64, 64);
    }

    /** The toilet with two jetpack tanks strapped to the back of the cistern (textured at u = 32 and 48, v = 0). */
    public static TexturedModelData getJetpackTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        addToilet(root, true);
        root.addChild("jetpack", ModelPartBuilder.create()
                .uv(32, 0).cuboid(-5, 4, 9, 4, 10, 3)        // tanks
                .uv(32, 0).cuboid(1, 4, 9, 4, 10, 3)
                .uv(48, 0).cuboid(-4, 14, 9.5f, 2, 2, 2)     // nozzles
                .uv(48, 0).cuboid(2, 14, 9.5f, 2, 2, 2), ModelTransform.NONE);
        return TexturedModelData.of(modelData, 64, 64);
    }

    /** The DJ: the toilet with headphones over the head (band textured at u = 32, v = 0; ear cups at v = 4). */
    public static TexturedModelData getDjTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        addToilet(root, true);
        root.getChild("head").addChild("headphones", ModelPartBuilder.create()
                .uv(32, 0).cuboid(-5, -9, -1, 10, 1, 2)       // band over the top
                .uv(32, 4).cuboid(-5, -6, -1.5f, 1, 3, 3)    // ear cups
                .uv(32, 4).cuboid(4, -6, -1.5f, 1, 3, 3), ModelTransform.NONE);
        return TexturedModelData.of(modelData, 64, 64);
    }

    /** The Saint: the toilet with a halo floating over its head (front/back bars at u = 32, v = 0; sides at v = 4). */
    public static TexturedModelData getSaintTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        addToilet(root, true);
        root.getChild("head").addChild("halo", ModelPartBuilder.create()
                .uv(32, 0).cuboid(-4, -11, -4, 8, 1, 1)
                .uv(32, 0).cuboid(-4, -11, 3, 8, 1, 1)
                .uv(32, 4).cuboid(-4, -11, -3, 1, 1, 6)
                .uv(32, 4).cuboid(3, -11, -3, 1, 1, 6), ModelTransform.NONE);
        return TexturedModelData.of(modelData, 64, 64);
    }

    /** The Hat Toilet: a top hat on the head, on a 128-wide texture (brim at u = 64, v = 0; crown at v = 12). */
    public static TexturedModelData getHatTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        addToilet(root, true);
        root.getChild("head").addChild("hat", ModelPartBuilder.create()
                .uv(64, 0).cuboid(-5, -9, -5, 10, 1, 10)
                .uv(64, 12).cuboid(-3, -15, -3, 6, 6, 6), ModelTransform.NONE);
        return TexturedModelData.of(modelData, 128, 64);
    }

    /**
     * A floor-standing urinal: a tall back plate, a bowl on its front, a flush pipe on top, and the head
     * popping out of the bowl.
     */
    public static TexturedModelData getUrinalTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        root.addChild("plate", ModelPartBuilder.create().uv(0, 16).cuboid(-5, 4, 4, 10, 20, 2), ModelTransform.NONE);
        root.addChild("bowl", ModelPartBuilder.create().uv(0, 38).cuboid(-4, 10, -2, 8, 9, 6), ModelTransform.NONE);
        root.addChild("pipe", ModelPartBuilder.create().uv(32, 0).cuboid(-1, 0, 4.5f, 2, 4, 2), ModelTransform.NONE);
        root.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4, -8, -4, 8, 8, 8),
                ModelTransform.pivot(0, 12, -1));
        return TexturedModelData.of(modelData, 64, 64);
    }

    public static void addToilet(ModelPartData root, boolean pedestal) {
        if (pedestal) {
            root.addChild("pedestal", ModelPartBuilder.create().uv(0, 50).cuboid(-3, 18, -3, 6, 6, 7), ModelTransform.NONE);
        }
        root.addChild("bowl", ModelPartBuilder.create().uv(0, 16).cuboid(-5, 12, -6, 10, 6, 11), ModelTransform.NONE);
        root.addChild("tank", ModelPartBuilder.create().uv(0, 33).cuboid(-6, 3, 5, 12, 13, 4), ModelTransform.NONE);
        root.addChild("lid", ModelPartBuilder.create().uv(32, 33).cuboid(-5, 3, 4, 10, 9, 1), ModelTransform.NONE);
        root.addChild("handle", ModelPartBuilder.create().uv(42, 16).cuboid(-7, 5, 6, 1, 1, 2), ModelTransform.NONE);
        root.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4, -8, -4, 8, 8, 8),
                ModelTransform.pivot(0, HEAD_PIVOT_Y, HEAD_PIVOT_Z));
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
        head.pivotY = this.headPivotY - bob * (1.5f + limbDistance * 2.0f);
        head.pivotZ = this.headPivotZ - lunge * 3.0f;

        // Flushed: the head spins faster and faster as it goes down into the bowl.
        float flush = flushProgress(entity, animationProgress);
        head.pivotY += flush * 8.5f;
        head.yaw += flush * flush * 20.0f;
        head.xScale = 1.0f - 0.25f * flush;
        head.zScale = 1.0f - 0.25f * flush;
    }

    /** 0 while alive, rising to 1 as a dying toilet's head goes down the bowl. */
    protected static float flushProgress(SkibidiToiletEntity entity, float animationProgress) {
        if (entity.deathTime <= 0 || !entity.flushesOnDeath()) {
            return 0.0f;
        }
        float partialTick = MathHelper.clamp(animationProgress - entity.age, 0.0f, 1.0f);
        return MathHelper.clamp((entity.deathTime + partialTick) / 18.0f, 0.0f, 1.0f);
    }
}
