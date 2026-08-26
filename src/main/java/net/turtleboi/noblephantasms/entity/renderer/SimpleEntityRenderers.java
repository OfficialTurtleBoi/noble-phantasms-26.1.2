package net.turtleboi.noblephantasms.entity.renderer;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.turtleboi.noblephantasms.entity.ModEntities;

public final class SimpleEntityRenderers {
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.APILOLLI_CLOUD.get(), NoopRenderer::new);
        YasakaniGuardianRenderer.register(event);
    }
}
