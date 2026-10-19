package com.skibiditoilet.client;

import com.skibiditoilet.SkibidiToiletMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;

public class SkibidiToiletClient implements ClientModInitializer {
    public static final EntityModelLayer SKIBIDI_TOILET_LAYER =
            new EntityModelLayer(SkibidiToiletMod.id("skibidi_toilet"), "main");

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(SKIBIDI_TOILET_LAYER, SkibidiToiletModel::getTexturedModelData);
        EntityRendererRegistry.register(SkibidiToiletMod.SKIBIDI_TOILET, SkibidiToiletRenderer::new);
    }
}
