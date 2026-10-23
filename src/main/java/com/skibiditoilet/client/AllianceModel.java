package com.skibiditoilet.client;

import com.skibiditoilet.entity.AllianceEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;

/**
 * A guy in a suit (the standard biped body, textured in the top half of the 64x64 skin)
 * with a camera, speaker or TV for a head (textured from v = 32 down).
 */
public class AllianceModel extends BipedEntityModel<AllianceEntity> {
    public AllianceModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getCameramanModelData() {
        return withHead(ModelPartBuilder.create()
                .uv(0, 32).cuboid(-3, -6, -5, 6, 6, 10)   // camera body
                .uv(32, 32).cuboid(-2, -5, -7, 4, 4, 2)); // lens
    }

    public static TexturedModelData getSpeakermanModelData() {
        return withHead(ModelPartBuilder.create()
                .uv(0, 32).cuboid(-5, -10, -4, 10, 10, 8)); // speaker cabinet
    }

    public static TexturedModelData getTvManModelData() {
        return withHead(ModelPartBuilder.create()
                .uv(0, 32).cuboid(-6, -9, -3.5f, 12, 9, 7) // TV set
                .uv(40, 32).cuboid(-3, -12, 0, 1, 3, 1)    // antennas
                .uv(40, 32).cuboid(2, -12, 0, 1, 3, 1));
    }

    private static TexturedModelData withHead(ModelPartBuilder head) {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0f);
        ModelPartData root = modelData.getRoot();
        root.addChild(EntityModelPartNames.HEAD, head, ModelTransform.NONE);
        root.addChild(EntityModelPartNames.HAT, ModelPartBuilder.create(), ModelTransform.NONE);
        return TexturedModelData.of(modelData, 64, 64);
    }
}
