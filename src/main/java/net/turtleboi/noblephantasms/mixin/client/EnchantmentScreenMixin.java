package net.turtleboi.noblephantasms.mixin.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.custom.MedjuNetjerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
    @Unique
    private static final Identifier noblePhantasms$bookOfThothTexture =
            Identifier.fromNamespaceAndPath(
                    NoblePhantasms.MOD_ID, "textures/entity/book_of_thoth.png");

    @Shadow
    public float flipT;

    protected EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @ModifyArg(
            method = "extractBook",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;book("
                            + "Lnet/minecraft/client/model/object/book/BookModel;"
                            + "Lnet/minecraft/resources/Identifier;FFFIIII)V"),
            index = 1)
    private Identifier noblePhantasms$useBookOfThothTexture(Identifier original) {
        return ((MedjuNetjerItem.MenuAccess)this.menu).noblePhantasms$hasMedjuNetjer()
                ? noblePhantasms$bookOfThothTexture
                : original;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void noblePhantasms$clickBookToReroll(MouseButtonEvent event, boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        if (!((MedjuNetjerItem.MenuAccess)this.menu).noblePhantasms$hasMedjuNetjer()
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
        if (!((MedjuNetjerItem.MenuAccess)this.menu).noblePhantasms$hasMedjuNetjer()
                || this.minecraft.level == null) {
            return;
        }

        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        if (mouseX >= left + 14 && mouseX < left + 52 && mouseY >= top + 14 && mouseY < top + 45) {
            graphics.setComponentTooltipForNextFrame(this.font,
                    List.of(Component.translatable("tooltip.noblephantasms.medju_netjer.reroll")
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
            tooltip.add(Component.translatable("tooltip.noblephantasms.medju_netjer.offer")
                    .withStyle(ChatFormatting.GOLD));
            List<EnchantmentInstance> offer = getOffer(target, slot, this.menu.costs[slot]);
            for (EnchantmentInstance enchantment : offer) {
                tooltip.add(Enchantment.getFullname(enchantment.enchantment(), enchantment.level())
                        .copy().withStyle(ChatFormatting.WHITE));
            }
            if (offer.isEmpty()) {
                tooltip.add(CommonComponents.EMPTY);
                tooltip.add(Component.translatable("neoforge.container.enchant.limitedEnchantability")
                        .withStyle(ChatFormatting.RED));
            } else if (!this.minecraft.player.hasInfiniteMaterials()) {
                tooltip.add(CommonComponents.EMPTY);
                int lapisCost = slot + 1;
                if (this.minecraft.player.experienceLevel < this.menu.costs[slot]) {
                    tooltip.add(Component.translatable("container.enchant.level.requirement", this.menu.costs[slot])
                            .withStyle(ChatFormatting.RED));
                } else {
                    MutableComponent lapisLine = lapisCost == 1
                            ? Component.translatable("container.enchant.lapis.one")
                            : Component.translatable("container.enchant.lapis.many", lapisCost);
                    tooltip.add(lapisLine.withStyle(this.menu.getGoldCount() >= lapisCost
                            ? ChatFormatting.GRAY : ChatFormatting.RED));
                    MutableComponent levelLine = lapisCost == 1
                            ? Component.translatable("container.enchant.level.one")
                            : Component.translatable("container.enchant.level.many", lapisCost);
                    tooltip.add(levelLine.withStyle(ChatFormatting.GRAY));
                }
            }
            graphics.setTooltipForNextFrame(this.font,
                    tooltip.stream().map(Component::getVisualOrderText).toList(),
                    DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
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
