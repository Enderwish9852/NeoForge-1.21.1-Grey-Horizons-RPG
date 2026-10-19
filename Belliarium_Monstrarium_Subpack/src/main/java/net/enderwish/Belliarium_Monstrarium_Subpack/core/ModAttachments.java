package net.enderwish.Belliarium_Monstrarium_Subpack.core;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.skills.LearnedSkillsCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SurvivalCapability;
import net.minecraft.world.item.ItemStack;
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

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SanityCapability>> SANITY =
            ATTACHMENT_TYPES.register("sanity", () ->
                    AttachmentType.builder(() -> new net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SanityCapability())
                            .serialize(net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SanityCapability.CODEC)
                            .build());

    /**
     * Which backpack special slot (if any) the player is currently
     * holding as their virtual extra hotbar slot (0 none, 1 left, 2 right).
     * RUNTIME ONLY -- deliberately not serialized: the client sets it
     * locally for prediction and mirrors the choice to the server via
     * BackpackExtensionSelectPacket, which re-validates it. Surviving a
     * reload would be meaningless.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Byte>> BACKPACK_EXTENSION =
            ATTACHMENT_TYPES.register("backpack_extension", () ->
                    AttachmentType.builder(() -> Byte.valueOf((byte) 0)).build());

    /**
     * The stable virtual "held" ItemStack of the active backpack extension:
     * seeded from the special slot when the extension is selected and
     * handed out by PlayerMixin.getItemBySlot(MAINHAND) for as long as it
     * stays active -- vanilla's item-use pipeline needs one stable
     * instance, not the fresh copy BackpackItem.getContents returns on
     * every read. RUNTIME ONLY -- deliberately not serialized: the special
     * slot on the Curios stack is the persisted truth.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> BACKPACK_HELD =
            ATTACHMENT_TYPES.register("backpack_held", () ->
                    AttachmentType.builder(() -> ItemStack.EMPTY).build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
