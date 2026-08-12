package net.turtleboi.noblephantasms.component;

import java.util.UUID;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.relic.RelicFragmentData;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NoblePhantasms.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> KAZAGURUMA_DEPLOYMENT =
            DATA_COMPONENTS.registerComponentType("kazaguruma_deployment", builder -> builder
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WEBEN_SUNLIGHT_CHARGE =
            DATA_COMPONENTS.registerComponentType("weben_sunlight_charge", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TYRFING_CURSE_ACTIVE =
            DATA_COMPONENTS.registerComponentType("tyrfing_curse_active", builder -> builder
                    .persistent(Codec.BOOL)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> TYRFING_CURSE_CHANGED_AT =
            DATA_COMPONENTS.registerComponentType("tyrfing_curse_changed_at", builder -> builder
                    .persistent(Codec.LONG)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RelicFragmentData>> RELIC_FRAGMENT =
            DATA_COMPONENTS.registerComponentType("relic_fragment", builder -> builder
                    .persistent(RelicFragmentData.CODEC)
                    .networkSynchronized(RelicFragmentData.STREAM_CODEC));

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
