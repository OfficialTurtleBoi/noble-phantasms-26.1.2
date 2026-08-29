package net.turtleboi.noblephantasms.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record FireFangsParticleOptions(float damage, float hitboxWidth, float hitboxHeight, int targetId)
        implements ParticleOptions {
    public static final MapCodec<FireFangsParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("damage").forGetter(FireFangsParticleOptions::damage),
            Codec.FLOAT.fieldOf("hitbox_width").forGetter(FireFangsParticleOptions::hitboxWidth),
            Codec.FLOAT.fieldOf("hitbox_height").forGetter(FireFangsParticleOptions::hitboxHeight),
            Codec.INT.fieldOf("target_id").forGetter(FireFangsParticleOptions::targetId)
    ).apply(instance, FireFangsParticleOptions::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FireFangsParticleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, FireFangsParticleOptions::damage,
                    ByteBufCodecs.FLOAT, FireFangsParticleOptions::hitboxWidth,
                    ByteBufCodecs.FLOAT, FireFangsParticleOptions::hitboxHeight,
                    ByteBufCodecs.VAR_INT, FireFangsParticleOptions::targetId,
                    FireFangsParticleOptions::new);

    public FireFangsParticleOptions {
        damage = Math.clamp(damage, 0.0F, 1000.0F);
        hitboxWidth = Math.clamp(hitboxWidth, 0.1F, 64.0F);
        hitboxHeight = Math.clamp(hitboxHeight, 0.1F, 64.0F);
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.FIRE_FANGS.get();
    }
}
