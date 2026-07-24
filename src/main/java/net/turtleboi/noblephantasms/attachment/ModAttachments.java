package net.turtleboi.noblephantasms.attachment;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.turtleboi.noblephantasms.NoblePhantasms;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NoblePhantasms.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BertilakCovenantState>> BERTILAK_COVENANT =
            ATTACHMENTS.register("bertilak_covenant", () -> AttachmentType.builder(() -> BertilakCovenantState.EMPTY)
                    .serialize(BertilakCovenantState.CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {
    }

    public static void register(IEventBus eventBus) {
        ATTACHMENTS.register(eventBus);
    }
}
