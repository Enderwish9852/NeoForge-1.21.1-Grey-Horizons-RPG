package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * KitSelectNotifyPacket (client -> server, empty payload)
 *
 * Sent when the local player selects the first aid kit in their hotbar.
 * The server can't observe hotbar selection itself, so the client notifies;
 * the server then broadcasts the kit_inspect animation to everyone
 * (including back to the selector) via MedicalAnimPacket.
 */
public record KitSelectNotifyPacket() implements CustomPacketPayload {

    public static final KitSelectNotifyPacket INSTANCE = new KitSelectNotifyPacket();

    public static final Type<KitSelectNotifyPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "kit_select_notify"));

    public static final StreamCodec<FriendlyByteBuf, KitSelectNotifyPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            PacketDistributor.sendToAllPlayers(new MedicalAnimPacket(
                    player.getUUID(), "kit_inspect"));
        });
    }
}
