package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.event.SkillLearningHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * SkillBookScreenStatePacket
 *
 * Client -> server. opened=true is a heartbeat sent once a second WHILE
 * the book screen stays open; opened=false is a one-shot sent from
 * onClose(). See SkillLearningHandler for why it's a heartbeat.
 *
 * VERIFY IF COMPILE FAILS -- IPayloadContext#player() as the way to get the
 * sending ServerPlayer from a server-bound packet is the standard NeoForge
 * pattern, not confirmed against source pasted in this conversation.
 */
public record SkillBookScreenStatePacket(boolean opened, String skillId, int handOrdinal)
        implements CustomPacketPayload {

    public static final Type<SkillBookScreenStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "skill_book_screen_state"));

    public static final StreamCodec<FriendlyByteBuf, SkillBookScreenStatePacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeBoolean(p.opened); buf.writeUtf(p.skillId); buf.writeVarInt(p.handOrdinal); },
            buf -> new SkillBookScreenStatePacket(buf.readBoolean(), buf.readUtf(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            InteractionHand hand = InteractionHand.values()[handOrdinal];
            if (opened) {
                SkillLearningHandler.onHeartbeat(player, hand, skillId);
            } else {
                SkillLearningHandler.onExplicitClose(player, hand, skillId);
            }
        });
    }
}
