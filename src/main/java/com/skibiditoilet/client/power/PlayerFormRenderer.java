package com.skibiditoilet.client.power;

import com.skibiditoilet.SkibidiToiletMod;
import com.skibiditoilet.character.AllianceType;
import com.skibiditoilet.client.AllianceModel;
import com.skibiditoilet.client.SkibidiToiletClient;
import com.skibiditoilet.client.TitanModel;
import com.skibiditoilet.power.Form;
import com.skibiditoilet.power.FormHolder;
import com.skibiditoilet.power.Gear;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Draws a player who's in a form as the Alliance mob it's modelled on, at the form's size, in place of the normal
 * player renderer (see PlayerEntityRendererMixin): a Large member, or a Titan with its glowing parts full-bright.
 * Held items are drawn in the model's hands, and the player's gear on the model (GearRenderer). A Titan's shoulder
 * cameras, speaker towers or claws only show once the player has bought shoulder gear.
 */
public class PlayerFormRenderer extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerFormModel> {
    private static final Map<Form, Map<AllianceType, PlayerFormRenderer>> RENDERERS = new EnumMap<>(Form.class);

    private final Identifier texture;
    private final float scale;
    /** A Titan's own shoulder gear, shown only with the shoulder gear bought. */
    private final List<ModelPart> shoulders;

    private PlayerFormRenderer(EntityRendererFactory.Context context, EntityModelLayer layer, String textureName,
                               float detail, float scale, float handDepth, boolean glow, GearRenderer.Fit fit,
                               List<String> shoulderParts) {
        super(context, new PlayerFormModel(context.getPart(layer), detail), 0.5f * scale);
        this.texture = SkibidiToiletMod.id("textures/entity/" + textureName + ".png");
        this.scale = scale;
        this.shoulders = shoulderParts.stream().map(name -> this.getModel().body.getChild(name)).toList();
        if (glow) {
            RenderLayer glowLayer = RenderLayer.getEyes(SkibidiToiletMod.id("textures/entity/" + textureName + "_glow.png"));
            this.addFeature(new EyesFeatureRenderer<>(this) {
                @Override
                public RenderLayer getEyesTexture() {
                    return glowLayer;
                }
            });
        }
        this.addFeature(new HandItems(this, context.getHeldItemRenderer(), detail, handDepth));
        this.addFeature(new GearRenderer.Feature<>(this, fit));
    }

    /** Builds a renderer for each form and type; called whenever the player renderers are made. */
    public static void init(EntityRendererFactory.Context context) {
        RENDERERS.clear();
        for (AllianceType type : AllianceType.values()) {
            put(Form.LARGE, type, new PlayerFormRenderer(context, largeLayer(type), "large_" + type.asString(),
                    AllianceModel.DETAILED, Form.LARGE.scale(type) / AllianceModel.DETAILED, 21.0f, false,
                    GearRenderer.Fit.DETAILED, List.of()));
            for (Form form : new Form[]{Form.TITAN, Form.TITAN_2}) {
                String texture = "titan_" + type.asString() + (form == Form.TITAN_2 ? "_2" : "");
                put(form, type, new PlayerFormRenderer(context, titanLayer(type, form), texture, TitanModel.DETAIL,
                        form.scale(type) / TitanModel.DETAIL, 56.0f, true, GearRenderer.Fit.titan(type),
                        titanShoulders(type, form)));
            }
        }
    }

    private static void put(Form form, AllianceType type, PlayerFormRenderer renderer) {
        RENDERERS.computeIfAbsent(form, f -> new EnumMap<>(AllianceType.class)).put(type, renderer);
    }

    private static EntityModelLayer largeLayer(AllianceType type) {
        return switch (type) {
            case CAMERAMAN -> SkibidiToiletClient.LARGE_CAMERAMAN_LAYER;
            case SPEAKERMAN -> SkibidiToiletClient.LARGE_SPEAKERMAN_LAYER;
            case TV_MAN -> SkibidiToiletClient.LARGE_TV_MAN_LAYER;
        };
    }

    /** The rotated parts of each Titan's body that are his shoulder gear (see the titan_*.bbmodel files). */
    private static List<String> titanShoulders(AllianceType type, Form form) {
        if (form == Form.TITAN_2) {
            return switch (type) {
                case CAMERAMAN -> bothSides("shoulder_cam", "shoulder_cam_hood", "shoulder_cam_lens");
                case SPEAKERMAN -> Stream.concat(Stream.of("tower_plate_right_r", "tower_plate_left_r"),
                        titanShoulders(type, Form.TITAN).stream()).toList();
                case TV_MAN -> Stream.concat(bothSides("shoulder_tv", "shoulder_tv_screen").stream(),
                        titanShoulders(type, Form.TITAN).stream()).toList();
            };
        }
        return switch (type) {
            case CAMERAMAN -> bothSides("shoulder_cam", "shoulder_cam_hood", "shoulder_cam_lens", "shoulder_cam_mount");
            case SPEAKERMAN -> bothSides("tower", "tower_cap", "tower_driver_wide", "tower_driver_mid", "tower_driver_tall",
                    "tower_driver_mid_wide", "tower_driver_mid_mid", "tower_driver_mid_tall", "tower_driver_low_wide",
                    "tower_driver_low_mid", "tower_driver_low_tall");
            case TV_MAN -> bothSides("claw0", "claw0_tip", "claw0_edge", "claw1", "claw1_tip", "claw1_edge", "claw2",
                    "claw2_tip", "claw2_edge");
        };
    }

    /** The parts made from the rotated cubes NAME_right and NAME_left of each name. */
    private static List<String> bothSides(String... names) {
        return Stream.of(names).flatMap(name -> Stream.of(name + "_right_r", name + "_left_r")).toList();
    }

    /** The 1.0 Titans for the Titan form, the 2.0s for Titan 2.0. */
    private static EntityModelLayer titanLayer(AllianceType type, Form form) {
        boolean two = form == Form.TITAN_2;
        return switch (type) {
            case CAMERAMAN -> two ? SkibidiToiletClient.TITAN_CAMERAMAN_2_LAYER : SkibidiToiletClient.TITAN_CAMERAMAN_LAYER;
            case SPEAKERMAN -> two ? SkibidiToiletClient.TITAN_SPEAKERMAN_2_LAYER : SkibidiToiletClient.TITAN_SPEAKERMAN_LAYER;
            case TV_MAN -> two ? SkibidiToiletClient.TITAN_TV_MAN_2_LAYER : SkibidiToiletClient.TITAN_TV_MAN_LAYER;
        };
    }

    /** The renderer for a player in {@code form}, or null if they're drawn as a normal player. */
    @Nullable
    public static PlayerFormRenderer get(Form form, @Nullable AllianceType type) {
        Map<AllianceType, PlayerFormRenderer> byType = RENDERERS.get(form);
        return byType != null && type != null ? byType.get(type) : null;
    }

    @Override
    public void render(AbstractClientPlayerEntity player, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        PlayerFormModel model = this.getModel();
        model.sneaking = player.isInSneakingPose();
        BipedEntityModel.ArmPose main = armPose(player, Hand.MAIN_HAND);
        BipedEntityModel.ArmPose off = armPose(player, Hand.OFF_HAND);
        if (main.isTwoHanded()) {
            off = player.getOffHandStack().isEmpty() ? BipedEntityModel.ArmPose.EMPTY : BipedEntityModel.ArmPose.ITEM;
        }
        boolean rightHanded = player.getMainArm() == Arm.RIGHT;
        model.rightArmPose = rightHanded ? main : off;
        model.leftArmPose = rightHanded ? off : main;
        boolean shoulders = Gear.SHOULDERS.in(FormHolder.gear(player));
        for (ModelPart part : this.shoulders) {
            part.visible = shoulders;
        }
        super.render(player, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    /** How an arm holds what's in that hand: aiming a bow, raising a shield, or just holding it. */
    private static BipedEntityModel.ArmPose armPose(AbstractClientPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) {
            return BipedEntityModel.ArmPose.EMPTY;
        }
        if (player.getActiveHand() == hand && player.getItemUseTimeLeft() > 0) {
            switch (stack.getUseAction()) {
                case BLOCK -> {
                    return BipedEntityModel.ArmPose.BLOCK;
                }
                case BOW -> {
                    return BipedEntityModel.ArmPose.BOW_AND_ARROW;
                }
                case SPEAR -> {
                    return BipedEntityModel.ArmPose.THROW_SPEAR;
                }
                case CROSSBOW -> {
                    return BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
                }
                case SPYGLASS -> {
                    return BipedEntityModel.ArmPose.SPYGLASS;
                }
                case TOOT_HORN -> {
                    return BipedEntityModel.ArmPose.TOOT_HORN;
                }
                case BRUSH -> {
                    return BipedEntityModel.ArmPose.BRUSH;
                }
                default -> {
                }
            }
        } else if (!player.handSwinging && stack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
            return BipedEntityModel.ArmPose.CROSSBOW_HOLD;
        }
        return BipedEntityModel.ArmPose.ITEM;
    }

    @Override
    protected void setupTransforms(AbstractClientPlayerEntity player, MatrixStack matrices, float animationProgress,
                                   float bodyYaw, float tickDelta) {
        super.setupTransforms(player, matrices, animationProgress, bodyYaw, tickDelta);
        // Lean forward to swim, like a normal player.
        float leaning = player.getLeaningPitch(tickDelta);
        if (leaning > 0.0f) {
            float target = player.isTouchingWater() ? -90.0f - player.getPitch() : -90.0f;
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(leaning, 0.0f, target)));
            if (player.isInSwimmingPose()) {
                float size = FormHolder.scale(player);
                matrices.translate(0.0f, -size, 0.3f * size);
            }
        }
    }

    @Override
    protected void scale(AbstractClientPlayerEntity player, MatrixStack matrices, float tickDelta) {
        matrices.scale(this.scale, this.scale, this.scale);
    }

    @Override
    public Identifier getTexture(AbstractClientPlayerEntity player) {
        return this.texture;
    }

    /**
     * What the player holds, in the model's hands. The hands of a detailed model are further down its arms and it's
     * drawn at a fraction of its size, so the item is moved down to the hand ({@code handDepth}, in model units) and
     * scaled back up.
     */
    private static final class HandItems extends FeatureRenderer<AbstractClientPlayerEntity, PlayerFormModel> {
        private final HeldItemRenderer heldItemRenderer;
        private final float detail;
        private final float handDepth;

        HandItems(FeatureRendererContext<AbstractClientPlayerEntity, PlayerFormModel> context,
                  HeldItemRenderer heldItemRenderer, float detail, float handDepth) {
            super(context);
            this.heldItemRenderer = heldItemRenderer;
            this.detail = detail;
            this.handDepth = handDepth;
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                           AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta,
                           float animationProgress, float headYaw, float headPitch) {
            boolean rightHanded = player.getMainArm() == Arm.RIGHT;
            ItemStack right = rightHanded ? player.getMainHandStack() : player.getOffHandStack();
            ItemStack left = rightHanded ? player.getOffHandStack() : player.getMainHandStack();
            renderItem(player, right, Arm.RIGHT, matrices, vertexConsumers, light);
            renderItem(player, left, Arm.LEFT, matrices, vertexConsumers, light);
        }

        private void renderItem(AbstractClientPlayerEntity player, ItemStack stack, Arm arm, MatrixStack matrices,
                                VertexConsumerProvider vertexConsumers, int light) {
            if (stack.isEmpty()) {
                return;
            }
            boolean left = arm == Arm.LEFT;
            matrices.push();
            this.getContextModel().setArmAngle(arm, matrices);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
            matrices.translate((left ? -1.0f : 1.0f) / 16.0f * this.detail, 0.125f * this.detail, -this.handDepth / 16.0f);
            matrices.scale(this.detail, this.detail, this.detail);
            this.heldItemRenderer.renderItem(player, stack, left ? ModelTransformationMode.THIRD_PERSON_LEFT_HAND
                    : ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, left, matrices, vertexConsumers, light);
            matrices.pop();
        }
    }
}
