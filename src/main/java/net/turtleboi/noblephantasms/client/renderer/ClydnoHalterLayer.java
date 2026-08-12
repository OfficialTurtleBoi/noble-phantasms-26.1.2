package net.turtleboi.noblephantasms.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.google.common.reflect.TypeToken;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DonkeyRenderer;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.attachment.ModAttachments;

public final class ClydnoHalterLayer<S extends EquineRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    private static final Identifier HORSE_TEXTURE = texture("horse");
    private static final Identifier DONKEY_TEXTURE = texture("donkey");
    private static final Identifier MULE_TEXTURE = texture("mule");
    private static final ContextKey<Boolean> HALTERED = new ContextKey<>(Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "clydno_haltered"));
    private final Identifier texture;

    private ClydnoHalterLayer(RenderLayerParent<S, M> parent, Identifier texture) {
        super(parent);
        this.texture = texture;
    }

    public static void registerRenderStateModifier(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(HorseRenderer.class, (horse, state) -> state.setRenderData(HALTERED, horse.getData(ModAttachments.CLYDNO_HALTERED)));
        event.registerEntityModifier(new TypeToken<DonkeyRenderer<AbstractChestedHorse>>() {},
                (horse, state) -> state.setRenderData(HALTERED, horse.getData(ModAttachments.CLYDNO_HALTERED)));
    }

    public static void addLayer(EntityRenderersEvent.AddLayers event) {
        HorseRenderer renderer = event.getRenderer(EntityType.HORSE);
        if (renderer != null) {
            renderer.addLayer(new ClydnoHalterLayer<>(renderer, HORSE_TEXTURE));
        }
        addDonkeyLayer(event, EntityType.DONKEY, DONKEY_TEXTURE);
        addDonkeyLayer(event, EntityType.MULE, MULE_TEXTURE);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
        if (!Boolean.TRUE.equals(state.getRenderData(HALTERED)) || state.isInvisible) {
            return;
        }
        collector.order(3).submitModel(getParentModel(), state, poseStack, RenderTypes.entityTranslucent(texture), lightCoords,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor, null);
    }

    private static <T extends AbstractChestedHorse> void addDonkeyLayer(EntityRenderersEvent.AddLayers event, EntityType<T> type, Identifier texture) {
        DonkeyRenderer<T> renderer = event.getRenderer(type);
        if (renderer != null) {
            renderer.addLayer(new ClydnoHalterLayer<>(renderer, texture));
        }
    }

    private static Identifier texture(String type) {
        return Identifier.fromNamespaceAndPath(NoblePhantasms.MOD_ID, "textures/entity/equipment/clydno_halter/" + type + ".png");
    }
}
