package com.skibiditoilet.client.character;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.character.AllianceType;
import com.skibiditoilet.character.BodyStyle;
import com.skibiditoilet.character.CharacterData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws a player's character: the device head instead of their own head, and the suit over their skin.
 * Colors come from grey textures dyed at draw time, one pass per color slot over the same model.
 */
public final class PlayerAppearance {
    private static final int UNTINTED = 0xFFFFFF;

    private static final Map<AllianceType, ModelPart> HEADS = new EnumMap<>(AllianceType.class);
    private static ModelPart suit;

    private PlayerAppearance() {
    }

    /** Builds the models once; called when the player renderers are created. */
    public static void init(EntityModelLoader loader) {
        HEADS.put(AllianceType.CAMERAMAN, loader.getModelPart(PlayerHeadModels.CAMERA_HEAD).getChild("head"));
        HEADS.put(AllianceType.SPEAKERMAN, loader.getModelPart(PlayerHeadModels.SPEAKER_HEAD).getChild("head"));
        HEADS.put(AllianceType.TV_MAN, loader.getModelPart(PlayerHeadModels.TV_HEAD).getChild("head"));
        suit = loader.getModelPart(PlayerHeadModels.SUIT);
    }

    /**
     * The look to draw for this player, or null to draw them normally. While the creator is open the
     * local player shows the look being edited.
     */
    @Nullable
    public static CharacterData appearanceOf(AbstractClientPlayerEntity player) {
        if (player == MinecraftClient.getInstance().player && CharacterPreview.get() != null) {
            return CharacterPreview.get();
        }
        CharacterData data = ClientCharacterState.get(player.getId());
        return data != null && data.created() ? data : null;
    }

    /** Hides the parts of the vanilla player model that the character covers. Called from setModelPose. */
    public static void hideCoveredParts(AbstractClientPlayerEntity player, PlayerEntityModel<?> model) {
        CharacterData data = appearanceOf(player);
        // Invisible players aren't given a device head, so leave their own head for anyone who can see them.
        if (data == null || player.isInvisible()) {
            return;
        }
        model.head.visible = false;
        model.hat.visible = false;
        if (data.body() == BodyStyle.SUIT) {
            model.jacket.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
            model.leftPants.visible = false;
            model.rightPants.visible = false;
        }
    }

    public static void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                              AbstractClientPlayerEntity player, PlayerEntityModel<?> playerModel, CharacterData data) {
        if (HEADS.isEmpty()) {
            return;
        }
        int overlay = LivingEntityRenderer.getOverlay(player, 0.0f);

        ModelPart head = HEADS.get(data.type());
        head.copyTransform(playerModel.head);
        showDetails(head, data);
        String type = data.type().asString();
        renderPass(head, texture(type + "_casing"), data.casingColor(), matrices, vertexConsumers, light, overlay);
        renderPass(head, texture(accentTexture(data)), data.accentColor(), matrices, vertexConsumers, light, overlay);
        renderPass(head, texture(type + "_detail"), UNTINTED, matrices, vertexConsumers, light, overlay);
        if (data.type() == AllianceType.TV_MAN && data.detail1() > 0) {
            renderPass(head, texture("tv_man_face_" + data.detail1()), faceColor(data.accentColor()),
                    matrices, vertexConsumers, light, overlay);
        }

        // Spectators only show a floating head, so they get no suit.
        if (data.body() == BodyStyle.SUIT && !player.isSpectator()) {
            for (String part : new String[]{"body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
                ModelPart suitPart = suit.getChild(part);
                suitPart.copyTransform(playerPart(playerModel, part));
                renderSuitPart(suitPart, data, matrices, vertexConsumers, light, overlay);
            }
        }
    }

    /** Draws the suit sleeve over the player's arm in first person. Called after vanilla draws the arm. */
    public static void renderFirstPersonSuitArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                                AbstractClientPlayerEntity player, ModelPart arm, boolean rightArm) {
        CharacterData data = appearanceOf(player);
        if (data == null || data.body() != BodyStyle.SUIT || suit == null) {
            return;
        }
        ModelPart suitArm = suit.getChild(rightArm ? "right_arm" : "left_arm");
        suitArm.copyTransform(arm);
        renderSuitPart(suitArm, data, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
    }

    private static void renderSuitPart(ModelPart part, CharacterData data, MatrixStack matrices,
                                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        renderPass(part, texture("suit_base"), data.suitColor(), matrices, vertexConsumers, light, overlay);
        renderPass(part, texture("suit_tie"), data.tieColor(), matrices, vertexConsumers, light, overlay);
        renderPass(part, texture("suit_detail"), UNTINTED, matrices, vertexConsumers, light, overlay);
    }

    private static void showDetails(ModelPart head, CharacterData data) {
        switch (data.type()) {
            case CAMERAMAN -> {
                head.getChild("lens_normal").visible = data.detail0() == 0;
                head.getChild("lens_big").visible = data.detail0() == 1;
                head.getChild("lens_double").visible = data.detail0() == 2;
                head.getChild("light").visible = data.detail1() == 1;
            }
            case TV_MAN -> {
                head.getChild("antenna_left").visible = data.detail0() == 0;
                head.getChild("antenna_right").visible = data.detail0() == 0;
                head.getChild("antenna_single").visible = data.detail0() == 1;
            }
            case SPEAKERMAN -> {
            }
        }
    }

    /** The Speakerman's front (number of speakers x grille style) is a separate texture per combination. */
    private static String accentTexture(CharacterData data) {
        String type = data.type().asString();
        return data.type() == AllianceType.SPEAKERMAN
                ? type + "_accent_" + data.detail0() + "_" + data.detail1()
                : type + "_accent";
    }

    /** TV faces are drawn white on dark screens and nearly black on light ones so they always show. */
    private static int faceColor(int screenColor) {
        int brightness = ((screenColor >> 16 & 0xFF) * 299 + (screenColor >> 8 & 0xFF) * 587 + (screenColor & 0xFF) * 114) / 1000;
        return brightness > 140 ? 0x202020 : UNTINTED;
    }

    private static ModelPart playerPart(PlayerEntityModel<?> model, String part) {
        return switch (part) {
            case "body" -> model.body;
            case "right_arm" -> model.rightArm;
            case "left_arm" -> model.leftArm;
            case "right_leg" -> model.rightLeg;
            default -> model.leftLeg;
        };
    }

    private static void renderPass(ModelPart part, Identifier texture, int color, MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers, int light, int overlay) {
        float red = (color >> 16 & 0xFF) / 255.0f;
        float green = (color >> 8 & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        part.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture)),
                light, overlay, red, green, blue, 1.0f);
    }

    private static Identifier texture(String name) {
        return SkibidiToiletMod.id("textures/player/" + name + ".png");
    }
}
