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
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.turtleboi.noblephantasms.particle.ModParticles;

public final class ChilledParticle extends SingleQuadParticle {
    public static void registerProvider(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.CHILLED.get(), Provider::new);
    }

    private ChilledParticle(ClientLevel level, double x, double y, double z,
                            double xSpeed, double ySpeed, double zSpeed,
                            TextureAtlasSprite sprite, RandomSource random) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprite);
        hasPhysics = true;
        friction = 0.8F;
        gravity = 0.1F;
        lifetime = (int) (24.0F / (random.nextFloat() * 0.8F + 0.2F));
        quadSize *= random.nextFloat() * 0.55F + 0.35F;
        float lighten = random.nextFloat();
        rCol = Mth.lerp(lighten, 128.0F / 255.0F, 1.0F);
        gCol = Mth.lerp(lighten, 233.0F / 255.0F, 1.0F);
        bCol = 1.0F;
    }

    @Override
    public void tick() {
        float progress = age / (float) Math.max(1, lifetime);
        gravity = Mth.lerp(Mth.clamp((progress - 0.5F) * 2.0F, 0.0F, 1.0F), 0.1F, 1.0F);
        super.tick();
        alpha = 1.0F - age / (float) Math.max(1, lifetime);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
        alpha = Mth.clamp(1.0F - (age + partialTick) / lifetime, 0.0F, 1.0F);
        super.extract(renderState, camera, partialTick);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType particleType, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed,
                                       RandomSource random) {
            return new ChilledParticle(level, x, y, z, xSpeed, ySpeed, zSpeed,
                    spriteSet.get(random), random);
        }
    }
}
