package net.enderwish.Belliarium_Monstrarium_Subpack.core;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.skills.LearnedSkillsCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SurvivalCapability;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

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

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LearnedSkillsCapability>> LEARNED_SKILLS =
            ATTACHMENT_TYPES.register("learned_skills", () ->
                    AttachmentType.builder(() -> new LearnedSkillsCapability())
                            .serialize(LearnedSkillsCapability.CODEC)
                            .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
