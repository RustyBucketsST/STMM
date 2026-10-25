package com.skibiditoilet.client;

import com.skibiditoilet.SkibidiToiletMod;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

import java.util.function.Function;

/** Renders a mob with its own model and one texture, scaled up or down. */
public class ScaledMobRenderer<T extends MobEntity, M extends EntityModel<T>> extends MobEntityRenderer<T, M> {
    private final Identifier texture;
    private final float scale;

    private ScaledMobRenderer(EntityRendererFactory.Context context, M model, Identifier texture, float scale,
                              float detail) {
        super(context, model, 0.5f * scale);
        this.texture = texture;
        this.scale = scale / detail;
    }

    public static <T extends MobEntity, M extends EntityModel<T>> EntityRendererFactory<T> factory(
            EntityModelLayer layer, Function<ModelPart, M> model, String textureName, float scale) {
        return factory(layer, model, textureName, scale, 1.0f);
    }

    /** A model built at {@code detail} units per pixel, drawn so that it ends up {@code scale} times normal size. */
    public static <T extends MobEntity, M extends EntityModel<T>> EntityRendererFactory<T> factory(
            EntityModelLayer layer, Function<ModelPart, M> model, String textureName, float scale, float detail) {
        Identifier texture = SkibidiToiletMod.id("textures/entity/" + textureName + ".png");
        return context -> new ScaledMobRenderer<>(context, model.apply(context.getPart(layer)), texture, scale, detail);
    }

    @Override
    protected void scale(T entity, MatrixStack matrices, float amount) {
        matrices.scale(this.scale, this.scale, this.scale);
    }

    @Override
    public Identifier getTexture(T entity) {
        return this.texture;
    }
}
