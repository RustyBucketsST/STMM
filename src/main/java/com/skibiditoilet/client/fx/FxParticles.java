package com.skibiditoilet.client.fx;

import com.skibiditoilet.registry.ModParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** The client side of ModParticles: how each particle moves and looks. */
public final class FxParticles {
    private FxParticles() {
    }

    public static void init() {
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        registry.register(ModParticles.GLOW, sprites -> (type, world, x, y, z, vx, vy, vz) ->
                new Glow(world, x, y, z, vx, vy, vz, sprites));
        registry.register(ModParticles.RING, sprites -> (type, world, x, y, z, vx, vy, vz) ->
                new Ring(world, x, y, z, sprites, false));
        registry.register(ModParticles.RING_FLAT, sprites -> (type, world, x, y, z, vx, vy, vz) ->
                new Ring(world, x, y, z, sprites, true));
        registry.register(ModParticles.SWIRL, sprites -> (type, world, x, y, z, vx, vy, vz) ->
                new Swirl(world, x, y, z, vx, vy, vz, sprites));
        registry.register(ModParticles.STATIC, sprites -> (type, world, x, y, z, vx, vy, vz) ->
                new Static(world, x, y, z, sprites));
    }

    /** A particle that glows in the dark, fades out over its life, and can be colored and sized after spawning. */
    public abstract static class Bright extends SpriteBillboardParticle {
        protected float startAlpha = 1.0f;

        protected Bright(ClientWorld world, double x, double y, double z) {
            super(world, x, y, z);
        }

        public Bright color(int rgb) {
            this.setColor((rgb >> 16 & 0xFF) / 255.0f, (rgb >> 8 & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f);
            return this;
        }

        public Bright size(float size) {
            this.scale = size;
            return this;
        }

        public Bright life(int ticks) {
            this.maxAge = ticks;
            return this;
        }

        public Bright opacity(float alpha) {
            this.startAlpha = alpha;
            this.setAlpha(alpha);
            return this;
        }

        @Override
        public void tick() {
            super.tick();
            this.setAlpha(this.startAlpha * (1.0f - (float) this.age / this.maxAge));
        }

        @Override
        public ParticleTextureSheet getType() {
            return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
        }

        @Override
        protected int getBrightness(float tint) {
            return LightmapTextureManager.MAX_LIGHT_COORDINATE;
        }
    }

    /** A soft glowing dot that drifts with the velocity it was given, optionally falling. */
    public static class Glow extends Bright {
        Glow(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
            super(world, x, y, z);
            this.velocityX = vx;
            this.velocityY = vy;
            this.velocityZ = vz;
            this.maxAge = 8;
            this.gravityStrength = 0.0f;
            this.velocityMultiplier = 0.9f;
            this.collidesWithWorld = false;
            this.setSprite(sprites);
        }

        public Glow gravity(float strength) {
            this.gravityStrength = strength;
            this.collidesWithWorld = true;
            return this;
        }
    }

    /** A ring that grows to a target size and fades: upright facing the camera, or flat on the ground. */
    public static class Ring extends Bright {
        private final boolean flat;
        private float startSize = 0.2f;
        private float endSize = 2.0f;

        Ring(ClientWorld world, double x, double y, double z, SpriteProvider sprites, boolean flat) {
            super(world, x, y, z);
            this.flat = flat;
            this.maxAge = 12;
            this.gravityStrength = 0.0f;
            this.velocityX = this.velocityY = this.velocityZ = 0.0;
            this.collidesWithWorld = false;
            this.setSprite(sprites);
        }

        public Ring grow(float from, float to) {
            this.startSize = from;
            this.endSize = to;
            this.scale = from;
            return this;
        }

        public Ring move(Vec3d velocity) {
            this.velocityX = velocity.x;
            this.velocityY = velocity.y;
            this.velocityZ = velocity.z;
            this.velocityMultiplier = 1.0f;
            return this;
        }

        @Override
        public float getSize(float tickDelta) {
            float t = MathHelper.clamp((this.age + tickDelta) / this.maxAge, 0.0f, 1.0f);
            // Eases out: fast at first, slowing as it reaches full size.
            float eased = 1.0f - (1.0f - t) * (1.0f - t);
            return MathHelper.lerp(eased, this.startSize, this.endSize);
        }

        @Override
        public void buildGeometry(VertexConsumer consumer, Camera camera, float tickDelta) {
            if (!this.flat) {
                super.buildGeometry(consumer, camera, tickDelta);
                return;
            }
            Vec3d cam = camera.getPos();
            float x = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - cam.x);
            float y = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - cam.y);
            float z = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - cam.z);
            float s = this.getSize(tickDelta);
            float u0 = this.getMinU(), u1 = this.getMaxU(), v0 = this.getMinV(), v1 = this.getMaxV();
            int light = this.getBrightness(tickDelta);
            // Both windings, so the ring shows from above and below.
            float[][] corners = {{-s, -s, u1, v1}, {-s, s, u1, v0}, {s, s, u0, v0}, {s, -s, u0, v1}};
            for (int side = 0; side < 2; side++) {
                for (int i = 0; i < 4; i++) {
                    float[] c = corners[side == 0 ? i : 3 - i];
                    consumer.vertex(x + c[0], y, z + c[1]).texture(c[2], c[3])
                            .color(this.red, this.green, this.blue, this.alpha).light(light).next();
                }
            }
        }
    }

    /** A water drop circling inward and down, like water going down a flushed toilet. */
    public static class Swirl extends Bright {
        private final double centerX;
        private final double centerZ;
        private final float spin;
        private final float sink;
        private float radius;
        private float angle;

        Swirl(ClientWorld world, double x, double y, double z, double radius, double spin, double sink,
              SpriteProvider sprites) {
            super(world, x, y, z);
            this.centerX = x;
            this.centerZ = z;
            this.radius = (float) radius;
            this.spin = (float) spin;
            this.sink = (float) sink;
            this.angle = this.random.nextFloat() * MathHelper.TAU;
            this.maxAge = 18 + this.random.nextInt(8);
            this.scale = 0.06f + this.random.nextFloat() * 0.05f;
            this.collidesWithWorld = false;
            this.gravityStrength = 0.0f;
            this.setSprite(sprites);
            this.color(0xBFE6FF);
            this.place();
            this.prevPosX = this.x;
            this.prevPosY = this.y;
            this.prevPosZ = this.z;
        }

        private void place() {
            this.setPos(this.centerX + MathHelper.cos(this.angle) * this.radius, this.y,
                    this.centerZ + MathHelper.sin(this.angle) * this.radius);
        }

        @Override
        public void tick() {
            this.prevPosX = this.x;
            this.prevPosY = this.y;
            this.prevPosZ = this.z;
            if (this.age++ >= this.maxAge) {
                this.markDead();
                return;
            }
            this.angle += this.spin;
            this.radius *= 0.94f;
            this.y -= this.sink;
            this.place();
            this.setAlpha(1.0f - (float) this.age / this.maxAge * 0.6f);
        }

        @Override
        protected int getBrightness(float tint) {
            // Water doesn't glow: it's lit like the world around it.
            return WorldRenderer.getLightmapCoordinates(this.world, BlockPos.ofFloored(this.x, this.y, this.z));
        }
    }

    /** A speck of TV static that flickers between frames and grays. */
    public static class Static extends Bright {
        private final SpriteProvider sprites;

        Static(ClientWorld world, double x, double y, double z, SpriteProvider sprites) {
            super(world, x, y, z);
            this.sprites = sprites;
            this.maxAge = 6 + this.random.nextInt(6);
            this.scale = 0.08f + this.random.nextFloat() * 0.1f;
            this.gravityStrength = 0.0f;
            this.collidesWithWorld = false;
            this.velocityX = (this.random.nextDouble() - 0.5) * 0.05;
            this.velocityY = (this.random.nextDouble() - 0.5) * 0.05;
            this.velocityZ = (this.random.nextDouble() - 0.5) * 0.05;
            this.setSprite(sprites.getSprite(this.random));
        }

        @Override
        public void tick() {
            super.tick();
            this.setSprite(this.sprites.getSprite(this.random));
            float gray = 0.4f + this.random.nextFloat() * 0.6f;
            this.setColor(gray, gray, gray);
        }
    }
}
