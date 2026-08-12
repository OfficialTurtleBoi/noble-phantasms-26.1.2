package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.mixin.client.BufferSourceAccessor;
import org.jspecify.annotations.Nullable;

public final class ColoredGlintRenderer {
    public static final int ENCHANTMENT_GLINT_COLOR = 0xA755FF;
    private static final int TRANSITION_STEPS = 32;
    private static final Identifier VANILLA_GLINT = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png");
    private static final List<Registration> REGISTRATIONS = new ArrayList<>();
    private static final Map<Item, RegisteredStyle> ITEM_STYLES = new IdentityHashMap<>();
    private static final Map<ItemStackRenderState, GlintStyle> RENDER_STATES = new WeakHashMap<>();
    private static final Map<SubmitNodeStorage.ItemSubmit, GlintStyle> SUBMITS = new IdentityHashMap<>();
    private static final ThreadLocal<GlintStyle> SUBMITTING = new ThreadLocal<>();
    private static final ThreadLocal<GlintStyle> RENDERING = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SPECIAL_SUBMITTED = new ThreadLocal<>();

    public static void register(Item item, int color, Predicate<ItemStack> condition) {
        REGISTRATIONS.removeIf(registration -> registration.item() == item);
        int normalizedColor = color & 0xFFFFFF;
        REGISTRATIONS.add(new Registration(item, normalizedColor, normalizedColor, normalizedColor, stack -> condition.test(stack) ? 1.0 : -1.0, stack -> true, false));
    }

    public static void registerTransitioning(Item item, @Nullable Integer startColor, @Nullable Integer endColor, ToDoubleFunction<ItemStack> transitionProvider) {
        registerTransitioning(item, startColor, startColor, endColor, transitionProvider, stack -> true);
    }

    public static void registerTransitioningFromEnchantment(Item item, @Nullable Integer endColor, ToDoubleFunction<ItemStack> transitionProvider) {
        registerTransitioning(item, ENCHANTMENT_GLINT_COLOR, null, endColor, transitionProvider, ItemStack::isEnchanted);
    }

    private static void registerTransitioning(Item item, @Nullable Integer startColor, @Nullable Integer fallbackStartColor, @Nullable Integer endColor,
                                              ToDoubleFunction<ItemStack> transitionProvider, Predicate<ItemStack> usePrimaryStart) {
        REGISTRATIONS.removeIf(registration -> registration.item() == item);
        REGISTRATIONS.add(new Registration(item, normalizeColor(startColor), normalizeColor(fallbackStartColor), normalizeColor(endColor), transitionProvider,
                usePrimaryStart, true));
    }

    public static void initialize() {
        Map<StyleKey, GlintStyle> styles = new LinkedHashMap<>();
        ITEM_STYLES.clear();
        RENDER_STATES.clear();
        SUBMITS.clear();
        SUBMITTING.remove();
        RENDERING.remove();
        SPECIAL_SUBMITTED.remove();
        for (Registration registration : REGISTRATIONS) {
            int styleCount = registration.transitioning() ? TRANSITION_STEPS + 1 : 1;
            GlintStyle[] primaryStyles = new GlintStyle[styleCount];
            GlintStyle[] fallbackStyles = new GlintStyle[styleCount];
            for (int index = 0; index < styleCount; index++) {
                int step = registration.transitioning() ? index : TRANSITION_STEPS;
                StyleKey primaryKey = new StyleKey(registration.startColor(), registration.endColor(), step);
                primaryStyles[index] = styles.computeIfAbsent(primaryKey, ignored -> createStyle(registration.startColor(), registration.endColor(), step));
                StyleKey fallbackKey = new StyleKey(registration.fallbackStartColor(), registration.endColor(), step);
                fallbackStyles[index] = styles.computeIfAbsent(fallbackKey, ignored -> createStyle(registration.fallbackStartColor(), registration.endColor(), step));
            }
            ITEM_STYLES.put(registration.item(), new RegisteredStyle(primaryStyles, fallbackStyles, registration.transitionProvider(), registration.usePrimaryStart()));
        }
    }

    public static void track(ItemStackRenderState state, ItemStack stack) {
        RegisteredStyle registered = ITEM_STYLES.get(stack.getItem());
        double transition = registered == null ? -1.0 : registered.transitionProvider().applyAsDouble(stack);
        if (transition < 0.0) {
            RENDER_STATES.remove(state);
        } else {
            RENDER_STATES.put(state, registered.style(stack, transition));
        }
    }

    public static boolean hasColoredGlint(ItemStack stack) {
        RegisteredStyle registered = ITEM_STYLES.get(stack.getItem());
        return registered != null && registered.transitionProvider().applyAsDouble(stack) >= 0.0;
    }

    public static void beginSubmit(ItemStackRenderState state) {
        GlintStyle style = RENDER_STATES.get(state);
        if (style == null) {
            SUBMITTING.remove();
            SPECIAL_SUBMITTED.remove();
        } else {
            SUBMITTING.set(style);
            SPECIAL_SUBMITTED.set(false);
        }
    }

    public static void endSubmit() {
        SUBMITTING.remove();
        SPECIAL_SUBMITTED.remove();
    }

    public static boolean hasSubmittingStyle() {
        return SUBMITTING.get() != null;
    }

    public static boolean isVanillaGlint(RenderType renderType) {
        return renderType == RenderTypes.glint()
                || renderType == RenderTypes.entityGlint()
                || renderType == RenderTypes.glintTranslucent();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void submitSpecialModel(SubmitNodeCollector collector, Model model, Object state,
                                          PoseStack poseStack, int lightCoords) {
        GlintStyle style = takeSpecialStyle();
        if (style != null) {
            collector.submitModel(model, state, poseStack, style.entityGlint(), lightCoords,
                    OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, null, 0, null);
        }
    }

    public static void submitSpecialPart(SubmitNodeCollector collector, ModelPart modelPart,
                                         PoseStack poseStack, int lightCoords) {
        GlintStyle style = takeSpecialStyle();
        if (style != null) {
            collector.submitModelPart(modelPart, poseStack, style.entityGlint(), lightCoords,
                    OverlayTexture.NO_OVERLAY, null, false, false, 0xFFFFFFFF, null, 0);
        }
    }

    private static GlintStyle takeSpecialStyle() {
        GlintStyle style = SUBMITTING.get();
        if (style == null || Boolean.TRUE.equals(SPECIAL_SUBMITTED.get())) {
            return null;
        }
        SPECIAL_SUBMITTED.set(true);
        return style;
    }

    public static void capture(SubmitNodeStorage.ItemSubmit submit) {
        GlintStyle style = SUBMITTING.get();
        if (style != null) {
            SUBMITS.put(submit, style);
        }
    }

    public static void beginRender(SubmitNodeStorage.ItemSubmit submit) {
        GlintStyle style = SUBMITS.remove(submit);
        if (style == null) {
            RENDERING.remove();
        } else {
            RENDERING.set(style);
        }
    }

    public static void endRender() {
        RENDERING.remove();
    }

    public static VertexConsumer getFoilBuffer(MultiBufferSource bufferSource, RenderType baseRenderType,
                                               PoseStack.Pose foilDecalPose) {
        GlintStyle style = RENDERING.get();
        if (style == null) {
            return null;
        }
        RenderType renderType = Minecraft.useShaderTransparency()
                && baseRenderType.outputTarget() == OutputTarget.ITEM_ENTITY_TARGET
                ? style.translucentGlint()
                : style.glint();
        if (bufferSource instanceof MultiBufferSource.BufferSource source) {
            Map<RenderType, ByteBufferBuilder> fixedBuffers =
                    ((BufferSourceAccessor) source).noblePhantasms$getFixedBuffers();
            try {
                fixedBuffers.computeIfAbsent(renderType,
                        ignored -> new ByteBufferBuilder(renderType.bufferSize()));
            } catch (UnsupportedOperationException ignored) {
            }
        }
        VertexConsumer buffer = bufferSource.getBuffer(renderType);
        if (foilDecalPose != null) {
            buffer = new SheetedDecalTextureGenerator(buffer, foilDecalPose, 0.0078125F);
        }
        return buffer;
    }

    private static GlintStyle createStyle(@Nullable Integer startColor, @Nullable Integer endColor, int transitionStep) {
        String startName = startColor == null ? "none" : String.format("%06x", startColor);
        String endName = endColor == null ? "none" : String.format("%06x", endColor);
        String colorName = String.format("%s_%s_%02d", startName, endName, transitionStep);
        float transition = transitionStep / (float) TRANSITION_STEPS;
        Identifier texture = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "dynamic/colored_glint_" + colorName);
        Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(() -> "Colored glint " + colorName, createTexture(startColor, endColor, transition)));
        return new GlintStyle(
                createRenderType(texture, "colored_glint_" + colorName, TextureTransform.GLINT_TEXTURING, OutputTarget.MAIN_TARGET),
                createRenderType(texture, "colored_entity_glint_" + colorName, TextureTransform.ENTITY_GLINT_TEXTURING, OutputTarget.MAIN_TARGET),
                createRenderType(texture, "colored_glint_translucent_" + colorName, TextureTransform.GLINT_TEXTURING, OutputTarget.ITEM_ENTITY_TARGET));
    }

    private static NativeImage createTexture(@Nullable Integer startColor, @Nullable Integer endColor, float transition) {
        try (InputStream stream = Minecraft.getInstance().getResourceManager().open(VANILLA_GLINT)) {
            try (NativeImage vanilla = NativeImage.read(stream)) {
                return vanilla.mappedCopy(pixel -> colorize(pixel, startColor, endColor, transition));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load vanilla enchantment glint texture", exception);
        }
    }

    private static int colorize(int pixel, @Nullable Integer startColor, @Nullable Integer endColor, float transition) {
        int intensity = Math.max((pixel >> 16) & 0xFF, Math.max((pixel >> 8) & 0xFF, pixel & 0xFF));
        int visibleStartColor = startColor != null ? startColor : endColor != null ? endColor : 0;
        int visibleEndColor = endColor != null ? endColor : visibleStartColor;
        float startOpacity = startColor == null ? 0.0F : 1.0F;
        float endOpacity = endColor == null ? 0.0F : 1.0F;
        float opacity = startOpacity + (endOpacity - startOpacity) * transition;
        int startRed = ((visibleStartColor >> 16) & 0xFF) * intensity / 0xFF;
        int startGreen = ((visibleStartColor >> 8) & 0xFF) * intensity / 0xFF;
        int startBlue = (visibleStartColor & 0xFF) * intensity / 0xFF;
        int endRed = ((visibleEndColor >> 16) & 0xFF) * intensity / 0xFF;
        int endGreen = ((visibleEndColor >> 8) & 0xFF) * intensity / 0xFF;
        int endBlue = (visibleEndColor & 0xFF) * intensity / 0xFF;
        int red = Math.round((startRed + (endRed - startRed) * transition) * opacity);
        int green = Math.round((startGreen + (endGreen - startGreen) * transition) * opacity);
        int blue = Math.round((startBlue + (endBlue - startBlue) * transition) * opacity);
        return pixel & 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static @Nullable Integer normalizeColor(@Nullable Integer color) {
        return color == null ? null : color & 0xFFFFFF;
    }

    private static RenderType createRenderType(Identifier texture, String name, TextureTransform transform, OutputTarget target) {
        return RenderType.create(NoblePhantasms.MOD_ID + "_" + name,
                RenderSetup.builder(RenderPipelines.GLINT)
                        .withTexture("Sampler0", texture)
                        .setTextureTransform(transform)
                        .setOutputTarget(target)
                        .createRenderSetup());
    }

    private record Registration(Item item, @Nullable Integer startColor, @Nullable Integer fallbackStartColor, @Nullable Integer endColor,
                                ToDoubleFunction<ItemStack> transitionProvider, Predicate<ItemStack> usePrimaryStart, boolean transitioning) {
    }

    private record RegisteredStyle(GlintStyle[] primaryStyles, GlintStyle[] fallbackStyles, ToDoubleFunction<ItemStack> transitionProvider, Predicate<ItemStack> usePrimaryStart) {
        private GlintStyle style(ItemStack stack, double transition) {
            GlintStyle[] styles = usePrimaryStart.test(stack) ? primaryStyles : fallbackStyles;
            int index = styles.length == 1 ? 0 : Math.clamp((int) Math.round(transition * TRANSITION_STEPS), 0, TRANSITION_STEPS);
            return styles[index];
        }
    }

    private record StyleKey(@Nullable Integer startColor, @Nullable Integer endColor, int transitionStep) {
    }

    private record GlintStyle(RenderType glint, RenderType entityGlint, RenderType translucentGlint) {
    }
}
