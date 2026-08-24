package net.turtleboi.noblephantasms.item.custom;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public abstract class SpearRelicItem extends TridentItem {
    protected SpearRelicItem(Properties properties, Rarity rarity) {
        super(properties
                .durability(ToolMaterial.NETHERITE.durability())
                .repairable(ToolMaterial.NETHERITE.repairItems())
                .enchantable(ToolMaterial.NETHERITE.enchantmentValue())
                .component(DataComponents.TOOL, TridentItem.createToolProperties())
                .component(DataComponents.WEAPON, new Weapon(1))
                .component(DataComponents.ATTACK_RANGE, new AttackRange(2.0F, 5.0F, 2.0F, 7.0F, 0.125F, 0.5F))
                .component(DataComponents.PIERCING_WEAPON, new PiercingWeapon(true, false,
                        Optional.of(SoundEvents.SPEAR_ATTACK), Optional.of(SoundEvents.SPEAR_HIT)))
                .component(DataComponents.MINIMUM_ATTACK_CHARGE, 1.0F)
                .component(DataComponents.SWING_ANIMATION, new SwingAnimation(SwingAnimationType.STAB, 23))
                .attributes(createSpearAttributes())
                .rarity(rarity)
                .fireResistant());
    }

    @Override
    public boolean supportsEnchantment(ItemStack itemStack, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.SHARPNESS)) {
            return false;
        }
        if (enchantment.is(Enchantments.IMPALING)) {
            return true;
        }
        return super.supportsEnchantment(itemStack, enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack itemStack, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.SHARPNESS)) {
            return false;
        }
        if (enchantment.is(Enchantments.IMPALING)) {
            return true;
        }
        return super.isPrimaryItemFor(itemStack, enchantment);
    }

    private static ItemAttributeModifiers createSpearAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 8.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, 1.0F / 1.15F - 4.0F,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }
}
