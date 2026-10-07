package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.FirstAidTriage;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.medical.FirstAidKitItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Checks main hand first, then off hand, for a First Aid Kit -- same pattern every other hand-sensitive packet here uses. */
public record FastUseFirstAidPacket() implements CustomPacketPayload {

    public static final Type<FastUseFirstAidPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "fast_use_first_aid"));

    public static final StreamCodec<FriendlyByteBuf, FastUseFirstAidPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {},
            buf -> new FastUseFirstAidPacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            InteractionHand hand = null;
            if (player.getMainHandItem().getItem() instanceof FirstAidKitItem) hand = InteractionHand.MAIN_HAND;
            else if (player.getOffhandItem().getItem() instanceof FirstAidKitItem) hand = InteractionHand.OFF_HAND;

            if (hand == null) {
                player.displayClientMessage(Component.literal("You need a First Aid Kit in hand."), true);
                return;
            }

            FirstAidTriage.performFastUse(player, hand);
        });
    }
}
