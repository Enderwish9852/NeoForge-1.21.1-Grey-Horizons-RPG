package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.skills.LearnedSkillsCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.SkillBookItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * SkillBindKeyPacket
 *
 * override=false means "tell me first if this conflicts with another of my
 * skills" (server replies with SkillBindResultPacket(conflict=true,...)
 * instead of finalizing); override=true means finalize regardless -- only
 * ever sent by the client after the player already confirmed the prompt.
 * Vanilla-keybind conflicts are checked entirely client-side before this
 * packet is even sent -- see SkillKeybindCaptureScreen.
 */
public record SkillBindKeyPacket(String skillId, int keyCode, boolean override) implements CustomPacketPayload {

    public static final Type<SkillBindKeyPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "skill_bind_key"));

    public static final StreamCodec<FriendlyByteBuf, SkillBindKeyPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUtf(p.skillId); buf.writeVarInt(p.keyCode); buf.writeBoolean(p.override); },
            buf -> new SkillBindKeyPacket(buf.readUtf(), buf.readVarInt(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            LearnedSkillsCapability skills = player.getData(ModAttachments.LEARNED_SKILLS);
            String conflictingSkill = skills.getSkillUsingKey(keyCode);

            if (conflictingSkill != null && !conflictingSkill.equals(skillId) && !override) {
                ModMessages.sendToPlayer(new SkillBindResultPacket(false, true, conflictingSkill, skillId, keyCode), player);
                return;
            }

            skills.bind(skillId, keyCode);

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (!stack.isEmpty() && stack.getItem() instanceof SkillBookItem
                        && skillId.equals(stack.get(ModDataComponents.SKILL_ID.get()))) {
                    stack.shrink(1);
                    break;
                }
            }

            ModMessages.sendToPlayer(new SkillBindResultPacket(true, false, null, skillId, keyCode), player);
        });
    }
}
