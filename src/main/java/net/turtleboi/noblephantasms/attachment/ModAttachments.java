package net.turtleboi.noblephantasms.attachment;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
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

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> MEDJU_NETJER_INSTALLED =
            ATTACHMENTS.register("medju_netjer_installed", () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL.fieldOf("installed"), Boolean::booleanValue)
                    .sync(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> LUMINOUS_COLOR =
            ATTACHMENTS.register("luminous_color", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT.fieldOf("color"))
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> EYE_OF_HORUS_GLOW_PROGRESS =
            ATTACHMENTS.register("eye_of_horus_glow_progress", () -> AttachmentType.builder(() -> 0.0F)
                    .sync(ByteBufCodecs.FLOAT)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> EYE_OF_HORUS_JUDGEMENT_GLOW =
            ATTACHMENTS.register("eye_of_horus_judgement_glow", () -> AttachmentType.builder(() -> false)
                    .sync(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> EYE_OF_HORUS_PIECE_MASK =
            ATTACHMENTS.register("eye_of_horus_piece_mask", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT.fieldOf("piece_mask"))
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> EYE_OF_HORUS_FRAGMENT_SEED =
            ATTACHMENTS.register("eye_of_horus_fragment_seed", () -> AttachmentType.builder(() -> 0L)
                    .serialize(Codec.LONG.fieldOf("fragment_seed"))
                    .sync(ByteBufCodecs.VAR_LONG)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> EYE_OF_HORUS_PIECES_EXPIRE_AT =
            ATTACHMENTS.register("eye_of_horus_pieces_expire_at", () -> AttachmentType.builder(() -> 0L)
                    .serialize(Codec.LONG.fieldOf("expires_at"))
                    .sync(ByteBufCodecs.VAR_LONG)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> EYE_OF_HORUS_ASSEMBLED =
            ATTACHMENTS.register("eye_of_horus_assembled", () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL.fieldOf("assembled"), Boolean::booleanValue)
                    .sync(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> BERTILAK_GLOW_PROGRESS =
            ATTACHMENTS.register("bertilak_glow_progress", () -> AttachmentType.builder(() -> 0.0F)
                    .sync(ByteBufCodecs.FLOAT)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> BERTILAK_COVENANT_GLOW =
            ATTACHMENTS.register("bertilak_covenant_glow", () -> AttachmentType.builder(() -> false)
                    .sync(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Optional<UUID>>> BLEED_SOURCE =
            ATTACHMENTS.register("bleed_source", () -> AttachmentType.builder(Optional::<UUID>empty)
                    .serialize(UUIDUtil.CODEC.optionalFieldOf("source"))
                    .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENTS.register(eventBus);
    }
}
