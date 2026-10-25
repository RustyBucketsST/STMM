package com.skibiditoilet.client;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.entity.AllianceEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Renders Alliance members and, scaled up, their Titan versions. */
public class AllianceRenderer extends MobEntityRenderer<AllianceEntity, AllianceModel> {
    private final Identifier texture;
    private final float scale;


    private AllianceRenderer(EntityRendererFactory.Context context, AllianceModel model, Identifier texture,
                             float scale, float shadowRadius) {
        super(context, model, shadowRadius);
        this.texture = texture;
        this.scale = scale;
    }

    /** An Alliance member drawn {@code scale} times normal size, with a model built at {@code detail} units per pixel. */
    public static EntityRendererFactory<AllianceEntity> factory(EntityModelLayer layer, String textureName, float scale,
                                                               float detail) {
        Identifier texture = SkibidiToiletMod.id("textures/entity/" + textureName + ".png");
        return context -> new AllianceRenderer(context, new AllianceModel(context.getPart(layer), detail), texture,
                scale / detail, 0.5f * scale);
    }

    /**
     * A Titan with its own detailed model (see TitanModel), drawn {@code scale} times a normal Alliance member,
     * with the glowing parts of textures/entity/NAME_glow.png drawn full-bright on top.
     */
    public static EntityRendererFactory<AllianceEntity> titanFactory(EntityModelLayer layer, String textureName,
                                                                    float scale) {
        Identifier texture = SkibidiToiletMod.id("textures/entity/" + textureName + ".png");
        RenderLayer glow = RenderLayer.getEyes(SkibidiToiletMod.id("textures/entity/" + textureName + "_glow.png"));
        return context -> {
            AllianceRenderer renderer = new AllianceRenderer(context, new TitanModel(context.getPart(layer)), texture,
                    scale / TitanModel.DETAIL, 0.5f * scale);
            renderer.addFeature(new EyesFeatureRenderer<AllianceEntity, AllianceModel>(renderer) {
                @Override
                public RenderLayer getEyesTexture() {
                    return glow;
                }
            });
            return renderer;
        };
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
