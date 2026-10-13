package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.DejaVuClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * DejaVuClearPacket
 *
 * Server -> client: the player just closed the remembered container's GUI
 * (server saw PlayerContainerEvent.Close and is about to delete the block).
 * Tells the client to drop its ghost-slot render data so it doesn't bleed
 * into the next, unrelated container screen the player opens.
 */
public record DejaVuClearPacket() implements CustomPacketPayload {

    public static final Type<DejaVuClearPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "deja_vu_clear"));

    public static final StreamCodec<FriendlyByteBuf, DejaVuClearPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {},
            buf -> new DejaVuClearPacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            DejaVuClientState.clearGhostSlots();
        });
    }
}
