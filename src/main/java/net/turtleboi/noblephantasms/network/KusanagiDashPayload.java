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
import net.turtleboi.noblephantasms.item.custom.KusanagiNoTsurugiItem;

public record KusanagiDashPayload(byte direction) implements CustomPacketPayload {
    public static final Type<KusanagiDashPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "kusanagi_dash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KusanagiDashPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BYTE, KusanagiDashPayload::direction, KusanagiDashPayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, KusanagiDashPayload::handle);
    }

    private static void handle(KusanagiDashPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            KusanagiNoTsurugiItem.tryDash(player, payload.direction());
        }
    }

    @Override
    public Type<KusanagiDashPayload> type() {
        return TYPE;
    }
}
