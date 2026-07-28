package net.turtleboi.noblephantasms.item.custom;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.turtleboi.noblephantasms.item.ModItems;

public class UchideNoKozuchiItem extends Item {
    private static final int MAX_GROWTH_STEPS = 16;

    public UchideNoKozuchiItem(Properties properties) {
        super(properties
                .axe(ToolMaterial.NETHERITE, 3.0F, -2.8F)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    public static void handleBlockBreak(BreakBlockEvent event) {
        Player player = event.getPlayer();
        ItemStack mallet = player.getMainHandItem();
        if (!mallet.is(ModItems.UCHIDE_NO_KOZUCHI)) {
            return;
        }

        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        if (state.getDestroySpeed(event.getLevel(), pos) < 0.0F) {
            return;
        }

        event.setCanceled(true);
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        event.setNotifyClient(true);

        if (state.getBlock() instanceof BonemealableBlock grower
                && grower.getType() == BonemealableBlock.Type.GROWER
                && growFully(level, pos, grower)) {
            level.levelEvent(1505, pos, 15);
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        ItemStack silkTool = mallet.copy();
        var silkTouch = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
        silkTool.enchant(silkTouch, 1);
        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, silkTool);

        if (!level.destroyBlock(pos, false, player)) {
            return;
        }
        level.levelEvent(2001, pos, Block.getId(state));
        for (ItemStack drop : drops) {
            if (!player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
        }
    }

    private static boolean growFully(ServerLevel level, BlockPos pos, BonemealableBlock grower) {
        boolean grew = false;
        for (int i = 0; i < MAX_GROWTH_STEPS; i++) {
            BlockState before = level.getBlockState(pos);
            if (before.getBlock() != grower || !grower.isValidBonemealTarget(level, pos, before)) {
                break;
            }
            grower.performBonemeal(level, level.getRandom(), pos, before);
            BlockState after = level.getBlockState(pos);
            grew = true;
            if (after == before || after.getBlock() != grower) {
                break;
            }
        }
        return grew;
    }
}
