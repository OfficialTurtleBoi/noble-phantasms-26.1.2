package net.turtleboi.noblephantasms.network;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;

public record TrophySupportPayload(UUID targetId, boolean supported) implements CustomPacketPayload {
    public static final Type<TrophySupportPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "trophy_support"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrophySupportPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, TrophySupportPayload::targetId,
                    ByteBufCodecs.BOOL, TrophySupportPayload::supported,
                    TrophySupportPayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, TrophySupportPayload::handle);
    }

    private static void handle(TrophySupportPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            BertilakItem.reportTrophySupport(player, payload.targetId(), payload.supported());
        }
    }

    @Override
    public Type<TrophySupportPayload> type() {
        return TYPE;
    }
}
