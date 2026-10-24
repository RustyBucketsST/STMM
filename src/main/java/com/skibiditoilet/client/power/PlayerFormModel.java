package com.skibiditoilet.client.power;

import com.skibiditoilet.client.TitanModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;

import java.util.List;

/**
 * A player in a form, drawn with the model of the Alliance mob the form is (a Large member's or a Titan's) and
 * animated like a player. Titan models are built at twice the detail; their pivots are converted around the biped
 * animation as in TitanModel.
 */
public class PlayerFormModel extends BipedEntityModel<AbstractClientPlayerEntity> {
    private final float detail;
    private final List<ModelPart> parts;

    public PlayerFormModel(ModelPart root, float detail) {
        super(root);
        this.detail = detail;
        this.parts = List.of(this.head, this.hat, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg);
    }

    /** Model units per pixel. */
    public float detail() {
        return this.detail;
    }

    @Override
    public void setAngles(AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        if (this.detail != 1.0f) {
            TitanModel.toStandard(this.parts, this.detail);
        }
        super.setAngles(player, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        if (this.detail != 1.0f) {
            TitanModel.fromStandard(this.parts, this.detail);
        }
    }
}
