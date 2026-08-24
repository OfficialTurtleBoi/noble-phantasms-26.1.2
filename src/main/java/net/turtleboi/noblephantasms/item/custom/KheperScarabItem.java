package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import top.theillusivec4.curios.api.SlotContext;

public final class KheperScarabItem extends CurioRelicItem {
    private static final int REPAIR_INTERVAL = 40;

    public KheperScarabItem(Properties properties) {
        super(properties
                .component(ModDataComponents.KHEPER_SCARAB_ACTIVE.get(), false)
                .rarity(Rarity.RARE));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        boolean active = !isActive(stack);
        stack.set(ModDataComponents.KHEPER_SCARAB_ACTIVE.get(), active);
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.6F, active ? 1.25F : 0.75F);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)
                || !(player.level() instanceof ServerLevel)
                || !isActive(stack)
                || slotContext.entity().tickCount % REPAIR_INTERVAL != 0) {
            return;
        }

        for (EquipmentSlot slot : EquipmentSlotGroup.HAND) {
            if (!repair(player, player.getItemBySlot(slot))) {
                return;
            }
        }
        for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            if (!repair(player, player.getItemBySlot(slot))) {
                return;
            }
        }
    }

    public static boolean isActive(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.KHEPER_SCARAB_ACTIVE.get(), false);
    }

    private static boolean repair(Player player, ItemStack stack) {
        if (!stack.isDamaged()) {
            return true;
        }
        if (!player.getAbilities().instabuild && player.totalExperience <= 0) {
            return false;
        }

        stack.setDamageValue(stack.getDamageValue() - 1);
        if (!player.getAbilities().instabuild) {
            player.giveExperiencePoints(-1);
        }
        return true;
    }
}
