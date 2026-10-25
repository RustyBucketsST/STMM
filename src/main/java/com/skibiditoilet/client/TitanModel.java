package com.skibiditoilet.client;

import com.skibiditoilet.client.bbmodel.TitanCameraman2BbModel;
import com.skibiditoilet.client.bbmodel.TitanCameramanBbModel;
import com.skibiditoilet.client.bbmodel.TitanSpeakerman2BbModel;
import com.skibiditoilet.client.bbmodel.TitanSpeakermanBbModel;
import com.skibiditoilet.client.bbmodel.TitanTvMan2BbModel;
import com.skibiditoilet.client.bbmodel.TitanTvManBbModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;

/**
 * A Titan, modelled in blockbench/titan_*.bbmodel. The 1.0 Titans (the mobs and the Titan form) are built at four
 * times the detail of a normal mob and drawn at a quarter of the Titan scale, so their textures stay fine at their
 * size; the 2.0s (only the Titan 2.0 form) at twice, like the other Alliance members.
 */
public class TitanModel extends AllianceModel {
    /** Model units per pixel of the 1.0 Titans. */
    public static final float DETAIL = 4.0f;
    /** Model units per pixel of the 2.0 Titans. */
    public static final float DETAIL_2 = AllianceModel.DETAILED;

    public TitanModel(ModelPart root) {
        super(root, DETAIL);
    }

    /** Titan Cameraman 1.0: camera head, blue core, black coat, shoulder cameras, riot shield and jetpack. */
    public static TexturedModelData getCameramanModelData() {
        return detailedAt(DETAIL, TitanCameramanBbModel.TEXTURE_WIDTH, TitanCameramanBbModel.TEXTURE_HEIGHT,
                TitanCameramanBbModel::addHead, TitanCameramanBbModel::addBody, TitanCameramanBbModel::addRightArm,
                TitanCameramanBbModel::addLeftArm, TitanCameramanBbModel::addRightLeg, TitanCameramanBbModel::addLeftLeg);
    }

    /** Titan Speakerman 1.0: speaker head, red core, red shirt, speaker towers, jetpack and an arm cannon. */
    public static TexturedModelData getSpeakermanModelData() {
        return detailedAt(DETAIL, TitanSpeakermanBbModel.TEXTURE_WIDTH, TitanSpeakermanBbModel.TEXTURE_HEIGHT,
                TitanSpeakermanBbModel::addHead, TitanSpeakermanBbModel::addBody, TitanSpeakermanBbModel::addRightArm,
                TitanSpeakermanBbModel::addLeftArm, TitanSpeakermanBbModel::addRightLeg, TitanSpeakermanBbModel::addLeftLeg);
    }

    /** Titan TV Man 1.0: a TV head showing static, purple core, black coat, claws on his back, elbow hooks. */
    public static TexturedModelData getTvManModelData() {
        return detailedAt(DETAIL, TitanTvManBbModel.TEXTURE_WIDTH, TitanTvManBbModel.TEXTURE_HEIGHT,
                TitanTvManBbModel::addHead, TitanTvManBbModel::addBody, TitanTvManBbModel::addRightArm,
                TitanTvManBbModel::addLeftArm, TitanTvManBbModel::addRightLeg, TitanTvManBbModel::addLeftLeg);
    }

    /**
     * Titan Cameraman 2.0 (players' Titan 2.0 form): cyan lens and core, antenna and fin, camcorders, grey shoulder
     * cameras, a silver winged jetpack, a railgun and a hammer, shin plates.
     */
    public static TexturedModelData getCameraman2ModelData() {
        return detailed(TitanCameraman2BbModel.TEXTURE_WIDTH, TitanCameraman2BbModel.TEXTURE_HEIGHT,
                TitanCameraman2BbModel::addHead, TitanCameraman2BbModel::addBody, TitanCameraman2BbModel::addRightArm,
                TitanCameraman2BbModel::addLeftArm, TitanCameraman2BbModel::addRightLeg, TitanCameraman2BbModel::addLeftLeg);
    }

    /** Titan Speakerman 2.0: a helmet over his speakers, red plated towers, a huge jetpack, cannons on both arms. */
    public static TexturedModelData getSpeakerman2ModelData() {
        return detailed(TitanSpeakerman2BbModel.TEXTURE_WIDTH, TitanSpeakerman2BbModel.TEXTURE_HEIGHT,
                TitanSpeakerman2BbModel::addHead, TitanSpeakerman2BbModel::addBody, TitanSpeakerman2BbModel::addRightArm,
                TitanSpeakerman2BbModel::addLeftArm, TitanSpeakerman2BbModel::addRightLeg, TitanSpeakerman2BbModel::addLeftLeg);
    }

    /** Titan TV Man 2.0: a wide flat TV with a crown, shoulder TVs, a big purple core, an energy sword. */
    public static TexturedModelData getTvMan2ModelData() {
        return detailed(TitanTvMan2BbModel.TEXTURE_WIDTH, TitanTvMan2BbModel.TEXTURE_HEIGHT,
                TitanTvMan2BbModel::addHead, TitanTvMan2BbModel::addBody, TitanTvMan2BbModel::addRightArm,
                TitanTvMan2BbModel::addLeftArm, TitanTvMan2BbModel::addRightLeg, TitanTvMan2BbModel::addLeftLeg);
    }
}
