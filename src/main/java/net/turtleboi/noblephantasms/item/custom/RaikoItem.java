package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.item.ModRarities;

public final class RaikoItem extends Item {
    private static final double RANGE = 40.0;
    private static final int COOLDOWN = 20 * 5;

    public RaikoItem(Properties properties) {
        super(properties.stacksTo(1).rarity(ModRarities.LEGENDARY.getValue()).fireResistant());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(RANGE));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 strikePosition = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (lightning == null) {
            return InteractionResult.FAIL;
        }
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(),
                SoundSource.PLAYERS, 2.0F, 0.55F);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(),
                32, 0.8, 0.8, 0.8, 0.12);
        lightning.snapTo(strikePosition);
        lightning.setCause(serverPlayer);
        serverLevel.addFreshEntity(lightning);
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        return InteractionResult.SUCCESS_SERVER;
    }
}
