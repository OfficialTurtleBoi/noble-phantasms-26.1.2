package net.turtleboi.noblephantasms.component;

import java.util.UUID;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> TECPATL_DEPLOYMENT =
            DATA_COMPONENTS.registerComponentType("tecpatl_deployment", builder -> builder
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TECPATL_RETURNED_SHARDS =
            DATA_COMPONENTS.registerComponentType("tecpatl_returned_shards", builder -> builder
                    .persistent(Codec.INT)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TECPATL_LAUNCHED_SHARDS =
            DATA_COMPONENTS.registerComponentType("tecpatl_launched_shards", builder -> builder
                    .persistent(Codec.INT)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> APILOLLI_FILLED =
            DATA_COMPONENTS.registerComponentType("apilolli_filled", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> KHEPER_SCARAB_ACTIVE =
            DATA_COMPONENTS.registerComponentType("kheper_scarab_active", builder -> builder
                    .persistent(Codec.BOOL)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WEBEN_SUNLIGHT_CHARGE =
            DATA_COMPONENTS.registerComponentType("weben_sunlight_charge", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> EXCALIBUR_CHARGE =
            DATA_COMPONENTS.registerComponentType("excalibur_charge", builder -> builder
                    .persistent(Codec.INT)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> EXCALIBUR_ENERGY =
            DATA_COMPONENTS.registerComponentType("excalibur_energy", builder -> builder
                    .persistent(Codec.INT)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> EXCALIBUR_NEXT_RECHARGE_TICK =
            DATA_COMPONENTS.registerComponentType("excalibur_next_recharge_tick", builder -> builder
                    .persistent(Codec.LONG)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> EXCALIBUR_RECHARGE_TICK =
            DATA_COMPONENTS.registerComponentType("excalibur_recharge_tick", builder -> builder
                    .persistent(Codec.LONG)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> EXCALIBUR_RELEASE_TICK =
            DATA_COMPONENTS.registerComponentType("excalibur_release_tick", builder -> builder
                    .persistent(Codec.LONG)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> PRIDWEN_ENERGY =
            DATA_COMPONENTS.registerComponentType("pridwen_energy", builder -> builder
                    .persistent(Codec.FLOAT)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.FLOAT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> PRIDWEN_BROKEN =
            DATA_COMPONENTS.registerComponentType("pridwen_broken", builder -> builder
                    .persistent(Codec.BOOL)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> PRIDWEN_NEXT_RECHARGE_TICK =
            DATA_COMPONENTS.registerComponentType("pridwen_next_recharge_tick", builder -> builder
                    .persistent(Codec.LONG)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GRAM_HIT_COUNT =
            DATA_COMPONENTS.registerComponentType("gram_hit_count", builder -> builder
                    .persistent(Codec.INT)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> GRAM_READY_TICK =
            DATA_COMPONENTS.registerComponentType("gram_ready_tick", builder -> builder
                    .persistent(Codec.LONG)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> GRAM_LAST_HIT_TICK =
            DATA_COMPONENTS.registerComponentType("gram_last_hit_tick", builder -> builder
                    .persistent(Codec.LONG)
                    .ignoreSwapAnimation());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HOLY_GRAIL_CHARGE =
            DATA_COMPONENTS.registerComponentType("holy_grail_charge", builder -> builder
                    .persistent(Codec.INT)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> HOLY_GRAIL_NEXT_RECHARGE_TICK =
            DATA_COMPONENTS.registerComponentType("holy_grail_next_recharge_tick", builder -> builder
                    .persistent(Codec.LONG)
                    .ignoreSwapAnimation()
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> MYTHICAL_RELIQUARY_FOCUS =
            DATA_COMPONENTS.registerComponentType("mythical_reliquary_focus", builder -> builder
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC));

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
