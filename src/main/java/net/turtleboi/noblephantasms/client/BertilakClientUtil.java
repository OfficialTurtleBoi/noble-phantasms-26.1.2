package net.turtleboi.noblephantasms.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Mob;
import net.turtleboi.noblephantasms.client.renderer.TrophyHeadRenderer;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;
import net.turtleboi.noblephantasms.network.TrophySupportPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class BertilakClientUtil {
    private static UUID lastTargetId;

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null
                || !player.isUsingItem()
                || !player.getUseItem().is(ModItems.BERTILAK)) {
            lastTargetId = null;
            return;
        }

        Mob target = BertilakItem.findLookTarget(player);
        if (target == null || target.getUUID().equals(lastTargetId)) {
            return;
        }

        lastTargetId = target.getUUID();
        boolean supported = TrophyHeadRenderer.hasRenderableHead(target);
        ClientPacketDistributor.sendToServer(
                new TrophySupportPayload(target.getUUID(), supported));
    }
}
