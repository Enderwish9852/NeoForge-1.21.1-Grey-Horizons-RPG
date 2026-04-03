package net.enderwish.HUD_Visuals_Subpack.network;

import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.core.PlayerCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Full Sync Packet for the HUD.
 * Updated: 17 Arguments (Now includes Core Temp and Wetness).
 */
public record LimbSyncPacket(
        int bpm, float energy, float thirst, float hunger, boolean watchEquipped,
        float head, float torso, float lArm, float rArm,
        float lLeg, float rLeg, float lFoot, float rFoot,
        int starvationTimer, int pollenExposure,
        float coreTemp, float wetness
) implements CustomPacketPayload {

    public static final Type<LimbSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "limb_sync"));

    /**
     * STREAM_CODEC handles writing/reading from the network.
     * Order MUST be identical to the record above.
     */
    public static final StreamCodec<FriendlyByteBuf, LimbSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeInt(packet.bpm);
                buffer.writeFloat(packet.energy);
                buffer.writeFloat(packet.thirst);
                buffer.writeFloat(packet.hunger);
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
                buffer.writeInt(packet.pollenExposure);
                buffer.writeFloat(packet.coreTemp); // New
                buffer.writeFloat(packet.wetness);  // New
            },
            (buffer) -> new LimbSyncPacket(
                    buffer.readInt(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readBoolean(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readFloat(), // New
                    buffer.readFloat()  // New
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                // IMPORTANT: Ensure this matches your ModAttachments field name (PLAYER_CAP or WRIST_CAP)
                PlayerCapability cap = player.getData(ModAttachments.PLAYER_CAP);
                if (cap != null) {
                    // Create serverData using all 17 arguments
                    PlayerCapability serverData = new PlayerCapability(
                            this.bpm, this.energy, this.thirst, this.hunger, this.watchEquipped,
                            this.head, this.torso, this.lArm, this.rArm,
                            this.lLeg, this.rLeg, this.lFoot, this.rFoot,
                            this.starvationTimer, this.pollenExposure,
                            this.coreTemp, this.wetness
                    );

                    // Sync the client-side capability
                    cap.copyFrom(serverData);
                }
            }
        });
    }
}