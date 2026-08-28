package net.turtleboi.noblephantasms.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.math.Axis;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.KineticWeapon;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.util.TransformationHelper;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.client.BertilakExtensions;
import net.turtleboi.noblephantasms.client.GungnirExtensions;
import net.turtleboi.noblephantasms.client.RhongomyniadSpinState;
import net.turtleboi.noblephantasms.client.renderer.ItemPoseDebugRenderer;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;
import net.turtleboi.noblephantasms.item.custom.GungnirItem;
import net.turtleboi.noblephantasms.item.custom.IwatoshiItem;
import net.turtleboi.noblephantasms.item.custom.RhongomyniadItem;
import net.turtleboi.noblephantasms.item.custom.SpearRelicItem;
import net.turtleboi.noblephantasms.screens.ItemPoseEditorScreen;
import org.joml.Quaternionf;

public final class ItemPoseEditor {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
    private static final Map<ItemStackRenderState, RenderedItem> RENDERED_ITEMS = new WeakHashMap<>();
    private static final Map<EditorKey, Map<String, RelicAnimation>> EDITED_ANIMATIONS = new HashMap<>();
    private static final Map<RuntimeAnimationKey, RelicAnimation> SHARED_RUNTIME_ANIMATIONS = new HashMap<>();
    private static final ThreadLocal<Target> MODEL_TARGET = new ThreadLocal<>();
    private static final ThreadLocal<RelicTransform> MODEL_TRANSFORM = new ThreadLocal<>();
    private static Session activeSession;

    public static void register(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("itempose")
                .executes(context -> open(context.getSource()));
        event.getDispatcher().register(command);
    }

    private static int open(CommandSourceStack source) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            source.sendFailure(Component.literal("Join a world before opening the item pose editor"));
            return 0;
        }

        CameraType cameraType = minecraft.options.getCameraType();
        List<Target> targets = new ArrayList<>();
        addTarget(targets, minecraft.player.getMainHandItem(), InteractionHand.MAIN_HAND,
                minecraft.player.getMainArm(), cameraType);
        addTarget(targets, minecraft.player.getOffhandItem(), InteractionHand.OFF_HAND,
                minecraft.player.getMainArm().getOpposite(), cameraType);
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("Hold an item before opening the item pose editor"));
            return 0;
        }

        activeSession = new Session(List.copyOf(targets));
        minecraft.setScreen(new ItemPoseEditorScreen(activeSession));
        return 1;
    }

    private static void addTarget(List<Target> targets, ItemStack stack, InteractionHand hand,
                                  HumanoidArm arm, CameraType cameraType) {
        if (stack.isEmpty()) {
            return;
        }
        ItemDisplayContext displayContext = displayContext(cameraType, arm);
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<String> poses = getPoses(stack);
        EditorKey key = new EditorKey(itemId, cameraType, displayContext, hand);
        Map<String, RelicAnimation> animations = EDITED_ANIMATIONS.computeIfAbsent(
                key, ignored -> new LinkedHashMap<>());
        poses.forEach(pose -> animations.computeIfAbsent(pose, ignored -> {
            String animationId = RelicWeaponAnimations.getEditorAnimationId(stack, cameraType, pose);
            if (animationId == null) {
                return initialAnimation(stack, cameraType, pose);
            }
            RuntimeAnimationKey animationKey = new RuntimeAnimationKey(itemId, animationId);
            return SHARED_RUNTIME_ANIMATIONS.computeIfAbsent(animationKey,
                    ignoredAnimation -> initialAnimation(stack, cameraType, pose));
        }));
        targets.add(new Target(key, stack.getHoverName(), poses, animations));
    }

    private static RelicAnimation initialAnimation(ItemStack stack, CameraType cameraType, String pose) {
        RelicAnimation registeredAnimation = RelicWeaponAnimations.getEditorAnimation(
                stack, cameraType, pose);
        if (registeredAnimation != null) {
            return registeredAnimation;
        }
        float durationTicks = pose.equals("held") ? 1.0F : getDefaultPreviewDurationTicks(stack, pose);
        RelicTransform transform = initialTransform(stack, cameraType, pose);
        RelicAnimationClip animation = new RelicAnimationClip(durationTicks);
        if (!pose.equals("held") && !transform.usesModelDisplay()) {
            animation.keyframe(0.0F,
                    RelicTransform.poseStack(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
                    RelicAnimationClip.Easing.LINEAR);
            animation.keyframe(durationTicks, transform);
        } else {
            animation.keyframe(0.0F, transform, RelicAnimationClip.Easing.LINEAR);
        }
        return new RelicAnimation().channel(RelicAnimation.Channel.ITEM, animation);
    }

    private static RelicTransform initialTransform(ItemStack stack, CameraType cameraType, String pose) {
        if (cameraType.isFirstPerson() && stack.getItem() instanceof RhongomyniadItem
                && pose.equals("stab")) {
            return RelicTransform.poseStack(0.0F, 0.15F, 0.0F, -15.0F, 0.0F, 15.0F);
        }
        if (cameraType.isFirstPerson() && stack.getItem() instanceof SpearRelicItem) {
            if (pose.equals("stab")) {
                return RelicTransform.poseStack(0.0F, GungnirExtensions.STAB_TRANSLATION_Y, 0.0F,
                        GungnirExtensions.STAB_ROTATION_X, 0.0F, GungnirExtensions.STAB_ROTATION_Z);
            }
            if (stack.getItem() instanceof GungnirItem && pose.equals("throw")) {
                return RelicTransform.poseStack(GungnirExtensions.THROW_TRANSLATION_X,
                        GungnirExtensions.THROW_TRANSLATION_Y, GungnirExtensions.THROW_TRANSLATION_Z,
                        GungnirExtensions.THROW_ROTATION_X, GungnirExtensions.THROW_ROTATION_Y,
                        GungnirExtensions.THROW_ROTATION_Z);
            }
        }
        if (stack.getItem() instanceof BertilakItem && pose.equals("covenant")) {
            if (cameraType.isFirstPerson()) {
                return RelicTransform.poseStack(BertilakExtensions.TARGETING_TRANSLATION_X,
                        BertilakExtensions.TARGETING_TRANSLATION_Y, BertilakExtensions.TARGETING_TRANSLATION_Z,
                        BertilakExtensions.TARGETING_ROTATION_X, BertilakExtensions.TARGETING_ROTATION_Y,
                        BertilakExtensions.TARGETING_ROTATION_Z);
            }
            return RelicTransform.poseStack(BertilakExtensions.THIRD_PERSON_TRANSLATION_X,
                    BertilakExtensions.THIRD_PERSON_TRANSLATION_Y,
                    BertilakExtensions.THIRD_PERSON_TRANSLATION_Z,
                    BertilakExtensions.THIRD_PERSON_ROTATION_X,
                    BertilakExtensions.THIRD_PERSON_ROTATION_Y,
                    BertilakExtensions.THIRD_PERSON_ROTATION_Z);
        }
        return RelicTransform.modelDisplay();
    }

    private static ItemDisplayContext displayContext(CameraType cameraType, HumanoidArm arm) {
        if (cameraType.isFirstPerson()) {
            return arm == HumanoidArm.RIGHT
                    ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        }
        return arm == HumanoidArm.RIGHT
                ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    private static List<String> getPoses(ItemStack stack) {
        RelicWeaponAnimations.initialize();
        List<String> poses = new ArrayList<>();
        poses.add("held");
        if (stack.getItem() instanceof RhongomyniadItem) {
            poses.add("stab");
            poses.add("joust_raised");
            poses.add("joust_lowered");
            return List.copyOf(poses);
        }
        if (stack.getItem() instanceof SpearRelicItem) {
            poses.add("stab");
            if (stack.getItem() instanceof GungnirItem) {
                poses.add("throw");
            } else if (stack.getItem() instanceof IwatoshiItem) {
                poses.add("use");
            }
            RelicAnimator.getEditorPoses(stack).forEach(pose -> {
                if (!poses.contains(pose)) {
                    poses.add(pose);
                }
            });
            return List.copyOf(poses);
        }
        if (stack.getItem() instanceof BertilakItem) {
            poses.add("covenant");
            return List.copyOf(poses);
        }
        if (stack.getSwingAnimation().type() == SwingAnimationType.STAB) {
            poses.add("attack");
        }
        if (stack.getUseAnimation() != ItemUseAnimation.NONE) {
            poses.add("use");
        }
        RelicAnimator.getEditorPoses(stack).forEach(pose -> {
            if (!poses.contains(pose)) {
                poses.add(pose);
            }
        });
        return List.copyOf(poses);
    }

    public static void track(ItemStackRenderState renderState, ItemStack itemStack,
                             ItemDisplayContext displayContext, ItemOwner owner) {
        if (itemStack.isEmpty()) {
            RENDERED_ITEMS.remove(renderState);
            return;
        }
        int ownerId = owner instanceof Entity entity ? entity.getId() : Integer.MIN_VALUE;
        RENDERED_ITEMS.put(renderState,
                new RenderedItem(BuiltInRegistries.ITEM.getKey(itemStack.getItem()), displayContext, ownerId));
    }

    public static void beginModelTransform(ItemStackRenderState renderState, ItemDisplayContext displayContext) {
        Target target = currentTarget();
        RenderedItem renderedItem = RENDERED_ITEMS.get(renderState);
        Minecraft minecraft = Minecraft.getInstance();
        if (renderedItem == null) {
            MODEL_TARGET.remove();
            MODEL_TRANSFORM.remove();
            return;
        }
        boolean editingRenderedItem = target != null && minecraft.player != null
                && renderedItem.ownerId() == minecraft.player.getId()
                && renderedItem.itemId().equals(target.key().itemId())
                && renderedItem.displayContext() == displayContext
                && target.key().displayContext() == displayContext;
        if (editingRenderedItem) {
            MODEL_TARGET.set(target);
        } else {
            MODEL_TARGET.remove();
        }

        RelicTransform previewTransform = editingRenderedItem ? target.previewTransform() : null;
        RelicTransform modelTransform = previewTransform != null && previewTransform.usesModelDisplay()
                ? previewTransform
                : RelicAnimationStorage.getModelTransform(renderedItem.itemId(), displayContext);
        if (modelTransform == null) {
            MODEL_TRANSFORM.remove();
        } else {
            MODEL_TRANSFORM.set(modelTransform);
        }
    }

    public static boolean applyModelTransform(ItemTransform source, boolean applyLeftHandFix, PoseStack.Pose pose) {
        RelicTransform transform = MODEL_TRANSFORM.get();
        if (transform == null || !transform.usesModelDisplay()) {
            return false;
        }
        transform.initialize(source.translation().x() * 16.0F, source.translation().y() * 16.0F,
                source.translation().z() * 16.0F, source.rotation().x(), source.rotation().y(),
                source.rotation().z(), source.scale().x(), source.scale().y(), source.scale().z());

        float translationX = transform.translationX / 16.0F;
        float rotationY = transform.rotationY;
        float rotationZ = transform.rotationZ;
        float anchorX = transform.anchorX;
        if (applyLeftHandFix) {
            translationX = -translationX;
            rotationY = -rotationY;
            rotationZ = -rotationZ;
            anchorX = 1.0F - anchorX;
        }
        pose.translate(translationX, transform.translationY / 16.0F, transform.translationZ / 16.0F);
        pose.translate(anchorX - 0.5F, transform.anchorY - 0.5F, transform.anchorZ - 0.5F);
        pose.rotate(new Quaternionf().rotationXYZ(
                transform.rotationX * ((float) Math.PI / 180.0F),
                rotationY * ((float) Math.PI / 180.0F),
                rotationZ * ((float) Math.PI / 180.0F)));
        pose.scale(transform.scaleX, transform.scaleY, transform.scaleZ);
        RhongomyniadSpinState.apply(pose);
        pose.rotate(TransformationHelper.quatFromXYZ(source.rightRotation().x(),
                source.rightRotation().y() * (applyLeftHandFix ? -1 : 1),
                source.rightRotation().z() * (applyLeftHandFix ? -1 : 1), true));
        pose.translate(-anchorX, -transform.anchorY, -transform.anchorZ);
        return true;
    }

    public static void submitDebugGeometry(ItemStackRenderState renderState, PoseStack poseStack,
                                           SubmitNodeCollector submitNodeCollector) {
        Target target = MODEL_TARGET.get();
        if (target != null) {
            ItemPoseDebugRenderer.submit(
                    renderState, poseStack, submitNodeCollector, target.previewTransform(),
                    target.key().displayContext().leftHand());
        }
    }

    public static void endModelTransform() {
        MODEL_TARGET.remove();
        MODEL_TRANSFORM.remove();
    }

    public static void applyFirstPersonAttackPreview(PoseStack poseStack, InteractionHand hand,
                                                     ItemStack stack, HumanoidArm arm) {
        Target target = matchingFirstPersonTarget(hand, stack);
        if (target == null || !isAttackPose(target.currentPose())) {
            return;
        }

        if (target.currentPose().startsWith("slash_") || target.currentPose().equals("spin")) {
            return;
        }

        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float attackTime = target.playhead() / target.currentAnimation().durationTicks();
        SpearAnimations.firstPersonAttack(attackTime, poseStack, direction, arm);
        RelicTransform transform = target.previewTransform();
        if (!transform.usesModelDisplay()) {
            transform.apply(poseStack, arm);
        }
    }

    public static void applyFirstPersonUsePreview(PoseStack poseStack, InteractionHand hand,
                                                   ItemStack stack, HumanoidArm arm, float inverseArmHeight) {
        Target target = matchingFirstPersonTarget(hand, stack);
        if (target == null || !isUsePose(target.currentPose())) {
            return;
        }

        poseStack.translate(0.0F, inverseArmHeight * 0.6F, 0.0F);
        if (stack.getItem() instanceof GungnirItem) {
            GungnirExtensions.applyEditorThrowTransform(poseStack, arm, target.previewTransform());
            return;
        }
        if (stack.getItem() instanceof BertilakItem) {
            BertilakExtensions.applyEditorCovenantTransform(poseStack, arm, target.previewTransform());
            return;
        }

        float timeHeld = getPreviewUseTick(target);
        if (stack.getUseAnimation() == ItemUseAnimation.SPEAR) {
            SpearAnimations.firstPersonUse(0.0F, poseStack, timeHeld, arm, stack);
            if (stack.getItem() instanceof RhongomyniadItem) {
                RhongomyniadSpinState.begin(stack, timeHeld);
            }
        } else if (stack.getUseAnimation() == ItemUseAnimation.TRIDENT) {
            int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
            poseStack.translate(direction * -0.5F, 0.7F, 0.1F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-55.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(direction * 35.3F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(direction * -9.785F));
            poseStack.translate(0.0F, 0.0F, 0.2F);
            poseStack.scale(1.0F, 1.0F, 1.2F);
            poseStack.mulPose(Axis.YN.rotationDegrees(direction * 45.0F));
        }
    }

    public static void applyThirdPersonPreview(Avatar entity, AvatarRenderState state) {
        Minecraft minecraft = Minecraft.getInstance();
        Target target = currentTarget();
        if (target == null || minecraft.player != entity || target.key().cameraType().isFirstPerson()) {
            return;
        }

        ItemStack stack = entity.getItemInHand(target.key().hand());
        if (!sameItem(target, stack)) {
            return;
        }
        HumanoidArm arm = target.key().hand() == InteractionHand.MAIN_HAND
                ? state.mainArm : state.mainArm.getOpposite();
        String pose = target.currentPose();
        if (isAttackPose(pose)) {
            state.attackArm = arm;
            state.attackTime = target.playhead() / target.currentAnimation().durationTicks();
            state.swingAnimationType = SwingAnimationType.STAB;
            setArmPose(state, arm, HumanoidModel.ArmPose.SPEAR);
        } else if (isUsePose(pose)) {
            state.isUsingItem = true;
            state.useItemHand = target.key().hand();
            state.ticksUsingItem = getPreviewUseTick(target);
            setArmPose(state, arm, armPose(stack.getUseAnimation()));
        }
    }

    private static HumanoidModel.ArmPose armPose(ItemUseAnimation animation) {
        return switch (animation) {
            case BLOCK -> HumanoidModel.ArmPose.BLOCK;
            case BOW -> HumanoidModel.ArmPose.BOW_AND_ARROW;
            case TRIDENT -> HumanoidModel.ArmPose.THROW_TRIDENT;
            case CROSSBOW -> HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            case SPYGLASS -> HumanoidModel.ArmPose.SPYGLASS;
            case TOOT_HORN -> HumanoidModel.ArmPose.TOOT_HORN;
            case BRUSH -> HumanoidModel.ArmPose.BRUSH;
            case SPEAR -> HumanoidModel.ArmPose.SPEAR;
            default -> HumanoidModel.ArmPose.ITEM;
        };
    }

    private static void setArmPose(AvatarRenderState state, HumanoidArm arm, HumanoidModel.ArmPose pose) {
        if (arm == HumanoidArm.RIGHT) {
            state.rightArmPose = pose;
        } else {
            state.leftArmPose = pose;
        }
    }

    public static boolean previewIsUsing(ItemStack stack, LivingEntity owner,
                                         ItemDisplayContext displayContext) {
        Target target = currentTarget();
        return target != null
                && owner == Minecraft.getInstance().player
                && target.key().displayContext() == displayContext
                && isUsePose(target.currentPose())
                && sameItem(target, stack)
                && owner.getItemInHand(target.key().hand()).getItem() == stack.getItem();
    }

    public static RelicTransform getThirdPersonTransform(ArmedEntityRenderState state,
                                                         ItemStack stack, String pose) {
        Target target = currentTarget();
        if (target == null || target.key().cameraType().isFirstPerson()
                || !isLocalPlayerState(state) || !target.currentPose().equals(pose)
                || !sameItem(target, stack)) {
            return null;
        }
        return target.previewTransform();
    }

    public static RelicTransform getAnimationOverride(ItemStack itemStack, String animationId,
                                                       RelicAnimation.Channel channel) {
        Target target = currentTarget();
        Minecraft minecraft = Minecraft.getInstance();
        if (target == null || minecraft.player == null
                || RelicWeaponAnimationContext.getEntity() != minecraft.player
                || RelicWeaponAnimationContext.getItemStack() != itemStack
                || RelicWeaponAnimationContext.getHand() != target.key().hand()
                || RelicWeaponAnimationContext.getDisplayContext() != target.key().displayContext()
                || !sameItem(target, itemStack)) {
            return null;
        }
        String editorAnimationId = RelicWeaponAnimations.getEditorAnimationId(
                itemStack, target.key().cameraType(), target.currentPose());
        return animationId.equals(editorAnimationId) ? target.previewTransform(channel) : null;
    }

    public static boolean isPreviewingUse(InteractionHand hand, ItemStack stack) {
        Target target = matchingFirstPersonTarget(hand, stack);
        return target != null && isUsePose(target.currentPose());
    }

    public static int getIwatoshiPreviewChargeLevel(ItemStack itemStack) {
        Target target = currentTarget();
        Minecraft minecraft = Minecraft.getInstance();
        if (target == null || minecraft.player == null
                || RelicWeaponAnimationContext.getEntity() != minecraft.player
                || target.key().hand() != RelicWeaponAnimationContext.getHand()
                || !sameItem(target, itemStack)) {
            return 0;
        }
        return chargeLevelForPose(target.currentPose());
    }

    public static int getIwatoshiPreviewChargeLevel(ArmedEntityRenderState state, ItemStack itemStack) {
        Target target = currentTarget();
        if (target == null || target.key().cameraType().isFirstPerson()
                || !isLocalPlayerState(state) || !sameItem(target, itemStack)) {
            return 0;
        }
        return chargeLevelForPose(target.currentPose());
    }

    public static boolean applyThirdPersonAttackItemPreview(ArmedEntityRenderState state,
                                                             PoseStack poseStack) {
        Target target = currentTarget();
        if (target == null || target.key().cameraType().isFirstPerson() || !isLocalPlayerState(state)
                || (!target.currentPose().startsWith("slash_")
                && !target.currentPose().equals("spin"))
                || !sameItem(target, state.getUseItemStackForArm(state.attackArm))) {
            return false;
        }
        RelicTransform transform = target.previewTransform();
        if (transform.usesModelDisplay()) {
            return false;
        }
        transform.apply(poseStack, state.attackArm);
        return true;
    }

    private static float getDefaultPreviewDurationTicks(ItemStack stack, String pose) {
        if (isAttackPose(pose)) {
            return Math.max(stack.getSwingAnimation().duration(), 1);
        }
        if (stack.getItem() instanceof RhongomyniadItem) {
            if (pose.equals("joust_raised")) {
                return RhongomyniadItem.getChargeStartTick(stack);
            }
            KineticWeapon kineticWeapon = stack.get(DataComponents.KINETIC_WEAPON);
            if (kineticWeapon != null) {
                return Math.max(kineticWeapon.delayTicks(), 1);
            }
        }
        if (stack.getItem() instanceof GungnirItem && pose.equals("throw")) {
            return GungnirItem.FULL_CHARGE_TICKS;
        }
        if (stack.getItem() instanceof IwatoshiItem && pose.equals("use")) {
            return IwatoshiItem.getMaxChargeTicks();
        }
        if (stack.getItem() instanceof BertilakItem && pose.equals("covenant")) {
            return BertilakExtensions.TARGETING_TRANSITION_TICKS;
        }
        return 20.0F;
    }

    private static float getPreviewUseTick(Target target) {
        return target.playhead();
    }

    private static Target matchingFirstPersonTarget(InteractionHand hand, ItemStack stack) {
        Target target = currentTarget();
        if (target == null || !target.key().cameraType().isFirstPerson()
                || target.key().hand() != hand || !sameItem(target, stack)) {
            return null;
        }
        return target;
    }

    private static boolean sameItem(Target target, ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(target.key().itemId());
    }

    private static boolean isAttackPose(String pose) {
        return pose.equals("stab") || pose.equals("attack")
                || pose.startsWith("slash_") || pose.equals("spin");
    }

    private static boolean isUsePose(String pose) {
        return !pose.equals("held") && !isAttackPose(pose);
    }

    private static Target currentTarget() {
        return activeSession != null ? activeSession.currentTarget() : null;
    }

    public static void close(Session session) {
        if (activeSession == session) {
            activeSession = null;
            RhongomyniadSpinState.end();
        }
    }

    public static SaveResult save(Session session) {
        int appliedAnimations = applyRuntimeAnimations(session);
        String export = createExport(session);
        Path logPath = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("logs").resolve("noblephantasms-item-poses.log");
        try {
            RelicAnimationStorage.SaveResult storageResult = RelicAnimationStorage.save(session);
            Files.createDirectories(logPath.getParent());
            Files.writeString(logPath, export, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Minecraft.getInstance().keyboardHandler.setClipboard(export.strip());
            NoblePhantasms.LOGGER.info(
                    "Saved relic animations to {} and applied {} runtime animations and {} model transforms",
                    storageResult.path(), appliedAnimations, storageResult.modelTransformCount());
            return new SaveResult(true, storageResult.path(), appliedAnimations,
                    storageResult.modelTransformCount());
        } catch (IOException exception) {
            NoblePhantasms.LOGGER.error("Unable to save item pose export", exception);
            return new SaveResult(false, logPath.toAbsolutePath(), 0, 0);
        }
    }

    private static String createExport(Session session) {
        StringBuilder output = new StringBuilder();
        output.append("# Noble Phantasms item pose export ")
                .append(TIMESTAMP.format(LocalDateTime.now())).append(System.lineSeparator());
        for (Target target : session.targets()) {
            for (String pose : target.poses()) {
                RelicAnimation animation = target.animations().get(pose);
                for (RelicAnimation.Channel channel : animation.channels()) {
                    RelicAnimationClip clip = animation.channel(channel);
                    for (int index = 0; index < clip.keyframes().size(); index++) {
                        RelicAnimationClip.Keyframe keyframe = clip.keyframes().get(index);
                        RelicTransform transform = keyframe.transform();
                        output.append(String.format(Locale.ROOT,
                                "item=%s, pov=%s, display_context=%s, hand=%s, pose=%s, channel=%s, "
                                        + "blend=%s, duration_ticks=%.2f, keyframe=%d, tick=%.2f, "
                                        + "easing=%s, transform_space=%s, "
                                        + "translation=(%.4f, %.4f, %.4f), rotation=(%.2f, %.2f, %.2f), "
                                        + "scale=(%.4f, %.4f, %.4f), anchor=(%.4f, %.4f, %.4f)%n",
                                target.key().itemId(), target.key().cameraType(), target.key().displayContext(),
                                target.key().hand(), pose, channel, animation.blendMode(channel),
                                clip.durationTicks(), index, keyframe.tick(), keyframe.easing(),
                                transform.spaceName(), transform.translationX, transform.translationY,
                                transform.translationZ, transform.rotationX, transform.rotationY,
                                transform.rotationZ, transform.scaleX, transform.scaleY, transform.scaleZ,
                                transform.anchorX, transform.anchorY, transform.anchorZ));
                    }
                }
            }
        }
        output.append(System.lineSeparator());
        return output.toString();
    }

    public record EditorKey(Identifier itemId, CameraType cameraType, ItemDisplayContext displayContext,
                            InteractionHand hand) {
    }

    public static final class Session {
        private final List<Target> targets;
        private int targetIndex;

        private Session(List<Target> targets) {
            this.targets = targets;
        }

        public List<Target> targets() {
            return targets;
        }

        public Target currentTarget() {
            return targets.get(targetIndex);
        }

        public int targetNumber() {
            return targetIndex + 1;
        }

        public void cycleTarget(int direction) {
            targetIndex = Math.floorMod(targetIndex + direction, targets.size());
        }
    }

    public static final class Target {
        private final EditorKey key;
        private final Component itemName;
        private final List<String> poses;
        private final Map<String, RelicAnimation> animations;
        private final Map<PoseChannel, Integer> keyframeIndices = new HashMap<>();
        private final Map<String, Integer> channelIndices = new HashMap<>();
        private final Map<String, Float> playheads = new HashMap<>();
        private int poseIndex;
        private boolean playing;

        private Target(EditorKey key, Component itemName,
                       List<String> poses, Map<String, RelicAnimation> animations) {
            this.key = key;
            this.itemName = itemName;
            this.poses = poses;
            this.animations = animations;
            for (String pose : poses) {
                RelicAnimation animation = animations.get(pose);
                channelIndices.put(pose, 0);
                for (RelicAnimation.Channel channel : animation.channels()) {
                    RelicAnimationClip clip = animation.channel(channel);
                    keyframeIndices.put(new PoseChannel(pose, channel), clip.keyframes().size() - 1);
                }
                playheads.put(pose, animation.channel(animation.channels().getFirst()).durationTicks());
            }
        }

        public EditorKey key() {
            return key;
        }

        public Component itemName() {
            return itemName;
        }

        public List<String> poses() {
            return poses;
        }

        public Map<String, RelicAnimation> animations() {
            return animations;
        }

        public String currentPose() {
            return poses.get(poseIndex);
        }

        public RelicAnimation currentAnimationDefinition() {
            return animations.get(currentPose());
        }

        public List<RelicAnimation.Channel> currentChannels() {
            return currentAnimationDefinition().channels();
        }

        public RelicAnimation.Channel currentChannel() {
            List<RelicAnimation.Channel> channels = currentChannels();
            int index = Math.floorMod(channelIndices.getOrDefault(currentPose(), 0), channels.size());
            return channels.get(index);
        }

        public RelicAnimationClip currentAnimation() {
            return currentAnimationDefinition().channel(currentChannel());
        }

        public RelicTransform currentTransform() {
            return currentAnimation().keyframes().get(currentKeyframeIndex()).transform();
        }

        public RelicTransform previewTransform() {
            return currentAnimation().sample(playhead());
        }

        public RelicTransform previewTransform(RelicAnimation.Channel channel) {
            RelicAnimationClip animation = currentAnimationDefinition().channel(channel);
            return animation == null ? null : animation.sample(playhead());
        }

        public int poseNumber() {
            return poseIndex + 1;
        }

        public void cyclePose(int direction) {
            poseIndex = Math.floorMod(poseIndex + direction, poses.size());
            playing = false;
        }

        public void cycleChannel(int direction) {
            List<RelicAnimation.Channel> channels = currentChannels();
            int index = Math.floorMod(channelIndices.getOrDefault(currentPose(), 0) + direction,
                    channels.size());
            channelIndices.put(currentPose(), index);
            playheads.put(currentPose(), Math.min(playhead(), currentAnimation().durationTicks()));
            playing = false;
        }

        public int currentKeyframeIndex() {
            return keyframeIndices.getOrDefault(new PoseChannel(currentPose(), currentChannel()), 0);
        }

        public int keyframeNumber() {
            return currentKeyframeIndex() + 1;
        }

        public float currentKeyframeTick() {
            return currentAnimation().keyframes().get(currentKeyframeIndex()).tick();
        }

        public float playhead() {
            return playheads.getOrDefault(currentPose(), 0.0F);
        }

        public float playheadProgress() {
            return playhead() / currentAnimation().durationTicks();
        }

        public void setPlayhead(float progress) {
            playheads.put(currentPose(),
                    Mth.clamp(progress, 0.0F, 1.0F) * currentAnimation().durationTicks());
            playing = false;
        }

        public void cycleKeyframe(int direction) {
            int count = currentAnimation().keyframes().size();
            int index = Math.floorMod(currentKeyframeIndex() + direction, count);
            keyframeIndices.put(new PoseChannel(currentPose(), currentChannel()), index);
            playheads.put(currentPose(), currentAnimation().keyframes().get(index).tick());
            playing = false;
        }

        public void addKeyframe() {
            RelicTransform transform = previewTransform().copy();
            currentAnimation().keyframe(playhead(), transform);
            keyframeIndices.put(new PoseChannel(currentPose(), currentChannel()),
                    currentAnimation().closestKeyframe(playhead()));
            playing = false;
        }

        public void removeKeyframe() {
            int index = currentKeyframeIndex();
            currentAnimation().removeKeyframe(index);
            int newIndex = Math.min(index, currentAnimation().keyframes().size() - 1);
            keyframeIndices.put(new PoseChannel(currentPose(), currentChannel()), newIndex);
            playheads.put(currentPose(), currentAnimation().keyframes().get(newIndex).tick());
            playing = false;
        }

        public void cycleEasing() {
            int index = currentKeyframeIndex();
            RelicAnimationClip.Keyframe keyframe = currentAnimation().keyframes().get(index);
            RelicAnimationClip.Easing[] values = RelicAnimationClip.Easing.values();
            RelicAnimationClip.Easing easing = values[(keyframe.easing().ordinal() + 1) % values.length];
            currentAnimation().setEasing(index, easing);
        }

        public RelicAnimationClip.Easing currentEasing() {
            return currentAnimation().keyframes().get(currentKeyframeIndex()).easing();
        }

        public void togglePlaying() {
            playing = !playing;
            if (playing && playhead() >= currentAnimation().durationTicks()) {
                playheads.put(currentPose(), 0.0F);
            }
        }

        public boolean playing() {
            return playing;
        }

        public void tick() {
            if (!playing) {
                return;
            }
            float tick = playhead() + 1.0F;
            if (tick > currentAnimation().durationTicks()) {
                tick = 0.0F;
            }
            playheads.put(currentPose(), tick);
        }

        private record PoseChannel(String pose, RelicAnimation.Channel channel) {
        }
    }

    private static boolean isLocalPlayerState(ArmedEntityRenderState state) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && state instanceof AvatarRenderState avatarRenderState
                && avatarRenderState.id == minecraft.player.getId();
    }

    private static int chargeLevelForPose(String pose) {
        if (pose.equals("spin")) {
            return IwatoshiItem.getMaxChargeLevel();
        }
        if (!pose.startsWith("slash_")) {
            return 0;
        }
        try {
            return Mth.clamp(Integer.parseInt(pose.substring("slash_".length())),
                    1, IwatoshiItem.getMaxChargeLevel() - 1);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int applyRuntimeAnimations(Session session) {
        Set<RuntimeAnimationKey> appliedAnimations = new HashSet<>();
        int appliedCount = 0;
        for (Target target : session.targets()) {
            for (String pose : target.poses()) {
                String animationId = RelicAnimator.getEditorAnimationId(
                        target.key().itemId(), target.key().cameraType(), pose);
                if (animationId == null) {
                    continue;
                }
                RuntimeAnimationKey animationKey = new RuntimeAnimationKey(
                        target.key().itemId(), animationId);
                if (!appliedAnimations.add(animationKey)) {
                    continue;
                }
                if (RelicAnimator.applyEditorAnimation(target.key().itemId(),
                        target.key().cameraType(), pose, target.animations().get(pose))) {
                    appliedCount++;
                }
            }
        }
        return appliedCount;
    }

    private record RenderedItem(Identifier itemId, ItemDisplayContext displayContext, int ownerId) {
    }

    private record RuntimeAnimationKey(Identifier itemId, String animationId) {
    }

    public record SaveResult(boolean success, Path path, int animationCount, int modelTransformCount) {
    }
}
