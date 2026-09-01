package net.turtleboi.noblephantasms.screens;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.component.ModDataComponents;
import net.turtleboi.noblephantasms.client.renderer.ReliquaryItemRenderState;
import net.turtleboi.noblephantasms.client.renderer.ReliquaryItemRenderer;
import net.turtleboi.noblephantasms.relic.RelicFragmentData;
import net.turtleboi.noblephantasms.relic.RelicFragmentDefinitions;
import net.turtleboi.noblephantasms.relic.RelicFragmentArchive;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.RelicFragmentItem;
import net.turtleboi.noblephantasms.network.MythicalReliquarySelectPayload;
import net.turtleboi.noblephantasms.screens.menus.custom.MythicalReliquaryMenu;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class MythicalReliquaryScreen extends AbstractContainerScreen<MythicalReliquaryMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            NoblePhantasms.MOD_ID, "textures/ui/mythical_reliquary.png");
    private static final int BOOK_WIDTH = 250;
    private static final int BOOK_HEIGHT = 156;
    private static final int PAGE_Y = 10;
    private static final int PAGE_WIDTH = 108;
    private static final int PAGE_HEIGHT = 132;
    private static final int LEFT_PAGE_X = 12;
    private static final int RIGHT_PAGE_X = 130;
    private static final int ROWS_PER_PAGE = 6;
    private static final int RELICS_PER_SPREAD = ROWS_PER_PAGE * 2;
    private static final int ROW_HEIGHT = 17;
    private static final int LIGHT_SEPIA = 0xFFDED1BA;
    private static final int MID_SEPIA = 0xFFAC9D87;
    private static final int DARK_SEPIA = 0xFF776853;
    private static final int HOVER_SEPIA = 0x40776853;
    private static final float INITIAL_PREVIEW_PITCH = (float) Math.toRadians(30.0);
    private static final float INITIAL_PREVIEW_YAW = (float) Math.toRadians(-45.0);
    private static final long FRAGMENT_ABSORB_DURATION = 520L;
    private static final long FRAGMENT_ABSORB_STAGGER = 42L;
    private static final float LARGE_PREVIEW_PITCH = (float) Math.toRadians(18.0);
    private static final float LARGE_PREVIEW_ROLL = (float) Math.toRadians(22.0);
    private static final float LARGE_PREVIEW_SCALE = 1.28F;
    private static final float PREVIEW_SPIN_SPEED = (float) Math.toRadians(12.0) / 1000.0F;
    private static final float FULL_ROTATION = (float) (Math.PI * 2.0);
    private static final List<RelicFragmentDefinitions.Definition> RELICS =
            RelicFragmentDefinitions.definitions();

    private final Inventory inventory;
    private final Map<Identifier, SepiaTexture> sepiaTextures = new HashMap<>();
    private int contentsSpread;
    private RelicFragmentDefinitions.Definition openRelic;
    private PageButton previousPageButton;
    private PageButton nextPageButton;
    private TrackingItemStackRenderState previewModel;
    private Vector3f previewModelCenter = new Vector3f();
    private float previewModelScale = 64.0F;
    private float previewPitch = INITIAL_PREVIEW_PITCH;
    private float previewYaw = INITIAL_PREVIEW_YAW;
    private float previewRoll;
    private boolean draggingPreview;
    private int previewDragButton = -1;
    private List<ItemStack> absorbingFragments = List.of();
    private long fragmentAbsorptionStarted;
    private long previewFrameTime;

    public MythicalReliquaryScreen(MythicalReliquaryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BOOK_WIDTH, BOOK_HEIGHT);
        this.inventory = inventory;
        inventoryLabelY = 10000;
        titleLabelY = 10000;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void init() {
        super.init();
        int buttonY = topPos + PAGE_Y + PAGE_HEIGHT - 15;
        previousPageButton = addRenderableWidget(new PageButton(
                leftPos + LEFT_PAGE_X + 2, buttonY, false, ignored -> turnBackward(), true));
        nextPageButton = addRenderableWidget(new PageButton(
                leftPos + RIGHT_PAGE_X + PAGE_WIDTH - 25, buttonY,
                true, ignored -> turnForward(), true));
        updatePageButtons();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F,
                BOOK_WIDTH, BOOK_HEIGHT, BOOK_WIDTH, BOOK_HEIGHT);
        if (openRelic == null) {
            drawContents(graphics, mouseX, mouseY);
        } else {
            drawRelic(graphics, mouseX, mouseY);
        }
    }

    private void drawContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int leftX = leftPos + LEFT_PAGE_X;
        int rightX = leftPos + RIGHT_PAGE_X;
        int pageTop = topPos + PAGE_Y;
        centeredTextNoShadow(graphics,
                Component.translatable("menu.noblephantasms.mythical_reliquary.contents"),
                leftX + PAGE_WIDTH / 2, pageTop + 4, DARK_SEPIA);
        centeredTextNoShadow(graphics,
                Component.translatable("menu.noblephantasms.mythical_reliquary"),
                rightX + PAGE_WIDTH / 2, pageTop + 4, DARK_SEPIA);

        int first = contentsSpread * RELICS_PER_SPREAD;
        for (int page = 0; page < 2; page++) {
            int pageX = page == 0 ? leftX : rightX;
            for (int row = 0; row < ROWS_PER_PAGE; row++) {
                int index = first + page * ROWS_PER_PAGE + row;
                if (index >= RELICS.size()) {
                    break;
                }
                int rowY = pageTop + 18 + row * ROW_HEIGHT;
                RelicFragmentDefinitions.Definition definition = RELICS.get(index);
                boolean hovered = contains(mouseX, mouseY, pageX, rowY, PAGE_WIDTH, 16);
                boolean focused = definition.relicId().equals(menu.focusedRelic());
                if (hovered || focused) {
                    graphics.fill(pageX, rowY, pageX + PAGE_WIDTH, rowY + 16, HOVER_SEPIA);
                }
                drawSepiaTexture(graphics, definition.inventoryTextureId(), pageX + 1, rowY, 16, 16, 0.0F);
                ItemStack relicStack = new ItemStack(definition.relic().get());
                String name = definition.relic().get().getName(relicStack).getString();
                name = fit(name, PAGE_WIDTH - 21);
                graphics.text(font, name, pageX + 20, rowY + 4, DARK_SEPIA, false);
            }
        }

    }

    private void drawRelic(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int leftX = leftPos + LEFT_PAGE_X;
        int rightX = leftPos + RIGHT_PAGE_X;
        int pageTop = topPos + PAGE_Y;
        ItemStack relicStack = new ItemStack(openRelic.relic().get());
        Component relicName = openRelic.relic().get().getName(relicStack);
        List<FormattedCharSequence> titleLines = font.split(relicName, PAGE_WIDTH - 8);
        int y = pageTop + 4;
        for (FormattedCharSequence line : titleLines) {
            centeredTextNoShadow(graphics, line, leftX + PAGE_WIDTH / 2, y, DARK_SEPIA);
            y += 10;
        }
        y += 3;
        Component flavor = Component.translatable("tooltip.noblephantasms."
                + openRelic.relicId().getPath() + ".flavor");
        y = drawWrappedHalfScale(graphics, flavor, leftX + 5, y, PAGE_WIDTH - 10, y + 58);
        y += 4;
        String abilityKey = "jei.noblephantasms.info." + openRelic.relicId().getPath();
        if (I18n.exists(abilityKey)) {
            drawWrappedHalfScale(graphics, Component.translatable(abilityKey), leftX + 5, y,
                    PAGE_WIDTH - 10, pageTop + PAGE_HEIGHT - 17);
        }

        int previewCenterX = rightX + PAGE_WIDTH / 2;
        int previewX0 = rightX + 5;
        int previewY0 = pageTop + 4;
        int previewX1 = rightX + PAGE_WIDTH - 5;
        int previewY1 = pageTop + 88;
        updatePreviewRotation(contains(mouseX, mouseY,
                previewX0, previewY0, previewX1 - previewX0, previewY1 - previewY0));
        drawPreview(graphics, relicStack, previewX0, previewY0, previewX1, previewY1);
        FragmentProgress progress = fragmentProgress(openRelic);
        Component progressText = Component.translatable("menu.noblephantasms.mythical_reliquary.fragments",
                progress.owned(), progress.required());
        centeredTextNoShadow(graphics, progressText, previewCenterX, pageTop + 91, DARK_SEPIA);
        drawFragmentAbsorption(graphics, previewCenterX, pageTop + 47);

    }

    private void beginFragmentAbsorption(RelicFragmentDefinitions.Definition definition) {
        List<ItemStack> fragments = new ArrayList<>();
        RelicFragmentArchive.RelicSet set = archive().get(definition.relicId());
        if (set != null) {
            for (int index = 0; index < set.pieceCount(); index++) {
                if ((set.discoveredMask() & 1 << index) != 0) {
                    fragments.add(RelicFragmentItem.create(ModItems.RELIC_FRAGMENT.get(),
                            new RelicFragmentData(set.relicId(), set.seed(), index, set.pieceCount()), 1));
                }
            }
        }
        absorbingFragments = List.copyOf(fragments);
        fragmentAbsorptionStarted = absorbingFragments.isEmpty() ? 0L : Util.getMillis();
    }

    private void drawFragmentAbsorption(GuiGraphicsExtractor graphics, int targetX, int targetY) {
        if (fragmentAbsorptionStarted == 0L || absorbingFragments.isEmpty()) {
            return;
        }
        long elapsedTotal = Util.getMillis() - fragmentAbsorptionStarted;
        boolean running = false;
        int count = absorbingFragments.size();
        for (int index = 0; index < count; index++) {
            long elapsed = elapsedTotal - index * FRAGMENT_ABSORB_STAGGER;
            if (elapsed < 0L) {
                running = true;
                continue;
            }
            float progress = Math.clamp(elapsed / (float) FRAGMENT_ABSORB_DURATION, 0.0F, 1.0F);
            if (progress >= 1.0F) {
                continue;
            }
            running = true;
            float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0);
            double angle = Math.PI * 2.0 * index / Math.max(1, count) - Math.PI * 0.5;
            float startX = targetX + (float) Math.cos(angle) * 74.0F;
            float startY = targetY + (float) Math.sin(angle) * 48.0F;
            float x = startX + (targetX - startX) * eased;
            float y = startY + (targetY - startY) * eased;
            float scale = Math.max(0.08F, 1.0F - eased * 0.92F);
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(scale, scale);
            graphics.item(absorbingFragments.get(index), -8, -8);
            graphics.pose().popMatrix();

            if (progress > 0.55F) {
                int dustAlpha = Math.round(110.0F * (1.0F - progress));
                int dust = dustAlpha << 24 | MID_SEPIA & 0x00FFFFFF;
                int offset = index % 3 - 1;
                graphics.fill(targetX + offset - 1, targetY - offset - 1,
                        targetX + offset + 1, targetY - offset + 1, dust);
            }
        }
        if (!running) {
            fragmentAbsorptionStarted = 0L;
            absorbingFragments = List.of();
        }
    }

    private int drawWrappedHalfScale(GuiGraphicsExtractor graphics, Component text, int x, int y,
                                     int width, int maxY) {
        int logicalY = 0;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(0.5F, 0.5F);
        for (FormattedCharSequence line : font.split(text, width * 2)) {
            if (y + (logicalY + 9) * 0.5F > maxY) {
                break;
            }
            graphics.text(font, line, 0, logicalY, DARK_SEPIA, false);
            logicalY += 10;
        }
        graphics.pose().popMatrix();
        return y + (logicalY + 1) / 2;
    }

    private void drawPreview(GuiGraphicsExtractor graphics, ItemStack stack,
                             int x0, int y0, int x1, int y1) {
        if (previewModel == null) {
            previewModel = ReliquaryItemRenderer.resolveHeldModel(stack);
            AABB bounds = previewModel.getModelBoundingBox();
            previewModelCenter = new Vector3f(
                    (float) ((bounds.minX + bounds.maxX) * 0.5),
                    (float) ((bounds.minY + bounds.maxY) * 0.5),
                    (float) ((bounds.minZ + bounds.maxZ) * 0.5));
            double halfX = Math.max(bounds.maxX - previewModelCenter.x,
                    previewModelCenter.x - bounds.minX);
            double halfY = Math.max(bounds.maxY - previewModelCenter.y,
                    previewModelCenter.y - bounds.minY);
            double halfZ = Math.max(bounds.maxZ - previewModelCenter.z,
                    previewModelCenter.z - bounds.minZ);
            double radius = Math.sqrt(halfX * halfX + halfY * halfY + halfZ * halfZ);
            previewModelScale = radius > 0.001
                    ? Math.clamp((float) ((Math.min(x1 - x0, y1 - y0) - 8) / (radius * 2.0)),
                    16.0F, 112.0F)
                    : 64.0F;
            if (isLargePreview()) {
                previewModelScale *= LARGE_PREVIEW_SCALE;
                previewPitch = LARGE_PREVIEW_PITCH;
                previewRoll = LARGE_PREVIEW_ROLL;
            }
        }
        Quaternionf rotation = new Quaternionf().rotationXYZ(previewPitch, previewYaw, previewRoll);
        graphics.submitPictureInPictureRenderState(new ReliquaryItemRenderState(
                previewModel, new Vector3f(previewModelCenter), rotation,
                x0, y0, x1, y1, previewModelScale, graphics.peekScissorStack()));
    }

    private void drawSepiaTexture(GuiGraphicsExtractor graphics, Identifier textureId,
                                  int x, int y, int width, int height, float rotation) {
        SepiaTexture texture = getSepiaTexture(textureId);
        if (texture == null) {
            return;
        }
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().rotate(rotation);
        graphics.pose().translate(-centerX, -centerY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture.id(), x, y, 0.0F, 0.0F,
                width, height, texture.width(), texture.frameHeight(), texture.width(), texture.height());
        graphics.pose().popMatrix();
    }

    private SepiaTexture getSepiaTexture(Identifier source) {
        if (sepiaTextures.containsKey(source)) {
            return sepiaTextures.get(source);
        }
        String path = "/assets/" + source.getNamespace() + "/textures/" + source.getPath() + ".png";
        try (InputStream stream = MythicalReliquaryScreen.class.getResourceAsStream(path)) {
            if (stream == null) {
                NoblePhantasms.LOGGER.warn("Missing Mythical Reliquary texture {}", path);
                sepiaTextures.put(source, null);
                return null;
            }
            NativeImage image = NativeImage.read(stream);
            for (int imageY = 0; imageY < image.getHeight(); imageY++) {
                for (int imageX = 0; imageX < image.getWidth(); imageX++) {
                    image.setPixel(imageX, imageY, sepia(image.getPixel(imageX, imageY)));
                }
            }
            Identifier dynamicId = Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID,
                    "dynamic/mythical_reliquary/" + source.getNamespace() + "_"
                            + source.getPath().replace('/', '_'));
            minecraft.getTextureManager().register(dynamicId,
                    new DynamicTexture(() -> "Sepia Mythical Reliquary relic", image));
            int frameHeight = image.getHeight() > image.getWidth()
                    && image.getHeight() % image.getWidth() == 0 ? image.getWidth() : image.getHeight();
            SepiaTexture texture = new SepiaTexture(dynamicId, image.getWidth(), image.getHeight(), frameHeight);
            sepiaTextures.put(source, texture);
            return texture;
        } catch (IOException exception) {
            NoblePhantasms.LOGGER.warn("Unable to load Mythical Reliquary texture {}", path, exception);
            sepiaTextures.put(source, null);
            return null;
        }
    }

    private static int sepia(int color) {
        int alpha = ARGB.alpha(color);
        if (alpha == 0) {
            return color;
        }
        float luminance = (ARGB.red(color) * 0.2126F + ARGB.green(color) * 0.7152F
                + ARGB.blue(color) * 0.0722F) / 255.0F;
        int low = luminance < 0.5F ? DARK_SEPIA : MID_SEPIA;
        int high = luminance < 0.5F ? MID_SEPIA : LIGHT_SEPIA;
        float amount = luminance < 0.5F ? luminance * 2.0F : (luminance - 0.5F) * 2.0F;
        int red = Math.round(ARGB.red(low) + (ARGB.red(high) - ARGB.red(low)) * amount);
        int green = Math.round(ARGB.green(low) + (ARGB.green(high) - ARGB.green(low)) * amount);
        int blue = Math.round(ARGB.blue(low) + (ARGB.blue(high) - ARGB.blue(low)) * amount);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private FragmentProgress fragmentProgress(RelicFragmentDefinitions.Definition definition) {
        RelicFragmentArchive.RelicSet set = archive().get(definition.relicId());
        return set == null
                ? new FragmentProgress(0, definition.maximumPieces())
                : new FragmentProgress(set.discoveredCount(), set.pieceCount());
    }

    private RelicFragmentArchive archive() {
        return menu.reliquary(inventory.player).getOrDefault(
                ModDataComponents.MYTHICAL_RELIQUARY_ARCHIVE.get(), RelicFragmentArchive.EMPTY);
    }

    private String fit(String value, int width) {
        if (font.width(value) <= width) {
            return value;
        }
        return font.plainSubstrByWidth(value, Math.max(0, width - font.width("..."))) + "...";
    }

    private void centeredTextNoShadow(GuiGraphicsExtractor graphics, Component text,
                                      int centerX, int y, int color) {
        FormattedCharSequence sequence = text.getVisualOrderText();
        centeredTextNoShadow(graphics, sequence, centerX, y, color);
    }

    private void centeredTextNoShadow(GuiGraphicsExtractor graphics, FormattedCharSequence text,
                                      int centerX, int y, int color) {
        graphics.text(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private int spreadCount() {
        return Math.max(1, (RELICS.size() + RELICS_PER_SPREAD - 1) / RELICS_PER_SPREAD);
    }

    private void turnBackward() {
        if (openRelic != null) {
            openRelic = null;
            clearPreview();
        } else if (contentsSpread > 0) {
            contentsSpread--;
        }
        updatePageButtons();
    }

    private void turnForward() {
        if (openRelic == null && contentsSpread + 1 < spreadCount()) {
            contentsSpread++;
        }
        updatePageButtons();
    }

    private void updatePageButtons() {
        if (previousPageButton != null) {
            previousPageButton.visible = openRelic != null || contentsSpread > 0;
        }
        if (nextPageButton != null) {
            nextPageButton.visible = openRelic == null && contentsSpread + 1 < spreadCount();
        }
    }

    private void clearPreview() {
        previewModel = null;
        previewModelCenter = new Vector3f();
        previewModelScale = 64.0F;
        previewPitch = INITIAL_PREVIEW_PITCH;
        previewYaw = INITIAL_PREVIEW_YAW;
        previewRoll = 0.0F;
        draggingPreview = false;
        previewDragButton = -1;
        previewFrameTime = 0L;
    }

    private boolean isLargePreview() {
        return openRelic != null && openRelic.textureVariant()
                == RelicFragmentDefinitions.TextureVariant.WEAPON;
    }

    private void updatePreviewRotation(boolean hovered) {
        long frameTime = Util.getMillis();
        if (previewFrameTime == 0L) {
            previewFrameTime = frameTime;
            return;
        }
        long elapsed = Math.min(frameTime - previewFrameTime, 100L);
        previewFrameTime = frameTime;
        if (!hovered && !draggingPreview) {
            previewYaw = (previewYaw + elapsed * PREVIEW_SPIN_SPEED) % FULL_ROTATION;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mouseX = (int) event.x();
            int mouseY = (int) event.y();
            if (openRelic == null) {
                RelicFragmentDefinitions.Definition clicked = contentsEntryAt(mouseX, mouseY);
                if (clicked != null) {
                    menu.selectRelic(minecraft.player, clicked.relicId());
                    ClientPacketDistributor.sendToServer(new MythicalReliquarySelectPayload(
                            menu.containerId, clicked.relicId()));
                    openRelic = clicked;
                    clearPreview();
                    beginFragmentAbsorption(clicked);
                    updatePageButtons();
                    return true;
                }
            } else {
                int pageTop = topPos + PAGE_Y;
                int rightX = leftPos + RIGHT_PAGE_X;
                if (contains(mouseX, mouseY, rightX + 5, pageTop + 4, PAGE_WIDTH - 10, 84)) {
                    draggingPreview = true;
                    previewDragButton = 0;
                    return true;
                }
            }
        } else if (event.button() == 1 && openRelic != null) {
            int pageTop = topPos + PAGE_Y;
            int rightX = leftPos + RIGHT_PAGE_X;
            if (contains(event.x(), event.y(), rightX + 5, pageTop + 4, PAGE_WIDTH - 10, 84)) {
                draggingPreview = true;
                previewDragButton = 1;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private RelicFragmentDefinitions.Definition contentsEntryAt(int mouseX, int mouseY) {
        int pageTop = topPos + PAGE_Y;
        int first = contentsSpread * RELICS_PER_SPREAD;
        for (int page = 0; page < 2; page++) {
            int pageX = leftPos + (page == 0 ? LEFT_PAGE_X : RIGHT_PAGE_X);
            for (int row = 0; row < ROWS_PER_PAGE; row++) {
                int index = first + page * ROWS_PER_PAGE + row;
                if (index >= RELICS.size()) {
                    break;
                }
                if (contains(mouseX, mouseY, pageX,
                        pageTop + 18 + row * ROW_HEIGHT, PAGE_WIDTH, 16)) {
                    return RELICS.get(index);
                }
            }
        }
        return null;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingPreview && event.button() == previewDragButton && openRelic != null) {
            if (previewDragButton == 1) {
                previewRoll += (float) (dragX - dragY) * 0.018F;
            } else {
                previewYaw += (float) dragX * 0.018F;
                previewPitch += (float) dragY * 0.018F;
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == previewDragButton && draggingPreview) {
            draggingPreview = false;
            previewDragButton = -1;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void removed() {
        if (minecraft != null) {
            for (SepiaTexture texture : sepiaTextures.values()) {
                if (texture != null) {
                    minecraft.getTextureManager().release(texture.id());
                }
            }
        }
        sepiaTextures.clear();
        super.removed();
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record SepiaTexture(Identifier id, int width, int height, int frameHeight) {
    }

    private record FragmentProgress(int owned, int required) {
    }
}
