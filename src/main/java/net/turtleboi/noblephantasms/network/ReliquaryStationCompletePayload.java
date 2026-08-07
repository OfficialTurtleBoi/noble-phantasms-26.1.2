package net.turtleboi.noblephantasms.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.screens.menus.custom.ReliquaryStationMenu;

public record ReliquaryStationCompletePayload(int containerId, long seed) implements CustomPacketPayload {
    public static final Type<ReliquaryStationCompletePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "reliquary_station_complete"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReliquaryStationCompletePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ReliquaryStationCompletePayload::containerId,
                    ByteBufCodecs.VAR_LONG, ReliquaryStationCompletePayload::seed,
                    ReliquaryStationCompletePayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, ReliquaryStationCompletePayload::handle);
    }

    private static void handle(ReliquaryStationCompletePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof ReliquaryStationMenu menu)
                || menu.containerId != payload.containerId()
                || !menu.complete(player, payload.seed())) {
            return;
        }
        Item relic = BuiltInRegistries.ITEM.getValue(menu.relicId());
        ItemStack output = new ItemStack(relic);
        if (!player.getInventory().add(output)) {
            player.drop(output, false);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE,
                SoundSource.PLAYERS, 1.0F, 1.25F);
        player.closeContainer();
    }

    @Override
    public Type<ReliquaryStationCompletePayload> type() {
        return TYPE;
    }
}
