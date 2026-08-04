package net.turtleboi.noblephantasms.particle.custom;

import static net.turtleboi.noblephantasms.client.renderer.LuminousRenderer.BERTILAK_BRIGHT_GREEN;
import static net.turtleboi.noblephantasms.client.renderer.LuminousRenderer.BERTILAK_DARK_GREEN;
import static net.turtleboi.noblephantasms.client.renderer.LuminousRenderer.BERTILAK_GREEN;
import static net.turtleboi.noblephantasms.client.renderer.LuminousRenderer.BERTILAK_LIGHT_GREEN;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FallingLeavesParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.particle.ModParticles;

public class CovenantLeafParticle extends FallingLeavesParticle {
    private static final float LIGHTEN_AMOUNT = 0.3F;
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "pipeline/covenant_leaf"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();
    private static final Layer LAYER = new Layer(true, TextureAtlas.LOCATION_PARTICLES, PIPELINE);

    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
    }

    public static void registerProvider(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.COVENANT_LEAF.get(), Provider::new);
    }

    private CovenantLeafParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite,
                                  RandomSource random) {
        super(level, x, y, z, sprite, 0.07F, 10.0F, true, false, 2.0F, 0.021F);
        int color = getRandomColor(random);
        setColor(ARGB.red(color) / 255.0F, ARGB.green(color) / 255.0F, ARGB.blue(color) / 255.0F);
    }

    @Override
    public Layer getLayer() {
        return LAYER;
    }

    private static int getRandomColor(RandomSource random) {
        float colorCycle = random.nextFloat() * 4.0F;
        int color;
        if (colorCycle < 1.0F) {
            color = ARGB.srgbLerp(colorCycle, BERTILAK_GREEN, BERTILAK_BRIGHT_GREEN);
        } else if (colorCycle < 2.0F) {
            color = ARGB.srgbLerp(colorCycle - 1.0F, BERTILAK_BRIGHT_GREEN, BERTILAK_LIGHT_GREEN);
        } else if (colorCycle < 3.0F) {
            color = ARGB.srgbLerp(colorCycle - 2.0F, BERTILAK_LIGHT_GREEN, BERTILAK_DARK_GREEN);
        } else {
            color = ARGB.srgbLerp(colorCycle - 3.0F, BERTILAK_DARK_GREEN, BERTILAK_GREEN);
        }
        return ARGB.srgbLerp(LIGHTEN_AMOUNT, color, 0xFFFFFF);
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
            return new CovenantLeafParticle(level, x, y, z, spriteSet.get(random), random);
        }
    }
}
