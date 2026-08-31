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
import net.turtleboi.noblephantasms.screens.menus.custom.MythicalReliquaryMenu;

public record MythicalReliquarySelectPayload(int containerId, Identifier relicId)
        implements CustomPacketPayload {
    public static final Type<MythicalReliquarySelectPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "mythical_reliquary_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MythicalReliquarySelectPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, MythicalReliquarySelectPayload::containerId,
                    Identifier.STREAM_CODEC, MythicalReliquarySelectPayload::relicId,
                    MythicalReliquarySelectPayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, MythicalReliquarySelectPayload::handle);
    }

    private static void handle(MythicalReliquarySelectPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof MythicalReliquaryMenu menu
                && menu.containerId == payload.containerId()) {
            menu.selectRelic(player, payload.relicId());
        }
    }

    @Override
    public Type<MythicalReliquarySelectPayload> type() {
        return TYPE;
    }
}
