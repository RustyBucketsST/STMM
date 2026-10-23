package com.skibiditoilet.client.power;

import com.skibiditoilet.power.PowerSync;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * The Holoshield: a glowing pane of hard light in front of the player, with a bright frame and scan
 * lines rolling up it. It turns with where they look, like the block it does (see PowerEffects#shieldBlocks).
 */
final class ShieldRenderer {
    private static final float R = 0x5F / 255.0f;
    private static final float G = 0xE3 / 255.0f;
    private static final float B = 0xFF / 255.0f;
    private static final double HALF_WIDTH = 0.65;
    private static final double BOTTOM = 0.15;
    private static final double TOP = 1.85;
    private static final double DISTANCE = 0.75;
    private static final double EDGE = 0.04;

    private ShieldRenderer() {
    }

    static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buffer = null;
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        Vec3d cam = context.camera().getPos();
        float tickDelta = context.tickDelta();
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            float[] span = ClientPowerState.visible(player.getId(), PowerSync.Visible.SHIELD, tickDelta);
            if (span == null || player.isInvisible()) {
                continue;
            }
            if (buffer == null) {
                buffer = consumers.getBuffer(RenderLayer.getLightning());
            }
            // Flickers on over a few ticks and fades out at the end.
            float alpha = MathHelper.clamp(span[0] / 4.0f, 0.0f, 1.0f) * MathHelper.clamp(span[1] / 10.0f, 0.0f, 1.0f);
            if (span[0] < 4.0f && (int) (span[0] * 2) % 2 == 1) {
                alpha *= 0.4f;
            }
            boolean ownView = player == client.player && client.options.getPerspective().isFirstPerson();
            if (ownView) {
                // Seen from behind, from right up close: keep it faint so you can still see past it.
                alpha *= 0.45f;
            }
            pane(buffer, matrix, player, cam, tickDelta, alpha, span[0]);
        }
        if (buffer != null) {
            consumers.draw(RenderLayer.getLightning());
        }
    }

    private static void pane(VertexConsumer buffer, Matrix4f matrix, AbstractClientPlayerEntity player, Vec3d cam,
                             float tickDelta, float alpha, float age) {
        float yaw = player.getYaw(tickDelta);
        Vec3d forward = Vec3d.fromPolar(0.0f, yaw);
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x);
        Vec3d base = player.getLerpedPos(tickDelta).add(forward.multiply(DISTANCE)).subtract(cam);

        // The pane itself.
        quad(buffer, matrix, base, right, -HALF_WIDTH, HALF_WIDTH, BOTTOM, TOP, 0.16f * alpha);
        // The frame.
        quad(buffer, matrix, base, right, -HALF_WIDTH, HALF_WIDTH, TOP - EDGE, TOP, 0.7f * alpha);
        quad(buffer, matrix, base, right, -HALF_WIDTH, HALF_WIDTH, BOTTOM, BOTTOM + EDGE, 0.7f * alpha);
        quad(buffer, matrix, base, right, -HALF_WIDTH, -HALF_WIDTH + EDGE, BOTTOM, TOP, 0.7f * alpha);
        quad(buffer, matrix, base, right, HALF_WIDTH - EDGE, HALF_WIDTH, BOTTOM, TOP, 0.7f * alpha);
        // Scan lines rolling up.
        double height = TOP - BOTTOM - 2 * EDGE;
        for (int i = 0; i < 4; i++) {
            double t = (age * 0.025 + i / 4.0) % 1.0;
            double y = BOTTOM + EDGE + t * height;
            quad(buffer, matrix, base, right, -HALF_WIDTH + EDGE, HALF_WIDTH - EDGE, y, Math.min(TOP - EDGE, y + 0.025),
                    0.35f * alpha);
        }
    }

    /** A rectangle on the pane, from {@code left} to {@code rightEdge} across and {@code bottom} to {@code top} up. */
    private static void quad(VertexConsumer buffer, Matrix4f matrix, Vec3d base, Vec3d right,
                             double left, double rightEdge, double bottom, double top, float alpha) {
        Vec3d[] corners = {
                base.add(right.multiply(left)).add(0.0, bottom, 0.0),
                base.add(right.multiply(rightEdge)).add(0.0, bottom, 0.0),
                base.add(right.multiply(rightEdge)).add(0.0, top, 0.0),
                base.add(right.multiply(left)).add(0.0, top, 0.0),
        };
        // Both windings, so it shows from either side.
        for (int side = 0; side < 2; side++) {
            for (int i = 0; i < 4; i++) {
                Vec3d v = corners[side == 0 ? i : 3 - i];
                buffer.vertex(matrix, (float) v.x, (float) v.y, (float) v.z).color(R, G, B, alpha).next();
            }
        }
    }
}
