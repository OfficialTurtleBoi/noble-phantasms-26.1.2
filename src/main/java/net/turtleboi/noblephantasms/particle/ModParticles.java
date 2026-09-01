package net.turtleboi.noblephantasms.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, NoblePhantasms.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GUNGNIR_RUNE =
            PARTICLE_TYPES.register("gungnir_rune", GungnirRuneType::new);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COVENANT_LEAF =
            PARTICLE_TYPES.register("covenant_leaf", CovenantLeafType::new);
    public static final DeferredHolder<ParticleType<?>, ParticleType<FireFangsParticleOptions>> FIRE_FANGS =
            PARTICLE_TYPES.register("fire_fangs", FireFangsType::new);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> APILOLLI_CLOUD =
            PARTICLE_TYPES.register("apilolli_cloud", ApilolliCloudType::new);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHILLED =
            PARTICLE_TYPES.register("chilled", ChilledType::new);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STUNNED =
            PARTICLE_TYPES.register("stunned", StunnedType::new);

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }

    private static final class GungnirRuneType extends SimpleParticleType {
        private GungnirRuneType() {
            super(false);
        }
    }

    private static final class CovenantLeafType extends SimpleParticleType {
        private CovenantLeafType() {
            super(false);
        }
    }

    private static final class FireFangsType extends ParticleType<FireFangsParticleOptions> {
        private FireFangsType() {
            super(false);
        }

        @Override
        public com.mojang.serialization.MapCodec<FireFangsParticleOptions> codec() {
            return FireFangsParticleOptions.CODEC;
        }

        @Override
        public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf,
                FireFangsParticleOptions> streamCodec() {
            return FireFangsParticleOptions.STREAM_CODEC;
        }
    }

    private static final class ApilolliCloudType extends SimpleParticleType {
        private ApilolliCloudType() {
            super(false);
        }
    }

    private static final class ChilledType extends SimpleParticleType {
        private ChilledType() {
            super(false);
        }
    }

    private static final class StunnedType extends SimpleParticleType {
        private StunnedType() {
            super(false);
        }
    }
}
