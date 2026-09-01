package net.turtleboi.noblephantasms.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.screens.ReliquaryStationScreen;

public record ReliquaryStationForgeResultPayload(int containerId, Identifier relicId,
                                                  long seed, int targetMenuSlot,
                                                  int pieceCount)
        implements CustomPacketPayload {
    public static final Type<ReliquaryStationForgeResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID,
                    "reliquary_station_forge_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReliquaryStationForgeResultPayload>
            STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ReliquaryStationForgeResultPayload::containerId,
                    Identifier.STREAM_CODEC, ReliquaryStationForgeResultPayload::relicId,
                    ByteBufCodecs.VAR_LONG, ReliquaryStationForgeResultPayload::seed,
                    ByteBufCodecs.VAR_INT, ReliquaryStationForgeResultPayload::targetMenuSlot,
                    ByteBufCodecs.VAR_INT, ReliquaryStationForgeResultPayload::pieceCount,
                    ReliquaryStationForgeResultPayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC,
                ReliquaryStationForgeResultPayload::handle);
    }

    private static void handle(ReliquaryStationForgeResultPayload payload,
                               IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof ReliquaryStationScreen screen
                    && screen.getMenu().containerId == payload.containerId()) {
                screen.beginForgeAnimation(payload.relicId(), payload.seed(),
                        payload.targetMenuSlot(), payload.pieceCount());
            }
        });
    }

    @Override
    public Type<ReliquaryStationForgeResultPayload> type() {
        return TYPE;
    }
}
