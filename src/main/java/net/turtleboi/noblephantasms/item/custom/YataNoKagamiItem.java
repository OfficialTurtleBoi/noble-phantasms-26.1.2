package net.turtleboi.noblephantasms.item.custom;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

public final class YataNoKagamiItem extends ShieldItem {
    public YataNoKagamiItem(Properties properties) {
        super(properties
                .durability(768)
                .component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                .repairable(ItemTags.GOLD_TOOL_MATERIALS)
                .equippableUnswappable(EquipmentSlot.OFFHAND)
                .delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
                        0.1F,
                        1.0F,
                        List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
                        new BlocksAttacks.ItemDamageFunction(4.0F, 1.0F, 1.0F),
                        Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                        Optional.of(SoundEvents.SHIELD_BLOCK),
                        Optional.of(SoundEvents.SHIELD_BREAK)))
                .component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK)
                .rarity(Rarity.RARE)
                .fireResistant());
    }

    public static void handleShieldBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !event.getOriginalBlock()
                || !(player.getUseItem().getItem() instanceof YataNoKagamiItem)
                || !(event.getDamageSource().getDirectEntity() instanceof Projectile projectile)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        projectile.deflect(ProjectileDeflection.REVERSE, player, EntityReference.of(player), true);
        projectile.setDeltaMovement(projectile.getDeltaMovement().scale(2.0));
        projectile.needsSync = true;
        event.setBlocked(true);
        event.setBlockedDamage(event.getDamageContainer().getNewDamage());
        event.setShieldDamage(1);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                player.getSoundSource(), 1.2F, 1.5F);
    }
}
