package com.skibiditoilet.client;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.entity.AllianceEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Renders Alliance members and, scaled up, their Titan versions. */
public class AllianceRenderer extends MobEntityRenderer<AllianceEntity, AllianceModel> {
    private final Identifier texture;
    private final float scale;

    public AllianceRenderer(EntityRendererFactory.Context context, EntityModelLayer layer, Identifier texture, float scale) {
        super(context, new AllianceModel(context.getPart(layer)), 0.5f * scale);
        this.texture = texture;
        this.scale = scale;
    }

    public static EntityRendererFactory<AllianceEntity> factory(EntityModelLayer layer, String textureName, float scale) {
        Identifier texture = SkibidiToiletMod.id("textures/entity/" + textureName + ".png");
        return context -> new AllianceRenderer(context, layer, texture, scale);
    }

    @Override
    protected void scale(AllianceEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(this.scale, this.scale, this.scale);
    }

    @Override
    public Identifier getTexture(AllianceEntity entity) {
        return this.texture;
    }
}
