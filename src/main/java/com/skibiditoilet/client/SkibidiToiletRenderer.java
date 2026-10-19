package com.skibiditoilet.client;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.entity.SkibidiToiletEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class SkibidiToiletRenderer extends MobEntityRenderer<SkibidiToiletEntity, SkibidiToiletModel> {
    private static final Identifier TEXTURE = SkibidiToiletMod.id("textures/entity/skibidi_toilet.png");

    public SkibidiToiletRenderer(EntityRendererFactory.Context context) {
        super(context, new SkibidiToiletModel(context.getPart(SkibidiToiletClient.SKIBIDI_TOILET_LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(SkibidiToiletEntity entity) {
        return TEXTURE;
    }
}
