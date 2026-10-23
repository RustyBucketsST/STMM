package com.skibiditoilet.client;

import com.skibiditoilet.entity.FastArmoredToiletEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * The Fast Armored Toilet: a big black armored box with a visor slit, and a raised plate on top.
 * It shakes while it revs up and charges. Ground at y = 24.
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
        ModelPartData root = modelData.getRoot();
        ModelPartData shell = root.addChild("shell", ModelPartBuilder.create().uv(0, 0).cuboid(-8, 10, -8, 16, 14, 16),
                ModelTransform.NONE);
        shell.addChild("plate", ModelPartBuilder.create().uv(0, 30).cuboid(-6, 8, -6, 12, 2, 12), ModelTransform.NONE);
        return TexturedModelData.of(modelData, 64, 64);
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
