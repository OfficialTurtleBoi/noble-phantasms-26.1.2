package net.turtleboi.noblephantasms.item.custom;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.turtleboi.noblephantasms.item.ModItems;

public class YamawariItem extends AxeItem {
    private static final int MAX_TREE_LOGS = 256;

    public YamawariItem(Properties properties) {
        super(ToolMaterial.NETHERITE, 6.0F, -3.0F,
                properties.rarity(Rarity.EPIC).fireResistant());
    }

    public static void handleBlockBreak(BreakBlockEvent event) {
        Player player = event.getPlayer();
        BlockState originState = event.getState();
        if (!player.getMainHandItem().is(ModItems.YAMAWARI)
                || !originState.is(BlockTags.LOGS)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos origin = event.getPos();
        Queue<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(origin);
        visited.add(origin);

        while (!pending.isEmpty() && visited.size() < MAX_TREE_LOGS) {
            BlockPos current = pending.remove();
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }
                        BlockPos next = current.offset(x, y, z);
                        if (Math.abs(next.getX() - origin.getX()) > 8
                                || Math.abs(next.getZ() - origin.getZ()) > 8
                                || Math.abs(next.getY() - origin.getY()) > 32
                                || visited.contains(next)
                                || !level.getBlockState(next).is(BlockTags.LOGS)) {
                            continue;
                        }
                        visited.add(next.immutable());
                        pending.add(next.immutable());
                    }
                }
            }
        }

        visited.remove(origin);
        visited.stream()
                .sorted((left, right) -> Integer.compare(right.getY(), left.getY()))
                .forEach(pos -> level.destroyBlock(pos, true, player));
    }
}
