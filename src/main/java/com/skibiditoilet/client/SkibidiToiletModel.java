package com.skibiditoilet.client;

import com.skibiditoilet.client.bbmodel.SkibidiToiletBbModel;
import com.skibiditoilet.client.bbmodel.SkibidiUrinalBbModel;
import com.skibiditoilet.entity.SkibidiToiletEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * A toilet facing -Z (the mob's front) with a head popping out of the bowl, the ground at y = 24. Most toilets are
 * modelled at twice the detail of a normal mob ({@link #DETAILED} units per pixel, for finer textures) and drawn at
 * that fraction of their size, so the head's bob and flush, which are measured in normal pixels, are scaled to match.
 */
public class SkibidiToiletModel extends SinglePartEntityModel<SkibidiToiletEntity> {
    /** Model units per pixel of the detailed toilets. */
    public static final float DETAILED = 2.0f;

    private final ModelPart root;
    protected final ModelPart head;
    // Where the head rests, which differs between the toilet and the urinal.
    protected final float headPivotY;
    protected final float headPivotZ;
    private final float detail;

    public SkibidiToiletModel(ModelPart root) {
        this(root, 1.0f);
    }

    public SkibidiToiletModel(ModelPart root, float detail) {
        this.root = root;
        this.head = root.getChild("head");
        this.headPivotY = this.head.pivotY;
        this.headPivotZ = this.head.pivotZ;
        this.detail = detail;
    }

    /** A toilet built at {@link #DETAILED} units per pixel, like the one in skibidi_toilet.bbmodel. */
    public static SkibidiToiletModel detailed(ModelPart root) {
        return new SkibidiToiletModel(root, DETAILED);
    }

    /** Model units per pixel. */
    public float detail() {
        return this.detail;
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        addToilet(modelData.getRoot(), true);
        return textured(modelData);
    }

    /** The toilet with two jetpack tanks strapped to the back of the cistern. */
    public static TexturedModelData getJetpackTexturedModelData() {
        ModelData modelData = new ModelData();
        addToilet(modelData.getRoot(), true);
        SkibidiToiletBbModel.addJetpack(modelData.getRoot());
        return textured(modelData);
    }

    /** The DJ: the toilet with headphones over the head. */
    public static TexturedModelData getDjTexturedModelData() {
        ModelData modelData = new ModelData();
        addToilet(modelData.getRoot(), true);
        SkibidiToiletBbModel.addHeadphones(modelData.getRoot().getChild("head"));
        return textured(modelData);
    }

    /** The Saint: the toilet with a halo floating over its head. */
    public static TexturedModelData getSaintTexturedModelData() {
        ModelData modelData = new ModelData();
        addToilet(modelData.getRoot(), true);
        SkibidiToiletBbModel.addHalo(modelData.getRoot().getChild("head"));
        return textured(modelData);
    }

    /** The Hat Toilet: a top hat on the head. */
    public static TexturedModelData getHatTexturedModelData() {
        ModelData modelData = new ModelData();
        addToilet(modelData.getRoot(), true);
        SkibidiToiletBbModel.addHat(modelData.getRoot().getChild("head"));
        return textured(modelData);
    }

    /**
     * A floor-standing urinal: a tall shell with side wings, a bowl on its front, a flush pipe on top, and the
     * head popping out of the bowl.
     */
    public static TexturedModelData getUrinalTexturedModelData() {
        ModelData modelData = new ModelData();
        SkibidiUrinalBbModel.addUrinal(modelData.getRoot());
        SkibidiUrinalBbModel.addHead(modelData.getRoot());
        return TexturedModelData.of(modelData, SkibidiUrinalBbModel.TEXTURE_WIDTH, SkibidiUrinalBbModel.TEXTURE_HEIGHT);
    }

    /**
     * The toilet from blockbench/skibidi_toilet.bbmodel: bowl, seat, open lid, tank and the head (part "head").
     * The spider toilet leaves out the pedestal and stands on legs instead.
     */
    public static void addToilet(ModelPartData root, boolean pedestal) {
        if (pedestal) {
            SkibidiToiletBbModel.addPedestal(root);
        }
        SkibidiToiletBbModel.addToilet(root);
        SkibidiToiletBbModel.addHead(root);
    }

    /** Textures for every toilet on this model share one layout (see tools/gen_texture.py). */
    public static TexturedModelData textured(ModelData modelData) {
        return TexturedModelData.of(modelData, SkibidiToiletBbModel.TEXTURE_WIDTH, SkibidiToiletBbModel.TEXTURE_HEIGHT);
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
        head.pivotY = this.headPivotY + (head.pivotY - this.headPivotY) * this.detail;
        head.pivotZ = this.headPivotZ + (head.pivotZ - this.headPivotZ) * this.detail;
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
