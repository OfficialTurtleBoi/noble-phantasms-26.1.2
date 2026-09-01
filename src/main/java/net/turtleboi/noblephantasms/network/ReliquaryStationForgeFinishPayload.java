package net.turtleboi.noblephantasms.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.screens.menus.custom.ReliquaryStationMenu;

public record ReliquaryStationForgeFinishPayload(int containerId, Identifier relicId,
                                                  long seed)
        implements CustomPacketPayload {
    public static final Type<ReliquaryStationForgeFinishPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID,
                    "reliquary_station_forge_finish"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReliquaryStationForgeFinishPayload>
            STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ReliquaryStationForgeFinishPayload::containerId,
                    Identifier.STREAM_CODEC, ReliquaryStationForgeFinishPayload::relicId,
                    ByteBufCodecs.VAR_LONG, ReliquaryStationForgeFinishPayload::seed,
                    ReliquaryStationForgeFinishPayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC,
                ReliquaryStationForgeFinishPayload::handle);
    }

    private static void handle(ReliquaryStationForgeFinishPayload payload,
                               IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof ReliquaryStationMenu menu
                && menu.containerId == payload.containerId()) {
            menu.finishForge(player, payload.relicId(), payload.seed());
        }
    }

    @Override
    public Type<ReliquaryStationForgeFinishPayload> type() {
        return TYPE;
    }
}
