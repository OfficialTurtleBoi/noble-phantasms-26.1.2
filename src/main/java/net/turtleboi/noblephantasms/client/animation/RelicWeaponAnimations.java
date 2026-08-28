package net.turtleboi.noblephantasms.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.BertilakExtensions;
import net.turtleboi.noblephantasms.item.custom.IwatoshiAttackState;
import net.turtleboi.noblephantasms.item.custom.IwatoshiItem;
import net.turtleboi.noblephantasms.item.custom.RhongomyniadItem;

public final class RelicWeaponAnimations {
    private static final Identifier RHONGOMYNIAD = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "rhongomyniad");
    private static final Identifier IWATOSHI = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "iwatoshi");
    private static final Identifier BERTILAK = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "bertilak");
    private static final String LANCE_FIRST_PERSON = "lance_lower_first_person";
    private static final String LANCE_THIRD_PERSON = "lance_lower_third_person";
    private static final String IWATOSHI_CHARGE_FIRST_PERSON = "iwatoshi_charge_first_person";
    private static final String IWATOSHI_CHARGE_THIRD_PERSON = "iwatoshi_charge_third_person";
    private static final String IWATOSHI_SPIN_FIRST_PERSON = "iwatoshi_spin_first_person";
    private static final String IWATOSHI_SPIN_THIRD_PERSON = "iwatoshi_spin_third_person";
    private static final String BERTILAK_COVENANT_THIRD_PERSON = "bertilak_covenant_third_person";

    static {
        registerAnimations();
        RelicAnimationStorage.load();
    }

    public static void initialize() {
    }

    public static boolean firstPersonUse(float hitFeedbackTime, PoseStack poseStack, float timeHeld,
                                         HumanoidArm arm, ItemStack itemStack) {
        if (itemStack.getItem() instanceof RhongomyniadItem) {
            animateFirstPersonLanceUse(hitFeedbackTime, poseStack, timeHeld, arm, itemStack);
            return true;
        }

        if (itemStack.getItem() instanceof IwatoshiItem) {
            animateFirstPersonIwatoshiUse(poseStack, timeHeld, arm, itemStack);
            return true;
        }

        return false;
    }

    public static boolean thirdPersonHandUse(ModelPart armPart, ModelPart headPart, boolean rightArm,
                                             ItemStack itemStack, HumanoidRenderState state) {
        HumanoidArm arm = rightArm ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        InteractionHand hand = arm == state.mainArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemDisplayContext displayContext = rightArm
                ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        LivingEntity entity = getEntity(state);
        RelicWeaponAnimationContext.begin(entity, itemStack, 0.0F, hand, displayContext);
        try {
            if (itemStack.getItem() instanceof RhongomyniadItem) {
                animateThirdPersonLanceHand(armPart, headPart, rightArm, itemStack,
                        state.ticksUsingItem, entity);
                return true;
            }

            if (itemStack.getItem() instanceof IwatoshiItem) {
                animateThirdPersonIwatoshiHand(armPart, headPart, rightArm,
                        state.ticksUsingItem, itemStack);
                return true;
            }
        } finally {
            RelicWeaponAnimationContext.end();
        }

        return false;
    }

    public static boolean thirdPersonUseItem(ArmedEntityRenderState state, PoseStack poseStack, float timeHeld,
                                             HumanoidArm arm, ItemStack itemStack) {
        LivingEntity entity = getEntity(state);
        InteractionHand hand = arm == state.mainArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemDisplayContext displayContext = arm == HumanoidArm.RIGHT
                ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        RelicWeaponAnimationContext.begin(entity, itemStack, 0.0F, hand, displayContext);
        try {
            if (itemStack.getItem() instanceof RhongomyniadItem) {
                animateThirdPersonLanceItem(state, poseStack, timeHeld, arm, itemStack);
                return true;
            }

            if (itemStack.getItem() instanceof IwatoshiItem) {
                animateThirdPersonIwatoshiItem(poseStack, timeHeld, arm, itemStack);
                return true;
            }
        } finally {
            RelicWeaponAnimationContext.end();
        }

        return false;
    }

    public static boolean firstPersonAttack(float attackTime, PoseStack poseStack, int direction, HumanoidArm arm) {
        LivingEntity entity = RelicWeaponAnimationContext.getEntity();
        ItemStack itemStack = RelicWeaponAnimationContext.getItemStack();
        int chargeLevel = IwatoshiAttackState.getChargeLevel(entity);
        if (chargeLevel <= 0) {
            chargeLevel = ItemPoseEditor.getIwatoshiPreviewChargeLevel(itemStack);
        }
        if (!(itemStack.getItem() instanceof IwatoshiItem) || chargeLevel <= 0) {
            return false;
        }

        if (chargeLevel == IwatoshiItem.getMaxChargeLevel()) {
            animateFirstPersonIwatoshiSpin(attackTime, poseStack, arm, itemStack);
        } else {
            animateFirstPersonIwatoshiSlash(attackTime, poseStack, arm, itemStack, chargeLevel);
        }
        return true;
    }

    public static boolean thirdPersonAttackHand(HumanoidModel<?> model, HumanoidRenderState state) {
        LivingEntity entity = getEntity(state);
        ItemStack itemStack = state.getUseItemStackForArm(state.attackArm);
        int chargeLevel = IwatoshiAttackState.getChargeLevel(entity);
        if (chargeLevel <= 0) {
            chargeLevel = ItemPoseEditor.getIwatoshiPreviewChargeLevel(
                    state, itemStack);
        }
        if (!(itemStack.getItem() instanceof IwatoshiItem) || chargeLevel <= 0) {
            return false;
        }

        ItemDisplayContext displayContext = state.attackArm == HumanoidArm.RIGHT
                ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        InteractionHand hand = state.attackArm == state.mainArm
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        RelicWeaponAnimationContext.begin(entity, itemStack, 0.0F,
                hand, displayContext);
        try {
            model.rightArm.yRot -= model.body.yRot;
            model.leftArm.yRot -= model.body.yRot;
            model.leftArm.xRot -= model.body.yRot;
            if (chargeLevel == IwatoshiItem.getMaxChargeLevel()) {
                animateThirdPersonIwatoshiSpinHand(model, state);
            } else {
                animateThirdPersonIwatoshiSlashHand(model, state, chargeLevel);
            }
        } finally {
            RelicWeaponAnimationContext.end();
        }
        return true;
    }

    public static boolean thirdPersonAttackItem(ArmedEntityRenderState state, PoseStack poseStack) {
        LivingEntity entity = getEntity(state);
        ItemStack itemStack = state.getUseItemStackForArm(state.attackArm);
        int chargeLevel = IwatoshiAttackState.getChargeLevel(entity);
        if (chargeLevel <= 0) {
            chargeLevel = ItemPoseEditor.getIwatoshiPreviewChargeLevel(
                    state, itemStack);
        }
        if (!(itemStack.getItem() instanceof IwatoshiItem) || chargeLevel <= 0) {
            return false;
        }

        ItemDisplayContext displayContext = state.attackArm == HumanoidArm.RIGHT
                ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        InteractionHand hand = state.attackArm == state.mainArm
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        RelicWeaponAnimationContext.begin(entity, itemStack, 0.0F,
                hand, displayContext);
        try {
            if (chargeLevel == IwatoshiItem.getMaxChargeLevel()) {
                animateThirdPersonIwatoshiSpinItem(
                        state.attackTime, poseStack, state.mainArm, itemStack);
            } else {
                animateThirdPersonIwatoshiSlashItem(
                        state.attackTime, poseStack, state.mainArm, itemStack, chargeLevel);
            }
        } finally {
            RelicWeaponAnimationContext.end();
        }
        return true;
    }

    public static boolean isIwatoshiRightClickAttack(LivingEntity entity, ItemStack itemStack) {
        return itemStack.getItem() instanceof IwatoshiItem
                && (IwatoshiAttackState.getChargeLevel(entity) > 0
                || ItemPoseEditor.getIwatoshiPreviewChargeLevel(itemStack) > 0);
    }

    public static RelicAnimation getEditorAnimation(ItemStack itemStack,
                                                     CameraType cameraType, String pose) {
        String animationId = getEditorAnimationId(itemStack, cameraType, pose);
        RelicAnimation animation = animationId == null
                ? null : RelicAnimator.getAnimation(itemStack, animationId);
        return animation == null ? null : animation.copy();
    }

    public static String getEditorAnimationId(ItemStack itemStack,
                                               CameraType cameraType, String pose) {
        return RelicAnimator.getEditorAnimationId(itemStack, cameraType, pose);
    }

    public static RelicTransform sampleThirdPersonBertilakCovenant(ItemStack itemStack, float tick) {
        return RelicAnimator.sample(itemStack, BERTILAK_COVENANT_THIRD_PERSON,
                Math.min(tick, BertilakExtensions.TARGETING_TRANSITION_TICKS));
    }

    private static void animateFirstPersonLanceUse(float hitFeedbackTime, PoseStack poseStack, float timeHeld,
                                                   HumanoidArm arm, ItemStack itemStack) {
        LivingEntity entity = RelicWeaponAnimationContext.getEntity();
        float animationTick = getLanceAnimationTick(itemStack, timeHeld, entity,
                RelicWeaponAnimationContext.getPartialTick());
        RelicAnimator.apply(itemStack, LANCE_FIRST_PERSON, animationTick, poseStack, arm);

        float hitFeedback = 0.4F * (Ease.outQuart(progress(hitFeedbackTime, 1.0F, 3.0F))
                - Ease.inOutSine(progress(hitFeedbackTime, 3.0F, 10.0F)));
        poseStack.translate(0.0F, -hitFeedback, 0.0F);
    }

    private static void animateThirdPersonLanceHand(ModelPart armPart, ModelPart headPart, boolean rightArm,
                                                     ItemStack itemStack, float timeHeld, LivingEntity entity) {
        HumanoidArm arm = rightArm ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        armPart.yRot = headPart.yRot;
        armPart.xRot = headPart.xRot;
        RelicAnimator.apply(itemStack, LANCE_THIRD_PERSON, RelicAnimation.Channel.MAIN_ARM,
                getLanceAnimationTick(itemStack, timeHeld, entity, 0.0F), armPart, arm);
        armPart.yRot = Mth.clamp(armPart.yRot, -(float) Math.PI / 3.0F, (float) Math.PI / 3.0F);
        armPart.xRot = Mth.clamp(armPart.xRot, -(float) Math.PI * 2.0F / 3.0F, (float) Math.PI / 6.0F);
    }

    private static void animateThirdPersonLanceItem(ArmedEntityRenderState state, PoseStack poseStack, float timeHeld,
                                                     HumanoidArm arm, ItemStack itemStack) {
        float animationTick = getLanceAnimationTick(itemStack, timeHeld, getEntity(state), 0.0F);
        RelicAnimator.apply(itemStack, LANCE_THIRD_PERSON, animationTick, poseStack, arm);
    }

    private static void animateFirstPersonIwatoshiUse(PoseStack poseStack, float timeHeld,
                                                       HumanoidArm arm, ItemStack itemStack) {
        RelicAnimator.apply(itemStack, IWATOSHI_CHARGE_FIRST_PERSON,
                Math.min(timeHeld, IwatoshiItem.getMaxChargeTicks()), poseStack, arm);
    }

    private static void animateThirdPersonIwatoshiHand(ModelPart armPart, ModelPart headPart, boolean rightArm,
                                                        float timeHeld, ItemStack itemStack) {
        HumanoidArm arm = rightArm ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        armPart.xRot = headPart.xRot;
        armPart.yRot = headPart.yRot;
        armPart.zRot = 0.0F;
        RelicAnimator.apply(itemStack, IWATOSHI_CHARGE_THIRD_PERSON,
                RelicAnimation.Channel.MAIN_ARM,
                Math.min(timeHeld, IwatoshiItem.getMaxChargeTicks()), armPart, arm);
    }

    private static void animateThirdPersonIwatoshiItem(PoseStack poseStack, float timeHeld,
                                                        HumanoidArm arm, ItemStack itemStack) {
        RelicAnimator.apply(itemStack, IWATOSHI_CHARGE_THIRD_PERSON,
                Math.min(timeHeld, IwatoshiItem.getMaxChargeTicks()), poseStack, arm);
    }

    private static void animateFirstPersonIwatoshiSlash(float attackTime, PoseStack poseStack,
                                                         HumanoidArm arm, ItemStack itemStack, int chargeLevel) {
        RelicAnimator.apply(itemStack, slashAnimation(true, chargeLevel),
                getAttackTick(attackTime), poseStack, arm);
    }

    private static void animateFirstPersonIwatoshiSpin(float attackTime, PoseStack poseStack,
                                                        HumanoidArm arm, ItemStack itemStack) {
        RelicAnimator.apply(itemStack, IWATOSHI_SPIN_FIRST_PERSON,
                getAttackTick(attackTime), poseStack, arm);
    }

    private static void animateThirdPersonIwatoshiSlashHand(HumanoidModel<?> model, HumanoidRenderState state,
                                                             int chargeLevel) {
        ModelPart armPart = model.getArm(state.attackArm);
        float attackTick = getAttackTick(state.attackTime);
        ItemStack itemStack = state.getUseItemStackForArm(state.attackArm);
        String animationId = slashAnimation(false, chargeLevel);
        RelicAnimator.apply(itemStack, animationId, RelicAnimation.Channel.BODY,
                attackTick, model.body, state.attackArm);
        RelicAnimator.apply(itemStack, animationId, RelicAnimation.Channel.MAIN_ARM,
                attackTick, armPart, state.attackArm);
    }

    private static void animateThirdPersonIwatoshiSpinHand(HumanoidModel<?> model, HumanoidRenderState state) {
        ModelPart armPart = model.getArm(state.attackArm);
        float attackTick = getAttackTick(state.attackTime);
        ItemStack itemStack = state.getUseItemStackForArm(state.attackArm);
        RelicAnimator.apply(itemStack, IWATOSHI_SPIN_THIRD_PERSON, RelicAnimation.Channel.BODY,
                attackTick, model.body, state.attackArm);
        RelicAnimator.apply(itemStack, IWATOSHI_SPIN_THIRD_PERSON,
                RelicAnimation.Channel.MAIN_ARM, attackTick, armPart, state.attackArm);
    }

    private static void animateThirdPersonIwatoshiSlashItem(float attackTime, PoseStack poseStack,
                                                             HumanoidArm arm, ItemStack itemStack, int chargeLevel) {
        RelicAnimator.apply(itemStack, slashAnimation(false, chargeLevel),
                getAttackTick(attackTime), poseStack, arm);
    }

    private static void animateThirdPersonIwatoshiSpinItem(float attackTime, PoseStack poseStack,
                                                            HumanoidArm arm, ItemStack itemStack) {
        RelicAnimator.apply(itemStack, IWATOSHI_SPIN_THIRD_PERSON,
                getAttackTick(attackTime), poseStack, arm);
    }

    private static void registerAnimations() {
        RelicAnimator.register(RHONGOMYNIAD, LANCE_FIRST_PERSON,
                new RelicAnimationClip(RhongomyniadItem.getJoustLowerTicks())
                        .keyframe(0.0F, identity(), RelicAnimationClip.Easing.LINEAR)
                        .keyframe(RhongomyniadItem.getJoustLowerTicks(), RelicTransform.poseStack(
                                        0.08F, 0.12F, -0.08F, -65.0F, -90.0F, 0.0F)
                                .anchor(0.15F, 0.1F, 0.0F)));
        RelicAnimator.register(RHONGOMYNIAD, LANCE_THIRD_PERSON,
                new RelicAnimation()
                        .channel(RelicAnimation.Channel.ITEM,
                                new RelicAnimationClip(RhongomyniadItem.getJoustLowerTicks())
                        .keyframe(0.0F, identity(), RelicAnimationClip.Easing.LINEAR)
                        .keyframe(RhongomyniadItem.getJoustLowerTicks(), RelicTransform.poseStack(
                                        0.0F, 0.0F, -0.38F, -65.0F, 90.0F, 0.0F)
                                .anchor(0.0F, -0.03125F, 0.125F)))
                        .channel(RelicAnimation.Channel.MAIN_ARM,
                                new RelicAnimationClip(RhongomyniadItem.getJoustLowerTicks())
                                        .keyframe(0.0F, armRotation(-43.0F, -5.73F, 0.0F),
                                                RelicAnimationClip.Easing.LINEAR)
                                        .keyframe(RhongomyniadItem.getJoustLowerTicks(),
                                                armRotation(-83.0F, -5.73F, 0.0F))));

        RelicAnimator.register(IWATOSHI, IWATOSHI_CHARGE_FIRST_PERSON,
                new RelicAnimationClip(IwatoshiItem.getMaxChargeTicks())
                        .keyframe(0.0F, identity(), RelicAnimationClip.Easing.LINEAR)
                        .keyframe(8.0F, RelicTransform.poseStack(
                                        -0.5014F, 0.2986F, -0.1029F, 25.0F, 50.0F, -40.0F)
                                .anchor(0.1F, 0.1F, 0.0F), RelicAnimationClip.Easing.IN_OUT_SINE)
                        .keyframe(IwatoshiItem.getMaxChargeTicks(), RelicTransform.poseStack(
                                        -0.1471F, 0.2029F, -0.0486F, 20.0F, 15.0F, -70.0F)
                                .anchor(0.1F, 0.1F, 0.0F)));
        RelicAnimator.register(IWATOSHI, IWATOSHI_CHARGE_THIRD_PERSON,
                new RelicAnimation()
                        .channel(RelicAnimation.Channel.ITEM,
                                new RelicAnimationClip(IwatoshiItem.getMaxChargeTicks())
                        .keyframe(0.0F, identity(), RelicAnimationClip.Easing.LINEAR)
                        .keyframe(8.0F, RelicTransform.poseStack(
                                0.08F, 0.05F, -0.08F, -15.0F, 25.0F, -45.0F),
                                RelicAnimationClip.Easing.OUT_BACK)
                        .keyframe(IwatoshiItem.getMaxChargeTicks(), RelicTransform.poseStack(
                                0.16F, 0.1F, -0.16F, -25.0F, 50.0F, -75.0F)))
                        .channel(RelicAnimation.Channel.MAIN_ARM,
                                new RelicAnimationClip(IwatoshiItem.getMaxChargeTicks())
                                        .keyframe(0.0F, identity(), RelicAnimationClip.Easing.LINEAR)
                                        .keyframe(8.0F, armRotation(-49.0F, 21.0F, 15.0F),
                                                RelicAnimationClip.Easing.OUT_BACK)
                                        .keyframe(IwatoshiItem.getMaxChargeTicks(),
                                                armRotation(-69.0F, 46.0F, 29.0F))));

        for (int chargeLevel = 1; chargeLevel < IwatoshiItem.getMaxChargeLevel(); chargeLevel++) {
            RelicAnimator.register(IWATOSHI, slashAnimation(true, chargeLevel),
                    createSlashAnimation(chargeLevel, true));
            RelicAnimator.register(IWATOSHI, slashAnimation(false, chargeLevel),
                    createThirdPersonSlashAnimation(chargeLevel));
        }
        RelicAnimator.register(IWATOSHI, IWATOSHI_SPIN_FIRST_PERSON, createSpinAnimation(true));
        RelicAnimator.register(IWATOSHI, IWATOSHI_SPIN_THIRD_PERSON, createThirdPersonSpinAnimation());
        RelicAnimator.register(BERTILAK, BERTILAK_COVENANT_THIRD_PERSON,
                new RelicAnimationClip(BertilakExtensions.TARGETING_TRANSITION_TICKS)
                        .keyframe(BertilakExtensions.TARGETING_TRANSITION_TICKS,
                                RelicTransform.poseStack(
                                        BertilakExtensions.THIRD_PERSON_TRANSLATION_X,
                                        BertilakExtensions.THIRD_PERSON_TRANSLATION_Y,
                                        BertilakExtensions.THIRD_PERSON_TRANSLATION_Z,
                                        BertilakExtensions.THIRD_PERSON_ROTATION_X,
                                        BertilakExtensions.THIRD_PERSON_ROTATION_Y,
                                        BertilakExtensions.THIRD_PERSON_ROTATION_Z)));

        RelicAnimator.registerEditorPose(
                RHONGOMYNIAD, "joust_raised", LANCE_FIRST_PERSON, LANCE_THIRD_PERSON);
        RelicAnimator.registerEditorPose(
                RHONGOMYNIAD, "joust_lowered", LANCE_FIRST_PERSON, LANCE_THIRD_PERSON);
        RelicAnimator.registerEditorPose(IWATOSHI, "use",
                IWATOSHI_CHARGE_FIRST_PERSON, IWATOSHI_CHARGE_THIRD_PERSON);
        for (int chargeLevel = 1; chargeLevel < IwatoshiItem.getMaxChargeLevel(); chargeLevel++) {
            RelicAnimator.registerEditorPose(IWATOSHI, "slash_" + chargeLevel,
                    slashAnimation(true, chargeLevel), slashAnimation(false, chargeLevel));
        }
        RelicAnimator.registerEditorPose(IWATOSHI, "spin",
                IWATOSHI_SPIN_FIRST_PERSON, IWATOSHI_SPIN_THIRD_PERSON);
        RelicAnimator.registerEditorPose(BERTILAK, "covenant",
                null, BERTILAK_COVENANT_THIRD_PERSON);
    }

    private static RelicAnimationClip createSlashAnimation(int chargeLevel, boolean firstPerson) {
        float startAngle = (firstPerson ? -30.0F : -35.0F) - chargeLevel * 15.0F;
        float endAngle = (firstPerson ? 45.0F : 50.0F) + chargeLevel * 25.0F;
        float rotationScale = firstPerson ? 0.45F : 0.35F;
        float translationX = firstPerson ? 0.16F : 0.0F;
        float translationY = firstPerson ? -0.08F : 0.08F;
        float translationZ = firstPerson ? 0.18F : -0.12F;
        float rotationX = firstPerson ? -20.0F - chargeLevel * 5.0F : 0.0F;
        float anchorX = firstPerson ? 0.15F : 0.0F;
        float anchorY = firstPerson ? -0.05F : 0.0F;
        float anchorZ = firstPerson ? 0.0F : 0.125F;
        return new RelicAnimationClip(IwatoshiAttackState.getAttackAnimationTicks())
                .keyframe(0.0F, RelicTransform.poseStack(
                                0.0F, 0.0F, 0.0F, 0.0F, startAngle, startAngle * rotationScale)
                        .anchor(anchorX, anchorY, anchorZ), RelicAnimationClip.Easing.LINEAR)
                .keyframe(3.0F, RelicTransform.poseStack(
                                translationX, translationY, translationZ,
                                rotationX, endAngle, endAngle * rotationScale)
                        .anchor(anchorX, anchorY, anchorZ), RelicAnimationClip.Easing.OUT_BACK)
                .keyframe(8.0F, RelicTransform.poseStack(
                                translationX * 0.65F, translationY * 0.65F, translationZ * 0.65F,
                                rotationX * 0.8F, endAngle, endAngle * rotationScale)
                        .anchor(anchorX, anchorY, anchorZ), RelicAnimationClip.Easing.LINEAR)
                .keyframe(IwatoshiAttackState.getAttackAnimationTicks(), identity());
    }

    private static RelicAnimation createThirdPersonSlashAnimation(int chargeLevel) {
        float startAngle = -35.0F - chargeLevel * 15.0F;
        float endAngle = 50.0F + chargeLevel * 25.0F;
        float armX = -75.0F - chargeLevel * 5.0F;
        float armZ = 25.0F + chargeLevel * 8.0F;
        return new RelicAnimation()
                .channel(RelicAnimation.Channel.ITEM, createSlashAnimation(chargeLevel, false))
                .channel(RelicAnimation.Channel.MAIN_ARM,
                        new RelicAnimationClip(IwatoshiAttackState.getAttackAnimationTicks())
                                .keyframe(0.0F, armRotation(armX, startAngle, 0.0F),
                                        RelicAnimationClip.Easing.LINEAR)
                                .keyframe(3.0F, armRotation(armX, endAngle, armZ),
                                        RelicAnimationClip.Easing.OUT_BACK)
                                .keyframe(8.0F, armRotation(armX * 0.8F, endAngle, armZ * 0.55F),
                                        RelicAnimationClip.Easing.LINEAR)
                                .keyframe(IwatoshiAttackState.getAttackAnimationTicks(), identity()))
                .channel(RelicAnimation.Channel.BODY,
                        new RelicAnimationClip(IwatoshiAttackState.getAttackAnimationTicks())
                                .keyframe(0.0F, armRotation(0.0F, startAngle * 0.25F, 0.0F),
                                        RelicAnimationClip.Easing.LINEAR)
                                .keyframe(3.0F, armRotation(0.0F, endAngle * 0.25F, 0.0F),
                                        RelicAnimationClip.Easing.OUT_BACK)
                                .keyframe(8.0F, armRotation(0.0F, endAngle * 0.18F, 0.0F),
                                        RelicAnimationClip.Easing.LINEAR)
                                .keyframe(IwatoshiAttackState.getAttackAnimationTicks(), identity()));
    }

    private static RelicAnimationClip createSpinAnimation(boolean firstPerson) {
        float translationY = firstPerson ? -0.12F : 0.1F;
        float translationZ = firstPerson ? 0.2F : -0.15F;
        float rotationX = firstPerson ? -25.0F : 0.0F;
        float rotationZ = -35.0F;
        float anchorY = firstPerson ? -0.1F : 0.0F;
        float anchorZ = firstPerson ? 0.1F : 0.125F;
        return new RelicAnimationClip(IwatoshiAttackState.getAttackAnimationTicks())
                .keyframe(0.0F, RelicTransform.poseStack(
                                0.0F, 0.0F, 0.0F, rotationX, 0.0F, rotationZ)
                        .anchor(0.0F, anchorY, anchorZ), RelicAnimationClip.Easing.LINEAR)
                .keyframe(2.0F, RelicTransform.poseStack(
                                0.12F, translationY * 0.7F, translationZ * 0.7F,
                                rotationX, 90.0F, rotationZ)
                        .anchor(0.0F, anchorY, anchorZ), RelicAnimationClip.Easing.LINEAR)
                .keyframe(4.0F, RelicTransform.poseStack(
                                0.0F, translationY, translationZ,
                                rotationX, 180.0F, rotationZ)
                        .anchor(0.0F, anchorY, anchorZ), RelicAnimationClip.Easing.LINEAR)
                .keyframe(6.0F, RelicTransform.poseStack(
                                -0.12F, translationY * 0.7F, translationZ * 0.7F,
                                rotationX, 270.0F, rotationZ)
                        .anchor(0.0F, anchorY, anchorZ), RelicAnimationClip.Easing.LINEAR)
                .keyframe(8.0F, RelicTransform.poseStack(
                                0.0F, 0.0F, 0.0F, rotationX, 360.0F, rotationZ)
                        .anchor(0.0F, anchorY, anchorZ), RelicAnimationClip.Easing.LINEAR)
                .keyframe(14.0F, RelicTransform.poseStack(
                                0.0F, 0.0F, 0.0F, rotationX * 0.65F, 360.0F, rotationZ * 0.65F)
                        .anchor(0.0F, anchorY, anchorZ), RelicAnimationClip.Easing.LINEAR)
                .keyframe(IwatoshiAttackState.getAttackAnimationTicks(), identity());
    }

    private static RelicAnimation createThirdPersonSpinAnimation() {
        return new RelicAnimation()
                .channel(RelicAnimation.Channel.ITEM, createSpinAnimation(false))
                .channel(RelicAnimation.Channel.MAIN_ARM,
                        new RelicAnimationClip(IwatoshiAttackState.getAttackAnimationTicks())
                                .keyframe(0.0F, armRotation(-90.0F, 0.0F, 26.0F),
                                        RelicAnimationClip.Easing.LINEAR)
                                .keyframe(2.0F, armRotation(-90.0F, 90.0F, 26.0F),
                                        RelicAnimationClip.Easing.LINEAR)
                                .keyframe(4.0F, armRotation(-90.0F, 180.0F, 26.0F),
                                        RelicAnimationClip.Easing.LINEAR)
                                .keyframe(6.0F, armRotation(-90.0F, 270.0F, 26.0F),
                                        RelicAnimationClip.Easing.LINEAR)
                                .keyframe(8.0F, armRotation(-90.0F, 360.0F, 26.0F),
                                        RelicAnimationClip.Easing.LINEAR)
                                .keyframe(14.0F, armRotation(-58.0F, 360.0F, 17.0F),
                                        RelicAnimationClip.Easing.LINEAR)
                                .keyframe(IwatoshiAttackState.getAttackAnimationTicks(), identity()))
                .channel(RelicAnimation.Channel.BODY,
                        new RelicAnimationClip(IwatoshiAttackState.getAttackAnimationTicks())
                                .keyframe(0.0F, identity(), RelicAnimationClip.Easing.LINEAR)
                                .keyframe(2.0F, armRotation(0.0F, 37.0F, 0.0F),
                                        RelicAnimationClip.Easing.LINEAR)
                                .keyframe(4.0F, identity(), RelicAnimationClip.Easing.LINEAR)
                                .keyframe(6.0F, armRotation(0.0F, -37.0F, 0.0F),
                                        RelicAnimationClip.Easing.LINEAR)
                                .keyframe(8.0F, identity(), RelicAnimationClip.Easing.LINEAR)
                                .keyframe(IwatoshiAttackState.getAttackAnimationTicks(), identity()));
    }

    private static String slashAnimation(boolean firstPerson, int chargeLevel) {
        return "iwatoshi_slash_" + chargeLevel + (firstPerson ? "_first_person" : "_third_person");
    }

    private static RelicTransform identity() {
        return RelicTransform.poseStack(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    private static RelicTransform armRotation(float rotationX, float rotationY, float rotationZ) {
        return RelicTransform.poseStack(0.0F, 0.0F, 0.0F, rotationX, rotationY, rotationZ);
    }

    private static float getLanceAnimationTick(ItemStack itemStack, float timeHeld,
                                                LivingEntity entity, float partialTick) {
        KineticWeapon kineticWeapon = itemStack.get(DataComponents.KINETIC_WEAPON);
        float lowerTime = kineticWeapon == null ? 8.0F : Math.max(kineticWeapon.delayTicks(), 1);
        float lowerProgress = progress(timeHeld, 0.0F, lowerTime);
        if (entity != null && RhongomyniadItem.isRecovering(entity)) {
            lowerProgress *= 1.0F - Ease.inOutSine(RhongomyniadItem.getRecoveryProgress(entity, partialTick));
        }
        return lowerProgress * RhongomyniadItem.getJoustLowerTicks();
    }

    private static LivingEntity getEntity(ArmedEntityRenderState state) {
        if (state instanceof AvatarRenderState avatarRenderState
                && Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getEntity(avatarRenderState.id) instanceof LivingEntity entity) {
            return entity;
        }
        return null;
    }

    private static float progress(float value, float start, float end) {
        return Mth.clamp(Mth.inverseLerp(value, start, end), 0.0F, 1.0F);
    }

    private static float getAttackTick(float attackProgress) {
        return attackProgress * IwatoshiAttackState.getAttackAnimationTicks();
    }

}
