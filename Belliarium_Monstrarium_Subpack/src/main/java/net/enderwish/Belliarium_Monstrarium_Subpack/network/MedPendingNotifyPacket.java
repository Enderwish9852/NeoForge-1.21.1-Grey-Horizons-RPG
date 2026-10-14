package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.BandagePending;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * MedPendingNotifyPacket (S2C)
 *
 * Tells the LOCAL client "your body-part choice was accepted — the next
 * right-click starts the hold, not the picker screen". The authoritative
 * pending state lives server-side (BandagePending.PENDING); this packet
 * mirrors it into BandagePending.CLIENT_PENDING so BandageItem.use()
 * behaves the same on both sides.
 */
public record MedPendingNotifyPacket(int handOrdinal, int kitSlotIndex, String bodyPartName)
        implements CustomPacketPayload {

    public static final Type<MedPendingNotifyPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "med_pending_notify"));

    public static final StreamCodec<FriendlyByteBuf, MedPendingNotifyPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.handOrdinal); buf.writeVarInt(p.kitSlotIndex); buf.writeUtf(p.bodyPartName); },
            buf -> new MedPendingNotifyPacket(buf.readVarInt(), buf.readVarInt(), buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        // S2C: safe to touch client classes here (same pattern as
        // OpenMedTargetFromKitPacket)
        context.enqueueWork(() -> BandagePending.setClient(new BandagePending.Pending(
                bodyPartName, handOrdinal, kitSlotIndex)));
    }
}
