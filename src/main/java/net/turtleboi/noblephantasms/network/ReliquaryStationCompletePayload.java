package net.turtleboi.noblephantasms.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.screens.menus.custom.ReliquaryStationMenu;

public record ReliquaryStationCompletePayload(int containerId, Identifier relicId, long seed) implements CustomPacketPayload {
    public static final Type<ReliquaryStationCompletePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "reliquary_station_complete"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReliquaryStationCompletePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ReliquaryStationCompletePayload::containerId,
                    Identifier.STREAM_CODEC, ReliquaryStationCompletePayload::relicId,
                    ByteBufCodecs.VAR_LONG, ReliquaryStationCompletePayload::seed,
                    ReliquaryStationCompletePayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, ReliquaryStationCompletePayload::handle);
    }

    private static void handle(ReliquaryStationCompletePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof ReliquaryStationMenu menu)
                || menu.containerId != payload.containerId()) {
            return;
        }
        ReliquaryStationMenu.ForgeStart start = menu.beginForge(
                player, payload.relicId(), payload.seed());
        if (start == null) {
            return;
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE,
                SoundSource.PLAYERS, 1.0F, 1.25F);
        PacketDistributor.sendToPlayer(player, new ReliquaryStationForgeResultPayload(
                menu.containerId, start.relicId(), start.seed(),
                start.targetMenuSlot(), start.pieceCount()));
    }

    @Override
    public Type<ReliquaryStationCompletePayload> type() {
        return TYPE;
    }
}
