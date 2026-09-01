package net.turtleboi.noblephantasms.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.relic.RelicFragmentBrushing;

public record RelicFragmentBrushPayload(int targetId, byte hand) implements CustomPacketPayload {
    public static final Type<RelicFragmentBrushPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "relic_fragment_brush"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RelicFragmentBrushPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, RelicFragmentBrushPayload::targetId,
                    ByteBufCodecs.BYTE, RelicFragmentBrushPayload::hand,
                    RelicFragmentBrushPayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, RelicFragmentBrushPayload::handle);
    }

    private static void handle(RelicFragmentBrushPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        InteractionHand hand = switch (payload.hand()) {
            case 0 -> InteractionHand.MAIN_HAND;
            case 1 -> InteractionHand.OFF_HAND;
            default -> null;
        };
        if (hand != null) {
            RelicFragmentBrushing.startServer(player, hand, payload.targetId());
        }
    }

    @Override
    public Type<RelicFragmentBrushPayload> type() {
        return TYPE;
    }
}
