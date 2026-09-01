package net.turtleboi.noblephantasms.particle.custom;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.turtleboi.noblephantasms.particle.ModParticles;

public final class StunnedParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    public static void registerProvider(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.STUNNED.get(), Provider::new);
    }

    private StunnedParticle(ClientLevel level, double x, double y, double z,
                            double xSpeed, double ySpeed, double zSpeed,
                            SpriteSet sprites, TextureAtlasSprite sprite, RandomSource random) {
        super(level, x, y, z, 0.0, 1.0, 0.0, sprite);
        this.sprites = sprites;
        friction = 0.5F;
        lifetime = (int) (10.0D / (random.nextDouble() * 0.6D + 0.4D));
        quadSize *= random.nextFloat() * 0.85F + 0.35F;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        alpha = Mth.clamp(1.0F - age / (float) Math.max(1, lifetime), 0.0F, 1.0F);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
        alpha = Mth.clamp(1.0F - (age + partialTick) / lifetime, 0.0F, 1.0F);
        super.extract(renderState, camera, partialTick);
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
            return new StunnedParticle(level, x, y, z, xSpeed, ySpeed, zSpeed,
                    sprites, sprites.get(random), random);
        }
    }
}
