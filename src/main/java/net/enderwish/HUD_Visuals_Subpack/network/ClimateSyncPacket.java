package net.enderwish.HUD_Visuals_Subpack.network;

import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
import net.enderwish.HUD_Visuals_Subpack.client.ClientClimateCache;
import net.enderwish.HUD_Visuals_Subpack.client.ClientColorHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Modern 1.21.1 Custom Packet for syncing Climate to the Client.
 * Ensures the Sports Watch updates while only refreshing world visuals on season changes.
 */
public record ClimateSyncPacket(ClimateData data) implements CustomPacketPayload {

    public static final Type<ClimateSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "climate_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ClimateSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ClimateData.STREAM_CODEC,
            ClimateSyncPacket::data,
            ClimateSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handles the climate data on the client side.
     */
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                // Get previous data from the cache before updating it
                ClimateData oldData = ClientClimateCache.getInstance();
                ClimateData newData = this.data;

                // 1. Always update the cache so the Sports Watch stays accurate
                ClientClimateCache.setInstance(newData);

                // 2. Only refresh chunks if the season has changed to prevent stuttering
                if (oldData == null || oldData.season() != newData.season()) {
                    ClientColorHandler.refreshVisuals();
                }
            }
        });
    }
}