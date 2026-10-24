package com.skibiditoilet.client;

import com.skibiditoilet.client.bbmodel.FastArmoredToiletBbModel;
import com.skibiditoilet.entity.FastArmoredToiletEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * The Fast Armored Toilet as in the series (blockbench/fast_armored_skibidi_toilet.bbmodel): a steel prison
 * toilet with a helmeted head and a gigantic rocket plate behind it. It shakes while it revs up and charges.
 */
public class FastArmoredToiletModel extends SinglePartEntityModel<FastArmoredToiletEntity> {
    private final ModelPart root;
    private final ModelPart shell;

    public FastArmoredToiletModel(ModelPart root) {
        this.root = root;
        this.shell = root.getChild("shell");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        FastArmoredToiletBbModel.addShell(modelData.getRoot());
        return TexturedModelData.of(modelData, FastArmoredToiletBbModel.TEXTURE_WIDTH,
                FastArmoredToiletBbModel.TEXTURE_HEIGHT);
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }

    @Override
    public void setAngles(FastArmoredToiletEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        if (entity.isCharging()) {
            this.shell.pivotX = MathHelper.sin(animationProgress * 2.7f) * 0.4f;
            this.shell.pivotY = MathHelper.cos(animationProgress * 3.1f) * 0.3f;
        } else {
            this.shell.pivotX = 0.0f;
            this.shell.pivotY = 0.0f;
        }
    }
}
