package net.turtleboi.noblephantasms.mixin.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.turtleboi.noblephantasms.item.custom.BookOfThothItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
    @Shadow
    public float flipT;

    protected EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void noblePhantasms$clickBookToReroll(MouseButtonEvent event, boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        if (!((BookOfThothItem.MenuAccess)this.menu).noblePhantasms$hasBookOfThoth()
                || event.button() != 0) {
            return;
        }

        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        double x = event.x() - (left + 14);
        double y = event.y() - (top + 14);
        if (x < 0.0 || y < 0.0 || x >= 38.0 || y >= 31.0
                || !this.menu.clickMenuButton(this.minecraft.player, 3)) {
            return;
        }

        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3);
        this.flipT += 1.0F;
        cir.setReturnValue(true);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void noblePhantasms$showFullEnchantments(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        if (!((BookOfThothItem.MenuAccess)this.menu).noblePhantasms$hasBookOfThoth()
                || this.minecraft.level == null) {
            return;
        }

        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        if (mouseX >= left + 14 && mouseX < left + 52 && mouseY >= top + 14 && mouseY < top + 45) {
            graphics.setComponentTooltipForNextFrame(this.font,
                    List.of(Component.translatable("tooltip.noblephantasms.book_of_thoth.reroll")
                            .withStyle(ChatFormatting.GOLD)), mouseX, mouseY);
            return;
        }

        ItemStack target = this.menu.getSlot(0).getItem();
        if (target.isEmpty()) {
            return;
        }
        for (int slot = 0; slot < 3; slot++) {
            if (!this.isHovering(60, 14 + 19 * slot, 108, 17, mouseX, mouseY)
                    || this.menu.costs[slot] <= 0) {
                continue;
            }

            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("tooltip.noblephantasms.book_of_thoth.offer")
                    .withStyle(ChatFormatting.GOLD));
            for (EnchantmentInstance enchantment : getOffer(target, slot, this.menu.costs[slot])) {
                tooltip.add(Enchantment.getFullname(enchantment.enchantment(), enchantment.level())
                        .copy().withStyle(ChatFormatting.WHITE));
            }
            graphics.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
            return;
        }
    }

    private List<EnchantmentInstance> getOffer(ItemStack target, int slot, int cost) {
        var registry = this.minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        HolderSet.Named<Enchantment> available = registry.get(EnchantmentTags.IN_ENCHANTING_TABLE).orElse(null);
        if (available == null) {
            return List.of();
        }

        RandomSource random = RandomSource.create(this.menu.getEnchantmentSeed() + slot);
        List<EnchantmentInstance> result = EnchantmentHelper.selectEnchantment(
                random, target, cost, available.stream().map(holder -> (Holder<Enchantment>)holder));
        if (target.is(Items.BOOK) && result.size() > 1) {
            result.remove(random.nextInt(result.size()));
        }
        return result;
    }
}
