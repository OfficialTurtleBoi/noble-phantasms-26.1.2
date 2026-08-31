package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.entity.custom.ApilolliCloudEntity;

public final class ApilolliItem extends Item {
    public ApilolliItem(Properties properties) {
        super(properties.stacksTo(1)
                .component(ModDataComponents.APILOLLI_FILLED.get(), true)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.getOrDefault(ModDataComponents.APILOLLI_FILLED.get(), false)) {
            BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (hit.getType() != HitResult.Type.BLOCK
                    || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                stack.set(ModDataComponents.APILOLLI_FILLED.get(), true);
                level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL,
                        SoundSource.PLAYERS, 1.0F, 0.9F);
            }
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (ApilolliCloudEntity.hasActiveCloud(player)) {
            return InteractionResult.FAIL;
        }

        ApilolliCloudEntity cloud = new ApilolliCloudEntity(serverLevel, player);
        serverLevel.addFreshEntity(cloud);
        stack.set(ModDataComponents.APILOLLI_FILLED.get(), false);
        player.getCooldowns().addCooldown(stack, 20);
        serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 3.5, player.getZ(),
                36, 0.9, 0.44, 0.9, 0.01);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.WEATHER, 0.8F, 1.8F);
        return InteractionResult.SUCCESS_SERVER;
    }
}
