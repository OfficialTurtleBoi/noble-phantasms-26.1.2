package net.turtleboi.noblephantasms.item.custom;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.item.ModItems;
import top.theillusivec4.curios.api.CurioAttributeModifiers;
import top.theillusivec4.curios.api.SlotContext;

public final class ClawsOfTepeyollotlItem extends CurioRelicItem {
    private static final CurioAttributeModifiers MODIFIERS = CurioAttributeModifiers.builder()
            .addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "claws_of_tepeyollotl_attack_damage"),
                    1.5, AttributeModifier.Operation.ADD_VALUE))
            .build();
    private static final Direction[] WALL_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final double WALL_CHECK_DISTANCE = 0.08;
    private static final double WALL_CRAWL_SPEED = 0.18;
    private static final double MAX_CLIMB_SPEED = 0.18;
    private static final double MAX_DESCENT_SPEED = 0.1;
    private static final float VERTICAL_DEAD_ZONE = 22.5F;
    private static final float FULL_CLIMB_PITCH = 45.0F;
    private static final float BARE_HAND_SPEED_MULTIPLIER = 2.0F;

    public ClawsOfTepeyollotlItem(Properties properties) {
        super(properties.rarity(Rarity.RARE));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            handlePlayerTick(player);
        }
    }

    @Override
    public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
        return MODIFIERS;
    }

    private static void handlePlayerTick(Player player) {
        if (!canCling(player)) {
            return;
        }

        Direction wallDirection = findWallDirection(player);
        if (wallDirection == null) {
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        Vec3 lateralMovement = getLateralMovement(player, wallDirection);
        double verticalSpeed = getVerticalSpeed(player.getXRot());
        player.setDeltaMovement(lateralMovement.x, verticalSpeed, lateralMovement.z);
        player.resetFallDistance();
        player.hurtMarked = true;
    }

    private static boolean canCling(Player player) {
        return player.isAlive()
                && !player.isSpectator()
                && player.isShiftKeyDown()
                && !player.isInWater()
                && !player.isInLava()
                && !player.isFallFlying()
                && !player.isPassenger()
                && !player.getAbilities().flying;
    }

    private static Direction findWallDirection(Player player) {
        AABB bounds = player.getBoundingBox().deflate(0.001, 0.05, 0.001);
        Vec3 look = player.getLookAngle();
        Direction closestDirection = null;
        double closestScore = -Double.MAX_VALUE;
        for (Direction direction : WALL_DIRECTIONS) {
            AABB shifted = bounds.move(direction.getStepX() * WALL_CHECK_DISTANCE,
                    0.0, direction.getStepZ() * WALL_CHECK_DISTANCE);
            if (!player.level().noBlockCollision(player, shifted)) {
                double score = look.x * direction.getStepX() + look.z * direction.getStepZ();
                if (score > closestScore) {
                    closestDirection = direction;
                    closestScore = score;
                }
            }
        }
        return closestDirection;
    }

    private static Vec3 getLateralMovement(Player player, Direction wallDirection) {
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 sideways = new Vec3(Mth.cos(yaw), 0.0, Mth.sin(yaw));
        Vec3 input = forward.scale(player.zza).add(sideways.scale(player.xxa));
        Vec3 wallNormal = new Vec3(wallDirection.getStepX(), 0.0, wallDirection.getStepZ());
        Vec3 alongWall = input.subtract(wallNormal.scale(input.dot(wallNormal)));
        return alongWall.lengthSqr() < 1.0E-4 ? Vec3.ZERO : alongWall.normalize().scale(WALL_CRAWL_SPEED);
    }

    private static double getVerticalSpeed(float pitch) {
        if (pitch < -VERTICAL_DEAD_ZONE) {
            float progress = Mth.clamp((-pitch - VERTICAL_DEAD_ZONE) / (FULL_CLIMB_PITCH - VERTICAL_DEAD_ZONE), 0.0F, 1.0F);
            return MAX_CLIMB_SPEED * progress;
        }
        if (pitch > VERTICAL_DEAD_ZONE) {
            float progress = Mth.clamp((pitch - VERTICAL_DEAD_ZONE) / (FULL_CLIMB_PITCH - VERTICAL_DEAD_ZONE), 0.0F, 1.0F);
            return -MAX_DESCENT_SPEED * progress;
        }
        return 0.0;
    }

    public static void handleBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!canMineWithClaws(player)) {
            return;
        }

        float speed = event.getNewSpeed() * BARE_HAND_SPEED_MULTIPLIER;
        ItemStack stonePickaxe = Items.STONE_PICKAXE.getDefaultInstance();
        if (stonePickaxe.isCorrectToolForDrops(event.getState())) {
            speed = Math.max(speed, stonePickaxe.getDestroySpeed(event.getState()));
        }
        event.setNewSpeed(speed);
    }

    public static void handleHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (canMineWithClaws(event.getEntity())
                && Items.STONE_PICKAXE.getDefaultInstance().isCorrectToolForDrops(event.getTargetBlock())) {
            event.setCanHarvest(true);
        }
    }

    private static boolean canMineWithClaws(Player player) {
        return !player.getMainHandItem().has(DataComponents.TOOL)
                && isEquipped(player, ModItems.CLAWS_OF_TEPEYOLLOTL.get());
    }
}
