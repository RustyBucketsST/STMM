package com.skibiditoilet.client.power;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.character.AllianceType;
import com.skibiditoilet.character.CharacterData;
import com.skibiditoilet.client.SkibidiToiletClient;
import com.skibiditoilet.client.bbmodel.GearBbModel;
import com.skibiditoilet.client.character.ClientCharacterState;
import com.skibiditoilet.power.FormHolder;
import com.skibiditoilet.power.Gear;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a player's gear (gear.bbmodel) on whatever model they have: their own, a Large member's or a Titan's. Each
 * piece follows the body or arm it's on. Large members and Titans are built at twice the detail (a Titan is also
 * wider), so there the pieces are stretched to fit; a Titan has his own jetpack (except the TV Man) and shoulder gear,
 * so those aren't added.
 */
public final class GearRenderer {
    private static final Identifier TEXTURE = SkibidiToiletMod.id("textures/entity/gear.png");

    /**
     * How gear sits on a model: how much to stretch the body and arm spaces, how far down the jetpack hangs (in normal
     * pixels), and which pieces to leave out.
     */
    public record Fit(float bodyX, float bodyY, float bodyZ, float armX, float armY, float armZ, float jetpackDrop,
                      boolean jetpack, boolean shoulders, boolean chestPlates) {
        /** A normal player: the standard biped body. */
        public static final Fit NORMAL = new Fit(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, true, true, true);

        /** A Large member: a suited body built at twice the detail, a little broader and deeper than a biped's. */
        public static final Fit DETAILED = new Fit(2.15f, 2.0f, 2.3f, 2.25f, 2.0f, 2.25f, 0.0f, true, true, true);

        /** A Titan: his own shoulder gear shows instead, and plates on his chest would hide his core. */
        public static Fit titan(AllianceType type) {
            boolean jetpack = type == AllianceType.TV_MAN;
            return new Fit(2.75f, 2.0f, 3.0f, 2.5f, 2.0f, 2.5f, 5.0f, jetpack, false, false);
        }
    }

    private static ModelPart jetpack;
    private static ModelPart cameras;
    private static ModelPart speakers;
    private static ModelPart blades;
    private static ModelPart plate;
    private static ModelPart pauldronRight;
    private static ModelPart pauldronLeft;

    private GearRenderer() {
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        GearBbModel.addJetpack(root);
        GearBbModel.addCameras(root);
        GearBbModel.addSpeakers(root);
        GearBbModel.addBlades(root);
        GearBbModel.addPlate(root);
        GearBbModel.addPauldronRight(root);
        GearBbModel.addPauldronLeft(root);
        return TexturedModelData.of(modelData, GearBbModel.TEXTURE_WIDTH, GearBbModel.TEXTURE_HEIGHT);
    }

    /** Builds the gear model; called whenever the player renderers are made. */
    public static void init(EntityRendererFactory.Context context) {
        ModelPart root = context.getPart(SkibidiToiletClient.GEAR_LAYER);
        jetpack = loose(root, "jetpack");
        cameras = loose(root, "cameras");
        speakers = loose(root, "speakers");
        blades = loose(root, "blades");
        plate = loose(root, "plate");
        pauldronRight = loose(root, "pauldron_right");
        pauldronLeft = loose(root, "pauldron_left");
    }

    /** A gear piece with its own transform cleared: it's drawn in the space of the part it's on. */
    private static ModelPart loose(ModelPart root, String name) {
        ModelPart part = root.getChild(name);
        part.setTransform(ModelTransform.NONE);
        return part;
    }

    /** The type whose shoulder gear a player wears, or null if they have no character. */
    @Nullable
    static AllianceType typeOf(AbstractClientPlayerEntity player) {
        CharacterData data = ClientCharacterState.get(player.getId());
        return data != null && data.created() ? data.type() : null;
    }

    public static void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay,
                              BipedEntityModel<?> model, int gear, @Nullable AllianceType type, Fit fit) {
        if (gear == 0 || jetpack == null) {
            return;
        }
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        if (Gear.JETPACK.in(gear) && fit.jetpack()) {
            draw(jetpack, model.body, fit.bodyX(), fit.bodyY(), fit.bodyZ(), fit.jetpackDrop(), matrices, buffer, light, overlay);
        }
        if (Gear.SHOULDERS.in(gear) && fit.shoulders() && type != null) {
            ModelPart shoulders = switch (type) {
                case CAMERAMAN -> cameras;
                case SPEAKERMAN -> speakers;
                case TV_MAN -> blades;
            };
            draw(shoulders, model.body, fit.bodyX(), fit.bodyY(), fit.bodyZ(), 0.0f, matrices, buffer, light, overlay);
        }
        if (Gear.PLATES.in(gear)) {
            if (fit.chestPlates()) {
                draw(plate, model.body, fit.bodyX(), fit.bodyY(), fit.bodyZ(), 0.0f, matrices, buffer, light, overlay);
            }
            draw(pauldronRight, model.rightArm, fit.armX(), fit.armY(), fit.armZ(), 0.0f, matrices, buffer, light, overlay);
            draw(pauldronLeft, model.leftArm, fit.armX(), fit.armY(), fit.armZ(), 0.0f, matrices, buffer, light, overlay);
        }
    }

    private static void draw(ModelPart piece, ModelPart on, float x, float y, float z, float drop, MatrixStack matrices,
                             VertexConsumer buffer, int light, int overlay) {
        if (!on.visible) {
            return;
        }
        matrices.push();
        on.rotate(matrices);
        matrices.scale(x, y, z);
        matrices.translate(0.0f, drop / 16.0f, 0.0f);
        piece.render(matrices, buffer, light, overlay);
        matrices.pop();
    }

    /** Draws the gear of a player drawn as a normal player (their own model, with or without a character). */
    public static final class Feature<M extends BipedEntityModel<AbstractClientPlayerEntity>>
            extends FeatureRenderer<AbstractClientPlayerEntity, M> {
        private final Fit fit;

        public Feature(FeatureRendererContext<AbstractClientPlayerEntity, M> context, @Nullable Fit fit) {
            super(context);
            this.fit = fit;
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                           AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta,
                           float animationProgress, float headYaw, float headPitch) {
            if (player.isInvisible() || player.isSpectator()) {
                return;
            }
            AllianceType type = typeOf(player);
            Fit fit = this.fit != null ? this.fit : Fit.NORMAL;
            GearRenderer.render(matrices, vertexConsumers, light, LivingEntityRenderer.getOverlay(player, 0.0f), this.getContextModel(),
                    FormHolder.gear(player), type, fit);
        }
    }

    /** For the first-person arm: the pauldron on it, if the player has plates. */
    public static void renderArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                 AbstractClientPlayerEntity player, ModelPart arm, boolean rightArm) {
        if (!Gear.PLATES.in(FormHolder.gear(player)) || pauldronRight == null) {
            return;
        }
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        draw(rightArm ? pauldronRight : pauldronLeft, arm, 1.0f, 1.0f, 1.0f, 0.0f, matrices, buffer, light,
                OverlayTexture.DEFAULT_UV);
    }
}
