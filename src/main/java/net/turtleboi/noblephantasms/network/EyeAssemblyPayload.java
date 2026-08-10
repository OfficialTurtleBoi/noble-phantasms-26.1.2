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
import net.turtleboi.noblephantasms.item.custom.EyeOfHorusItem;

public record EyeAssemblyPayload(long seed) implements CustomPacketPayload {
    public static final Type<EyeAssemblyPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "eye_of_horus_assembly_complete"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeAssemblyPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_LONG, EyeAssemblyPayload::seed,
                    EyeAssemblyPayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, EyeAssemblyPayload::handle);
    }

    private static void handle(EyeAssemblyPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            EyeOfHorusItem.finishAssembly(player, payload.seed());
        }
    }

    @Override
    public Type<EyeAssemblyPayload> type() {
        return TYPE;
    }
}
