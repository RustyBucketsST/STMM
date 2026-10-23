package com.skibiditoilet.client.fx;

import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModParticles;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the effects the server asks for (ModFx): glowing beams, flashes, sparks, shockwaves, sound waves
 * and static, plus the screen shake that CameraMixin applies.
 */
public final class ClientFx {
    private static final int BEAM_TICKS = 7;
    /** How far the view can be knocked by the strongest shake, in degrees. */
    private static final float MAX_SHAKE_DEGREES = 4.0f;

    private record Beam(Vec3d from, Vec3d to, int color, float width, long startTick) {
    }

    private static final List<Beam> BEAMS = new ArrayList<>();
    private static long ticks;
    // 0 to 1; the view shakes by trauma squared, and trauma wears off over time.
    private static float trauma;

    private ClientFx() {
    }

    public static void init() {
        FxParticles.init();
        ClientPlayNetworking.registerGlobalReceiver(ModFx.FX, (client, handler, buf, sender) -> {
            ModFx.Kind kind = buf.readEnumConstant(ModFx.Kind.class);
            Vec3d a = ModFx.readVec(buf);
            Vec3d b = ModFx.readVec(buf);
            int color = buf.readInt();
            float f1 = buf.readFloat();
            float f2 = buf.readFloat();
            client.execute(() -> play(client, kind, a, b, color, f1, f2));
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ticks++;
            trauma = Math.max(0.0f, trauma - 0.04f);
            BEAMS.removeIf(beam -> ticks - beam.startTick > BEAM_TICKS);
        });
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ClientFx::renderBeams);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BEAMS.clear();
            trauma = 0.0f;
        });
    }

    private static void play(MinecraftClient client, ModFx.Kind kind, Vec3d a, Vec3d b, int color, float f1, float f2) {
        ClientWorld world = client.world;
        if (world == null) {
            return;
        }
        switch (kind) {
            case BEAM -> beam(world, a, b, color, f1);
            case SPRAY -> spray(world, a, b);
            case SHOCKWAVE -> shockwave(client, world, a, f1, f2);
            case SHAKE -> shake(client, a, f1, f2);
            case CAMERA_FLASH -> cameraFlash(world, a, f1);
            case SPEAKER_WAVE -> speakerWave(world, a, b, f1, color);
            case TV_STATIC -> tvStatic(world, a, f1);
            case SPLASH -> splash(world, a, f1);
            case RING -> ring(client, a, f1, color);
            case TELEPORT -> teleport(world, a, f1, f2 > 0.0f);
            case CONE -> cone(world, a, b, f1, color);
            case SPARKS -> sparks(world, a, b, color);
            case NOTES -> notes(world, a, f1);
        }
    }

    // ---------------------------------------------------------------- effects

    /** A beam with a flash where it was fired and a shower of sparks where it hit. */
    private static void beam(ClientWorld world, Vec3d from, Vec3d to, int color, float width) {
        BEAMS.add(new Beam(from, to, color, width, ticks));
        glow(from, Vec3d.ZERO, color, width * 3.5f, 4);
        glow(to, Vec3d.ZERO, 0xFFFFFF, width * 2.5f, 3);
        glow(to, Vec3d.ZERO, color, width * 4.0f, 5);
        Random random = world.random;
        for (int i = 0; i < 8; i++) {
            Vec3d velocity = new Vec3d(random.nextGaussian(), random.nextGaussian() + 0.8, random.nextGaussian()).multiply(0.12);
            FxParticles.Glow spark = glow(to, velocity, i % 2 == 0 ? color : 0xFFF2B0, 0.06f, 10 + random.nextInt(8));
            if (spark != null) {
                spark.gravity(0.6f);
            }
        }
    }

    private static void spray(ClientWorld world, Vec3d from, Vec3d to) {
        Vec3d step = to.subtract(from);
        double length = step.length();
        step = step.normalize();
        Random random = world.random;
        for (double d = 0.5; d < length; d += 0.35) {
            Vec3d p = from.add(step.multiply(d));
            world.addParticle(ParticleTypes.SPLASH, p.x, p.y, p.z, step.x * 0.2, 0.1, step.z * 0.2);
            if (random.nextInt(3) == 0) {
                world.addParticle(ParticleTypes.FALLING_WATER, p.x, p.y, p.z, 0.0, 0.0, 0.0);
            }
        }
        splash(world, to, 0.6f);
    }

    /** A ring racing out over the ground, chunks of the ground thrown up, dust, and a shake for anyone close. */
    private static void shockwave(MinecraftClient client, ClientWorld world, Vec3d pos, float radius, float strength) {
        Particle ring = client.particleManager.addParticle(ModParticles.RING_FLAT, pos.x, pos.y + 0.05, pos.z, 0, 0, 0);
        if (ring instanceof FxParticles.Ring r) {
            r.grow(0.3f, radius * 1.2f).life(12).opacity(1.0f);
            r.color(0xFFE9B0);
        }
        Random random = world.random;
        BlockState ground = world.getBlockState(BlockPos.ofFloored(pos.x, pos.y - 0.5, pos.z));
        ParticleEffect debris = ground.getRenderType() != BlockRenderType.INVISIBLE
                ? new BlockStateParticleEffect(ParticleTypes.BLOCK, ground) : ParticleTypes.POOF;
        int pieces = (int) (radius * 14);
        for (int i = 0; i < pieces; i++) {
            float angle = random.nextFloat() * MathHelper.TAU;
            double r = radius * (0.3 + random.nextDouble() * 0.7);
            double x = pos.x + MathHelper.cos(angle) * r;
            double z = pos.z + MathHelper.sin(angle) * r;
            world.addParticle(debris, x, pos.y + 0.1, z, MathHelper.cos(angle) * 0.2, 0.3 + random.nextDouble() * 0.3,
                    MathHelper.sin(angle) * 0.2);
            if (i % 3 == 0) {
                world.addParticle(ParticleTypes.CLOUD, x, pos.y + 0.1, z, MathHelper.cos(angle) * 0.08, 0.02,
                        MathHelper.sin(angle) * 0.08);
            }
        }
        shake(client, pos, strength, radius * 6.0f);
    }

    /** Shakes the view, harder the closer the local player is to {@code pos}. */
    private static void shake(MinecraftClient client, Vec3d pos, float strength, float radius) {
        if (client.player == null || radius <= 0.0f) {
            return;
        }
        double distance = client.player.getPos().distanceTo(pos);
        float falloff = (float) MathHelper.clamp(1.0 - distance / radius, 0.0, 1.0);
        trauma = Math.min(1.0f, trauma + strength * falloff);
    }

    private static void cameraFlash(ClientWorld world, Vec3d pos, float size) {
        glow(pos, Vec3d.ZERO, 0xFFFFFF, 1.2f * size, 4);
        glow(pos, Vec3d.ZERO, 0xBFE8FF, 2.2f * size, 6);
        Random random = world.random;
        for (int i = 0; i < 6; i++) {
            Vec3d velocity = new Vec3d(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).multiply(0.08 * size);
            glow(pos, velocity, 0xFFFFFF, 0.08f * size, 6 + random.nextInt(4));
        }
    }

    /** Three rings pushed out in front of the speaker, one after another. */
    private static void speakerWave(ClientWorld world, Vec3d from, Vec3d toward, float size, int color) {
        Vec3d direction = toward.subtract(from).normalize();
        MinecraftClient client = MinecraftClient.getInstance();
        for (int i = 0; i < 3; i++) {
            Vec3d start = from.add(direction.multiply(0.3 + i * 0.35 * size));
            Particle particle = client.particleManager.addParticle(ModParticles.RING, start.x, start.y, start.z, 0, 0, 0);
            if (particle instanceof FxParticles.Ring ring) {
                ring.grow(0.3f * size, (1.1f + i * 0.3f) * size).move(direction.multiply(0.18 * size)).life(9 + i * 2).opacity(0.85f);
                ring.color(color);
            }
        }
    }

    private static void tvStatic(ClientWorld world, Vec3d pos, float size) {
        Random random = world.random;
        for (int i = 0; i < (int) (18 * size); i++) {
            world.addParticle(ModParticles.STATIC, pos.x + random.nextGaussian() * 0.35 * size,
                    pos.y + random.nextGaussian() * 0.3 * size, pos.z + random.nextGaussian() * 0.35 * size, 0, 0, 0);
        }
    }

    private static void splash(ClientWorld world, Vec3d pos, float size) {
        Random random = world.random;
        for (int i = 0; i < (int) (24 * size); i++) {
            world.addParticle(ParticleTypes.SPLASH, pos.x + random.nextGaussian() * 0.3 * size, pos.y,
                    pos.z + random.nextGaussian() * 0.3 * size, random.nextGaussian() * 0.1, 0.25 + random.nextDouble() * 0.2,
                    random.nextGaussian() * 0.1);
        }
        for (int i = 0; i < (int) (6 * size); i++) {
            world.addParticle(ParticleTypes.BUBBLE_POP, pos.x + random.nextGaussian() * 0.3 * size, pos.y + 0.2,
                    pos.z + random.nextGaussian() * 0.3 * size, 0.0, 0.05, 0.0);
        }
    }

    /** A thin colored ring racing out along the ground: a scan sweep, a flash going off. */
    private static void ring(MinecraftClient client, Vec3d pos, float radius, int color) {
        for (int i = 0; i < 2; i++) {
            Particle particle = client.particleManager.addParticle(ModParticles.RING_FLAT, pos.x, pos.y + 0.1 + i * 0.6, pos.z, 0, 0, 0);
            if (particle instanceof FxParticles.Ring ring) {
                ring.grow(0.3f, radius * (1.0f - i * 0.15f)).life(16 + i * 4).opacity(0.9f);
                ring.color(color);
            }
        }
    }

    /**
     * Dark smoke billowing out with flickers of static: a TV Man blinking out of place. Where they reappear
     * it's a lighter puff, kept low so it doesn't fill their own view.
     */
    private static void teleport(ClientWorld world, Vec3d pos, float size, boolean arrival) {
        Random random = world.random;
        int puffs = (int) ((arrival ? 8 : 26) * size);
        double height = arrival ? 0.6 : 1.9;
        for (int i = 0; i < puffs; i++) {
            double x = pos.x + random.nextGaussian() * (arrival ? 0.5 : 0.3) * size;
            double y = pos.y + random.nextDouble() * height * size;
            double z = pos.z + random.nextGaussian() * (arrival ? 0.5 : 0.3) * size;
            ParticleEffect smoke = arrival ? ParticleTypes.SMOKE : i % 2 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SQUID_INK;
            world.addParticle(smoke, x, y, z, random.nextGaussian() * 0.04, 0.02 + random.nextDouble() * 0.04,
                    random.nextGaussian() * 0.04);
        }
        tvStatic(world, pos.add(0.0, (arrival ? 0.4 : 1.0) * size, 0.0), (arrival ? 0.8f : 1.2f) * size);
    }

    /** Light pouring out of a TV screen: bright glows and flames spreading through a cone. */
    private static void cone(ClientWorld world, Vec3d from, Vec3d toward, float length, int color) {
        Vec3d direction = toward.subtract(from).normalize();
        Random random = world.random;
        glow(from.add(direction.multiply(0.4)), Vec3d.ZERO, 0xFFFFFF, 1.2f, 5);
        glow(from.add(direction.multiply(0.6)), Vec3d.ZERO, color, 2.0f, 7);
        for (int i = 0; i < (int) (length * 12); i++) {
            double d = random.nextDouble();
            Vec3d spread = new Vec3d(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).multiply(0.06 + d * 0.12);
            Vec3d velocity = direction.multiply(0.35 + random.nextDouble() * 0.25).add(spread).multiply(length / 8.0);
            Vec3d start = from.add(direction.multiply(0.5));
            if (i % 3 == 0) {
                world.addParticle(ParticleTypes.FLAME, start.x, start.y, start.z, velocity.x, velocity.y, velocity.z);
            } else {
                glow(start, velocity, i % 2 == 0 ? color : 0xFFF2B0, 0.22f + random.nextFloat() * 0.25f, 10 + random.nextInt(6));
            }
        }
    }

    /** Sparks glancing off a shield. */
    private static void sparks(ClientWorld world, Vec3d pos, Vec3d toward, int color) {
        Vec3d direction = toward.subtract(pos).normalize();
        Random random = world.random;
        glow(pos, Vec3d.ZERO, color, 0.6f, 4);
        for (int i = 0; i < 10; i++) {
            Vec3d velocity = direction.multiply(0.15).add(random.nextGaussian() * 0.12, random.nextGaussian() * 0.12 + 0.08,
                    random.nextGaussian() * 0.12);
            FxParticles.Glow spark = glow(pos, velocity, i % 2 == 0 ? color : 0xFFFFFF, 0.05f, 8 + random.nextInt(6));
            if (spark != null) {
                spark.gravity(0.5f);
            }
        }
    }

    private static void notes(ClientWorld world, Vec3d pos, float radius) {
        Random random = world.random;
        for (int i = 0; i < 6; i++) {
            float angle = random.nextFloat() * MathHelper.TAU;
            double r = radius * random.nextDouble();
            // A note particle's x velocity picks its color (0 to 1 around the color wheel).
            world.addParticle(ParticleTypes.NOTE, pos.x + MathHelper.cos(angle) * r, pos.y + 1.0 + random.nextDouble() * 1.2,
                    pos.z + MathHelper.sin(angle) * r, random.nextDouble(), 0.0, 0.0);
        }
    }

    @Nullable
    private static FxParticles.Glow glow(Vec3d pos, Vec3d velocity, int color, float size, int life) {
        Particle particle = MinecraftClient.getInstance().particleManager.addParticle(ModParticles.GLOW,
                pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        if (particle instanceof FxParticles.Glow glow) {
            glow.color(color).size(size).life(life);
            return glow;
        }
        return null;
    }

    // ---------------------------------------------------------------- screen shake

    /** The view offset for this frame, {yaw, pitch} in degrees. Read by CameraMixin. */
    public static float[] shakeOffset(float tickDelta) {
        if (trauma <= 0.0f) {
            return null;
        }
        float amount = trauma * trauma * MAX_SHAKE_DEGREES;
        float time = ticks + tickDelta;
        // Two wobbling waves per axis, so the shake doesn't look like a regular back-and-forth.
        float yaw = (MathHelper.sin(time * 1.9f) + 0.5f * MathHelper.sin(time * 4.3f + 1.0f)) * amount * 0.6f;
        float pitch = (MathHelper.cos(time * 2.3f) + 0.5f * MathHelper.sin(time * 5.1f + 2.0f)) * amount * 0.6f;
        return new float[]{yaw, pitch};
    }

    // ---------------------------------------------------------------- beams

    /**
     * Each beam is two strips facing the camera, drawn with additive blending: a wide, faint, colored glow
     * and a thin bright core. Both shrink and fade over a few ticks.
     */
    private static void renderBeams(WorldRenderContext context) {
        if (BEAMS.isEmpty()) {
            return;
        }
        MatrixStack matrices = context.matrixStack();
        Vec3d cam = context.camera().getPos();
        VertexConsumerProvider.Immediate consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getLightning());
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (Beam beam : BEAMS) {
            float age = (ticks - beam.startTick) + context.tickDelta();
            float life = 1.0f - MathHelper.clamp(age / BEAM_TICKS, 0.0f, 1.0f);
            if (life <= 0.0f) {
                continue;
            }
            Vec3d a = beam.from.subtract(cam);
            Vec3d b = beam.to.subtract(cam);
            float r = (beam.color >> 16 & 0xFF) / 255.0f;
            float g = (beam.color >> 8 & 0xFF) / 255.0f;
            float bl = (beam.color & 0xFF) / 255.0f;
            strip(buffer, matrix, a, b, beam.width * (0.6f + 0.4f * life), r, g, bl, 0.45f * life);
            strip(buffer, matrix, a, b, beam.width * 0.3f * life, Math.min(1.0f, r + 0.6f), Math.min(1.0f, g + 0.6f),
                    Math.min(1.0f, bl + 0.6f), 0.95f * life);
        }
        consumers.draw(RenderLayer.getLightning());
    }

    /** A flat strip from a to b, turned to face the camera (which sits at the origin here). */
    private static void strip(VertexConsumer buffer, Matrix4f matrix, Vec3d a, Vec3d b, float width,
                              float r, float g, float bl, float alpha) {
        Vec3d direction = b.subtract(a);
        Vec3d sideA = direction.crossProduct(a).normalize().multiply(width / 2);
        Vec3d sideB = direction.crossProduct(b).normalize().multiply(width / 2);
        Vec3d[] quad = {a.add(sideA), a.subtract(sideA), b.subtract(sideB), b.add(sideB)};
        // Both windings, in case the strip is seen from behind.
        for (int side = 0; side < 2; side++) {
            for (int i = 0; i < 4; i++) {
                Vec3d v = quad[side == 0 ? i : 3 - i];
                buffer.vertex(matrix, (float) v.x, (float) v.y, (float) v.z).color(r, g, bl, alpha).next();
            }
        }
    }
}
