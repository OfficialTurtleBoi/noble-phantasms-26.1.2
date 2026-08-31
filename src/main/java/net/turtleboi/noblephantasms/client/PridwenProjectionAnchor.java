package net.turtleboi.noblephantasms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.item.custom.PridwenItem;
import net.turtleboi.noblephantasms.entity.custom.PridwenBarrierEntity;
import org.joml.Matrix4f;

public final class PridwenProjectionAnchor {
    private static final Map<ItemStackRenderState, TrackedItem> TRACKED_ITEMS = new WeakHashMap<>();
    private static final Map<AnchorKey, Matrix4f> ANCHORS = new HashMap<>();
    private static final ThreadLocal<Capture> ACTIVE_CAPTURE = new ThreadLocal<>();

    public static void track(ItemStackRenderState renderState, ItemStack itemStack,
                             ItemDisplayContext displayContext, ItemOwner owner) {
        if (!(itemStack.getItem() instanceof PridwenItem)
                || !(owner instanceof LivingEntity livingEntity)
                || !isRaised(livingEntity, itemStack, displayContext)
                || !isHandContext(displayContext)) {
            TRACKED_ITEMS.remove(renderState);
            return;
        }
        TRACKED_ITEMS.put(renderState, new TrackedItem(
                livingEntity.getUUID(), isFirstPerson(displayContext)));
    }

    public static void beginSubmit(ItemStackRenderState renderState) {
        TrackedItem trackedItem = TRACKED_ITEMS.get(renderState);
        if (trackedItem == null) {
            ACTIVE_CAPTURE.remove();
        } else {
            ACTIVE_CAPTURE.set(new Capture(trackedItem));
        }
    }

    public static void capture(PoseStack.Pose pose) {
        Capture capture = ACTIVE_CAPTURE.get();
        if (capture == null || capture.captured) {
            return;
        }
        ANCHORS.put(new AnchorKey(capture.trackedItem.ownerId, capture.trackedItem.firstPerson),
                new Matrix4f(pose.pose()));
        capture.captured = true;
    }

    public static Matrix4f get(LivingEntity owner) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean firstPerson = minecraft.player == owner
                && minecraft.options.getCameraType() == CameraType.FIRST_PERSON;
        Matrix4f anchor = ANCHORS.get(new AnchorKey(owner.getUUID(), firstPerson));
        return anchor == null ? null : new Matrix4f(anchor);
    }

    public static void endSubmit() {
        ACTIVE_CAPTURE.remove();
    }

    public static boolean isRaised(LivingEntity owner, ItemStack itemStack,
                                   ItemDisplayContext displayContext) {
        InteractionHand hand = handFor(owner, displayContext);
        if (hand != null && owner.isUsingItem() && owner.getUsedItemHand() == hand
                && owner.getUseItem().getItem() instanceof PridwenItem) {
            return true;
        }
        return hand != null && PridwenBarrierEntity.shouldKeepRaised(owner, hand, itemStack);
    }

    private static InteractionHand handFor(LivingEntity owner, ItemDisplayContext displayContext) {
        boolean right = displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        boolean left = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        if (!right && !left) {
            return null;
        }
        HumanoidArm renderedArm = right ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        return renderedArm == owner.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    private static boolean isHandContext(ItemDisplayContext displayContext) {
        return displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    private static boolean isFirstPerson(ItemDisplayContext displayContext) {
        return displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }

    private record TrackedItem(UUID ownerId, boolean firstPerson) {
    }

    private record AnchorKey(UUID ownerId, boolean firstPerson) {
    }

    private static final class Capture {
        private final TrackedItem trackedItem;
        private boolean captured;

        private Capture(TrackedItem trackedItem) {
            this.trackedItem = trackedItem;
        }
    }
}
