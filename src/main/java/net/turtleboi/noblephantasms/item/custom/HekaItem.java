package net.turtleboi.noblephantasms.item.custom;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.turtleboi.noblephantasms.item.ModItems;

public class HekaItem extends Item {
    private static final double HERD_RANGE = 10.0;

    public HekaItem(Properties properties) {
        super(properties
                .sword(ToolMaterial.NETHERITE, 1.0F, -2.2F)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return NekhakhaItem.tryActivatePharaohsDecree(level, player)
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    public static boolean isSetHeld(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return mainHand.is(ModItems.HEKA) && offHand.is(ModItems.NEKHAKHA)
                || mainHand.is(ModItems.NEKHAKHA) && offHand.is(ModItems.HEKA);
    }

    public static void handlePlayerTick(Player player) {
        if (!(player.level() instanceof ServerLevel level)
                || player.tickCount % 5 != 0
                || !player.getMainHandItem().is(ModItems.HEKA)
                && !player.getOffhandItem().is(ModItems.HEKA)) {
            return;
        }

        boolean protectedBySet = isSetHeld(player);
        AABB area = player.getBoundingBox().inflate(HERD_RANGE);
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, area,
                animal -> animal.isAlive() && !animal.isPassenger());
        for (Animal animal : animals) {
            if (animal.distanceToSqr(player) > 6.25) {
                animal.getNavigation().moveTo(player, 1.15);
            } else {
                animal.getNavigation().stop();
            }
            if (protectedBySet) {
                animal.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 15, 0, false, false, true));
            }
        }

        if (!protectedBySet || animals.isEmpty()) {
            return;
        }
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area.inflate(4.0),
                candidate -> candidate instanceof Enemy)) {
            if (animals.contains(mob.getTarget())) {
                mob.setTarget(null);
            }
        }
    }
}
