package com.skibiditoilet.client;

import com.skibiditoilet.client.bbmodel.TitanCameraman2BbModel;
import com.skibiditoilet.client.bbmodel.TitanCameramanBbModel;
import com.skibiditoilet.client.bbmodel.TitanSpeakerman2BbModel;
import com.skibiditoilet.client.bbmodel.TitanSpeakermanBbModel;
import com.skibiditoilet.client.bbmodel.TitanTvMan2BbModel;
import com.skibiditoilet.client.bbmodel.TitanTvManBbModel;
import com.skibiditoilet.entity.AllianceEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelPartNames;

import java.util.List;
import java.util.function.Consumer;

/**
 * A Titan, modelled in blockbench/titan_*.bbmodel at twice the detail of a normal mob and drawn at half the
 * Titan scale, so its texture is as fine as any other mob's. It keeps the biped parts and animation: the biped
 * code sets the standard pivots (head at y 0, legs at y 12, ...), so they're converted to the model's units,
 * in which every pivot is doubled and the feet still stand at y = 24.
 */
public class TitanModel extends AllianceModel {
    /** Model units per pixel. */
    public static final float DETAIL = 2.0f;
    private static final float RAISE = 24.0f * (DETAIL - 1.0f);

    private final List<ModelPart> parts;

    public TitanModel(ModelPart root) {
        super(root);
        this.parts = List.of(this.head, this.hat, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg);
    }

    /** Titan Cameraman 1.0: camera head, blue core, black coat, shoulder cameras, riot shield and jetpack. */
    public static TexturedModelData getCameramanModelData() {
        return build(TitanCameramanBbModel.TEXTURE_WIDTH, TitanCameramanBbModel.TEXTURE_HEIGHT,
                TitanCameramanBbModel::addHead, TitanCameramanBbModel::addBody, TitanCameramanBbModel::addRightArm,
                TitanCameramanBbModel::addLeftArm, TitanCameramanBbModel::addRightLeg, TitanCameramanBbModel::addLeftLeg);
    }

    /** Titan Speakerman 1.0: speaker head, red core, red shirt, speaker towers, jetpack and an arm cannon. */
    public static TexturedModelData getSpeakermanModelData() {
        return build(TitanSpeakermanBbModel.TEXTURE_WIDTH, TitanSpeakermanBbModel.TEXTURE_HEIGHT,
                TitanSpeakermanBbModel::addHead, TitanSpeakermanBbModel::addBody, TitanSpeakermanBbModel::addRightArm,
                TitanSpeakermanBbModel::addLeftArm, TitanSpeakermanBbModel::addRightLeg, TitanSpeakermanBbModel::addLeftLeg);
    }

    /** Titan TV Man 1.0: a TV head showing static, purple core, black coat, claws on his back, elbow hooks. */
    public static TexturedModelData getTvManModelData() {
        return build(TitanTvManBbModel.TEXTURE_WIDTH, TitanTvManBbModel.TEXTURE_HEIGHT,
                TitanTvManBbModel::addHead, TitanTvManBbModel::addBody, TitanTvManBbModel::addRightArm,
                TitanTvManBbModel::addLeftArm, TitanTvManBbModel::addRightLeg, TitanTvManBbModel::addLeftLeg);
    }

    /**
     * Titan Cameraman 2.0 (players' Titan 2.0 form): cyan lens and core, antenna and fin, camcorders, grey shoulder
     * cameras, a silver winged jetpack, a railgun and a hammer, shin plates.
     */
    public static TexturedModelData getCameraman2ModelData() {
        return build(TitanCameraman2BbModel.TEXTURE_WIDTH, TitanCameraman2BbModel.TEXTURE_HEIGHT,
                TitanCameraman2BbModel::addHead, TitanCameraman2BbModel::addBody, TitanCameraman2BbModel::addRightArm,
                TitanCameraman2BbModel::addLeftArm, TitanCameraman2BbModel::addRightLeg, TitanCameraman2BbModel::addLeftLeg);
    }

    /** Titan Speakerman 2.0: a helmet over his speakers, red plated towers, a huge jetpack, cannons on both arms. */
    public static TexturedModelData getSpeakerman2ModelData() {
        return build(TitanSpeakerman2BbModel.TEXTURE_WIDTH, TitanSpeakerman2BbModel.TEXTURE_HEIGHT,
                TitanSpeakerman2BbModel::addHead, TitanSpeakerman2BbModel::addBody, TitanSpeakerman2BbModel::addRightArm,
                TitanSpeakerman2BbModel::addLeftArm, TitanSpeakerman2BbModel::addRightLeg, TitanSpeakerman2BbModel::addLeftLeg);
    }

    /** Titan TV Man 2.0: a wide flat TV with a crown, shoulder TVs, a big purple core, an energy sword. */
    public static TexturedModelData getTvMan2ModelData() {
        return build(TitanTvMan2BbModel.TEXTURE_WIDTH, TitanTvMan2BbModel.TEXTURE_HEIGHT,
                TitanTvMan2BbModel::addHead, TitanTvMan2BbModel::addBody, TitanTvMan2BbModel::addRightArm,
                TitanTvMan2BbModel::addLeftArm, TitanTvMan2BbModel::addRightLeg, TitanTvMan2BbModel::addLeftLeg);
    }

    @SafeVarargs
    private static TexturedModelData build(int textureWidth, int textureHeight, Consumer<ModelPartData>... parts) {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        for (Consumer<ModelPartData> part : parts) {
            part.accept(root);
        }
        root.addChild(EntityModelPartNames.HAT, ModelPartBuilder.create(), ModelTransform.pivot(0, -RAISE, 0));
        return TexturedModelData.of(modelData, textureWidth, textureHeight);
    }

    @Override
    public void setAngles(AllianceEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        toStandard(this.parts, DETAIL);
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        fromStandard(this.parts, DETAIL);
    }

    /** Moves the pivots of a model built at {@code detail} units per pixel to the standard biped units... */
    public static void toStandard(List<ModelPart> parts, float detail) {
        float raise = 24.0f * (detail - 1.0f);
        for (ModelPart part : parts) {
            part.setPivot(part.pivotX / detail, (part.pivotY + raise) / detail, part.pivotZ / detail);
        }
    }

    /** ...and back, once the biped animation has set them. */
    public static void fromStandard(List<ModelPart> parts, float detail) {
        float raise = 24.0f * (detail - 1.0f);
        for (ModelPart part : parts) {
            part.setPivot(part.pivotX * detail, part.pivotY * detail - raise, part.pivotZ * detail);
        }
    }
}
