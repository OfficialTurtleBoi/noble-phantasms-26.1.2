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

public class GungnirRuneParticle extends SingleQuadParticle {
    private static final float FADE_START = 0.6F;
    private GungnirRuneParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                                TextureAtlasSprite sprite, RandomSource random) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprite);
        setParticleSpeed(xSpeed, ySpeed, zSpeed);
        hasPhysics = false;
        gravity = -0.025F;
        friction = 0.94F;
        lifetime = 30 + random.nextInt(21);
        quadSize = 0.04F + random.nextFloat() * 0.05F;
        float brightness = 0.55F + random.nextFloat() * 0.45F;
        rCol = 0.75F * brightness;
        gCol = 0.8F * brightness;
        bCol = brightness;
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
        float progress = (age + partialTick) / lifetime;
        float fade = 1.0F - Mth.clamp((progress - FADE_START) / (1.0F - FADE_START), 0.0F, 1.0F);
        setAlpha(fade);
        super.extract(renderState, camera, partialTick);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType particleType, ClientLevel level,
                                       double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                                       RandomSource random) {
            return new GungnirRuneParticle(level, x, y, z, xSpeed, ySpeed, zSpeed,
                    spriteSet.get(random), random);
        }
    }
}
