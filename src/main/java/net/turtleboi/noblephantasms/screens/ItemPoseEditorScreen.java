package net.turtleboi.noblephantasms.screens;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor.SaveResult;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor.Session;
import net.turtleboi.noblephantasms.client.animation.ItemPoseEditor.Target;
import net.turtleboi.noblephantasms.client.animation.RelicTransform;

public class ItemPoseEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 208;
    private static final int PANEL_HEIGHT = 234;
    private static final int SLIDER_HEIGHT = 14;
    private static final String EDIT_HINT = "Select/type + Enter · Alt: 0.01";
    private final Session session;
    private String status = EDIT_HINT;
    private int panelX;
    private int panelY;
    private boolean waitingForTransform;
    private PoseSlider timelineSlider;

    public ItemPoseEditorScreen(Session session) {
        super(Component.literal("Item Pose Editor"));
        this.session = session;
    }

    @Override
    protected void init() {
        int panelWidth = panelWidth();
        panelX = panelOnLeft() ? 6 : width - panelWidth - 6;
        panelY = Math.max(6, (height - PANEL_HEIGHT) / 2);
        Target target = session.currentTarget();

        if (session.targets().size() > 1) {
            addRenderableWidget(smallArrow(panelX + 5, panelY + 4, "<", () -> cycleTarget(-1)));
            addRenderableWidget(smallArrow(panelX + panelWidth - 19, panelY + 4, ">", () -> cycleTarget(1)));
        }
        if (target.poses().size() > 1) {
            addRenderableWidget(smallArrow(panelX + 5, panelY + 23, "<", () -> cyclePose(-1)));
            addRenderableWidget(smallArrow(panelX + panelWidth - 19, panelY + 23, ">", () -> cyclePose(1)));
        }

        RelicTransform transform = target.currentTransform();
        waitingForTransform = !transform.initialized();
        int margin = 6;
        int gap = 2;
        int sliderWidth = (panelWidth - margin * 2 - gap * 2) / 3;
        int gridY = panelY + 43;
        addSlider(panelX + margin, gridY, sliderWidth, "PX", transform.translationX, -16.0F, 16.0F,
                0.01F, value -> transform.translationX = (float) value);
        addSlider(panelX + margin + sliderWidth + gap, gridY, sliderWidth, "PY", transform.translationY, -16.0F, 16.0F,
                0.01F, value -> transform.translationY = (float) value);
        addSlider(panelX + margin + (sliderWidth + gap) * 2, gridY, sliderWidth, "PZ", transform.translationZ, -16.0F, 16.0F,
                0.01F, value -> transform.translationZ = (float) value);

        addSlider(panelX + margin, gridY + 16, sliderWidth, "RX", transform.rotationX, -720.0F, 720.0F,
                1.0F, value -> transform.rotationX = (float) value);
        addSlider(panelX + margin + sliderWidth + gap, gridY + 16, sliderWidth, "RY", transform.rotationY, -720.0F, 720.0F,
                1.0F, value -> transform.rotationY = (float) value);
        addSlider(panelX + margin + (sliderWidth + gap) * 2, gridY + 16, sliderWidth, "RZ", transform.rotationZ, -720.0F, 720.0F,
                1.0F, value -> transform.rotationZ = (float) value);

        addSlider(panelX + margin, gridY + 32, sliderWidth, "SX", transform.scaleX, 0.1F, 4.0F,
                0.01F, value -> transform.scaleX = (float) value);
        addSlider(panelX + margin + sliderWidth + gap, gridY + 32, sliderWidth, "SY", transform.scaleY, 0.1F, 4.0F,
                0.01F, value -> transform.scaleY = (float) value);
        addSlider(panelX + margin + (sliderWidth + gap) * 2, gridY + 32, sliderWidth, "SZ", transform.scaleZ, 0.1F, 4.0F,
                0.01F, value -> transform.scaleZ = (float) value);

        float anchorMinimum = transform.usesModelDisplay() ? -1.0F : -2.0F;
        float anchorMaximum = transform.usesModelDisplay() ? 2.0F : 2.0F;
        addSlider(panelX + margin, gridY + 48, sliderWidth, "AX", transform.anchorX,
                anchorMinimum, anchorMaximum, 0.01F, value -> transform.anchorX = (float) value);
        addSlider(panelX + margin + sliderWidth + gap, gridY + 48, sliderWidth, "AY", transform.anchorY,
                anchorMinimum, anchorMaximum, 0.01F, value -> transform.anchorY = (float) value);
        addSlider(panelX + margin + (sliderWidth + gap) * 2, gridY + 48, sliderWidth, "AZ", transform.anchorZ,
                anchorMinimum, anchorMaximum, 0.01F, value -> transform.anchorZ = (float) value);

        int timelineY = panelY + 109;
        timelineSlider = new PoseSlider(panelX + margin, timelineY, panelWidth - margin * 2,
                "Tick", target.playhead(), 0.0F, target.currentAnimation().durationTicks(), 1.0F,
                value -> target.setPlayhead((float) value / target.currentAnimation().durationTicks()));
        addRenderableWidget(timelineSlider);

        int keyframeY = panelY + 127;
        int smallButtonWidth = (panelWidth - margin * 2 - gap * 3) / 4;
        addRenderableWidget(Button.builder(Component.literal("< Key"), button -> cycleKeyframe(-1))
                .bounds(panelX + margin, keyframeY, smallButtonWidth, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+ Key"), button -> addKeyframe())
                .bounds(panelX + margin + smallButtonWidth + gap, keyframeY, smallButtonWidth, 16).build());
        addRenderableWidget(Button.builder(Component.literal("- Key"), button -> removeKeyframe())
                .bounds(panelX + margin + (smallButtonWidth + gap) * 2, keyframeY, smallButtonWidth, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Key >"), button -> cycleKeyframe(1))
                .bounds(panelX + margin + (smallButtonWidth + gap) * 3, keyframeY, smallButtonWidth, 16).build());

        int toolY = panelY + 145;
        int toolButtonWidth = (panelWidth - margin * 2 - gap * 2) / 3;
        addRenderableWidget(Button.builder(Component.literal(target.playing() ? "Pause" : "Play"),
                        button -> togglePlayback())
                .bounds(panelX + margin, toolY, toolButtonWidth, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Ease"), button -> cycleEasing())
                .bounds(panelX + margin + toolButtonWidth + gap, toolY, toolButtonWidth, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), button -> resetPose())
                .bounds(panelX + margin + (toolButtonWidth + gap) * 2, toolY, toolButtonWidth, 16).build());

        int finishY = panelY + 163;
        int finishButtonWidth = (panelWidth - margin * 2 - gap) / 2;
        addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
                .bounds(panelX + margin, finishY, finishButtonWidth, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(panelX + margin + finishButtonWidth + gap, finishY, finishButtonWidth, 16).build());

        if (target.currentChannels().size() > 1) {
            int channelY = panelY + 181;
            addRenderableWidget(Button.builder(Component.literal("< Channel"), button -> cycleChannel(-1))
                    .bounds(panelX + margin, channelY, finishButtonWidth, 16).build());
            addRenderableWidget(Button.builder(Component.literal("Channel >"), button -> cycleChannel(1))
                    .bounds(panelX + margin + finishButtonWidth + gap, channelY, finishButtonWidth, 16).build());
        }
    }

    private Button smallArrow(int x, int y, String label, Runnable action) {
        return Button.builder(Component.literal(label), button -> action.run()).bounds(x, y, 14, 14).build();
    }

    private void addSlider(int x, int y, int width, String label, float current, float minimum, float maximum,
                           float step, DoubleConsumer setter) {
        addRenderableWidget(new PoseSlider(x, y, width, label, current, minimum, maximum, step, setter));
    }

    private void cycleTarget(int direction) {
        session.cycleTarget(direction);
        status = EDIT_HINT;
        rebuildWidgets();
    }

    private void cyclePose(int direction) {
        session.currentTarget().cyclePose(direction);
        status = EDIT_HINT;
        rebuildWidgets();
    }

    private void resetPose() {
        session.currentTarget().currentTransform().reset();
        status = "Reset";
        rebuildWidgets();
    }

    private void cycleKeyframe(int direction) {
        session.currentTarget().cycleKeyframe(direction);
        status = "Selected keyframe";
        rebuildWidgets();
    }

    private void cycleChannel(int direction) {
        session.currentTarget().cycleChannel(direction);
        status = "Editing " + session.currentTarget().currentChannel();
        rebuildWidgets();
    }

    private void addKeyframe() {
        session.currentTarget().addKeyframe();
        status = "Keyframe added";
        rebuildWidgets();
    }

    private void removeKeyframe() {
        session.currentTarget().removeKeyframe();
        status = "Keyframe removed";
        rebuildWidgets();
    }

    private void togglePlayback() {
        session.currentTarget().togglePlaying();
        status = session.currentTarget().playing() ? "Playing" : "Paused";
        rebuildWidgets();
    }

    private void cycleEasing() {
        session.currentTarget().cycleEasing();
        status = "Ease: " + session.currentTarget().currentEasing();
        rebuildWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        Target target = session.currentTarget();
        target.tick();
        if (timelineSlider != null) {
            timelineSlider.setCurrent(target.playhead());
        }
        if (waitingForTransform && session.currentTarget().currentTransform().initialized()) {
            status = EDIT_HINT;
            rebuildWidgets();
        }
    }

    private void save() {
        SaveResult result = ItemPoseEditor.save(session);
        status = result.success()
                ? "Saved " + result.animationCount() + " anim · "
                + result.modelTransformCount() + " model"
                : "Save failed";
    }

    private int panelWidth() {
        return Math.min(PANEL_WIDTH, width - 12);
    }

    private boolean panelOnLeft() {
        Target target = session.currentTarget();
        boolean leftHand = target.key().displayContext().leftHand();
        CameraType cameraType = target.key().cameraType();
        boolean itemOnRight = cameraType == CameraType.THIRD_PERSON_FRONT ? leftHand : !leftHand;
        return itemOnRight;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() && getFocused() instanceof PoseSlider slider
                && slider.cancelTypedValue()) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = panelWidth();
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + PANEL_HEIGHT, 0xB0101010);
        graphics.outline(panelX, panelY, panelWidth, PANEL_HEIGHT, 0x805F5F5F);
        Target target = session.currentTarget();
        String hand = target.key().hand() == net.minecraft.world.InteractionHand.MAIN_HAND ? "Main" : "Offhand";
        String itemLabel = hand + " · " + target.itemName().getString();
        itemLabel = font.plainSubstrByWidth(itemLabel, panelWidth - 44);
        graphics.centeredText(font, itemLabel, panelX + panelWidth / 2, panelY + 7, 0xFFFFFFFF);
        String poseLabel = target.currentPose() + "  " + target.poseNumber() + "/" + target.poses().size();
        graphics.centeredText(font, poseLabel, panelX + panelWidth / 2, panelY + 26, 0xFFD8D8D8);
        String keyframeLabel = target.currentChannel() + " · Key " + target.keyframeNumber() + "/"
                + target.currentAnimation().keyframes().size() + " @ "
                + String.format(Locale.ROOT, "%.0f", target.currentKeyframeTick())
                + " · " + target.currentEasing();
        graphics.centeredText(font, keyframeLabel, panelX + panelWidth / 2, panelY + 204, 0xFFD8D8D8);
        graphics.centeredText(font, status, panelX + panelWidth / 2, panelY + 219, 0xFFAAAAAA);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        ItemPoseEditor.close(session);
    }

    private static final class PoseSlider extends AbstractSliderButton {
        private final String label;
        private final double minimum;
        private final double maximum;
        private final double step;
        private final DoubleConsumer setter;
        private String typedValue = "";
        private boolean typing;

        private PoseSlider(int x, int y, int width, String label, double current,
                           double minimum, double maximum, double step, DoubleConsumer setter) {
            super(x, y, width, SLIDER_HEIGHT, Component.empty(), normalize(current, minimum, maximum));
            this.label = label;
            this.minimum = minimum;
            this.maximum = maximum;
            this.step = step;
            this.setter = setter;
            updateMessage();
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (typing) {
                if (event.isConfirmation()) {
                    commitTypedValue();
                    return true;
                }
                if (event.isEscape()) {
                    cancelTypedValue();
                    return true;
                }
                if (event.key() == 259) {
                    if (!typedValue.isEmpty()) {
                        typedValue = typedValue.substring(0, typedValue.length() - 1);
                    }
                    updateMessage();
                    return true;
                }
                if (event.isPaste()) {
                    setTypedValue(Minecraft.getInstance().keyboardHandler.getClipboard().strip());
                    return true;
                }
                if (event.isLeft() || event.isRight()) {
                    return true;
                }
            }
            if (event.isLeft() || event.isRight()) {
                double precision = event.hasAltDown() ? 0.01 : event.hasShiftDown() ? step * 0.1 : step;
                double direction = event.isLeft() ? -1.0 : 1.0;
                value = Mth.clamp(value + direction * precision / (maximum - minimum), 0.0, 1.0);
                applyValue();
                updateMessage();
                return true;
            }
            return super.keyPressed(event);
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            int codepoint = event.codepoint();
            if (!isFocused() || !isNumberCharacter(codepoint)) {
                return false;
            }
            if (!typing) {
                typedValue = "";
                typing = true;
            }
            if ((codepoint == '-' && !typedValue.isEmpty())
                    || (codepoint == '.' && typedValue.contains("."))) {
                return true;
            }
            typedValue += Character.toString(codepoint);
            updateMessage();
            return true;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (!isFocused()) {
                dragging = false;
                canChangeValue = false;
                return;
            }
            cancelTypedValue();
            canChangeValue = true;
            super.onClick(event, doubleClick);
        }

        @Override
        protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
            if (canChangeValue) {
                super.onDrag(event, dragX, dragY);
            }
        }

        @Override
        public void setFocused(boolean focused) {
            boolean wasFocused = isFocused();
            if (!focused && typing) {
                commitTypedValue();
            }
            super.setFocused(focused);
            if (!focused || !wasFocused) {
                canChangeValue = false;
                dragging = false;
            }
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            String displayedValue = typing
                    ? typedValue + "_" : String.format(Locale.ROOT, "%.2f", currentValue());
            setMessage(Component.literal(label + " " + displayedValue));
        }

        @Override
        protected void applyValue() {
            setter.accept(currentValue());
        }

        private double currentValue() {
            return minimum + value * (maximum - minimum);
        }

        private void setCurrent(double current) {
            value = Mth.clamp(normalize(current, minimum, maximum), 0.0, 1.0);
            updateMessage();
        }

        private void setTypedValue(String typedValue) {
            try {
                double typedNumber = Double.parseDouble(typedValue);
                if (!Double.isFinite(typedNumber)) {
                    return;
                }
                this.typedValue = typedValue;
                typing = true;
                updateMessage();
            } catch (NumberFormatException ignored) {
            }
        }

        private void commitTypedValue() {
            if (!typing) {
                return;
            }
            try {
                double typedNumber = Double.parseDouble(typedValue);
                if (Double.isFinite(typedNumber)) {
                    value = Mth.clamp(normalize(typedNumber, minimum, maximum), 0.0, 1.0);
                    applyValue();
                }
            } catch (NumberFormatException ignored) {
            }
            typing = false;
            typedValue = "";
            updateMessage();
        }

        private boolean cancelTypedValue() {
            boolean wasTyping = typing;
            typing = false;
            typedValue = "";
            updateMessage();
            return wasTyping;
        }

        private static boolean isNumberCharacter(int codepoint) {
            return (codepoint >= '0' && codepoint <= '9') || codepoint == '-' || codepoint == '.';
        }

        private static double normalize(double value, double minimum, double maximum) {
            return (value - minimum) / (maximum - minimum);
        }
    }
}
