package net.enderwish.HUD_Visuals_Subpack.network;

import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WristSyncPacket(int bpm, float energy, boolean hasWatch) implements CustomPacketPayload {

    public static final Type<WristSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "wrist_sync"));

    // Using a more robust composite codec for 1.21.1
    public static final StreamCodec<RegistryFriendlyByteBuf, WristSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WristSyncPacket::bpm,
            ByteBufCodecs.FLOAT, WristSyncPacket::energy,
            ByteBufCodecs.BOOL, WristSyncPacket::hasWatch,
            WristSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Changed to non-static 'handle' to fix the "Cannot resolve method" error in ModMessages
     */
    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                // Get the attachment data on the client side and update it with server data
                var cap = player.getData(ModAttachments.PLAYER_CAP);
                if (cap != null) {
                    cap.setBPM(this.bpm);
                    cap.setEnergy(this.energy);
                    cap.setWatchEquipped(this.hasWatch);
                }
            }
        });
    }
}