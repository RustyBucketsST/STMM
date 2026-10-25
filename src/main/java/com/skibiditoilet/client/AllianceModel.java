package com.skibiditoilet.client;

import com.skibiditoilet.client.bbmodel.CameramanBbModel;
import com.skibiditoilet.client.bbmodel.LargeCameramanBbModel;
import com.skibiditoilet.client.bbmodel.LargeSpeakermanBbModel;
import com.skibiditoilet.client.bbmodel.LargeTvManBbModel;
import com.skibiditoilet.client.bbmodel.SpeakermanBbModel;
import com.skibiditoilet.client.bbmodel.TvManBbModel;
import com.skibiditoilet.entity.AllianceEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;

import java.util.List;
import java.util.function.Consumer;

/**
 * An Alliance member: a biped modelled in blockbench/*.bbmodel. Most are built at twice the detail of a normal mob
 * ({@link #DETAILED}), so their textures are finer, and drawn at half the size. They keep the biped parts and
 * animation: the biped code sets the standard pivots (head at y 0, legs at y 12, ...), so they're converted to the
 * model's units, in which every pivot is scaled up and the feet still stand at y = 24.
 */
public class AllianceModel extends BipedEntityModel<AllianceEntity> {
    /** Model units per pixel of the detailed models. */
    public static final float DETAILED = 2.0f;

    private final float detail;
    private final List<ModelPart> parts;

    public AllianceModel(ModelPart root) {
        this(root, 1.0f);
    }

    public AllianceModel(ModelPart root, float detail) {
        super(root);
        this.detail = detail;
        this.parts = List.of(this.head, this.hat, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg);
    }

    /** Cameraman 1.0 (blockbench/cameraman.bbmodel, detailed): a white CCTV camera with a sun shield, a black suit. */
    public static TexturedModelData getCameramanModelData() {
        return detailed(CameramanBbModel.TEXTURE_WIDTH, CameramanBbModel.TEXTURE_HEIGHT,
                CameramanBbModel::addHead, CameramanBbModel::addBody, CameramanBbModel::addRightArm,
                CameramanBbModel::addLeftArm, CameramanBbModel::addRightLeg, CameramanBbModel::addLeftLeg);
    }

    /** Speakerman 1.0 (speakerman.bbmodel, detailed): a grey speaker cabinet with a big woofer, a black suit. */
    public static TexturedModelData getSpeakermanModelData() {
        return detailed(SpeakermanBbModel.TEXTURE_WIDTH, SpeakermanBbModel.TEXTURE_HEIGHT,
                SpeakermanBbModel::addHead, SpeakermanBbModel::addBody, SpeakermanBbModel::addRightArm,
                SpeakermanBbModel::addLeftArm, SpeakermanBbModel::addRightLeg, SpeakermanBbModel::addLeftLeg);
    }

    /** TV Man 1.0 (tv_man.bbmodel, detailed): a hooded CRT TV full of static, a grey double-breasted overcoat. */
    public static TexturedModelData getTvManModelData() {
        return detailed(TvManBbModel.TEXTURE_WIDTH, TvManBbModel.TEXTURE_HEIGHT,
                TvManBbModel::addHead, TvManBbModel::addBody, TvManBbModel::addRightArm,
                TvManBbModel::addLeftArm, TvManBbModel::addRightLeg, TvManBbModel::addLeftLeg);
    }

    /** The Large Cameraman: a long black coat and a black camcorder head (blockbench/large_cameraman.bbmodel). */
    public static TexturedModelData getLargeCameramanModelData() {
        return fullBody(LargeCameramanBbModel.TEXTURE_WIDTH, LargeCameramanBbModel.TEXTURE_HEIGHT,
                LargeCameramanBbModel::addHead, LargeCameramanBbModel::addBody, LargeCameramanBbModel::addRightArm,
                LargeCameramanBbModel::addLeftArm, LargeCameramanBbModel::addRightLeg, LargeCameramanBbModel::addLeftLeg);
    }

    /** The Large Speakerman: a white shirt and red tie, and a tall speaker head (large_speakerman.bbmodel). */
    public static TexturedModelData getLargeSpeakermanModelData() {
        return fullBody(LargeSpeakermanBbModel.TEXTURE_WIDTH, LargeSpeakermanBbModel.TEXTURE_HEIGHT,
                LargeSpeakermanBbModel::addHead, LargeSpeakermanBbModel::addBody, LargeSpeakermanBbModel::addRightArm,
                LargeSpeakermanBbModel::addLeftArm, LargeSpeakermanBbModel::addRightLeg, LargeSpeakermanBbModel::addLeftLeg);
    }

    /**
     * Large TV Man 1.0: a big static-filled CRT on a pipe, a black suit with a purple tie, and four little TVs on
     * tendrils from his back (large_tv_man.bbmodel). Only players in the Large form use it so far.
     */
    public static TexturedModelData getLargeTvManModelData() {
        return fullBody(LargeTvManBbModel.TEXTURE_WIDTH, LargeTvManBbModel.TEXTURE_HEIGHT,
                LargeTvManBbModel::addHead, LargeTvManBbModel::addBody, LargeTvManBbModel::addRightArm,
                LargeTvManBbModel::addLeftArm, LargeTvManBbModel::addRightLeg, LargeTvManBbModel::addLeftLeg);
    }

    /** A whole body from a .bbmodel whose groups are the biped parts at the standard pivots. */
    @SafeVarargs
    private static TexturedModelData fullBody(int textureWidth, int textureHeight, Consumer<ModelPartData>... parts) {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        for (Consumer<ModelPartData> part : parts) {
            part.accept(root);
        }
        root.addChild(EntityModelPartNames.HAT, ModelPartBuilder.create(), ModelTransform.NONE);
        return TexturedModelData.of(modelData, textureWidth, textureHeight);
    }

    /** A whole body from a detailed .bbmodel whose groups are the biped parts at doubled pivots, feet on the ground. */
    @SafeVarargs
    protected static TexturedModelData detailed(int textureWidth, int textureHeight, Consumer<ModelPartData>... parts) {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        for (Consumer<ModelPartData> part : parts) {
            part.accept(root);
        }
        root.addChild(EntityModelPartNames.HAT, ModelPartBuilder.create(), ModelTransform.pivot(0, -raise(DETAILED), 0));
        return TexturedModelData.of(modelData, textureWidth, textureHeight);
    }

    /** How far up the detailed models' pivots move, so that their feet stay on the ground. */
    private static float raise(float detail) {
        return 24.0f * (detail - 1.0f);
    }

    @Override
    public void setAngles(AllianceEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        if (this.detail == 1.0f) {
            super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
            return;
        }
        toStandard(this.parts, this.detail);
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        fromStandard(this.parts, this.detail);
    }

    /** Moves the pivots of a model built at {@code detail} units per pixel to the standard biped units... */
    public static void toStandard(List<ModelPart> parts, float detail) {
        float raise = raise(detail);
        for (ModelPart part : parts) {
            part.setPivot(part.pivotX / detail, (part.pivotY + raise) / detail, part.pivotZ / detail);
        }
    }

    /** ...and back, once the biped animation has set them. */
    public static void fromStandard(List<ModelPart> parts, float detail) {
        float raise = raise(detail);
        for (ModelPart part : parts) {
            part.setPivot(part.pivotX * detail, part.pivotY * detail - raise, part.pivotZ * detail);
        }
    }
}
