package com.skibiditoilet.client;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.client.character.AllianceAppearanceFeature;
import com.skibiditoilet.client.character.ClientCharacterState;
import com.skibiditoilet.client.character.PlayerAppearance;
import com.skibiditoilet.client.character.PlayerHeadModels;
import com.skibiditoilet.client.fx.ClientFx;
import com.skibiditoilet.client.power.ClientPowerState;
import com.skibiditoilet.client.power.GearRenderer;
import com.skibiditoilet.client.power.PlayerFormRenderer;
import com.skibiditoilet.client.story.ClientStoryState;
import com.skibiditoilet.entity.BigSkibidiToiletEntity;
import com.skibiditoilet.entity.CameraStriderEntity;
import com.skibiditoilet.entity.FastArmoredToiletEntity;
import com.skibiditoilet.entity.GiantFlyingToiletEntity;
import com.skibiditoilet.entity.GManToiletEntity;
import com.skibiditoilet.entity.LargeAllianceEntity;
import com.skibiditoilet.entity.MicroToiletEntity;
import com.skibiditoilet.entity.SaintToiletEntity;
import com.skibiditoilet.entity.TitanEntity;
import com.skibiditoilet.entity.TripleToiletEntity;
import com.skibiditoilet.registry.ModBlocks;
import com.skibiditoilet.registry.ModEntities;
import com.skibiditoilet.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;

public class SkibidiToiletClient implements ClientModInitializer {
    // Default water color, used for the toilet item and when there's no world to sample.
    private static final int WATER_COLOR = 0x3F76E4;
    private static final int WATER_TINT_INDEX = 1;

    public static final EntityModelLayer SKIBIDI_TOILET_LAYER = layer("skibidi_toilet");
    public static final EntityModelLayer GMAN_TOILET_LAYER = layer("gman_toilet");
    public static final EntityModelLayer SPIDER_TOILET_LAYER = layer("spider_toilet");
    public static final EntityModelLayer JETPACK_TOILET_LAYER = layer("jetpack_toilet");
    public static final EntityModelLayer SKIBIDI_URINAL_LAYER = layer("skibidi_urinal");
    public static final EntityModelLayer DJ_TOILET_LAYER = layer("dj_skibidi_toilet");
    public static final EntityModelLayer SAINT_TOILET_LAYER = layer("saint_skibidi_toilet");
    public static final EntityModelLayer TRIPLE_TOILET_LAYER = layer("triple_skibidi_toilet");
    public static final EntityModelLayer HAT_TOILET_LAYER = layer("hat_skibidi_toilet");
    public static final EntityModelLayer FAST_ARMORED_TOILET_LAYER = layer("fast_armored_skibidi_toilet");
    public static final EntityModelLayer CAMERA_STRIDER_LAYER = layer("camera_strider");
    public static final EntityModelLayer CAMERAMAN_LAYER = layer("cameraman");
    public static final EntityModelLayer SPEAKERMAN_LAYER = layer("speakerman");
    public static final EntityModelLayer TV_MAN_LAYER = layer("tv_man");
    public static final EntityModelLayer LARGE_CAMERAMAN_LAYER = layer("large_cameraman");
    public static final EntityModelLayer LARGE_SPEAKERMAN_LAYER = layer("large_speakerman");
    public static final EntityModelLayer LARGE_TV_MAN_LAYER = layer("large_tv_man");
    public static final EntityModelLayer GEAR_LAYER = layer("gear");
    public static final EntityModelLayer TITAN_CAMERAMAN_LAYER = layer("titan_cameraman");
    public static final EntityModelLayer TITAN_SPEAKERMAN_LAYER = layer("titan_speakerman");
    public static final EntityModelLayer TITAN_TV_MAN_LAYER = layer("titan_tv_man");
    public static final EntityModelLayer TITAN_CAMERAMAN_2_LAYER = layer("titan_cameraman_2");
    public static final EntityModelLayer TITAN_SPEAKERMAN_2_LAYER = layer("titan_speakerman_2");
    public static final EntityModelLayer TITAN_TV_MAN_2_LAYER = layer("titan_tv_man_2");

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(SKIBIDI_TOILET_LAYER, SkibidiToiletModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(GMAN_TOILET_LAYER, GManToiletModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(SPIDER_TOILET_LAYER, SpiderToiletModel::getSpiderTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(JETPACK_TOILET_LAYER, SkibidiToiletModel::getJetpackTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(SKIBIDI_URINAL_LAYER, SkibidiToiletModel::getUrinalTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(DJ_TOILET_LAYER, SkibidiToiletModel::getDjTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(SAINT_TOILET_LAYER, SkibidiToiletModel::getSaintTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(TRIPLE_TOILET_LAYER, TripleToiletModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(HAT_TOILET_LAYER, SkibidiToiletModel::getHatTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(FAST_ARMORED_TOILET_LAYER, FastArmoredToiletModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(CAMERA_STRIDER_LAYER, CameraStriderModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(CAMERAMAN_LAYER, AllianceModel::getCameramanModelData);
        EntityModelLayerRegistry.registerModelLayer(SPEAKERMAN_LAYER, AllianceModel::getSpeakermanModelData);
        EntityModelLayerRegistry.registerModelLayer(TV_MAN_LAYER, AllianceModel::getTvManModelData);
        EntityModelLayerRegistry.registerModelLayer(LARGE_CAMERAMAN_LAYER, AllianceModel::getLargeCameramanModelData);
        EntityModelLayerRegistry.registerModelLayer(LARGE_SPEAKERMAN_LAYER, AllianceModel::getLargeSpeakermanModelData);
        EntityModelLayerRegistry.registerModelLayer(LARGE_TV_MAN_LAYER, AllianceModel::getLargeTvManModelData);
        EntityModelLayerRegistry.registerModelLayer(GEAR_LAYER, GearRenderer::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(TITAN_CAMERAMAN_LAYER, TitanModel::getCameramanModelData);
        EntityModelLayerRegistry.registerModelLayer(TITAN_SPEAKERMAN_LAYER, TitanModel::getSpeakermanModelData);
        EntityModelLayerRegistry.registerModelLayer(TITAN_TV_MAN_LAYER, TitanModel::getTvManModelData);
        EntityModelLayerRegistry.registerModelLayer(TITAN_CAMERAMAN_2_LAYER, TitanModel::getCameraman2ModelData);
        EntityModelLayerRegistry.registerModelLayer(TITAN_SPEAKERMAN_2_LAYER, TitanModel::getSpeakerman2ModelData);
        EntityModelLayerRegistry.registerModelLayer(TITAN_TV_MAN_2_LAYER, TitanModel::getTvMan2ModelData);

        EntityRendererRegistry.register(ModEntities.SKIBIDI_TOILET,
                SkibidiToiletRenderer.factory(SKIBIDI_TOILET_LAYER, SkibidiToiletModel::new, "skibidi_toilet", 1.0f));
        EntityRendererRegistry.register(ModEntities.SPIDER_TOILET,
                SkibidiToiletRenderer.factory(SPIDER_TOILET_LAYER, SpiderToiletModel::new, "spider_toilet", 1.0f));
        EntityRendererRegistry.register(ModEntities.JETPACK_TOILET,
                SkibidiToiletRenderer.factory(JETPACK_TOILET_LAYER, SkibidiToiletModel::new, "jetpack_toilet", 1.0f));
        EntityRendererRegistry.register(ModEntities.BIG_SKIBIDI_TOILET, SkibidiToiletRenderer.factory(
                SKIBIDI_TOILET_LAYER, SkibidiToiletModel::new, "big_skibidi_toilet", BigSkibidiToiletEntity.SCALE));
        EntityRendererRegistry.register(ModEntities.SCIENTIST_TOILET,
                SkibidiToiletRenderer.factory(SKIBIDI_TOILET_LAYER, SkibidiToiletModel::new, "scientist_toilet", 1.0f));
        EntityRendererRegistry.register(ModEntities.GMAN_TOILET, SkibidiToiletRenderer.detailed(
                GMAN_TOILET_LAYER, GManToiletModel::new, "gman_toilet", GManToiletEntity.SCALE, GManToiletModel.DETAIL));
        EntityRendererRegistry.register(ModEntities.SKIBIDI_URINAL, SkibidiToiletRenderer.factory(
                SKIBIDI_URINAL_LAYER, SkibidiToiletModel::new, "skibidi_urinal", 1.0f));
        EntityRendererRegistry.register(ModEntities.MICRO_TOILET, SkibidiToiletRenderer.factory(
                SKIBIDI_TOILET_LAYER, SkibidiToiletModel::new, "skibidi_toilet", MicroToiletEntity.SCALE));
        EntityRendererRegistry.register(ModEntities.FLYING_TOILET, SkibidiToiletRenderer.factory(
                SKIBIDI_TOILET_LAYER, SkibidiToiletModel::new, "flying_skibidi_toilet", 1.0f));
        // In the series the Giant Flying Toilet has the same tall toilet as G-Toilet.
        EntityRendererRegistry.register(ModEntities.GIANT_FLYING_TOILET, SkibidiToiletRenderer.detailed(
                GMAN_TOILET_LAYER, GManToiletModel::new, "giant_flying_skibidi_toilet", GiantFlyingToiletEntity.SCALE,
                GManToiletModel.DETAIL));
        EntityRendererRegistry.register(ModEntities.DJ_TOILET, SkibidiToiletRenderer.factory(
                DJ_TOILET_LAYER, SkibidiToiletModel::new, "dj_skibidi_toilet", 1.0f));
        EntityRendererRegistry.register(ModEntities.SAINT_TOILET, SkibidiToiletRenderer.factory(
                SAINT_TOILET_LAYER, SkibidiToiletModel::new, "saint_skibidi_toilet", "saint_skibidi_toilet_demonic",
                toilet -> toilet instanceof SaintToiletEntity saint && saint.isDemonic(), 1.0f));
        EntityRendererRegistry.register(ModEntities.TRIPLE_TOILET, SkibidiToiletRenderer.factory(
                TRIPLE_TOILET_LAYER, TripleToiletModel::new, "triple_skibidi_toilet", TripleToiletEntity.SCALE));
        EntityRendererRegistry.register(ModEntities.HAT_TOILET, SkibidiToiletRenderer.factory(
                HAT_TOILET_LAYER, SkibidiToiletModel::new, "hat_skibidi_toilet", 1.0f));
        EntityRendererRegistry.register(ModEntities.FAST_ARMORED_TOILET, ScaledMobRenderer.factory(
                FAST_ARMORED_TOILET_LAYER, FastArmoredToiletModel::new, "fast_armored_skibidi_toilet", FastArmoredToiletEntity.SCALE));

        EntityRendererRegistry.register(ModEntities.CAMERAMAN, AllianceRenderer.factory(CAMERAMAN_LAYER, "cameraman", 1.0f, AllianceModel.DETAILED));
        EntityRendererRegistry.register(ModEntities.SPEAKERMAN, AllianceRenderer.factory(SPEAKERMAN_LAYER, "speakerman", 1.0f, AllianceModel.DETAILED));
        EntityRendererRegistry.register(ModEntities.TV_MAN, AllianceRenderer.factory(TV_MAN_LAYER, "tv_man", 1.0f, AllianceModel.DETAILED));
        EntityRendererRegistry.register(ModEntities.TITAN_CAMERAMAN,
                AllianceRenderer.titanFactory(TITAN_CAMERAMAN_LAYER, "titan_cameraman", TitanEntity.CAMERAMAN_SCALE));
        EntityRendererRegistry.register(ModEntities.TITAN_SPEAKERMAN,
                AllianceRenderer.titanFactory(TITAN_SPEAKERMAN_LAYER, "titan_speakerman", TitanEntity.SPEAKERMAN_SCALE));
        EntityRendererRegistry.register(ModEntities.TITAN_TV_MAN,
                AllianceRenderer.titanFactory(TITAN_TV_MAN_LAYER, "titan_tv_man", TitanEntity.TV_MAN_SCALE));
        EntityRendererRegistry.register(ModEntities.LARGE_CAMERAMAN,
                AllianceRenderer.factory(LARGE_CAMERAMAN_LAYER, "large_cameraman", LargeAllianceEntity.SCALE, 1.0f));
        EntityRendererRegistry.register(ModEntities.LARGE_SPEAKERMAN,
                AllianceRenderer.factory(LARGE_SPEAKERMAN_LAYER, "large_speakerman", LargeAllianceEntity.SCALE, 1.0f));
        EntityRendererRegistry.register(ModEntities.CAMERA_STRIDER, ScaledMobRenderer.factory(
                CAMERA_STRIDER_LAYER, CameraStriderModel::new, "camera_strider", CameraStriderEntity.SCALE));
        EntityRendererRegistry.register(ModEntities.COMMANDER, AllianceRenderer.factory(CAMERAMAN_LAYER, "commander", 1.0f, AllianceModel.DETAILED));

        EntityRendererRegistry.register(ModEntities.SEAT, EmptyEntityRenderer::new);

        // Player characters: device heads and the suit, drawn on top of the normal player model.
        EntityModelLayerRegistry.registerModelLayer(PlayerHeadModels.CAMERA_HEAD, PlayerHeadModels::camera);
        EntityModelLayerRegistry.registerModelLayer(PlayerHeadModels.SPEAKER_HEAD, PlayerHeadModels::speaker);
        EntityModelLayerRegistry.registerModelLayer(PlayerHeadModels.TV_HEAD, PlayerHeadModels::tv);
        EntityModelLayerRegistry.registerModelLayer(PlayerHeadModels.SUIT, PlayerHeadModels::suit);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, helper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                PlayerAppearance.init(context.getModelLoader());
                GearRenderer.init(context);
                PlayerFormRenderer.init(context);
                helper.register(new AllianceAppearanceFeature(playerRenderer));
                helper.register(new GearRenderer.Feature<>(playerRenderer, GearRenderer.Fit.NORMAL));
            }
        });
        ClientCharacterState.init();
        ClientStoryState.init();
        ClientFx.init();
        ClientPowerState.init();

        // Tints the water in the toilet bowl (the face with tintindex 1 in models/block/toilet.json).
        // Index 0 stays white because block break/landing particles always ask for index 0.
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            if (tintIndex != WATER_TINT_INDEX) {
                return 0xFFFFFF;
            }
            return world != null && pos != null ? BiomeColors.getWaterColor(world, pos) : WATER_COLOR;
        }, ModBlocks.TOILET);
        ColorProviderRegistry.ITEM.register((stack, tintIndex) ->
                tintIndex == WATER_TINT_INDEX ? WATER_COLOR : 0xFFFFFF, ModItems.TOILET);
    }

    private static EntityModelLayer layer(String name) {
        return new EntityModelLayer(SkibidiToiletMod.id(name), "main");
    }
}
