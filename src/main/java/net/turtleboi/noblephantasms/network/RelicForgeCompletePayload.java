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
import net.turtleboi.noblephantasms.screens.menus.custom.RelicForgeMenu;

public record RelicForgeCompletePayload(int containerId, long seed) implements CustomPacketPayload {
    public static final Type<RelicForgeCompletePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "relic_forge_complete"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RelicForgeCompletePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, RelicForgeCompletePayload::containerId,
                    ByteBufCodecs.VAR_LONG, RelicForgeCompletePayload::seed,
                    RelicForgeCompletePayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, RelicForgeCompletePayload::handle);
    }

    private static void handle(RelicForgeCompletePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof RelicForgeMenu menu)
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
    public Type<RelicForgeCompletePayload> type() {
        return TYPE;
    }
}
