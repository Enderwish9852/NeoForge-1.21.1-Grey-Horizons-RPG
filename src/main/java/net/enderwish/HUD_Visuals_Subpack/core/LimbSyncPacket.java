package net.enderwish.HUD_Visuals_Subpack.core;

import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from Server to Client to sync WristCapability data.
 */
public record LimbSyncPacket(
        int bpm,
        float energy,
        boolean watchEquipped,
        float head,
        float torso,
        float lArm,
        float rArm,
        float lLeg,
        float rLeg
) implements CustomPacketPayload {

    public static final Type<LimbSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "limb_sync"));

    /**
     * MANUAL STREAM CODEC
     */
    public static final StreamCodec<FriendlyByteBuf, LimbSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                // Writing to the buffer (Server -> Client)
                buffer.writeInt(packet.bpm);
                buffer.writeFloat(packet.energy);
                buffer.writeBoolean(packet.watchEquipped);
                buffer.writeFloat(packet.head);
                buffer.writeFloat(packet.torso);
                buffer.writeFloat(packet.lArm);
                buffer.writeFloat(packet.rArm);
                buffer.writeFloat(packet.lLeg);
                buffer.writeFloat(packet.rLeg);
            },
            (buffer) -> {
                // Reading from the buffer (Client receives)
                // MUST be in the exact same order as writing
                return new LimbSyncPacket(
                        buffer.readInt(),
                        buffer.readFloat(),
                        buffer.readBoolean(),
                        buffer.readFloat(),
                        buffer.readFloat(),
                        buffer.readFloat(),
                        buffer.readFloat(),
                        buffer.readFloat(),
                        buffer.readFloat()
                );
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * This runs on the CLIENT when the packet is received.
     */
    public static void handle(final LimbSyncPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
        });
    }
}