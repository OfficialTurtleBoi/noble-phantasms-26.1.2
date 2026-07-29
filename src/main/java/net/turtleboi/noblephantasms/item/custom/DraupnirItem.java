package net.turtleboi.noblephantasms.item.custom;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import org.jspecify.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;

public final class DraupnirItem extends CurioRelicItem {
    private static final int GENERATION_INTERVAL = 20 * 30;
    private static final Map<Player, Long> LAST_GENERATION = new WeakHashMap<>();

    public DraupnirItem(Properties properties) {
        super(properties.rarity(Rarity.RARE));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof Player player) {
            generateGold(player, level);
        }
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player
                && player.level() instanceof ServerLevel level) {
            generateGold(player, level);
        }
    }

    @Override
    public boolean makesPiglinsNeutral(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    private static void generateGold(Player player, ServerLevel level) {
        long gameTime = level.getGameTime();
        Long lastGeneration = LAST_GENERATION.putIfAbsent(player, gameTime);
        if (lastGeneration == null || gameTime - lastGeneration < GENERATION_INTERVAL) {
            return;
        }

        LAST_GENERATION.put(player, gameTime);
        int roll = player.getRandom().nextInt(100);
        ItemStack generated;
        if (roll < 75) {
            generated = new ItemStack(Items.GOLD_NUGGET, 1 + player.getRandom().nextInt(4));
        } else if (roll < 97) {
            generated = new ItemStack(Items.GOLD_INGOT);
        } else {
            generated = new ItemStack(Items.GOLD_BLOCK);
        }

        if (!player.getInventory().add(generated)) {
            player.drop(generated, false);
        }
    }
}
