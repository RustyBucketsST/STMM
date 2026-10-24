package com.skibiditoilet.client;

import com.skibiditoilet.client.bbmodel.CameramanBbModel;
import com.skibiditoilet.client.bbmodel.LargeCameramanBbModel;
import com.skibiditoilet.client.bbmodel.LargeSpeakermanBbModel;
import com.skibiditoilet.client.bbmodel.LargeTvManBbModel;
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

    private static TexturedModelData withHead(Consumer<ModelPartData> addHead) {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0f);
        ModelPartData root = modelData.getRoot();
        addHead.accept(root);
        root.addChild(EntityModelPartNames.HAT, ModelPartBuilder.create(), ModelTransform.NONE);
        return TexturedModelData.of(modelData, 64, 64);
    }
}
