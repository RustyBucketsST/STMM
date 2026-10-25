package com.skibiditoilet.client;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.entity.SkibidiToiletEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Renders every kind of Skibidi Toilet: a toilet model, a texture, and an optional scale. A toilet can
 * also switch to a second texture, like the Saint Toilet when it turns demonic. A model built at a higher detail
 * (see SkibidiToiletModel) is drawn smaller to match, so {@code scale} is always relative to a normal toilet.
 */
public class SkibidiToiletRenderer extends MobEntityRenderer<SkibidiToiletEntity, SkibidiToiletModel> {
    private final Function<SkibidiToiletEntity, Identifier> texture;
    private final float scale;

    private SkibidiToiletRenderer(EntityRendererFactory.Context context, SkibidiToiletModel model,
                                  Function<SkibidiToiletEntity, Identifier> texture, float scale) {
        super(context, model, 0.5f * scale);
        this.texture = texture;
        this.scale = scale / model.detail();
    }

    public static EntityRendererFactory<SkibidiToiletEntity> factory(EntityModelLayer layer,
                                                                    Function<ModelPart, SkibidiToiletModel> model,
                                                                    String textureName, float scale) {
        Identifier texture = texture(textureName);
        return context -> new SkibidiToiletRenderer(context, model.apply(context.getPart(layer)), toilet -> texture, scale);
    }

    /** Uses {@code alternateName} instead of {@code textureName} while {@code useAlternate} holds. */
    public static EntityRendererFactory<SkibidiToiletEntity> factory(EntityModelLayer layer,
                                                                    Function<ModelPart, SkibidiToiletModel> model,
                                                                    String textureName, String alternateName,
                                                                    Predicate<SkibidiToiletEntity> useAlternate, float scale) {
        Identifier texture = texture(textureName);
        Identifier alternate = texture(alternateName);
        return context -> new SkibidiToiletRenderer(context, model.apply(context.getPart(layer)),
                toilet -> useAlternate.test(toilet) ? alternate : texture, scale);
    }

    private static Identifier texture(String name) {
        return SkibidiToiletMod.id("textures/entity/" + name + ".png");
    }

    /** A flushed toilet stays upright while its head goes down the bowl, instead of tipping over like other mobs. */
    @Override
    protected void setupTransforms(SkibidiToiletEntity entity, MatrixStack matrices, float animationProgress,
                                   float bodyYaw, float tickDelta) {
        if (entity.deathTime > 0 && entity.flushesOnDeath()) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
            return;
        }
        super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta);
    }

    @Override
    protected void scale(SkibidiToiletEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(this.scale, this.scale, this.scale);
    }

    @Override
    public Identifier getTexture(SkibidiToiletEntity entity) {
        return this.texture.apply(entity);
    }
}
