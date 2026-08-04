package net.turtleboi.noblephantasms.client;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.turtleboi.noblephantasms.client.gui.ItemPoseEditorScreen;
import net.turtleboi.noblephantasms.item.custom.BertilakItem;
import net.turtleboi.noblephantasms.item.custom.GungnirItem;
import net.turtleboi.noblephantasms.item.custom.RhongomyniadItem;
import org.joml.Quaternionf;

public final class ItemPoseEditor {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
    private static final Map<ItemStackRenderState, RenderedItem> RENDERED_ITEMS = new WeakHashMap<>();
    private static final Map<EditorKey, Map<String, Transform>> EDITED_TRANSFORMS = new HashMap<>();
    private static final ThreadLocal<Target> MODEL_TARGET = new ThreadLocal<>();
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
        Map<String, Transform> transforms = EDITED_TRANSFORMS.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
        poses.forEach(pose -> transforms.computeIfAbsent(pose,
                ignored -> initialTransform(stack, cameraType, pose)));
        targets.add(new Target(key, stack.getHoverName(), poses, transforms));
    }

    private static Transform initialTransform(ItemStack stack, CameraType cameraType, String pose) {
        if (cameraType.isFirstPerson() && stack.getItem() instanceof GungnirItem) {
            if (pose.equals("stab")) {
                return Transform.poseStack(0.0F, GungnirExtensions.STAB_TRANSLATION_Y, 0.0F,
                        GungnirExtensions.STAB_ROTATION_X, 0.0F, GungnirExtensions.STAB_ROTATION_Z);
            }
            if (pose.equals("throw")) {
                return Transform.poseStack(GungnirExtensions.THROW_TRANSLATION_X,
                        GungnirExtensions.THROW_TRANSLATION_Y, GungnirExtensions.THROW_TRANSLATION_Z,
                        GungnirExtensions.THROW_ROTATION_X, GungnirExtensions.THROW_ROTATION_Y,
                        GungnirExtensions.THROW_ROTATION_Z);
            }
        }
        if (stack.getItem() instanceof BertilakItem && pose.equals("covenant")) {
            if (cameraType.isFirstPerson()) {
                return Transform.poseStack(BertilakExtensions.TARGETING_TRANSLATION_X,
                        BertilakExtensions.TARGETING_TRANSLATION_Y, BertilakExtensions.TARGETING_TRANSLATION_Z,
                        BertilakExtensions.TARGETING_ROTATION_X, BertilakExtensions.TARGETING_ROTATION_Y,
                        BertilakExtensions.TARGETING_ROTATION_Z);
            }
            return Transform.poseStack(BertilakExtensions.THIRD_PERSON_TRANSLATION_X,
                    BertilakExtensions.THIRD_PERSON_TRANSLATION_Y,
                    BertilakExtensions.THIRD_PERSON_TRANSLATION_Z,
                    BertilakExtensions.THIRD_PERSON_ROTATION_X,
                    BertilakExtensions.THIRD_PERSON_ROTATION_Y,
                    BertilakExtensions.THIRD_PERSON_ROTATION_Z);
        }
        return Transform.modelDisplay();
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
        List<String> poses = new ArrayList<>();
        poses.add("held");
        if (stack.getItem() instanceof RhongomyniadItem) {
            poses.add("stab");
            poses.add("joust_raised");
            poses.add("joust_lowered");
            return List.copyOf(poses);
        }
        if (stack.getItem() instanceof GungnirItem) {
            poses.add("stab");
            poses.add("throw");
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
        if (target == null || renderedItem == null || minecraft.player == null
                || renderedItem.ownerId() != minecraft.player.getId()
                || !renderedItem.itemId().equals(target.key().itemId())
                || renderedItem.displayContext() != displayContext
                || target.key().displayContext() != displayContext) {
            MODEL_TARGET.remove();
            return;
        }
        MODEL_TARGET.set(target);
    }

    public static boolean applyModelTransform(ItemTransform source, boolean applyLeftHandFix, PoseStack.Pose pose) {
        Target target = MODEL_TARGET.get();
        if (target == null || !target.currentTransform().usesModelDisplay()) {
            return false;
        }

        Transform transform = target.currentTransform();
        transform.initialize(source.translation().x() * 16.0F, source.translation().y() * 16.0F,
                source.translation().z() * 16.0F, source.rotation().x(), source.rotation().y(),
                source.rotation().z(), source.scale().x(), source.scale().y(), source.scale().z());

        float translationX = transform.translationX / 16.0F;
        float rotationY = transform.rotationY;
        float rotationZ = transform.rotationZ;
        if (applyLeftHandFix) {
            translationX = -translationX;
            rotationY = -rotationY;
            rotationZ = -rotationZ;
        }
        pose.translate(translationX, transform.translationY / 16.0F, transform.translationZ / 16.0F);
        pose.rotate(new Quaternionf().rotationXYZ(
                transform.rotationX * ((float) Math.PI / 180.0F),
                rotationY * ((float) Math.PI / 180.0F),
                rotationZ * ((float) Math.PI / 180.0F)));
        pose.scale(transform.scaleX, transform.scaleY, transform.scaleZ);
        RhongomyniadSpinState.apply(pose);
        pose.rotate(TransformationHelper.quatFromXYZ(source.rightRotation().x(),
                source.rightRotation().y() * (applyLeftHandFix ? -1 : 1),
                source.rightRotation().z() * (applyLeftHandFix ? -1 : 1), true));
        pose.translate(-0.5F, -0.5F, -0.5F);
        return true;
    }

    public static void endModelTransform() {
        MODEL_TARGET.remove();
    }

    public static void applyFirstPersonAttackPreview(PoseStack poseStack, InteractionHand hand,
                                                     ItemStack stack, HumanoidArm arm) {
        Target target = matchingFirstPersonTarget(hand, stack);
        if (target == null || !isAttackPose(target.currentPose())) {
            return;
        }

        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        SpearAnimations.firstPersonAttack(0.2F, poseStack, direction, arm);
        if (stack.getItem() instanceof RhongomyniadItem) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(direction * 15.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
            poseStack.translate(0.0F, 0.15F, 0.0F);
            return;
        }
        if (!(stack.getItem() instanceof GungnirItem)) {
            return;
        }

        Transform transform = target.currentTransform();
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * transform.rotationZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(transform.rotationX));
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * transform.rotationY));
        poseStack.translate(direction * transform.translationX, transform.translationY, transform.translationZ);
        poseStack.scale(transform.scaleX, transform.scaleY, transform.scaleZ);
    }

    public static void applyFirstPersonUsePreview(PoseStack poseStack, InteractionHand hand,
                                                  ItemStack stack, HumanoidArm arm) {
        Target target = matchingFirstPersonTarget(hand, stack);
        if (target == null || !isUsePose(target.currentPose())) {
            return;
        }

        if (stack.getItem() instanceof GungnirItem) {
            GungnirExtensions.applyEditorThrowTransform(poseStack, arm, target.currentTransform());
            return;
        }
        if (stack.getItem() instanceof BertilakItem) {
            BertilakExtensions.applyEditorCovenantTransform(poseStack, arm, target.currentTransform());
            return;
        }

        float timeHeld = getPreviewUseTime(stack, target.currentPose());
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
            state.attackTime = 0.2F;
            state.swingAnimationType = SwingAnimationType.STAB;
            setArmPose(state, arm, HumanoidModel.ArmPose.SPEAR);
        } else if (isUsePose(pose)) {
            state.isUsingItem = true;
            state.useItemHand = target.key().hand();
            state.ticksUsingItem = getPreviewUseTime(stack, pose);
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

    public static Transform getThirdPersonTransform(ItemStack stack, String pose) {
        Target target = currentTarget();
        if (target == null || target.key().cameraType().isFirstPerson()
                || !target.currentPose().equals(pose) || !sameItem(target, stack)) {
            return null;
        }
        return target.currentTransform();
    }

    private static float getPreviewUseTime(ItemStack stack, String pose) {
        if (stack.getItem() instanceof RhongomyniadItem) {
            if (pose.equals("joust_raised")) {
                return RhongomyniadItem.getChargeStartTick(stack);
            }
            KineticWeapon kineticWeapon = stack.get(DataComponents.KINETIC_WEAPON);
            if (kineticWeapon != null) {
                int finishLoweringTick = kineticWeapon.delayTicks()
                        + kineticWeapon.knockbackConditions()
                        .map(KineticWeapon.Condition::maxDurationTicks).orElse(0);
                return finishLoweringTick + 20.0F;
            }
        }
        if (stack.getItem() instanceof GungnirItem) {
            return GungnirItem.THROW_THRESHOLD_TIME;
        }
        if (stack.getItem() instanceof BertilakItem) {
            return 40.0F;
        }
        return 20.0F;
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
        return pose.equals("stab") || pose.equals("attack");
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
        String export = createExport(session);
        Path logPath = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("logs").resolve("noblephantasms-item-poses.log");
        try {
            Files.createDirectories(logPath.getParent());
            Files.writeString(logPath, export, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Minecraft.getInstance().keyboardHandler.setClipboard(export.strip());
            NoblePhantasms.LOGGER.info("Saved item pose export to {}", logPath.toAbsolutePath());
            return new SaveResult(true, logPath.toAbsolutePath());
        } catch (IOException exception) {
            NoblePhantasms.LOGGER.error("Unable to save item pose export", exception);
            return new SaveResult(false, logPath.toAbsolutePath());
        }
    }

    private static String createExport(Session session) {
        StringBuilder output = new StringBuilder();
        output.append("# Noble Phantasms item pose export ")
                .append(TIMESTAMP.format(LocalDateTime.now())).append(System.lineSeparator());
        for (Target target : session.targets()) {
            for (String pose : target.poses()) {
                Transform transform = target.transforms().get(pose);
                output.append(String.format(Locale.ROOT,
                        "item=%s, pov=%s, display_context=%s, hand=%s, pose=%s, transform_space=%s, "
                                + "translation=(%.4f, %.4f, %.4f), rotation=(%.2f, %.2f, %.2f), "
                                + "scale=(%.4f, %.4f, %.4f)%n",
                        target.key().itemId(), target.key().cameraType(), target.key().displayContext(),
                        target.key().hand(), pose, transform.spaceName(),
                        transform.translationX, transform.translationY, transform.translationZ,
                        transform.rotationX, transform.rotationY, transform.rotationZ,
                        transform.scaleX, transform.scaleY, transform.scaleZ));
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
        private final Map<String, Transform> transforms;
        private int poseIndex;

        private Target(EditorKey key, Component itemName,
                       List<String> poses, Map<String, Transform> transforms) {
            this.key = key;
            this.itemName = itemName;
            this.poses = poses;
            this.transforms = transforms;
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

        public Map<String, Transform> transforms() {
            return transforms;
        }

        public String currentPose() {
            return poses.get(poseIndex);
        }

        public Transform currentTransform() {
            return transforms.get(currentPose());
        }

        public int poseNumber() {
            return poseIndex + 1;
        }

        public void cyclePose(int direction) {
            poseIndex = Math.floorMod(poseIndex + direction, poses.size());
        }
    }

    public static final class Transform {
        private final boolean modelDisplay;
        private boolean initialized;
        private float initialTranslationX;
        private float initialTranslationY;
        private float initialTranslationZ;
        private float initialRotationX;
        private float initialRotationY;
        private float initialRotationZ;
        private float initialScaleX = 1.0F;
        private float initialScaleY = 1.0F;
        private float initialScaleZ = 1.0F;
        public float translationX;
        public float translationY;
        public float translationZ;
        public float rotationX;
        public float rotationY;
        public float rotationZ;
        public float scaleX = 1.0F;
        public float scaleY = 1.0F;
        public float scaleZ = 1.0F;

        private Transform(boolean modelDisplay) {
            this.modelDisplay = modelDisplay;
        }

        private static Transform modelDisplay() {
            return new Transform(true);
        }

        private static Transform poseStack(float translationX, float translationY, float translationZ,
                                           float rotationX, float rotationY, float rotationZ) {
            Transform transform = new Transform(false);
            transform.initialize(translationX, translationY, translationZ,
                    rotationX, rotationY, rotationZ, 1.0F, 1.0F, 1.0F);
            return transform;
        }

        private void initialize(float translationX, float translationY, float translationZ,
                                float rotationX, float rotationY, float rotationZ,
                                float scaleX, float scaleY, float scaleZ) {
            if (initialized) {
                return;
            }
            this.translationX = initialTranslationX = translationX;
            this.translationY = initialTranslationY = translationY;
            this.translationZ = initialTranslationZ = translationZ;
            this.rotationX = initialRotationX = rotationX;
            this.rotationY = initialRotationY = rotationY;
            this.rotationZ = initialRotationZ = rotationZ;
            this.scaleX = initialScaleX = scaleX;
            this.scaleY = initialScaleY = scaleY;
            this.scaleZ = initialScaleZ = scaleZ;
            initialized = true;
        }

        public boolean initialized() {
            return initialized;
        }

        private boolean usesModelDisplay() {
            return modelDisplay;
        }

        private String spaceName() {
            return modelDisplay ? "model_display" : "pose_stack";
        }

        public void reset() {
            translationX = initialTranslationX;
            translationY = initialTranslationY;
            translationZ = initialTranslationZ;
            rotationX = initialRotationX;
            rotationY = initialRotationY;
            rotationZ = initialRotationZ;
            scaleX = initialScaleX;
            scaleY = initialScaleY;
            scaleZ = initialScaleZ;
        }
    }

    private record RenderedItem(Identifier itemId, ItemDisplayContext displayContext, int ownerId) {
    }

    public record SaveResult(boolean success, Path path) {
    }
}
