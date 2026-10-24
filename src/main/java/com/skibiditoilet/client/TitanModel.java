package com.skibiditoilet.client;

import com.skibiditoilet.client.bbmodel.TitanCameramanBbModel;
import com.skibiditoilet.client.bbmodel.TitanSpeakermanBbModel;
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
        for (ModelPart part : this.parts) {
            part.setPivot(part.pivotX / DETAIL, (part.pivotY + RAISE) / DETAIL, part.pivotZ / DETAIL);
        }
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        for (ModelPart part : this.parts) {
            part.setPivot(part.pivotX * DETAIL, part.pivotY * DETAIL - RAISE, part.pivotZ * DETAIL);
        }
    }
}
