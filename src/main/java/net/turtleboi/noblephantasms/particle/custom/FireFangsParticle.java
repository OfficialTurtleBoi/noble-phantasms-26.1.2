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

public final class FireFangsParticle extends SingleQuadParticle {
    public static void registerProvider(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.FIRE_FANGS.get(), Provider::new);
    }

    private FireFangsParticle(ClientLevel level, double x, double y, double z,
                              TextureAtlasSprite sprite) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
        hasPhysics = false;
        lifetime = 12;
        quadSize = 1.35F;
        setAlpha(0.0F);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
        float progress = (age + partialTick) / lifetime;
        setAlpha(Mth.clamp(Math.min(progress * 5.0F, (1.0F - progress) * 4.0F), 0.0F, 1.0F));
        quadSize = Mth.lerp(progress, 1.6F, 0.72F);
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
            return new FireFangsParticle(level, x, y, z, spriteSet.get(random));
        }
    }
}
