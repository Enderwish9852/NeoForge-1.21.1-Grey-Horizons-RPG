package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.DejaVuClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * DejaVuFadePacket
 *
 * Server -> client: a remembered container was just opened for real (the
 * vanilla GUI is up). Carries each item's original slot index plus what it
 * was (read off the real block entity right before it's cleared for real),
 * so the client can draw a ghost icon directly over that slot inside the
 * now-open container screen. The player never actually receives these items.
 */
public record DejaVuFadePacket(List<Integer> slotIndices, List<String> itemIds, List<Integer> counts) implements CustomPacketPayload {

    public static final Type<DejaVuFadePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "deja_vu_fade"));

    public static final StreamCodec<FriendlyByteBuf, DejaVuFadePacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.slotIndices.size());
                for (int i = 0; i < p.slotIndices.size(); i++) {
                    buf.writeVarInt(p.slotIndices.get(i));
                    buf.writeUtf(p.itemIds.get(i));
                    buf.writeVarInt(p.counts.get(i));
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<Integer> slots = new ArrayList<>(size);
                List<String> ids = new ArrayList<>(size);
                List<Integer> counts = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    slots.add(buf.readVarInt());
                    ids.add(buf.readUtf());
                    counts.add(buf.readVarInt());
                }
                return new DejaVuFadePacket(slots, ids, counts);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            DejaVuClientState.triggerGhostSlots(slotIndices, itemIds, counts);
        });
    }
}
