package net.turtleboi.noblephantasms.item.custom;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import top.theillusivec4.curios.api.SlotContext;

public final class HofskorItem extends CurioRelicItem {
    private static final Identifier SPEED_ID = id("speed");
    private static final Identifier STEP_ID = id("step_height");
    private static final Identifier FALL_ID = id("fall_damage");
    private static final Map<LivingEntity, LivingEntity> BOOSTED_MOUNTS = new WeakHashMap<>();

    public HofskorItem(Properties properties) {
        super(properties.rarity(Rarity.RARE));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        LivingEntity current = wearer.getVehicle() instanceof LivingEntity mount ? mount : null;
        LivingEntity previous = BOOSTED_MOUNTS.get(wearer);
        if (previous != current) {
            removeModifiers(previous);
        }
        if (current == null) {
            BOOSTED_MOUNTS.remove(wearer);
            return;
        }
        addModifiers(current);
        BOOSTED_MOUNTS.put(wearer, current);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        removeModifiers(BOOSTED_MOUNTS.remove(slotContext.entity()));
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_FALL)) {
            return;
        }
        LivingEntity entity = event.getEntity();
        LivingEntity mount = isProtected(entity) ? entity : entity.getVehicle() instanceof LivingEntity vehicle ? vehicle : null;
        if (isProtected(mount)) {
            event.setCanceled(true);
        }
    }

    private static void addModifiers(LivingEntity mount) {
        addModifier(mount.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, 0.4,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addModifier(mount.getAttribute(Attributes.STEP_HEIGHT), STEP_ID, 1.0,
                AttributeModifier.Operation.ADD_VALUE);
        addModifier(mount.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER), FALL_ID, -1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void addModifier(AttributeInstance attribute, Identifier id, double amount,
                                    AttributeModifier.Operation operation) {
        if (attribute != null && attribute.getModifier(id) == null) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void removeModifiers(LivingEntity mount) {
        if (mount == null) {
            return;
        }
        removeModifier(mount.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID);
        removeModifier(mount.getAttribute(Attributes.STEP_HEIGHT), STEP_ID);
        removeModifier(mount.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER), FALL_ID);
    }

    private static boolean isProtected(LivingEntity mount) {
        AttributeInstance fallDamage = mount == null ? null : mount.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
        return fallDamage != null && fallDamage.getModifier(FALL_ID) != null;
    }

    private static void removeModifier(AttributeInstance attribute, Identifier id) {
        if (attribute != null) {
            attribute.removeModifier(id);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "hofskor_" + path);
    }
}
