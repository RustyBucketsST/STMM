package com.skibiditoilet.client;

import com.skibiditoilet.client.bbmodel.CameramanBbModel;
import com.skibiditoilet.client.bbmodel.SpeakermanBbModel;
import com.skibiditoilet.client.bbmodel.TvManBbModel;
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

import java.util.function.Consumer;

/**
 * A guy in a suit (the standard biped body, textured in rows 16-32 of the 64x64 skin) with a camera,
 * speaker or TV for a head, modelled in blockbench/cameraman.bbmodel etc. (textured in the rest of the skin).
 */
public class AllianceModel extends BipedEntityModel<AllianceEntity> {
    public AllianceModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getCameramanModelData() {
        return withHead(CameramanBbModel::addHead);   // a CCTV camera under a sun hood
    }

    public static TexturedModelData getSpeakermanModelData() {
        return withHead(SpeakermanBbModel::addHead);  // a speaker cabinet with a woofer and tweeter
    }

    public static TexturedModelData getTvManModelData() {
        return withHead(TvManBbModel::addHead);       // a CRT TV, its screen set back inside the bezel
    }

    private static TexturedModelData withHead(Consumer<ModelPartData> addHead) {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0f);
        ModelPartData root = modelData.getRoot();
        addHead.accept(root);
        root.addChild(EntityModelPartNames.HAT, ModelPartBuilder.create(), ModelTransform.NONE);
        return TexturedModelData.of(modelData, 64, 64);
    }
}
