package net.enderwish.HUD_Visuals_Subpack.network;

import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.core.WristCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Full Sync Packet for the HUD.
 * Updated: 14 Arguments (Includes Hunger/Fuel, Thirst, and Starvation Timer).
 */
public record LimbSyncPacket(
        int bpm, float energy, float thirst, float hunger, boolean watchEquipped,
        float head, float torso, float lArm, float rArm,
        float lLeg, float rLeg, float lFoot, float rFoot,
        int starvationTimer
) implements CustomPacketPayload {

    public static final Type<LimbSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "limb_sync"));

    /**
     * The StreamCodec handles writing data to the network buffer and reading it back.
     * MUST be in the exact same order as the record parameters.
     */
    public static final StreamCodec<FriendlyByteBuf, LimbSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeInt(packet.bpm);
                buffer.writeFloat(packet.energy);
                buffer.writeFloat(packet.thirst);
                buffer.writeFloat(packet.hunger); // Added Hunger
                buffer.writeBoolean(packet.watchEquipped);
                buffer.writeFloat(packet.head);
                buffer.writeFloat(packet.torso);
                buffer.writeFloat(packet.lArm);
                buffer.writeFloat(packet.rArm);
                buffer.writeFloat(packet.lLeg);
                buffer.writeFloat(packet.rLeg);
                buffer.writeFloat(packet.lFoot);
                buffer.writeFloat(packet.rFoot);
                buffer.writeInt(packet.starvationTimer);
            },
            (buffer) -> new LimbSyncPacket(
                    buffer.readInt(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(), // Read Hunger
                    buffer.readBoolean(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /**
     * Handles the packet on the client side to update the local player's HUD data.
     */
    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                WristCapability cap = player.getData(ModAttachments.WRIST_CAP);
                if (cap != null) {
                    // Create serverData using the 14-argument constructor from WristCapability
                    WristCapability serverData = new WristCapability(
                            this.bpm, this.energy, this.thirst, this.hunger, this.watchEquipped,
                            this.head, this.torso, this.lArm, this.rArm,
                            this.lLeg, this.rLeg, this.lFoot, this.rFoot,
                            this.starvationTimer
                    );

                    // Sync the client-side capability with the new server data
                    cap.copyFrom(serverData);
                }
            }
        });
    }
}