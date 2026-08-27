package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.google.common.reflect.TypeToken;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DonkeyRenderer;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.attachment.ModAttachments;

public final class ClydnoHalterLayer<S extends EquineRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    private static final Identifier HALTER_TEXTURE = texture("clydno_halter");
    private static final ContextKey<Boolean> HALTERED = new ContextKey<>(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "clydno_haltered"));
    private final Identifier texture;
    private final EntityModel<? super S> saddleModel;

    private ClydnoHalterLayer(RenderLayerParent<S, M> parent, Identifier texture, EntityModel<? super S> saddleModel) {
        super(parent);
        this.texture = texture;
        this.saddleModel = saddleModel;
    }

    public static void registerRenderStateModifier(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(HorseRenderer.class, ClydnoHalterLayer::extractRenderState);
        event.registerEntityModifier(new TypeToken<DonkeyRenderer<AbstractChestedHorse>>() {}, ClydnoHalterLayer::extractRenderState);
    }

    public static void addLayer(EntityRenderersEvent.AddLayers event) {
        HorseRenderer renderer = event.getRenderer(EntityType.HORSE);
        if (renderer != null) {
            renderer.addLayer(new ClydnoHalterLayer<>(renderer, HALTER_TEXTURE,
                    new EquineSaddleModel(event.getEntityModels().bakeLayer(ModelLayers.HORSE_SADDLE))));
        }
        addDonkeyLayer(event, EntityType.DONKEY, ModelLayers.DONKEY_SADDLE);
        addDonkeyLayer(event, EntityType.MULE, ModelLayers.MULE_SADDLE);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
        if (!Boolean.TRUE.equals(state.getRenderData(HALTERED)) || state.isInvisible || state.isBaby) {
            return;
        }
        collector.order(2).submitModel(saddleModel, state, poseStack, RenderTypes.armorCutoutNoCull(texture), lightCoords,
                OverlayTexture.NO_OVERLAY, state.outlineColor, null);
    }

    private static void extractRenderState(AbstractHorse horse, EquineRenderState state) {
        boolean haltered = horse.getData(ModAttachments.CLYDNO_HALTERED);
        state.setRenderData(HALTERED, haltered);
        if (haltered) {
            state.saddle = ItemStack.EMPTY;
        }
    }

    private static <T extends AbstractChestedHorse> void addDonkeyLayer(EntityRenderersEvent.AddLayers event, EntityType<T> type,
                                                                        ModelLayerLocation saddleLayer) {
        DonkeyRenderer<T> renderer = event.getRenderer(type);
        if (renderer != null) {
            renderer.addLayer(new ClydnoHalterLayer<>(renderer, HALTER_TEXTURE,
                    new EquineSaddleModel(event.getEntityModels().bakeLayer(saddleLayer))));
        }
    }

    private static Identifier texture(String type) {
        return Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "textures/entity/equipment/clydno_halter/" + type + ".png");
    }
}
