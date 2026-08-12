package net.turtleboi.noblephantasms.item.custom;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.turtleboi.noblephantasms.attachment.ModAttachments;
import net.turtleboi.noblephantasms.item.ModItems;

public final class RecallBellItem extends Item {
    private static final String MOUNT_ID = "ClydnoMount";
    private static final String MOUNT_NAME = "ClydnoMountName";
    private static final String DIMENSION = "ClydnoDimension";
    private static final String X = "ClydnoX";
    private static final String Y = "ClydnoY";
    private static final String Z = "ClydnoZ";

    public RecallBellItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE).fireResistant());
    }

    public static ItemStack createBound(AbstractHorse mount) {
        ItemStack bell = new ItemStack(ModItems.RECALL_BELL.get());
        updateBinding(bell, mount);
        return bell;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack bell = player.getItemInHand(hand);
        CompoundTag data = getData(bell);
        UUID mountId = parseUuid(data.getStringOr(MOUNT_ID, ""));
        if (mountId == null) {
            return InteractionResult.PASS;
        }
        serverPlayer.level().playSound(null, serverPlayer.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 1.0F);
        AbstractHorse mount = findMount(serverPlayer, mountId, data);
        if (mount == null || !mount.isAlive() || !mount.getData(ModAttachments.CLYDNO_HALTERED)) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.noblephantasms.clydno_halter.unanswered"));
            return InteractionResult.FAIL;
        }
        mount.stopRiding();
        mount.ejectPassengers();
        Vec3 look = serverPlayer.getLookAngle();
        Vec3 summonPosition = serverPlayer.position().add(-look.z * 1.5, 0.0, look.x * 1.5);
        if (!mount.teleportTo(serverPlayer.level(), summonPosition.x, summonPosition.y, summonPosition.z, Set.of(), serverPlayer.getYRot(), mount.getXRot(), false)) {
            return InteractionResult.FAIL;
        }
        Entity summoned = serverPlayer.level().getEntity(mountId);
        AbstractHorse summonedMount = summoned instanceof AbstractHorse horse ? horse : mount;
        summonedMount.setDeltaMovement(0.0, 0.0, 0.0);
        updateBinding(bell, summonedMount);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.isShiftKeyDown() || !(target instanceof AbstractHorse mount) || !isBoundTo(stack, mount)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        mount.removeData(ModAttachments.CLYDNO_HALTERED);
        player.setItemInHand(hand, new ItemStack(ModItems.CLYDNO_HALTER.get()));
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.noblephantasms.clydno_halter.recovered", mount.getDisplayName()));
        }
        player.level().playSound(null, mount.blockPosition(), SoundEvents.LEAD_UNTIED, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (owner.tickCount % 20 != 0) {
            return;
        }
        UUID mountId = parseUuid(getData(stack).getStringOr(MOUNT_ID, ""));
        AbstractHorse mount = mountId == null ? null : findLoadedMount(level, mountId);
        if (mount != null && mount.isAlive() && mount.getData(ModAttachments.CLYDNO_HALTERED)) {
            updateBinding(stack, mount);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        String mountName = getData(stack).getStringOr(MOUNT_NAME, "");
        if (!mountName.isEmpty()) {
            builder.accept(Component.translatable("tooltip.noblephantasms.recall_bell.mount", mountName).withStyle(ChatFormatting.GRAY));
        }
    }

    private static boolean isBoundTo(ItemStack stack, AbstractHorse mount) {
        UUID mountId = parseUuid(getData(stack).getStringOr(MOUNT_ID, ""));
        return mountId != null && mountId.equals(mount.getUUID()) && mount.getData(ModAttachments.CLYDNO_HALTERED);
    }

    private static AbstractHorse findMount(ServerPlayer player, UUID mountId, CompoundTag data) {
        AbstractHorse loaded = findLoadedMount(player.level(), mountId);
        if (loaded != null) {
            return loaded;
        }
        Identifier dimensionId = Identifier.tryParse(data.getStringOr(DIMENSION, ""));
        if (dimensionId == null) {
            return null;
        }
        ServerLevel level = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (level == null) {
            return null;
        }
        BlockPos savedPos = new BlockPos(data.getIntOr(X, 0), data.getIntOr(Y, 0), data.getIntOr(Z, 0));
        level.getChunk(savedPos);
        Entity entity = level.getEntity(mountId);
        return entity instanceof AbstractHorse mount ? mount : null;
    }

    private static AbstractHorse findLoadedMount(ServerLevel currentLevel, UUID mountId) {
        for (ServerLevel level : currentLevel.getServer().getAllLevels()) {
            Entity entity = level.getEntity(mountId);
            if (entity instanceof AbstractHorse mount) {
                return mount;
            }
        }
        return null;
    }

    private static void updateBinding(ItemStack stack, AbstractHorse mount) {
        CompoundTag data = getData(stack);
        data.putString(MOUNT_ID, mount.getUUID().toString());
        data.putString(MOUNT_NAME, mount.getDisplayName().getString());
        data.putString(DIMENSION, mount.level().dimension().identifier().toString());
        data.putInt(X, mount.getBlockX());
        data.putInt(Y, mount.getBlockY());
        data.putInt(Z, mount.getBlockZ());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    private static CompoundTag getData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static UUID parseUuid(String value) {
        try {
            return value.isEmpty() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
