package net.turtleboi.noblephantasms.client.ui;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.turtleboi.noblephantasms.client.ItemPoseEditor.SaveResult;
import net.turtleboi.noblephantasms.client.ItemPoseEditor.Session;
import net.turtleboi.noblephantasms.client.ItemPoseEditor.Target;
import net.turtleboi.noblephantasms.client.ItemPoseEditor.Transform;

public class ItemPoseEditor extends Screen {
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 130;
    private static final int SLIDER_HEIGHT = 14;
    private final Session session;
    private String status = "Alt + arrows: 0.01";
    private int panelX;
    private int panelY;
    private boolean waitingForTransform;

    public ItemPoseEditor(Session session) {
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

        Transform transform = target.currentTransform();
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

        addSlider(panelX + margin, gridY + 16, sliderWidth, "RX", transform.rotationX, -180.0F, 180.0F,
                1.0F, value -> transform.rotationX = (float) value);
        addSlider(panelX + margin + sliderWidth + gap, gridY + 16, sliderWidth, "RY", transform.rotationY, -180.0F, 180.0F,
                1.0F, value -> transform.rotationY = (float) value);
        addSlider(panelX + margin + (sliderWidth + gap) * 2, gridY + 16, sliderWidth, "RZ", transform.rotationZ, -180.0F, 180.0F,
                1.0F, value -> transform.rotationZ = (float) value);

        addSlider(panelX + margin, gridY + 32, sliderWidth, "SX", transform.scaleX, 0.1F, 4.0F,
                0.01F, value -> transform.scaleX = (float) value);
        addSlider(panelX + margin + sliderWidth + gap, gridY + 32, sliderWidth, "SY", transform.scaleY, 0.1F, 4.0F,
                0.01F, value -> transform.scaleY = (float) value);
        addSlider(panelX + margin + (sliderWidth + gap) * 2, gridY + 32, sliderWidth, "SZ", transform.scaleZ, 0.1F, 4.0F,
                0.01F, value -> transform.scaleZ = (float) value);

        int buttonY = panelY + 94;
        int buttonWidth = (panelWidth - margin * 2 - gap * 2) / 3;
        addRenderableWidget(Button.builder(Component.literal("Reset"), button -> resetPose())
                .bounds(panelX + margin, buttonY, buttonWidth, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
                .bounds(panelX + margin + buttonWidth + gap, buttonY, buttonWidth, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(panelX + margin + (buttonWidth + gap) * 2, buttonY, buttonWidth, 16).build());
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
        status = "Alt + arrows: 0.01";
        rebuildWidgets();
    }

    private void cyclePose(int direction) {
        session.currentTarget().cyclePose(direction);
        status = "Alt + arrows: 0.01";
        rebuildWidgets();
    }

    private void resetPose() {
        session.currentTarget().currentTransform().reset();
        status = "Reset";
        rebuildWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        if (waitingForTransform && session.currentTarget().currentTransform().initialized()) {
            status = "Alt + arrows: 0.01";
            rebuildWidgets();
        }
    }

    private void save() {
        SaveResult result = net.turtleboi.noblephantasms.client.ItemPoseEditor.save(session);
        status = result.success() ? "Saved + copied" : "Save failed";
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
        graphics.centeredText(font, status, panelX + panelWidth / 2, panelY + 116, 0xFFAAAAAA);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        net.turtleboi.noblephantasms.client.ItemPoseEditor.close(session);
    }

    private static final class PoseSlider extends AbstractSliderButton {
        private final String label;
        private final double minimum;
        private final double maximum;
        private final double step;
        private final DoubleConsumer setter;

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
            if (canChangeValue && (event.isLeft() || event.isRight())) {
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
        protected void updateMessage() {
            setMessage(Component.literal(label + " " + String.format(Locale.ROOT, "%.2f", currentValue())));
        }

        @Override
        protected void applyValue() {
            setter.accept(currentValue());
        }

        private double currentValue() {
            return minimum + value * (maximum - minimum);
        }

        private static double normalize(double value, double minimum, double maximum) {
            return (value - minimum) / (maximum - minimum);
        }
    }
}
