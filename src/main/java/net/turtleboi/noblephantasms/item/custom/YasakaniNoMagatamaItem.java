package net.turtleboi.noblephantasms.item.custom;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.entity.custom.YasakaniGuardianEntity;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.ModRarities;
import top.theillusivec4.curios.api.SlotContext;

public final class YasakaniNoMagatamaItem extends CurioRelicItem {
    private static final float BASE_SPAWN_CHANCE = 0.25F;
    private static final float WOUNDED_SPAWN_CHANCE = 0.50F;
    private static final long BEAD_REGROW_TICKS = 20L * 20L;
    private static final Identifier TOUGHNESS_ID = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "yasakani_guardian_toughness");
    private static final Map<UUID, EnumMap<Spirit, UUID>> ACTIVE = new HashMap<>();
    private static final Map<UUID, EnumMap<Spirit, Long>> READY_AT = new HashMap<>();

    public YasakaniNoMagatamaItem(Properties properties) {
        super(properties.rarity(ModRarities.LEGENDARY.getValue()));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player && player.level() instanceof ServerLevel level) {
            updateToughness(player, activeCount(level, player));
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            dismissAll(player);
            updateToughness(player, 0);
        }
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(player.level() instanceof ServerLevel level)
                || !isEquipped(player, ModItems.YASAKANI_NO_MAGATAMA.get())) {
            return;
        }

        YasakaniGuardianEntity interposer = findGuardian(level, player, Spirit.IKUTSUHIKONE);
        if (interposer != null && event.getSource().getDirectEntity() instanceof Projectile projectile) {
            event.setCanceled(true);
            interposer.blockProjectile(level, player, projectile);
            return;
        }

        int active = activeCount(level, player);
        if (active > 0) {
            event.setAmount(event.getAmount() * (1.0F - Math.min(0.20F, active * 0.04F)));
        }
    }

    public static void handleDamageComplete(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.isAlive()
                || !isEquipped(player, ModItems.YASAKANI_NO_MAGATAMA.get())) {
            return;
        }
        float chance = player.getHealth() < player.getMaxHealth() * 0.5F
                ? WOUNDED_SPAWN_CHANCE : BASE_SPAWN_CHANCE;
        if (player.getRandom().nextFloat() < chance) {
            trySpawnGuardian(player);
        }
    }

    public static void handleLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            dismissAll(player);
        }
    }

    public static void handlePlayerTick(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!player.isAlive() || !isEquipped(player, ModItems.YASAKANI_NO_MAGATAMA.get())) {
            dismissAll(player);
            updateToughness(player, 0);
            return;
        }
        updateToughness(player, activeCount(level, player));
    }

    public static void dismissAll(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        EnumMap<Spirit, UUID> guardians = ACTIVE.remove(player.getUUID());
        if (guardians == null) {
            return;
        }
        for (UUID guardianId : guardians.values()) {
            Entity entity = level.getEntity(guardianId);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    public static void guardianDeparted(UUID ownerId, Spirit spirit, UUID guardianId, long gameTime) {
        EnumMap<Spirit, UUID> guardians = ACTIVE.get(ownerId);
        if (guardians != null && guardianId.equals(guardians.get(spirit))) {
            guardians.remove(spirit);
            if (guardians.isEmpty()) {
                ACTIVE.remove(ownerId);
            }
            READY_AT.computeIfAbsent(ownerId, ignored -> new EnumMap<>(Spirit.class))
                    .put(spirit, gameTime + BEAD_REGROW_TICKS);
        }
    }

    public static void registerGuardian(Player owner, Spirit spirit, UUID guardianId) {
        ACTIVE.computeIfAbsent(owner.getUUID(), ignored -> new EnumMap<>(Spirit.class))
                .putIfAbsent(spirit, guardianId);
    }

    public static boolean isCurrentlyEquipped(Player player) {
        return isEquipped(player, ModItems.YASAKANI_NO_MAGATAMA.get());
    }

    private static void trySpawnGuardian(ServerPlayer player) {
        ServerLevel level = player.level();
        activeCount(level, player);
        EnumMap<Spirit, UUID> active = ACTIVE.computeIfAbsent(
                player.getUUID(), ignored -> new EnumMap<>(Spirit.class));
        EnumMap<Spirit, Long> readyAt = READY_AT.computeIfAbsent(
                player.getUUID(), ignored -> new EnumMap<>(Spirit.class));
        List<Spirit> available = new ArrayList<>();
        for (Spirit spirit : Spirit.values()) {
            if (!active.containsKey(spirit)
                    && level.getGameTime() >= readyAt.getOrDefault(spirit, 0L)) {
                available.add(spirit);
            }
        }
        if (available.isEmpty()) {
            return;
        }

        Spirit spirit = available.get(player.getRandom().nextInt(available.size()));
        YasakaniGuardianEntity guardian = new YasakaniGuardianEntity(level, player, spirit);
        level.addFreshEntity(guardian);
        active.put(spirit, guardian.getUUID());
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 1.0F, 0.8F + spirit.ordinal() * 0.1F);
    }

    private static int activeCount(ServerLevel level, Player player) {
        EnumMap<Spirit, UUID> guardians = ACTIVE.get(player.getUUID());
        if (guardians == null) {
            return 0;
        }
        guardians.entrySet().removeIf(entry -> {
            Entity entity = level.getEntity(entry.getValue());
            return !(entity instanceof YasakaniGuardianEntity guardian)
                    || !guardian.isAlive() || !guardian.isOwnedBy(player);
        });
        if (guardians.isEmpty()) {
            ACTIVE.remove(player.getUUID());
            return 0;
        }
        return guardians.size();
    }

    private static YasakaniGuardianEntity findGuardian(ServerLevel level, Player player, Spirit spirit) {
        EnumMap<Spirit, UUID> guardians = ACTIVE.get(player.getUUID());
        if (guardians == null) {
            return null;
        }
        Entity entity = level.getEntity(guardians.get(spirit));
        return entity instanceof YasakaniGuardianEntity guardian && guardian.isAlive() ? guardian : null;
    }

    private static void updateToughness(Player player, int count) {
        AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness == null) {
            return;
        }
        if (count <= 0) {
            toughness.removeModifier(TOUGHNESS_ID);
        } else {
            toughness.addOrUpdateTransientModifier(new AttributeModifier(
                    TOUGHNESS_ID, count, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public enum Spirit {
        OSHIHOMIMI("Oshihomimi"),
        HOHI("Hohi"),
        AMATSUHIKONE("Amatsuhikone"),
        IKUTSUHIKONE("Ikutsuhikone"),
        KUMANOKUSUBI("Kumanokusubi");

        private final String displayName;

        Spirit(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }
}
