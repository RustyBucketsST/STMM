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
 * also switch to a second texture, like the Saint Toilet when it turns demonic.
 */
public class SkibidiToiletRenderer extends MobEntityRenderer<SkibidiToiletEntity, SkibidiToiletModel> {
    private final Function<SkibidiToiletEntity, Identifier> texture;
    private final float scale;

    public SkibidiToiletRenderer(EntityRendererFactory.Context context, SkibidiToiletModel model,
                                 Function<SkibidiToiletEntity, Identifier> texture, float scale) {
        this(context, model, texture, scale, 0.5f * scale);
    }

    private SkibidiToiletRenderer(EntityRendererFactory.Context context, SkibidiToiletModel model,
                                  Function<SkibidiToiletEntity, Identifier> texture, float scale, float shadowRadius) {
        super(context, model, shadowRadius);
        this.texture = texture;
        this.scale = scale;
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

    /**
     * A toilet whose model is built at {@code detail} times the usual size for a finer texture (like
     * GManToiletModel), drawn so that it ends up {@code scale} times a normal toilet.
     */
    public static EntityRendererFactory<SkibidiToiletEntity> detailed(EntityModelLayer layer,
                                                                     Function<ModelPart, SkibidiToiletModel> model,
                                                                     String textureName, float scale, float detail) {
        Identifier texture = texture(textureName);
        return context -> new SkibidiToiletRenderer(context, model.apply(context.getPart(layer)), toilet -> texture,
                scale / detail, 0.5f * scale);
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
