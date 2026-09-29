package net.enderwish.Belliarium_Monstrarium_Subpack.core;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * ModAttachments
 *
 * VERIFY IF COMPILE FAILS -- AttachmentType.builder(...).serialize(...).build()
 * is written from the standard, long-stable NeoForge attachment pattern, not
 * from source pasted in this conversation. If the builder method names differ
 * slightly, IntelliJ's autocomplete on "AttachmentType.builder(" will resolve
 * it in seconds.
 */
public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, BelliariumMonstrariumSubpack.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BodyHealthCapability>> BODY_HEALTH =
            ATTACHMENT_TYPES.register("body_health", () ->
                    AttachmentType.builder(BodyHealthCapability::new)
                            .serialize(BodyHealthCapability.CODEC)
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SurvivalCapability>> SURVIVAL =
            ATTACHMENT_TYPES.register("survival", () ->
                    AttachmentType.builder(SurvivalCapability::new)
                            .serialize(SurvivalCapability.CODEC)
                            .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
