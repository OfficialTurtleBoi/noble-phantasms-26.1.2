package net.turtleboi.noblephantasms.mixin;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.item.custom.MedjuNetjerItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends AbstractContainerMenu implements MedjuNetjerItem.MenuAccess {
    @Shadow
    @Final
    private Container enchantSlots;

    @Shadow
    @Final
    private ContainerLevelAccess access;

    @Shadow
    @Final
    private DataSlot enchantmentSeed;

    @Unique
    private boolean noblePhantasms$bookOfThothInstalled;

    protected EnchantmentMenuMixin(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL"))
    private void noblePhantasms$addBookState(int containerId, Inventory inventory,
            ContainerLevelAccess access, CallbackInfo ci) {
        this.noblePhantasms$bookOfThothInstalled = access.evaluate((level, pos) -> {
            var table = level.getBlockEntity(pos);
            return table != null && table.getData(ModAttachments.MEDJU_NETJER_INSTALLED.get());
        }, false);
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return noblePhantasms$bookOfThothInstalled ? 1 : 0;
            }

            @Override
            public void set(int value) {
                noblePhantasms$bookOfThothInstalled = value != 0;
            }
        });
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void noblePhantasms$rerollEnchantments(Player player, int buttonId,
            CallbackInfoReturnable<Boolean> cir) {
        if (buttonId != 3) {
            return;
        }
        if (!this.noblePhantasms$bookOfThothInstalled) {
            cir.setReturnValue(false);
            return;
        }

        ItemStack target = this.enchantSlots.getItem(0);
        ItemStack lapis = this.enchantSlots.getItem(1);
        if (target.isEmpty() || !target.isEnchantable()
                || lapis.isEmpty() && !player.hasInfiniteMaterials()) {
            cir.setReturnValue(false);
            return;
        }
        if (player.level().isClientSide()) {
            cir.setReturnValue(true);
            return;
        }

        if (!player.hasInfiniteMaterials()) {
            lapis.consume(1, player);
            if (lapis.isEmpty()) {
                this.enchantSlots.setItem(1, ItemStack.EMPTY);
            }
        }
        player.onEnchantmentPerformed(target, 0);
        this.enchantmentSeed.set(player.getEnchantmentSeed());
        ((EnchantmentMenu)(Object)this).slotsChanged(this.enchantSlots);
        this.access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS, 1.0F, 1.2F));
        cir.setReturnValue(true);
    }

    @Override
    public boolean noblePhantasms$hasMedjuNetjer() {
        return this.noblePhantasms$bookOfThothInstalled;
    }
}
