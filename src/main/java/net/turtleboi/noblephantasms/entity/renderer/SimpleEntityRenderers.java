package net.turtleboi.noblephantasms.entity.renderer;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.entity.ModEntities;

public final class SimpleEntityRenderers {
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.APILOLLI_CLOUD.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.YASAKANI_GUARDIAN.get(),
                context -> new ThrownItemRenderer<>(context, 0.6F, true));
    }
}
