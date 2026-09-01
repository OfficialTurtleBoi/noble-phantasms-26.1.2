package net.turtleboi.noblephantasms.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.ui.RelicFragmentRevealHud;
import net.turtleboi.noblephantasms.relic.RelicFragmentData;

public record RelicFragmentRevealPayload(RelicFragmentData fragment) implements CustomPacketPayload {
    public static final Type<RelicFragmentRevealPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "relic_fragment_reveal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RelicFragmentRevealPayload> STREAM_CODEC =
            StreamCodec.composite(RelicFragmentData.STREAM_CODEC,
                    RelicFragmentRevealPayload::fragment, RelicFragmentRevealPayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, RelicFragmentRevealPayload::handle);
    }

    private static void handle(RelicFragmentRevealPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RelicFragmentRevealHud.show(payload.fragment()));
    }

    @Override
    public Type<RelicFragmentRevealPayload> type() {
        return TYPE;
    }
}
