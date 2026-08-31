package net.turtleboi.noblephantasms.particle.custom;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.turtleboi.noblephantasms.particle.ModParticles;

public final class ApilolliCloudParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final float maximumAlpha;
    private final int anchorId;
    private double anchorX;
    private double anchorY;
    private double anchorZ;

    public static void registerProvider(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.APILOLLI_CLOUD.get(), Provider::new);
    }

    private ApilolliCloudParticle(ClientLevel level, double x, double y, double z,
                                  int anchorId, float sizeScale, SpriteSet sprites, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.sprites = sprites;
        this.anchorId = anchorId;
        Entity anchor = level.getEntity(anchorId);
        anchorX = anchor == null ? Double.NaN : anchor.getX();
        anchorY = anchor == null ? Double.NaN : anchor.getY();
        anchorZ = anchor == null ? Double.NaN : anchor.getZ();
        hasPhysics = false;
        lifetime = 16 + random.nextInt(7);
        friction = 0.92F;
        quadSize = (0.17F + random.nextFloat() * 0.065F) * sizeScale;
        maximumAlpha = 0.52F + random.nextFloat() * 0.16F;
        float shade = 0.91F + random.nextFloat() * 0.09F;
        setColor(shade, shade, shade);
        roll = random.nextFloat() * Mth.TWO_PI;
        oRoll = roll;
        setAlpha(0.0F);
        setSpriteFromAge(sprites);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float progress = (age + partialTick) / lifetime;
        return quadSize * Mth.lerp(Mth.clamp(progress * 5.0F, 0.0F, 1.0F), 0.65F, 1.0F);
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
        float progress = (age + partialTick) / lifetime;
        float fadeIn = Mth.clamp(progress * 5.0F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((1.0F - progress) * 3.2F, 0.0F, 1.0F);
        setAlpha(maximumAlpha * Math.min(fadeIn, fadeOut));
        super.extract(renderState, camera, partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            Entity anchor = level.getEntity(anchorId);
            if (anchor != null) {
                if (!Double.isNaN(anchorX)) {
                    setPos(x + anchor.getX() - anchorX,
                            y + anchor.getY() - anchorY,
                            z + anchor.getZ() - anchorZ);
                }
                anchorX = anchor.getX();
                anchorY = anchor.getY();
                anchorZ = anchor.getZ();
            }
            setSpriteFromAge(sprites);
        }
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType particleType, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed,
                                       RandomSource random) {
            float sizeScale = ySpeed > 0.0 ? (float) ySpeed : 1.0F;
            return new ApilolliCloudParticle(
                    level, x, y, z, (int) xSpeed, sizeScale, sprites, sprites.get(random));
        }
    }
}
