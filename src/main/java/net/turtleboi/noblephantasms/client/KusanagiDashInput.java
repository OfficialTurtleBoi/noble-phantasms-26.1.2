package net.turtleboi.noblephantasms.client;

import java.util.EnumMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.network.KusanagiDashPayload;

public final class KusanagiDashInput {
    private static final int DOUBLE_TAP_WINDOW_TICKS = 7;
    private static final EnumMap<Direction, TapState> TAP_STATES = new EnumMap<>(Direction.class);
    private static int tick;

    static {
        for (Direction direction : Direction.values()) {
            TAP_STATES.put(direction, new TapState());
        }
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        tick++;
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            reset();
            return;
        }

        ItemStack kusanagi = getHeldKusanagi(player);
        if (kusanagi == null) {
            reset();
            return;
        }

        for (Direction direction : Direction.values()) {
            TapState state = TAP_STATES.get(direction);
            boolean down = getKey(minecraft.options, direction).isDown();
            if (down && !state.wasDown) {
                if (state.hasFirstTap && tick - state.lastPressTick <= DOUBLE_TAP_WINDOW_TICKS) {
                    ClientPacketDistributor.sendToServer(new KusanagiDashPayload(direction.id));
                    state.hasFirstTap = false;
                } else {
                    state.lastPressTick = tick;
                    state.hasFirstTap = true;
                }
            }
            state.wasDown = down;
        }
    }

    private static ItemStack getHeldKusanagi(LocalPlayer player) {
        if (player.getMainHandItem().is(ModItems.KUSANAGI_NO_TSURUGI)) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(ModItems.KUSANAGI_NO_TSURUGI)) {
            return player.getOffhandItem();
        }
        return null;
    }

    private static KeyMapping getKey(Options options, Direction direction) {
        return switch (direction) {
            case FORWARD -> options.keyUp;
            case BACKWARD -> options.keyDown;
            case LEFT -> options.keyLeft;
            case RIGHT -> options.keyRight;
        };
    }

    private static void reset() {
        for (TapState state : TAP_STATES.values()) {
            state.wasDown = false;
            state.hasFirstTap = false;
        }
    }

    private enum Direction {
        FORWARD((byte) 0),
        BACKWARD((byte) 1),
        LEFT((byte) 2),
        RIGHT((byte) 3);

        private final byte id;

        Direction(byte id) {
            this.id = id;
        }
    }

    private static final class TapState {
        private boolean wasDown;
        private boolean hasFirstTap;
        private int lastPressTick;
    }
}
