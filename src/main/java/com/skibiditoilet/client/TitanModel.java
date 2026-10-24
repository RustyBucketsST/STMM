package com.skibiditoilet.client;

import com.skibiditoilet.client.bbmodel.TitanCameramanBbModel;
import com.skibiditoilet.entity.AllianceEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelPartNames;

import java.util.List;

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

    /** A giant in a long coat with a camera cluster for a head, a glowing core and thrusters on its back. */
    public static TexturedModelData getCameramanModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        TitanCameramanBbModel.addHead(root);
        TitanCameramanBbModel.addBody(root);
        TitanCameramanBbModel.addRightArm(root);
        TitanCameramanBbModel.addLeftArm(root);
        TitanCameramanBbModel.addRightLeg(root);
        TitanCameramanBbModel.addLeftLeg(root);
        root.addChild(EntityModelPartNames.HAT, ModelPartBuilder.create(), ModelTransform.pivot(0, -RAISE, 0));
        return TexturedModelData.of(modelData, TitanCameramanBbModel.TEXTURE_WIDTH, TitanCameramanBbModel.TEXTURE_HEIGHT);
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
