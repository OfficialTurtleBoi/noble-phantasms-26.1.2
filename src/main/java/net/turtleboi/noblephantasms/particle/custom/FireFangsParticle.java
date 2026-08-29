package net.turtleboi.noblephantasms.particle.custom;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.particle.FireFangsParticleOptions;
import net.turtleboi.noblephantasms.particle.ModParticles;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class FireFangsParticle extends SingleQuadParticle {
    private static final int IMPACT_AGE = 6;
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(
                    NoblePhantasms.MOD_ID, "pipeline/fire_fangs"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();
    private static final Layer LAYER = new Layer(true, TextureAtlas.LOCATION_PARTICLES, PIPELINE);
    private final float damage;
    private final float hitboxWidth;
    private final float hitboxHeight;
    private final int targetId;

    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
    }

    public static void registerProvider(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.FIRE_FANGS.get(), Provider::new);
    }

    private FireFangsParticle(ClientLevel level, double x, double y, double z,
                              TextureAtlasSprite sprite, FireFangsParticleOptions options) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
        damage = options.damage();
        hitboxWidth = options.hitboxWidth();
        hitboxHeight = options.hitboxHeight();
        targetId = options.targetId();
        hasPhysics = false;
        lifetime = 15;
        quadSize = 0.95F;
        setAlpha(0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        Entity target = level.getEntity(targetId);
        if (target instanceof LivingEntity livingTarget) {
            setPos(livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() * 0.5,
                    livingTarget.getZ());
        }
        if (age == IMPACT_AGE) {
            burst();
        }
    }

    @Override
    protected Layer getLayer() {
        return LAYER;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
        float progress = Mth.clamp((age + partialTick) / lifetime, 0.0F, 1.0F);
        float appear = smooth(Mth.clamp(progress / 0.10F, 0.0F, 1.0F));
        float fade = 1.0F - smooth(Mth.clamp((progress - 0.55F) / 0.45F, 0.0F, 1.0F));
        float alpha = appear * fade;

        float halfGap;
        float scale;
        if (progress < 0.22F) {
            float anticipation = smooth(progress / 0.22F);
            halfGap = Mth.lerp(anticipation, 0.52F, 0.74F);
            scale = Mth.lerp(anticipation, 0.96F, 1.08F);
        } else if (progress < 0.40F) {
            float snap = (progress - 0.22F) / 0.18F;
            float weightedSnap = easeOutCubic(snap);
            halfGap = Mth.lerp(weightedSnap, 0.74F, 0.025F);
            scale = Mth.lerp(weightedSnap, 1.08F, 1.00F);
        } else {
            halfGap = 0.025F;
            float impactDecay = 1.0F - smooth(Mth.clamp((progress - 0.40F) / 0.16F, 0.0F, 1.0F));
            scale = 1.00F + impactDecay * 0.18F;
        }

        Quaternionf rotation = new Quaternionf(camera.rotation());
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F).rotate(rotation);
        float centerX = (float) (Mth.lerp((double) partialTick, xo, x) - camera.position().x());
        float centerY = (float) (Mth.lerp((double) partialTick, yo, y) - camera.position().y());
        float centerZ = (float) (Mth.lerp((double) partialTick, zo, z) - camera.position().z());
        int color = ARGB.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F);
        int light = 0xF000F0;

        addJaw(renderState, rotation,
                centerX + up.x * halfGap, centerY + up.y * halfGap, centerZ + up.z * halfGap,
                scale, getV0(), getV1(), color, light);
        addJaw(renderState, rotation,
                centerX - up.x * halfGap, centerY - up.y * halfGap, centerZ - up.z * halfGap,
                scale, getV1(), getV0(), color, light);
    }

    private void addJaw(QuadParticleRenderState renderState, Quaternionf rotation,
                        float x, float y, float z, float scale,
                        float v0, float v1, int color, int light) {
        renderState.add(getLayer(), x, y, z,
                rotation.x, rotation.y, rotation.z, rotation.w,
                scale, getU0(), getU1(), v0, v1, color, light);
    }

    private void burst() {
        int particleCount = Math.clamp(Math.round(10.0F + damage * 3.5F), 10, 240);
        float velocityScale = Math.clamp(0.06F + damage * 0.0025F, 0.06F, 0.22F);
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (random.nextDouble() - 0.5) * hitboxWidth;
            double offsetY = (random.nextDouble() - 0.5) * hitboxHeight;
            double offsetZ = (random.nextDouble() - 0.5) * hitboxWidth;
            double directionX = random.nextGaussian();
            double directionY = random.nextGaussian();
            double directionZ = random.nextGaussian();
            double directionLength = Math.sqrt(
                    directionX * directionX + directionY * directionY + directionZ * directionZ);
            double speed = velocityScale * (0.35 + random.nextDouble() * 0.65);
            double velocityX = directionX / directionLength * speed;
            double velocityY = directionY / directionLength * speed;
            double velocityZ = directionZ / directionLength * speed;
            level.addParticle(i < particleCount * 0.7F ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
                    x + offsetX, y + offsetY, z + offsetZ,
                    velocityX, velocityY, velocityZ);
        }
        level.playLocalSound(x, y, z, SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS, 0.9F, 0.78F + random.nextFloat() * 0.08F, false);
        level.playLocalSound(x, y, z, SoundEvents.RAVAGER_ATTACK,
                SoundSource.PLAYERS, 0.75F, 1.35F + random.nextFloat() * 0.10F, false);
    }

    private static float smooth(float progress) {
        return progress * progress * (3.0F - 2.0F * progress);
    }

    private static float easeOutCubic(float progress) {
        float inverse = 1.0F - Mth.clamp(progress, 0.0F, 1.0F);
        return 1.0F - inverse * inverse * inverse;
    }

    public static final class Provider implements ParticleProvider<FireFangsParticleOptions> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(FireFangsParticleOptions options, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed,
                                       RandomSource random) {
            return new FireFangsParticle(level, x, y, z, spriteSet.get(random), options);
        }
    }
}
