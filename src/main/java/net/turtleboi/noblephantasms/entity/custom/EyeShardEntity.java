package net.turtleboi.noblephantasms.entity.custom;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.turtleboi.noblephantasms.entity.ModEntities;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.CurioRelicItem;
import net.turtleboi.noblephantasms.item.custom.EyeOfHorusItem;
import net.turtleboi.noblephantasms.item.custom.RelicFragmentItem;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.relic.RelicFragmentData;

public final class EyeShardEntity extends ItemEntity {
    private static final int[] GLOW_COLORS = {0xEFBF04, 0xFFC766, 0xFFDD00, 0xF07200};

    public EyeShardEntity(EntityType<? extends EyeShardEntity> type, Level level) {
        super(type, level);
    }

    public EyeShardEntity(ServerLevel level, double x, double y, double z, UUID owner,
                          RelicFragmentData fragment) {
        this(ModEntities.EYE_SHARD.get(), level);
        setPos(x, y, z);
        setItem(RelicFragmentItem.create(ModItems.RELIC_FRAGMENT.get(), fragment, 1));
        setDeltaMovement(random.nextDouble() * 0.2 - 0.1, 0.3, random.nextDouble() * 0.2 - 0.1);
        setTarget(owner);
        setGlowingTag(true);
        setExtendedLifetime();
    }

    @Override
    public void tick() {
        if (!hasGlowingTag()) {
            setGlowingTag(true);
        }
        super.tick();
    }

    @Override
    public int getTeamColor() {
        float cycle = (tickCount % 40) / 10.0F;
        int index = (int) cycle;
        int next = (index + 1) % GLOW_COLORS.length;
        return ARGB.srgbLerp(cycle - index, GLOW_COLORS[index], GLOW_COLORS[next]) & 0xFFFFFF;
    }

    public RelicFragmentData getFragment() {
        return getItem().get(ModDataComponents.RELIC_FRAGMENT.get());
    }

    @Override
    public void playerTouch(Player player) {
        if (level().isClientSide()
                || !(player instanceof ServerPlayer serverPlayer)
                || getTarget() != null && !getTarget().equals(player.getUUID())
                || !CurioRelicItem.isEquipped(player, ModItems.EYE_OF_HORUS.get())
                || getFragment() == null
                || !EyeOfHorusItem.collectShard(serverPlayer, getFragment())) {
            return;
        }

        player.take(this, 1);
        level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.8F, 1.5F);
        discard();
    }
}
